package com.lshzzz.mato.model.map.dto;

import com.lshzzz.mato.model.map.Map;
import org.hibernate.Hibernate;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 * 방 목록 표시에 사용되는 맵 요약 정보 DTO
 */
@Slf4j
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
        int songCount = 0;
        try {
            // 지연 로딩된 컬렉션에 안전하게 접근
            if (Hibernate.isInitialized(map.getMapSongs())) {
                songCount = map.getMapSongs().size();
            } else {
                log.debug("Map ID {}의 mapSongs 컬렉션이 초기화되지 않았습니다.", map.getId());
            }
        } catch (Exception e) {
            log.warn("Map ID {}의 곡 수 계산 중 오류 발생: {}", map.getId(), e.getMessage());
        }
        
        return new MapSummaryDto(
            map.getId(),
            map.getName(),
            map.getDescription(),
            map.getIsPublic(),
            songCount,
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