package com.lshzzz.mato.service.v2;

import com.lshzzz.mato.model.v2.V2CreateMapRequest;
import com.lshzzz.mato.model.v2.V2MapDetail;
import com.lshzzz.mato.model.v2.V2MapSongDefinition;
import com.lshzzz.mato.model.v2.V2MapSummary;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class V2MapCatalogService {

    private static final int DEFAULT_HINT_REVEAL_DELAY_SECONDS = 8;

    private final AtomicLong sequence = new AtomicLong(100L);
    private final Map<Long, V2MapDetail> maps = new ConcurrentHashMap<>();

    public V2MapCatalogService() {
        seed(
            "Anime Rush",
            "애니 오프닝 위주로 빠르게 한 판 돌리는 기본 맵입니다.",
            "system",
            "normal",
            "public",
            30,
            DEFAULT_HINT_REVEAL_DELAY_SECONDS,
            List.of(
                new V2MapSongDefinition(
                    "문제: 일본 애니메이션 에반게리온 오프닝입니다. 곡 제목을 입력하세요.",
                    "A Cruel Angel's Thesis",
                    "Yoko Takahashi",
                    List.of("a cruel angel's thesis", "zankoku na tenshi no thesis")
                ),
                new V2MapSongDefinition(
                    "문제: 귀멸의 칼날 1기 오프닝입니다.",
                    "Gurenge",
                    "LiSA",
                    List.of("gurenge")
                ),
                new V2MapSongDefinition(
                    "문제: 강철의 연금술사 브라더후드 1기 오프닝입니다.",
                    "Again",
                    "YUI",
                    List.of("again")
                ),
                new V2MapSongDefinition(
                    "문제: 나루토 질풍전 16기 오프닝입니다.",
                    "Silhouette",
                    "KANA-BOON",
                    List.of("silhouette")
                )
            )
        );

        seed(
            "Boss Battle",
            "조금 더 빡센 난이도의 보컬 곡 위주 테스트 맵입니다.",
            "system",
            "hard",
            "public",
            25,
            DEFAULT_HINT_REVEAL_DELAY_SECONDS,
            List.of(
                new V2MapSongDefinition(
                    "문제: 코드 기어스 1기 오프닝입니다.",
                    "COLORS",
                    "FLOW",
                    List.of("colors")
                ),
                new V2MapSongDefinition(
                    "문제: 나루토 2기 오프닝입니다.",
                    "Haruka Kanata",
                    "ASIAN KUNG-FU GENERATION",
                    List.of("haruka kanata")
                ),
                new V2MapSongDefinition(
                    "문제: 소드 아트 온라인 1기 오프닝입니다.",
                    "crossing field",
                    "LiSA",
                    List.of("crossing field")
                )
            )
        );
    }

    public List<V2MapSummary> getMaps() {
        return maps.values().stream()
            .sorted(Comparator.comparing(V2MapDetail::id))
            .map(this::toSummary)
            .toList();
    }

    public V2MapDetail getMap(long mapId) {
        V2MapDetail map = maps.get(mapId);
        if (map == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "맵을 찾을 수 없습니다.");
        }
        return map;
    }

    public V2MapDetail createMap(V2CreateMapRequest request) {
        long mapId = sequence.incrementAndGet();
        V2MapDetail map = new V2MapDetail(
            mapId,
            sanitize(request.name()),
            Objects.requireNonNullElse(request.description(), "").trim(),
            sanitize(request.createdBy()),
            normalizeDifficulty(request.difficulty()),
            normalizeVisibility(request.visibility()),
            sanitizeNonNegative(request.roundTimeLimitSeconds(), "라운드 제한시간"),
            sanitizeNonNegative(request.hintRevealDelaySeconds(), "힌트 공개 지연시간"),
            sanitizeSongs(request.songs())
        );
        maps.put(map.id(), map);
        return map;
    }

    public V2MapSummary getSummary(long mapId) {
        return toSummary(getMap(mapId));
    }

    private void seed(
        String name,
        String description,
        String createdBy,
        String difficulty,
        String visibility,
        int roundTimeLimitSeconds,
        int hintRevealDelaySeconds,
        List<V2MapSongDefinition> songs
    ) {
        long mapId = sequence.getAndIncrement();
        maps.put(
            mapId,
            new V2MapDetail(
                mapId,
                name,
                description,
                createdBy,
                difficulty,
                visibility,
                roundTimeLimitSeconds,
                hintRevealDelaySeconds,
                songs
            )
        );
    }

    private V2MapSummary toSummary(V2MapDetail map) {
        return new V2MapSummary(
            map.id(),
            map.name(),
            map.songs().size(),
            map.difficulty(),
            map.visibility()
        );
    }

    private String sanitize(String value) {
        String candidate = Objects.requireNonNullElse(value, "").trim();
        if (candidate.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비어 있는 값은 허용되지 않습니다.");
        }
        return candidate;
    }

    private String normalizeDifficulty(String value) {
        String candidate = sanitize(value).toLowerCase();
        return switch (candidate) {
            case "easy", "normal", "hard" -> candidate;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "난이도 값이 올바르지 않습니다.");
        };
    }

    private String normalizeVisibility(String value) {
        String candidate = sanitize(value).toLowerCase();
        return switch (candidate) {
            case "public", "private" -> candidate;
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "공개 범위 값이 올바르지 않습니다.");
        };
    }

    private int sanitizeNonNegative(Integer value, String fieldName) {
        if (value == null || value < 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                fieldName + "은(는) 0 이상이어야 합니다."
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
                song.answers().stream()
                    .map(this::sanitize)
                    .toList()
            ))
            .toList();

        if (sanitized.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "맵에는 최소 한 곡 이상이 필요합니다.");
        }

        return sanitized;
    }
}
