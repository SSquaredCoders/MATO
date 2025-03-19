package com.lshzzz.mato.model.song.dto;

public record HintDto(
	Long id,
	Long songId,
	String hintText,
	int hintTime
) {
}
