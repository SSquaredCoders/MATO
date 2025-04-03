package com.lshzzz.mato.model.mapsongs.dto;

import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.dto.SongSummaryDto;

/**
 * 맵에 포함된 곡의 요약 정보 DTO
 */
public record MapSongSummaryDto(
    Long id,
    Integer startTime,
    Integer endTime,
    Integer repeatCount,
    SongSummaryDto song,
    int answerCount,
    int hintCount
) {
    /**
     * MapSong 엔티티에서 MapSongSummaryDto 생성
     */
    public static MapSongSummaryDto fromEntity(MapSong mapSong) {
        return new MapSongSummaryDto(
            mapSong.getId(),
            mapSong.getStartTime(),
            mapSong.getEndTime(),
            mapSong.getRepeatCount(),
            SongSummaryDto.fromEntity(mapSong.getSong()),
            mapSong.getAnswers().size(),
            mapSong.getHints().size()
        );
    }
} 