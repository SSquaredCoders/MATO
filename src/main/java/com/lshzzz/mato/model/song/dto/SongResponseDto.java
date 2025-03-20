package com.lshzzz.mato.model.song.dto;

import com.lshzzz.mato.model.song.Song;
import java.time.LocalDateTime;

public record SongResponseDto(
	Long id,
	String title,
	String artist,
	String composer,
	String youtubeUrl,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
	public SongResponseDto(Song song) {
		this(
			song.getId(),
			song.getTitle(),
			song.getArtist(),
			song.getComposer(),
			song.getYoutubeUrl(),
			song.getCreatedAt(),
			song.getUpdatedAt()
		);
	}
}
