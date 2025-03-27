package com.lshzzz.mato.model.map.dto;

import java.util.List;

import com.lshzzz.mato.model.mapsongs.dto.MapSongRequestDto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record MapRequestDto(
	@NotNull String userId,
	@NotNull String name,
	String description,
	@NotNull Boolean isPublic,
	@Valid List<MapSongRequestDto> songs  // List of songs to add to the map (can be existing or new)
) {}