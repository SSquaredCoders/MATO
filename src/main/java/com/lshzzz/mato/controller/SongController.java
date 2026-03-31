package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.song.dto.BulkYoutubeMetaRequestDto;
import com.lshzzz.mato.model.song.dto.SongRequestDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;
import com.lshzzz.mato.model.song.dto.YoutubeMetaDto;
import com.lshzzz.mato.service.SongService;
import com.lshzzz.mato.service.YoutubeMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/songs")
public class SongController {
	private final SongService songService;
	private final YoutubeMetaService youtubeMetaService;

	// 새로운 노래 생성
	@PostMapping
	public ResponseEntity<SongResponseDto> createSong(@RequestBody @Valid SongRequestDto requestDto) {
		SongResponseDto createdSong = songService.createSong(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdSong);
	}

	// 모든 노래 조회
	@GetMapping
	public ResponseEntity<List<SongResponseDto>> getAllSongs() {
		List<SongResponseDto> songs = songService.getAllSongs();
		return ResponseEntity.ok(songs);
	}

	// 특정 노래 조회 (ID 기준)
	@GetMapping("/{id}")
	public ResponseEntity<SongResponseDto> getSong(@PathVariable Long id) {
		Optional<SongResponseDto> song = songService.getSong(id);
		return song.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
	}

	// 기존 노래 정보 수정
	@PutMapping("/{id}")
	public ResponseEntity<SongResponseDto> updateSong(@PathVariable Long id,
		@RequestBody @Valid SongRequestDto requestDto) {
		SongResponseDto updatedSong = songService.updateSong(id, requestDto);
		return ResponseEntity.ok(updatedSong);
	}

	// 노래 삭제 (ID 기준)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteSong(@PathVariable Long id) {
		songService.deleteSong(id);
		return ResponseEntity.noContent().build();
	}

	// URL로 노래 검색
	@GetMapping("/search/url")
	public ResponseEntity<SongResponseDto> searchByUrl(@RequestParam String youtubeUrl) {
		Optional<SongResponseDto> song = songService.getSongByUrl(youtubeUrl);
		return song.map(ResponseEntity::ok)
			.orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
	}

	// 유튜브 URL 벌크 메타데이터 조회
	@PostMapping("/bulk-meta")
	public ResponseEntity<List<YoutubeMetaDto>> fetchBulkMeta(@RequestBody @Valid BulkYoutubeMetaRequestDto requestDto) {
		List<YoutubeMetaDto> result = youtubeMetaService.fetchBulkMeta(requestDto.urls());
		return ResponseEntity.ok(result);
	}

	// 제목으로 노래 검색
	@GetMapping("/search/title")
	public ResponseEntity<List<SongResponseDto>> searchByTitle(@RequestParam String title) {
		List<SongResponseDto> songs = songService.searchSongsByTitle(title);
		return ResponseEntity.ok(songs);
	}
}
