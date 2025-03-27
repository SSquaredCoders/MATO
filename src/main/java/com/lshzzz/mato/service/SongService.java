package com.lshzzz.mato.service;

import com.lshzzz.mato.model.song.Song;
import com.lshzzz.mato.model.song.dto.SongRequestDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;
import com.lshzzz.mato.repository.SongRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SongService {

	private final SongRepository songRepository;

	// 노래 등록
	public SongResponseDto createSong(SongRequestDto requestDto) {
		Song song = Song.builder()
			.youtubeUrl(requestDto.youtubeUrl())
			.title(requestDto.title())
			.build();
		songRepository.save(song);
		return new SongResponseDto(song);
	}

	// 모든 노래 조회
	@Transactional(readOnly = true)
	public List<SongResponseDto> getAllSongs() {
		return songRepository.findAll()
			.stream()
			.map(SongResponseDto::new)
			.collect(Collectors.toList());
	}

	// 특정 노래 조회
	@Transactional(readOnly = true)
	public Optional<SongResponseDto> getSong(Long id) {
		return songRepository.findById(id)
			.map(SongResponseDto::new);
	}

	// 노래 수정
	public SongResponseDto updateSong(Long id, SongRequestDto requestDto) {
		Song song = songRepository.findById(id)
			.orElseThrow(() -> new IllegalArgumentException("존재하지 않는 노래입니다."));
		song.update(
			requestDto.youtubeUrl(),
			requestDto.title()
		);
		return new SongResponseDto(song);
	}

	// 노래 삭제
	public void deleteSong(Long id) {
		songRepository.deleteById(id);
	}

	// URL로 노래 검색
	@Transactional(readOnly = true)
	public Optional<SongResponseDto> getSongByUrl(String youtubeUrl) {
		return songRepository.findByYoutubeUrl(youtubeUrl)
			.map(SongResponseDto::new);
	}

	// 제목으로 노래 검색 (부분 일치)
	@Transactional(readOnly = true)
	public List<SongResponseDto> searchSongsByTitle(String title) {
		return songRepository.findByTitleContaining(title).stream()
			.map(SongResponseDto::new)
			.collect(Collectors.toList());
	}
}
