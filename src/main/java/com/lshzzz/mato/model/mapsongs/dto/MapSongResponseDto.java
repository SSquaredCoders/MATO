package com.lshzzz.mato.model.mapsongs.dto;

import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.dto.AnswerDto;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;

import java.util.List;

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
}
