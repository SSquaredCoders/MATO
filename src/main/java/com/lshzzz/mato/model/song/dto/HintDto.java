package com.lshzzz.mato.model.song.dto;

import com.lshzzz.mato.model.song.Hint;

/**
 * 힌트 정보 DTO
 */
public record HintDto(
	Long id,
	String text,
	int revealTime
) {
	/**
	 * 기존 코드 호환성을 위한 추가 생성자
	 */
	public HintDto(Long id, Long mapSongId, String hintText, int revealTime) {
		this(id, hintText, revealTime);
	}
	
	/**
	 * Hint 엔티티로부터 DTO 생성
	 */
	public static HintDto fromEntity(Hint hint) {
		return new HintDto(
			hint.getId(),
			hint.getHintText(),
			hint.getRevealTime()
		);
	}
}
