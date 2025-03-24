package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class RoomWebSocketController {

	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat.join")
	public void joinRoom(@Payload ChatMessage message) {
		String nickname = message.sender();
		String roomName = message.roomName();
		String content = nickname + "님이 입장하셨습니다.";

		ChatMessage joinMessage = new ChatMessage("SYSTEM", content, roomName, "JOIN");
		messagingTemplate.convertAndSend("/topic/rooms/" + roomName, joinMessage);
	}

	@MessageMapping("/chat.leave")
	public void leaveRoom(@Payload ChatMessage message) {
		String nickname = message.sender();
		String roomName = message.roomName();
		String content = nickname + "님이 퇴장하셨습니다.";

		ChatMessage leaveMessage = new ChatMessage("SYSTEM", content, roomName, "LEAVE");
		messagingTemplate.convertAndSend("/topic/rooms/" + roomName, leaveMessage);
	}
}