package com.lshzzz.mato.model.v2;

import java.util.List;

public record V2MapDetail(
    long id,
    String name,
    String description,
    String createdBy,
    String difficulty,
    String visibility,
    int roundTimeLimitSeconds,
    int hintRevealDelaySeconds,
    List<V2MapSongDefinition> songs
) {
}
