package com.lshzzz.mato.model.song.dto;

import java.util.List;

public record HintRequestDto(
	List<HintData> hints
) {
	public record HintData(String hintText, int revealTime) {}
}
