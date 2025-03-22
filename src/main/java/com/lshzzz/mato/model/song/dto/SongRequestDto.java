package com.lshzzz.mato.model.song.dto;

import jakarta.validation.constraints.NotBlank;

public record SongRequestDto(
	@NotBlank String youtubeUrl  // 오디오 URL (유튜브 링크 OR 업로드된 파일)
) {}
