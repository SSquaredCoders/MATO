package com.lshzzz.mato.service;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.map.dto.MapRequestDto;
import com.lshzzz.mato.model.map.dto.MapResponseDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongRequestDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;
import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.SongRequestDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;
import com.lshzzz.mato.repository.MapRepository;
import com.lshzzz.mato.repository.MapSongRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MapService {
	private final MapRepository mapRepository;
	private final MapSongRepository mapSongRepository;
	private final SongService songService;
	private final MapSongService mapSongService;

	// 맵 생성
	@Transactional
	public MapResponseDto createMap(MapRequestDto requestDto) {

		Map map = mapRepository.save(
			Map.builder()
				.userId(requestDto.userId())
				.name(requestDto.name())
				.description(requestDto.description())
				.isPublic(requestDto.isPublic())
				.build()
		);

		if (requestDto.songs() != null) {
			for (MapSongRequestDto songReq : requestDto.songs()) {
				if (songReq.songId() != null) {
					mapSongService.addSongToMap(map.getId(), songReq);
				} else if (songReq.newSong() != null) {
					SongResponseDto createdSong = songService.createSong(songReq.newSong());
					MapSongRequestDto linkDto = new MapSongRequestDto(
						createdSong.id(),
						null,
						songReq.startTime(),
						songReq.endTime(),
						songReq.repeatCount()
					);
					mapSongService.addSongToMap(map.getId(), linkDto);
				}
			}
		}

		return buildMapResponseDto(map);
	}

	// 특정 맵 조회
	@Transactional(readOnly = true)
	public MapResponseDto getMapById(Long mapId) {
		Map map = mapRepository.findById(mapId)
			.orElseThrow(() -> new RuntimeException("맵이 존재하지 않습니다."));
		return buildMapResponseDto(map);
	}


	// 모든 공개된 맵 조회
	@Transactional(readOnly = true)
	public List<MapResponseDto> getPublicMaps() {
		return mapRepository.findByIsPublicTrue().stream()
			.map(this::buildMapResponseDto)
			.toList();
	}

	// 맵 수정
	@Transactional
	public MapResponseDto updateMap(Long id, MapRequestDto requestDto) {
		Map map = mapRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("해당 ID의 맵이 존재하지 않습니다."));

		if (!map.getUserId().equals(requestDto.userId())) {
			throw new IllegalArgumentException("맵 수정 권한이 없습니다.");
		}

		map.update(requestDto.name(), requestDto.description(), requestDto.isPublic());
		return buildMapResponseDto(map);
	}


	// 맵 삭제
	@Transactional
	public void deleteMap(Long id, Long userId) {
		Map map = mapRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("해당 ID의 맵이 존재하지 않습니다."));
		if (!map.getUserId().equals(userId)) {
			throw new IllegalArgumentException("맵 삭제 권한이 없습니다.");
		}
		mapRepository.delete(map);
	}

	// 맵 이름 중복 체크
	public boolean checkDuplicateMap(String name) {
		return mapRepository.existsByName(name);
	}

	// ✅ 중복 검사 시 예외 발생하도록 변경
	public void validateDuplicateMap(String name) {
		if (mapRepository.existsByName(name)) {
			throw new IllegalArgumentException("이미 존재하는 맵 이름입니다: " + name);
		}
	}

	// 🔁 Map → MapResponseDto 변환 함수
	private MapResponseDto buildMapResponseDto(Map map) {
		List<MapSongResponseDto> mapSongDtos = mapSongRepository.findByMap(map).stream()
			.map(mapSong -> {
				List<AnswerDto> answers = mapSong.getAnswers().stream()
					.map(a -> new AnswerDto(a.getId(), a.getMapSong().getId(), a.getAnswerText()))
					.toList();

				List<HintDto> hints = mapSong.getHints().stream()
					.map(h -> new HintDto(h.getId(), h.getMapSong().getId(),h.getHintText(), h.getRevealTime()))
					.toList();

				Song song = mapSong.getSong();

				return new MapSongResponseDto(
					mapSong.getId(),
					map.getId(),
					song.getId(),
					mapSong.getStartTime(),
					mapSong.getEndTime(),
					mapSong.getRepeatCount(),
					answers,
					hints
				);
			})
			.toList();

		// 🔥 여기서 깔끔하게 생성자 사용!
		return new MapResponseDto(map, mapSongDtos);
	}
}
