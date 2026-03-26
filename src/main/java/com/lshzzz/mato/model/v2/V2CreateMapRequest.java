package com.lshzzz.mato.model.v2;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record V2CreateMapRequest(
    @NotBlank String name,
    String description,
    @NotBlank String createdBy,
    @NotBlank String difficulty,
    @NotBlank String visibility,
    @NotNull Integer roundTimeLimitSeconds,
    @NotNull Integer hintRevealDelaySeconds,
    @Valid @NotEmpty List<V2MapSongDefinition> songs
) {
}
