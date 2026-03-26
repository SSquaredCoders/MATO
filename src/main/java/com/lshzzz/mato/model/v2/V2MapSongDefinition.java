package com.lshzzz.mato.model.v2;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record V2MapSongDefinition(
    @NotBlank String clue,
    @NotBlank String title,
    @NotBlank String artist,
    @NotEmpty List<@NotBlank String> answers,
    String audioSourceType,
    String audioSourceValue,
    String audioSourceLabel
) {

    public V2MapSongDefinition(
        String clue,
        String title,
        String artist,
        List<String> answers
    ) {
        this(clue, title, artist, answers, null, null, null);
    }
}
