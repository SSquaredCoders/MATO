package com.lshzzz.mato.service;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.mapsongs.dto.MapSongRequestDto;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;
import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;
import com.lshzzz.mato.repository.MapRepository;
import com.lshzzz.mato.repository.MapSongRepository;
import com.lshzzz.mato.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
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
		try {
			MapSong mapSong = mapSongRepository.findById(mapSongId)
				.orElseThrow(() -> new IllegalArgumentException("MapSong을 찾을 수 없습니다. ID=" + mapSongId));

			Song song = mapSong.getSong();

			if (requestDto.songId() != null) {
				song = songRepository.findById(requestDto.songId())
					.orElseThrow(() -> new IllegalArgumentException("해당 Song ID 없음: " + requestDto.songId()));
			} else if (requestDto.newSong() != null) {
				String url = Optional.ofNullable(requestDto.newSong().youtubeUrl())
					.orElseThrow(() -> new IllegalArgumentException("youtubeUrl은 null일 수 없습니다."));
				String title = Optional.ofNullable(requestDto.newSong().title()).orElse("제목 없음");

				song = Song.builder()
					.youtubeUrl(url)
					.title(title)
					.build();
				songRepository.save(song);
			}

			mapSong.updateSong(song);
			mapSong.updateTiming(requestDto.startTime(), requestDto.endTime(), requestDto.repeatCount());

			return convertToDto(mapSong);
		} catch (Exception e) {
			log.error("🔴 MapSong 수정 중 예외 발생: {}", e.getMessage(), e);
			throw new RuntimeException("MapSong 수정 중 오류 발생", e);  // 예외의 원인 포함
		}
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
	public MapSongResponseDto convertToDto(MapSong mapSong) {
		Song song = mapSong.getSong();

		List<AnswerDto> answers = mapSong.getAnswers().stream()
			.filter(a -> a != null && a.getAnswerText() != null)
			.map(a -> new AnswerDto(a.getId(), a.getId(), a.getAnswerText()))
			.toList();


		List<HintDto> hints = mapSong.getHints().stream()
			.filter(h -> h != null && h.getHintText() != null)
			.map(h -> new HintDto(h.getId(), h.getId(), h.getHintText(), h.getRevealTime()))
			.toList();


		SongResponseDto songDto = new SongResponseDto(song); // ✅ 곡 정보 포함

		return new MapSongResponseDto(
			mapSong.getId(),
			mapSong.getMap().getId(),
			song.getId(),
			mapSong.getStartTime(),
			mapSong.getEndTime(),
			mapSong.getRepeatCount(),
			songDto,
			answers,
			hints
		);
	}
}
