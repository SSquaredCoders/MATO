package com.lshzzz.mato.model.chat.dto;

public record ChatMessage(

    String sender,    // 세션에서 추출한 유저 이름
    String content,   // 메시지 내용
    String roomName,   // 방 제목
    String type        // 메시지 타입: CHAT, JOIN, LEAVE
) {}