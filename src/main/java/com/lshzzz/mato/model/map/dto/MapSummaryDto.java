package com.lshzzz.mato.model.map.dto;

import com.lshzzz.mato.model.map.Map;

import java.time.LocalDateTime;

/**
 * 방 목록 표시에 사용되는 맵 요약 정보 DTO
 */
public record MapSummaryDto(
    Long id,
    String name,
    String description,
    boolean isPublic,
    int songCount,
    String userId,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    /**
     * Map 엔티티에서 MapSummaryDto 생성
     */
    public static MapSummaryDto fromEntity(Map map) {
        return new MapSummaryDto(
            map.getId(),
            map.getName(),
            map.getDescription(),
            map.getIsPublic(),
            map.getMapSongs().size(),
            map.getUserId(),
            map.getCreatedAt(),
            map.getUpdatedAt()
        );
    }
    
    /**
     * MapResponseDto에서 MapSummaryDto 생성
     */
    public static MapSummaryDto fromMapResponseDto(MapResponseDto responseDto) {
        return new MapSummaryDto(
            responseDto.id(),
            responseDto.name(),
            responseDto.description(),
            responseDto.isPublic(),
            responseDto.songs() != null ? responseDto.songs().size() : 0,
            responseDto.userId(),
            responseDto.CreatedAt(),
            responseDto.UpdatedAt()
        );
    }
} 