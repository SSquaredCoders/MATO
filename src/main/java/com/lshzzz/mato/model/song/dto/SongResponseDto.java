package com.lshzzz.mato.model.song.dto;

import java.time.LocalDateTime;

import com.lshzzz.mato.model.song.Song;

public record SongResponseDto(
	Long id,
	String youtubeUrl,
	String title,
	String artist,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public SongResponseDto(Song song) {
		this(song.getId(), song.getYoutubeUrl(), song.getTitle(), song.getArtist(), song.getCreatedAt(), song.getUpdatedAt());
	}
}
