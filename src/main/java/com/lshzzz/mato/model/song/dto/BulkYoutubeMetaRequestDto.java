package com.lshzzz.mato.model.song.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkYoutubeMetaRequestDto(
	@NotEmpty List<String> urls
) {}
