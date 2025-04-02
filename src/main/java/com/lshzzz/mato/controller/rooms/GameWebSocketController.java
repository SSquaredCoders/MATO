package com.lshzzz.mato.controller.rooms;

import com.lshzzz.mato.model.chat.dto.ChatMessage;
import com.lshzzz.mato.model.game.dto.GameStatusResponse;
import com.lshzzz.mato.service.game.GameService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class GameWebSocketController {
	private final SimpMessagingTemplate messagingTemplate;
	private final GameService gameService;

	@MessageMapping("/chat.join")
	public void handleJoin(@Payload ChatMessage message) {
		String content = message.sender() + "님이 입장하셨습니다.";
		ChatMessage joinMessage = new ChatMessage("SYSTEM", content, message.roomName(), "JOIN");
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), joinMessage);
	}

	@MessageMapping("/chat.leave")
	public void handleLeave(@Payload ChatMessage message) {
		String content = message.sender() + "님이 퇴장하셨습니다.";
		ChatMessage leaveMessage = new ChatMessage("SYSTEM", content, message.roomName(), "LEAVE");
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), leaveMessage);
	}

	@MessageMapping("/chat.ready")
	public void handleReady(@Payload ChatMessage message) {
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}

	@MessageMapping("/game.start")
	public void handleGameStart(@Payload ChatMessage message) {
		gameService.startGame(message.roomName());
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}

	@MessageMapping("/game.end")
	public void handleGameEnd(@Payload ChatMessage message) {
		gameService.endGame(message.roomName());
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}

	@MessageMapping("/game.next")
	public void handleNextSong(@Payload ChatMessage message) {
		gameService.nextSong(message.roomName());
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}

	@MessageMapping("/game.skip")
	public void handleSkipSong(@Payload ChatMessage message) {
		gameService.skipSong(message.roomName());
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}

	@MessageMapping("/game.correct")
	public void handleCorrectAnswer(@Payload ChatMessage message) {
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}

	@MessageMapping("/score.update")
	public void handleScoreUpdate(@Payload ChatMessage message) {
		try {
			// content에서 점수 정보 파싱
			String content = message.content();
			if (content != null && !content.isEmpty()) {
				// 점수 업데이트 처리
				gameService.updateScore(
					message.roomName(),
					message.sender(),
					1, // 정답 맞춤 시 1점
					"정답 맞춤"
				);
			}
			messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@MessageMapping("/game.status")
	public void getGameStatus(@Payload ChatMessage message) {
		GameStatusResponse status = gameService.getGameStatus(message.roomName());
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), status);
	}
}

