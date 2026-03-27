package com.lshzzz.mato.service.v2;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2MapDetail;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import com.lshzzz.mato.model.v2.V2MapSummary;
import com.lshzzz.mato.model.v2.persistence.V2MapEntity;
import com.lshzzz.mato.model.v2.persistence.V2MapSongEntity;
import com.lshzzz.mato.repository.V2MapEntityRepository;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class V2MapCatalogService {

    private final V2MapEntityRepository mapRepository;

    public List<V2MapSummary> getMaps(String viewer) {
        String normalizedViewer = normalizeViewer(viewer);
        if (normalizedViewer == null) {
            return List.of();
        }

        return mapRepository.findByCreatedByOrderByIdAsc(normalizedViewer).stream()
            .map(this::toSummary)
            .toList();
    }

    public V2MapDetail getMap(long mapId, String viewer) {
        return toDetail(getRequiredMap(mapId, viewer));
    }

    @Transactional
    public V2MapDetail createMap(V2CreateMapRequest request) {
        V2MapEntity map = V2MapEntity.create(
            sanitize(request.name()),
            Objects.requireNonNullElse(request.description(), "").trim(),
            sanitize(request.createdBy()),
            normalizeDifficulty(request.difficulty()),
            normalizeVisibility(request.visibility()),
            sanitizeNonNegative(request.roundTimeLimitSeconds(), "round time limit"),
            sanitizeNonNegative(request.hintRevealDelaySeconds(), "hint reveal delay"),
            sanitizeSongs(request.songs())
        );
        return toDetail(mapRepository.save(map));
    }

    public V2MapSummary getSummary(long mapId, String viewer) {
        return toSummary(getRequiredMap(mapId, viewer));
    }

    @Transactional
    void resetForTests() {
        mapRepository.deleteAll();
    }

    private V2MapEntity getRequiredMap(long mapId, String viewer) {
        String normalizedViewer = normalizeViewer(viewer);
        if (normalizedViewer == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Map not found.");
        }

        return mapRepository.findByIdAndCreatedBy(mapId, normalizedViewer)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Map not found."));
    }

    private V2MapSummary toSummary(V2MapEntity map) {
        return new V2MapSummary(
            map.getId(),
            map.getName(),
            map.getSongs().size(),
            map.getDifficulty(),
            map.getVisibility()
        );
    }

    private V2MapDetail toDetail(V2MapEntity map) {
        return new V2MapDetail(
            map.getId(),
            map.getName(),
            map.getDescription(),
            map.getCreatedBy(),
            map.getDifficulty(),
            map.getVisibility(),
            map.getRoundTimeLimitSeconds(),
            map.getHintRevealDelaySeconds(),
            map.getSongs().stream()
                .map(this::toSongDefinition)
                .toList()
        );
    }

    private V2MapSongDefinition toSongDefinition(V2MapSongEntity song) {
        return new V2MapSongDefinition(
            song.getClue(),
            song.getTitle(),
            song.getArtist(),
            List.copyOf(song.getAnswers()),
            song.getAudioSourceType(),
            song.getAudioSourceValue(),
            song.getAudioSourceLabel()
        );
    }

    private String sanitize(String value) {
        String candidate = Objects.requireNonNullElse(value, "").trim();
        if (candidate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Blank values are not allowed.");
        }
        return candidate;
    }

    private String normalizeDifficulty(String value) {
        String candidate = sanitize(value).toLowerCase();
        return switch (candidate) {
            case "easy", "normal", "hard" -> candidate;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported difficulty.");
        };
    }

    private String normalizeVisibility(String value) {
        String candidate = sanitize(value).toLowerCase();
        return switch (candidate) {
            case "public", "private" -> candidate;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported visibility.");
        };
    }

    private String normalizeViewer(String viewer) {
        String normalizedViewer = Objects.requireNonNullElse(viewer, "").trim();
        if (normalizedViewer.isBlank()) {
            return null;
        }
        return normalizedViewer;
    }

    private int sanitizeNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + " must be zero or greater."
            );
        }
        return value;
    }

    private List<V2MapSongDefinition> sanitizeSongs(List<V2MapSongDefinition> songs) {
        List<V2MapSongDefinition> source = songs == null ? List.of() : songs;
        List<V2MapSongDefinition> sanitized = source.stream()
            .map(song -> new V2MapSongDefinition(
                sanitize(song.clue()),
                sanitize(song.title()),
                sanitize(song.artist()),
                (song.answers() == null ? List.<String>of() : song.answers()).stream()
                    .map(this::sanitize)
                    .toList(),
                normalizeAudioSourceType(song.audioSourceType()),
                sanitizeOptional(song.audioSourceValue()),
                sanitizeOptional(song.audioSourceLabel())
            ))
            .toList();

        if (sanitized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one song is required.");
        }

        return sanitized;
    }

    private String normalizeAudioSourceType(String value) {
        String candidate = sanitizeOptional(value);
        if (candidate == null) {
            return null;
        }
        String normalized = candidate.toLowerCase();
        return switch (normalized) {
            case "youtube", "file" -> normalized;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported audio source type.");
        };
    }

    private String sanitizeOptional(String value) {
        String candidate = Objects.requireNonNullElse(value, "").trim();
        return candidate.isBlank() ? null : candidate;
    }
}
