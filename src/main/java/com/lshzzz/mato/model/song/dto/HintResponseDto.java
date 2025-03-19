package com.lshzzz.mato.model.song.dto;

import java.util.List;

public record HintResponseDto(
	Long id,
	Long songId,
	String hintText,
	int revealTime
) {
}
