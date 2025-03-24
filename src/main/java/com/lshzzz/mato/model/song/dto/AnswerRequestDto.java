package com.lshzzz.mato.model.song.dto;

import java.util.List;

public record AnswerRequestDto(
	Long mapSongId,
	List<String> answerTexts
) {
}
