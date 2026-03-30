package com.lshzzz.mato.model.v2;

import java.util.List;

public record V2MapSongPage(
    int page,
    int size,
    long totalElements,
    int totalPages,
    String query,
    List<V2MapSongSummary> items
) {
}
