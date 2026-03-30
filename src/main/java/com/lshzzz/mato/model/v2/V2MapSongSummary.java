package com.lshzzz.mato.model.v2;

public record V2MapSongSummary(
    long id,
    int songOrder,
    String clue,
    String title,
    String artist,
    String audioSourceType,
    String audioSourceLabel,
    int clipStartSeconds,
    Integer clipEndSeconds,
    int answerCount
) {
}
