package com.lshzzz.mato.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.map.dto.MapRequestDto;
import com.lshzzz.mato.model.map.dto.MapResponseDto;

import com.lshzzz.mato.model.mapsongs.dto.MapSongRequestDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;
import com.lshzzz.mato.repository.MapRepository;
import com.lshzzz.mato.repository.MapSongRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MapService {
	private final MapRepository mapRepository;
	private final MapSongRepository mapSongRepository;
	private final SongService songService;
	private final MapSongService mapSongService;

	@Transactional
	public MapResponseDto createMap(MapRequestDto requestDto, String authenticatedUserId) {
		if (!authenticatedUserId.equals(requestDto.userId())) {
			throw new IllegalArgumentException("맵 생성 권한이 없습니다.");
		}

		validateDuplicateMap(requestDto.name());

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
				handleSongRequest(map.getId(), songReq, authenticatedUserId);
			}
		}

		return buildMapResponseDto(map);
	}

	@Transactional(readOnly = true)
	public MapResponseDto getMapById(Long mapId) {
		Map map = mapRepository.findById(mapId)
			.orElseThrow(() -> new RuntimeException("맵이 존재하지 않습니다."));
		return buildMapResponseDto(map);
	}

	@Transactional(readOnly = true)
	public List<MapResponseDto> getPublicMaps() {
		return mapRepository.findByIsPublicTrue().stream()
			.map(this::buildMapResponseDto)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<MapResponseDto> getMapsByUserId(String userId) {
		return mapRepository.findByUserId(userId).stream()
			.map(this::buildMapResponseDto)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<MapResponseDto> getPublicMapsByUserId(String userId) {
		return mapRepository.findByUserIdAndIsPublicTrue(userId).stream()
			.map(this::buildMapResponseDto)
			.toList();
	}

	@Transactional
	public MapResponseDto updateMap(Long id, MapRequestDto requestDto, String authenticatedUserId) {
		Map map = mapRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("해당 ID의 맵이 존재하지 않습니다."));

		if (!authenticatedUserId.equals(map.getUserId())) {
			throw new IllegalArgumentException("맵 수정 권한이 없습니다.");
		}

		if (!map.getName().equals(requestDto.name())) {
			validateDuplicateMap(requestDto.name());
		}

		map.update(requestDto.name(), requestDto.description(), requestDto.isPublic());
		return buildMapResponseDto(map);
	}

	@Transactional
	public void deleteMap(Long id, String authenticatedUserId) {
		Map map = mapRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("해당 ID의 맵이 존재하지 않습니다."));

		if (!authenticatedUserId.equals(map.getUserId())) {
			throw new IllegalArgumentException("맵 삭제 권한이 없습니다.");
		}

		mapRepository.delete(map);
	}

	public boolean checkDuplicateMap(String name) {
		return mapRepository.existsByName(name);
	}

	public void validateDuplicateMap(String name) {
		if (mapRepository.existsByName(name)) {
			throw new IllegalArgumentException("이미 존재하는 맵 이름입니다: " + name);
		}
	}

	private MapResponseDto buildMapResponseDto(Map map) {
		List<MapSongResponseDto> mapSongDtos = mapSongRepository.findByMap(map).stream()
			.map(mapSongService::convertToDto)
			.toList();

		return new MapResponseDto(map, mapSongDtos);
	}

	private void handleSongRequest(Long mapId, MapSongRequestDto songReq, String authenticatedUserId) {
		if (songReq.songId() != null) {
			mapSongService.addSongToMap(mapId, songReq, authenticatedUserId);
		} else if (songReq.newSong() != null) {
			SongResponseDto createdSong = songService.createSong(songReq.newSong());
			MapSongRequestDto linkDto = new MapSongRequestDto(
				createdSong.id(),
				null,
				songReq.startTime(),
				songReq.endTime(),
				songReq.repeatCount()
			);
			mapSongService.addSongToMap(mapId, linkDto, authenticatedUserId);
		}
	}
}
