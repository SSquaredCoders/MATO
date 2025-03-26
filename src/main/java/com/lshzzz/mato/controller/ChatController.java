package com.lshzzz.mato.controller;

import com.lshzzz.mato.model.chat.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

	private final SimpMessagingTemplate messagingTemplate;

	@MessageMapping("/chat.send")
	public void sendMessage(@Payload ChatMessage message) {
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}
}