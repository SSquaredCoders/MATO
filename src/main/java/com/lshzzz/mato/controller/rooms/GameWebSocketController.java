package com.lshzzz.mato.controller.rooms;

import com.lshzzz.mato.model.chat.dto.ChatMessage;
import com.lshzzz.mato.model.game.dto.GameStatusResponse;
import com.lshzzz.mato.service.game.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Slf4j
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
		// 중복 브로드캐스트 제거 (GameService에서 GameStatusResponse 전송함)
	}

	@MessageMapping("/game.end")
	public void handleGameEnd(@Payload ChatMessage message) {
		gameService.endGame(message.roomName());
	}

	@MessageMapping("/game.next")
	public void handleNextSong(@Payload ChatMessage message) {
		gameService.nextSong(message.roomName());
	}

	@MessageMapping("/game.skip")
	public void handleSkipSong(@Payload ChatMessage message) {
		gameService.skipSong(message.roomName());
	}

	@MessageMapping("/game.correct")
	public void handleCorrectAnswer(@Payload ChatMessage message) {
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
	}

	@MessageMapping("/score.update")
	public void handleScoreUpdate(@Payload ChatMessage message) {
		try {
			String content = message.content();
			if (content != null && !content.isEmpty()) {
				gameService.updateScore(
					message.roomName(),
					message.sender(),
					1,
					"정답 맞춤"
				);
			}
			messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), message);
		} catch (Exception e) {
			log.error("점수 업데이트 중 오류 발생", e);
		}
	}

	@MessageMapping("/game.status")
	public void getGameStatus(@Payload ChatMessage message) {
		GameStatusResponse status = gameService.getGameStatus(message.roomName());
		messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), status);
	}
}
