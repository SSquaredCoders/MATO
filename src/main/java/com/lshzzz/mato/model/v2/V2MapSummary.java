package com.lshzzz.mato.model.v2;

public record V2MapSummary(
    long id,
    String name,
    int songCount,
    String difficulty,
    String visibility
) {
}
