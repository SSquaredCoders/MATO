package com.lshzzz.mato.model.map.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.lshzzz.mato.model.map.Map;
import com.lshzzz.mato.model.mapsongs.dto.MapSongResponseDto;

public record MapResponseDto(
	Long id,
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
			map.getName(),
			map.getDescription(),
			map.getIsPublic(),
			songs,
			map.getCreatedAt(),
			map.getUpdatedAt()
		);
	}
}