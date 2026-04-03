package com.lshzzz.mato.model.v2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record V2CreateMapRequest(
    @NotBlank String name,
    String description,
    String createdBy,
    @NotBlank String difficulty,
    @NotBlank String visibility,
    @NotNull Boolean showMediaControls,
    @NotBlank String songOrderMode,
    @NotBlank String answerMode,
    @NotBlank String roundFlowMode,
    @NotNull Integer roundTimeLimitSeconds,
    @NotNull Integer skipVotesRequired,
    @NotNull Integer hintRevealDelaySeconds,
    @Valid @NotEmpty List<V2MapSongDefinition> songs
) {
    public V2CreateMapRequest(
        String name,
        String description,
        String createdBy,
        String difficulty,
        String visibility,
        Boolean showMediaControls,
        String songOrderMode,
        String answerMode,
        String roundFlowMode,
        Integer roundTimeLimitSeconds,
        Integer hintRevealDelaySeconds,
        List<V2MapSongDefinition> songs
    ) {
        this(
            name,
            description,
            createdBy,
            difficulty,
            visibility,
            showMediaControls,
            songOrderMode,
            answerMode,
            roundFlowMode,
            roundTimeLimitSeconds,
            2,
            hintRevealDelaySeconds,
            songs
        );
    }
}
