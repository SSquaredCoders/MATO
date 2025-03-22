package com.lshzzz.mato.model.song.dto;

import com.lshzzz.mato.model.song.Answer;

public record AnswerResponseDto(
	Long id,
	Long mapSongId,
	String answerText
) {
	public AnswerResponseDto(Answer answer) {
		this(answer.getId(), answer.getMapSong().getId(), answer.getAnswerText());
	}
}
