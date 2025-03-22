package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.map.dto.MapRequestDto;
import com.lshzzz.mato.model.map.dto.MapResponseDto;
import com.lshzzz.mato.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.Collections;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/maps")
public class MapController {
	private final MapService mapService;

	// 새로운 맵 생성 (옵션: 노래 목록도 함께 추가 가능)
	@PostMapping
	public ResponseEntity<MapResponseDto> createMap(@RequestBody @Valid MapRequestDto requestDto) {
		// 요청 DTO에는 맵 정보와 추가할 노래 목록(기존 또는 새로운 노래)이 포함됨
		MapResponseDto createdMap = mapService.createMap(requestDto);
		return ResponseEntity.status(HttpStatus.CREATED).body(createdMap);
	}

	@GetMapping("/check")
	public ResponseEntity<?> checkMapExists(@RequestParam String name) {
		try {
			boolean exists = mapService.checkDuplicateMap(name);
			return ResponseEntity.ok(Collections.singletonMap("isDuplicate", exists));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(Collections.singletonMap("error", "중복 검사 중 오류 발생"));
		}
	}

	@GetMapping("/check-duplicate")
	public ResponseEntity<Boolean> checkDuplicateMap(@RequestParam String name) {
		return ResponseEntity.ok(mapService.checkDuplicateMap(name));
	}

	// 특정 맵 정보 조회 (ID 기반)
	@GetMapping("/{id}")
	public ResponseEntity<MapResponseDto> getMap(@PathVariable Long id) {
		MapResponseDto map = mapService.getMapById(id);
		return ResponseEntity.ok(map);
	}

	// 모든 공개된 맵 조회 (isPublic=true인 맵만 반환)
	@GetMapping("/public")
	public ResponseEntity<List<MapResponseDto>> getPublicMaps() {
		List<MapResponseDto> publicMaps = mapService.getPublicMaps();
		return ResponseEntity.ok(publicMaps);
	}

	// 맵 정보 수정
	@PutMapping("/{id}")
	public ResponseEntity<MapResponseDto> updateMap(@PathVariable Long id,
		@RequestBody @Valid MapRequestDto requestDto) {
		MapResponseDto updatedMap = mapService.updateMap(id, requestDto);
		return ResponseEntity.ok(updatedMap);
	}

	// 맵 삭제 (삭제 권한 검사를 위해 userId를 요청 파라미터로 전달)
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteMap(@PathVariable Long id,
		@RequestParam Long userId) {
		mapService.deleteMap(id, userId);
		return ResponseEntity.noContent().build();
	}
}
