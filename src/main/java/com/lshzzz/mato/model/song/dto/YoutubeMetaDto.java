package com.lshzzz.mato.model.song.dto;

public record YoutubeMetaDto(
	String youtubeUrl,
	String title,
	String artist,
	boolean success
) {}
