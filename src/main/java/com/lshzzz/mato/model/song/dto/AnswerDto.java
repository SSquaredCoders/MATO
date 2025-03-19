package com.lshzzz.mato.model.song.dto;

public record AnswerDto(
	Long id,
	Long songId,
	String answerText
) {
}
