package com.lshzzz.mato.model.map.dto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;

public record MapResponseDto(
	Long id,
	String userId,
	String name,
	String description,
	Boolean isPublic,
	List<MapSongResponseDto> songs,
	LocalDateTime CreatedAt,
	LocalDateTime UpdatedAt
) {
	public MapResponseDto(Map map, List<MapSongResponseDto> songs) {
		this(
			map.getId(),
			map.getUserId(),
			map.getName(),
			map.getDescription(),
			map.getIsPublic(),
			songs,
			map.getCreatedAt(),
			map.getUpdatedAt()
		);
	}
	
	/**
	 * MapSummaryDto에서 MapResponseDto 생성
	 */
	public static MapResponseDto fromSummaryDto(MapSummaryDto summaryDto) {
	    return new MapResponseDto(
	        summaryDto.id(),
	        summaryDto.userId(),
	        summaryDto.name(),
	        summaryDto.description(),
	        summaryDto.isPublic(),
	        Collections.emptyList(), // 요약 정보에서는 곡 목록 생략
	        summaryDto.createdAt(),
	        summaryDto.updatedAt()
	    );
	}
}