package com.lshzzz.mato.service;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.mapsongs.dto.MapSongRequestDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;
import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.repository.MapRepository;
import com.lshzzz.mato.repository.MapSongRepository;
import com.lshzzz.mato.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MapSongService {
	private final MapSongRepository mapSongRepository;
	private final MapRepository mapRepository;
	private final SongRepository songRepository;

	// 맵에 노래 추가
	public MapSongResponseDto addSongToMap(Long mapId, MapSongRequestDto requestDto) {
		Map map = mapRepository.findById(mapId)
			.orElseThrow(() -> new IllegalArgumentException("맵을 찾을 수 없습니다."));

		Song song = resolveSong(requestDto);
		System.out.println("✅ song resolved: " + song.getId());

		MapSong mapSong = MapSong.builder()
			.map(map)
			.song(song)
			.startTime(requestDto.startTime())
			.endTime(requestDto.endTime())
			.repeatCount(requestDto.repeatCount())
			.answers(new ArrayList<>())  // ✅ 기본값으로 비어있는 리스트
			.hints(new ArrayList<>())    // ✅ 마찬가지
			.build();

		MapSong savedMapSong = mapSongRepository.save(mapSong);

		System.out.println("✅ 저장된 mapSong ID: " + mapSong.getId());

		return convertToDto(savedMapSong);
	}

	// 맵에 연결된 노래 목록 조회
	@Transactional(readOnly = true)
	public List<MapSongResponseDto> getSongsByMapId(Long mapId) {
		return mapSongRepository.findByMapId(mapId).stream()
			.map(this::convertToDto)
			.toList();
	}

	// 노래 업데이트
	@Transactional
	public MapSongResponseDto updateMapSong(Long mapSongId, MapSongRequestDto requestDto) {
		MapSong mapSong = mapSongRepository.findById(mapSongId)
			.orElseThrow(() -> new IllegalArgumentException("MapSong을 찾을 수 없습니다."));

		// 🔁 노래 교체 or 새 노래 생성
		Song song;
		if (requestDto.songId() != null) {
			song = songRepository.findById(requestDto.songId())
				.orElseThrow(() -> new IllegalArgumentException("해당 Song ID 없음"));
		} else if (requestDto.newSong() != null) {
			song = Song.builder()
				.youtubeUrl(requestDto.newSong().youtubeUrl())
				// .title(requestDto.newSong().title()) 등 확장 가능
				.build();
			songRepository.save(song);
		} else {
			throw new IllegalArgumentException("수정할 Song 정보가 없습니다.");
		}

		// ✅ mapSong 정보 수정
		mapSong.updateSong(song);
		mapSong.updateTiming(requestDto.startTime(), requestDto.endTime(), requestDto.repeatCount());

		return convertToDto(mapSong);
	}


	// 노래 제거
	public void removeSongFromMap(Long mapSongId) {
		mapSongRepository.deleteById(mapSongId);
	}

	// 🔧 기존 노래 or 새 노래 생성 분기
	private Song resolveSong(MapSongRequestDto requestDto) {
		if (requestDto.songId() != null) {
			return songRepository.findById(requestDto.songId())
				.orElseThrow(() -> new IllegalArgumentException("노래를 찾을 수 없습니다."));
		} else if (requestDto.newSong() != null) {
			return songRepository.save(Song.builder()
				.youtubeUrl(requestDto.newSong().youtubeUrl())
				.build());
		}
		throw new IllegalArgumentException("노래 정보가 제공되지 않았습니다.");
	}

	// ✅ MapSong → MapSongResponseDto 변환 함수
	private MapSongResponseDto convertToDto(MapSong mapSong) {
		Song song = mapSong.getSong();

		List<AnswerDto> answers = mapSong.getAnswers().stream()
			.map(a -> new AnswerDto(a.getId(), a.getId(), a.getAnswerText()))
			.toList();

		List<HintDto> hints = mapSong.getHints().stream()
			.map(h -> new HintDto(h.getId(), h.getId(), h.getHintText(), h.getRevealTime()))
			.toList();

		return new MapSongResponseDto(
			mapSong.getId(),
			mapSong.getMap().getId(),
			song.getId(),
			mapSong.getStartTime(),
			mapSong.getEndTime(),
			mapSong.getRepeatCount(),
			answers,
			hints
		);
	}
}
