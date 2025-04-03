package com.lshzzz.mato.model.room.dto;

/**
 * 방 참가자 정보 DTO
 */
public record ParticipantDto(
    String nickname,
    boolean isReady,
    boolean isHost
) {} 