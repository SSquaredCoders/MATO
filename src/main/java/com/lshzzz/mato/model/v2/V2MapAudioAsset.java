package com.lshzzz.mato.model.v2;

public record V2MapAudioAsset(
    String assetId,
    String originalFileName,
    String assetUrl,
    String contentType,
    long size
) {
}
