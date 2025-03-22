package com.lshzzz.mato.model.song.dto;

import com.lshzzz.mato.model.song.Hint;

public record HintResponseDto(
	Long id,
	Long mapSongId,
	String hintText,
	int revealTime
) {
	public HintResponseDto(Hint hint) {
		this(hint.getId(), hint.getMapSong().getId(), hint.getHintText(), hint.getRevealTime());
	}
}
