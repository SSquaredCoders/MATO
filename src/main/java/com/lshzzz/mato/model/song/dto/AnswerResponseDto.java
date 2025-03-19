package com.lshzzz.mato.model.song.dto;

public record AnswerResponseDto(
	Long id,
	Long songId,
	String answerText
) {
}
