package com.lshzzz.mato.model.mapsongs.dto;

import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;

import java.util.List;
import java.util.stream.Collectors;

public record MapSongResponseDto(
	Long id,
	Long mapId,
	Long songId,
	Integer startTime,
	Integer endTime,
	Integer repeatCount,
	SongResponseDto song,
	List<AnswerDto> answers,
	List<HintDto> hints
) {
	public MapSongResponseDto(MapSong mapSong, SongResponseDto songDto, List<AnswerDto> answers, List<HintDto> hints) {
		this(
			mapSong.getId(),
			mapSong.getMap().getId(),
			mapSong.getSong().getId(),
			mapSong.getStartTime(),
			mapSong.getEndTime(),
			mapSong.getRepeatCount(),
			songDto, // ✅
			answers,
			hints
		);
	}

	// 메서드 참조를 위해 MapSong 하나만 받는 간략 버전 추가
	public static MapSongResponseDto fromEntity(MapSong mapSong) {
		// 노래 정보 변환
		SongResponseDto songDto = new SongResponseDto(mapSong.getSong());
		
		// 답변 목록 변환 - MapSongService.convertToDto와 동일한 방식 사용
		List<AnswerDto> answers = mapSong.getAnswers().stream()
			.filter(a -> a != null && a.getAnswerText() != null)
			.map(answer -> new AnswerDto(
				answer.getId(), 
				mapSong.getId(), 
				answer.getAnswerText()
			))
			.collect(Collectors.toList());
			
		// 힌트 목록 변환 - MapSongService.convertToDto와 동일한 방식 사용
		List<HintDto> hints = mapSong.getHints().stream()
			.filter(h -> h != null && h.getHintText() != null)
			.map(hint -> new HintDto(
				hint.getId(),
				mapSong.getId(),
				hint.getHintText(),
				hint.getRevealTime()
			))
			.collect(Collectors.toList());
			
		return new MapSongResponseDto(mapSong, songDto, answers, hints);
	}

	public static MapSongResponseDto fromEntity(
		MapSong mapSong,
		SongResponseDto songDto,
		List<AnswerDto> answers,
		List<HintDto> hints	) {
		return new MapSongResponseDto(mapSong, songDto, answers, hints);
	}
}
