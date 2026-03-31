package com.lshzzz.mato.service;

import com.lshzzz.mato.model.song.dto.YoutubeMetaDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class YoutubeMetaService {

	private final RestTemplate restTemplate = new RestTemplate();

	public List<YoutubeMetaDto> fetchBulkMeta(List<String> urls) {
		return urls.stream()
			.map(this::fetchMeta)
			.collect(Collectors.toList());
	}

	private YoutubeMetaDto fetchMeta(String url) {
		try {
			String normalizedUrl = url.startsWith("http") ? url : "https://" + url;
			String apiUrl = "https://noembed.com/embed?url=" + URLEncoder.encode(normalizedUrl, StandardCharsets.UTF_8);

			@SuppressWarnings("unchecked")
			Map<String, Object> response = restTemplate.getForObject(apiUrl, Map.class);

			if (response == null || !response.containsKey("title")) {
				return new YoutubeMetaDto(url, null, null, false);
			}

			String rawTitle = (String) response.get("title");
			String authorName = (String) response.get("author_name");

			// "Artist - Title" 패턴 파싱
			String parsedTitle = rawTitle;
			String parsedArtist = authorName;

			if (rawTitle.contains(" - ")) {
				String[] parts = rawTitle.split(" - ", 2);
				parsedArtist = parts[0].trim();
				parsedTitle = parts[1].trim();
			}

			// 제목에서 (Official Video) 같은 불필요한 태그 제거
			parsedTitle = parsedTitle
				.replaceAll("\\s*\\(Official.*?\\)", "")
				.replaceAll("\\s*\\[Official.*?\\]", "")
				.replaceAll("\\s*\\(MV\\)", "")
				.replaceAll("\\s*\\(M/V\\)", "")
				.replaceAll("\\s*MV$", "")
				.replaceAll("\\s*M/V$", "")
				.trim();

			return new YoutubeMetaDto(normalizedUrl, parsedTitle, parsedArtist, true);
		} catch (Exception e) {
			return new YoutubeMetaDto(url, null, null, false);
		}
	}
}
