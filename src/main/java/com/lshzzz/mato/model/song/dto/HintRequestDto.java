package com.lshzzz.mato.model.song.dto;

public record HintRequestDto(
	Long id,
	Long songId,
	String hintText,
	int hintTime
) {
}
