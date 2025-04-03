package com.lshzzz.mato.model.game.dto;

import com.lshzzz.mato.model.mapsongs.MapSong;
import com.lshzzz.mato.model.song.Answer;
import com.lshzzz.mato.model.song.dto.HintDto;
import com.lshzzz.mato.model.song.dto.SongResponseDto;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 현재 플레이 중인 곡 정보 DTO
 */
public record CurrentSongDto(
    Long id,
    SongResponseDto song,
    int startTime,
    int endTime,
    int repeatCount,
    List<String> answers,
    List<HintDto> hints
) {
    /**
     * MapSong 엔티티에서 현재 곡 정보 생성
     */
    public static CurrentSongDto fromEntity(MapSong mapSong) {
        // 답변 텍스트만 추출
        List<String> answerTexts = mapSong.getAnswers().stream()
            .map(Answer::getAnswerText)
            .collect(Collectors.toList());
            
        // 힌트 정보 변환
        List<HintDto> hintDtos = mapSong.getHints().stream()
            .map(hint -> new HintDto(
                hint.getId(),
                hint.getHintText(),
                hint.getRevealTime()
            ))
            .collect(Collectors.toList());
            
        return new CurrentSongDto(
            mapSong.getId(),
            new SongResponseDto(mapSong.getSong()),
            mapSong.getStartTime(),
            mapSong.getEndTime(),
            mapSong.getRepeatCount(),
            answerTexts,
            hintDtos
        );
    }
} 