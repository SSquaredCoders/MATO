package com.lshzzz.mato.model.map.dto;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.dto.MapSongSummaryDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 방 상세 정보에 사용되는 맵 상세 정보 DTO
 */
public record MapDetailDto(
    Long id,
    String userId,
    String name,
    String description,
    boolean isPublic,
    List<MapSongSummaryDto> mapSongs,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Map 엔티티에서 MapDetailDto 생성
     */
    public static MapDetailDto fromEntity(Map map) {
        List<MapSongSummaryDto> songDtos = map.getMapSongs().stream()
            .map(MapSongSummaryDto::fromEntity)
            .collect(Collectors.toList());
            
        return new MapDetailDto(
            map.getId(),
            map.getUserId(),
            map.getName(),
            map.getDescription(),
            map.getIsPublic(),
            songDtos,
            map.getCreatedAt(),
            map.getUpdatedAt()
        );
    }
} 