package com.lshzzz.mato.model.song.dto;

import java.util.List;

public record HintRequestDto(
	Long mapSongId,
	List<HintData> hints
) {
	public record HintData(String hintText, int revealTime) {}
}
