package com.lshzzz.mato.model.song.dto;

import com.lshzzz.mato.model.song.Song;

/**
 * 곡 요약 정보 DTO
 */
public record SongSummaryDto(
    Long id,
    String title,
    String youtubeUrl
) {
    /**
     * Song 엔티티에서 SongSummaryDto 생성
     */
    public static SongSummaryDto fromEntity(Song song) {
        return new SongSummaryDto(
            song.getId(),
            song.getTitle(),
            song.getYoutubeUrl()
        );
    }
} 