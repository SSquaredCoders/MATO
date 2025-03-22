package com.lshzzz.mato.model.song.dto;

public record HintDto(
	Long id,
	Long mapSongId,
	String hintText,
	int hintTime
) {
}
