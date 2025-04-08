package com.lshzzz.mato.controller.rooms;

import com.lshzzz.mato.model.chat.dto.ChatMessage;
import com.lshzzz.mato.model.game.dto.GameStatusResponse;
import com.lshzzz.mato.service.game.GameService;
import com.lshzzz.mato.service.rooms.RedisRoomsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Set;
import java.util.HashSet;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {
	private final SimpMessagingTemplate messagingTemplate;
	private final GameService gameService;
	private final RedisRoomsService roomsService;

	@MessageMapping("/chat.join")
	public void handleJoin(@Payload ChatMessage message) {
		log.info("입장 메시지 수신: {} 사용자가 {} 방에 입장 시도", message.sender(), message.roomName());
		
		try {
			// 이미 참가자인지 확인 (중복 참가 방지)
			Set<String> participants = roomsService.getParticipants(message.roomName());
			if (participants.contains(message.sender())) {
				log.info("{} 사용자는 이미 {} 방에 참가 중입니다. 중복 입장 무시.", message.sender(), message.roomName());
				// 이미 참가 중인 경우 입장 메시지 전송하지 않음
				
				// 하지만 클라이언트에게 최신 참가자 목록을 갱신하도록 알림은 보낸다
				ChatMessage updateMessage = new ChatMessage("SYSTEM", "참가자 목록 갱신", message.roomName(), "UPDATE_PARTICIPANTS");
				messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), updateMessage);
				
				// 로비에는 업데이트 메시지를 보내지 않음 (참가자 수 변경 없음)
				return;
			}
			
			// 새로운 참가자인 경우에만 정상 처리
			log.info("{} 사용자를 {} 방의 참가자로 추가합니다.", message.sender(), message.roomName());
			
			// 입장 메시지 생성 및 전송
			String content = message.sender() + "님이 입장하셨습니다.";
			ChatMessage joinMessage = new ChatMessage("SYSTEM", content, message.roomName(), "JOIN");
			messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), joinMessage);
			
			// 로비에도 방 업데이트 메시지 전송 (참가자 수 변경됨)
			ChatMessage lobbyUpdateMessage = new ChatMessage("SYSTEM", message.roomName(), "LOBBY", "PARTICIPANT_CHANGE");
			messagingTemplate.convertAndSend("/topic/lobby", lobbyUpdateMessage);
		} catch (Exception e) {
			log.error("입장 처리 중 오류 발생", e);
		}
	}

	@MessageMapping("/chat.leave")
	public void handleLeave(@Payload ChatMessage message) {
		log.info("퇴장 메시지 수신: {} 사용자가 {} 방에서 퇴장", message.sender(), message.roomName());
		
		// 참가자 목록에서 제거 요청 (별도 HTTP 요청도 있지만 추가 안전장치)
		try {
			// 제거 전에 남은 참가자 수를 확인 (지금 퇴장하는 참가자 포함)
			Set<String> participants = roomsService.getParticipants(message.roomName());
			int participantsCount = participants.size();
			log.info("퇴장 전 {} 방 참가자 수: {}", message.roomName(), participantsCount);
			
			// 이미 참가자가 HTTP 요청으로 제거되었을 수 있으므로 다시 확인
			boolean isStillParticipant = participants.contains(message.sender());
			
			// 아직 참가자인 경우에만 제거 처리
			if (isStillParticipant) {
				// 참가자 제거 처리 (이 메서드 내에서 0명일 경우 방 삭제 로직 포함)
				log.info("{} 사용자를 {} 방에서 제거합니다(웹소켓 요청)", message.sender(), message.roomName());
				roomsService.removeParticipant(message.roomName(), message.sender());
			} else {
				log.info("{} 사용자는 이미 {} 방에서 제거되어 있습니다", message.sender(), message.roomName());
			}
			
			// 퇴장 후 남은 참가자 수 확인
			Set<String> remainingParticipants;
			try {
				remainingParticipants = roomsService.getParticipants(message.roomName());
				int remainingCount = remainingParticipants.size();
				log.info("퇴장 후 {} 방 남은 참가자 수: {}", message.roomName(), remainingCount);
				
				// 남은 참가자가 0명이면 방 삭제
				if (remainingCount == 0) {
					log.info("방 {}에 참가자가 없으므로 방을 삭제합니다.", message.roomName());
					try {
						roomsService.deleteRoomByName(message.roomName());
						log.info("방 {} 삭제 성공", message.roomName());
					} catch (Exception e) {
						log.info("방 {}이 이미 삭제되었습니다.", message.roomName());
					}
				} else if (remainingCount == 1) {
					log.info("방 {} 남은 참가자가 1명 남았습니다.", message.roomName());
				}
			} catch (Exception e) {
				// 방이 이미 삭제된 경우 발생할 수 있는 예외를 처리
				log.info("방 {}이 이미 삭제되었습니다", message.roomName());
				remainingParticipants = new HashSet<>();
			}
			
			// 이벤트 타입을 동적으로 결정
			String eventType = remainingParticipants.isEmpty() ? "ROOM_DELETE" : "PARTICIPANT_CHANGE";
			
			// 로비에 방 업데이트 메시지 전송 (참가자 수 변경 또는 방 삭제)
			ChatMessage lobbyUpdateMessage = new ChatMessage(
				"SYSTEM", 
				message.roomName(), 
				"LOBBY", 
				eventType
			);
			messagingTemplate.convertAndSend("/topic/lobby", lobbyUpdateMessage);
			log.info("로비에 {} 이벤트를 전송했습니다", eventType);
			
			// 남은 참가자들에게 업데이트된 참가자 목록을 보내도록 추가 메시지 전송
			if (!remainingParticipants.isEmpty()) {
				ChatMessage roomUpdateMessage = new ChatMessage(
					"SYSTEM", 
					"참가자 목록이 업데이트되었습니다", 
					message.roomName(), 
					"UPDATE_PARTICIPANTS"
				);
				messagingTemplate.convertAndSend("/topic/rooms/" + message.roomName(), roomUpdateMessage);
				log.info("방 {}에 참가자 목록 업데이트 메시지를 전송했습니다", message.roomName());
			}
		} catch (Exception e) {
			log.error("퇴장 처리 중 오류 발생", e);
		}
		
		// 퇴장 메시지 전송
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
		
		// 로비에도 방 상태 업데이트 메시지 전송 (게임 시작됨)
		ChatMessage lobbyUpdateMessage = new ChatMessage("SYSTEM", message.roomName(), "LOBBY", "ROOM_UPDATE");
		messagingTemplate.convertAndSend("/topic/lobby", lobbyUpdateMessage);
	}

	@MessageMapping("/game.end")
	public void handleGameEnd(@Payload ChatMessage message) {
		gameService.endGame(message.roomName());
		
		// 로비에도 방 상태 업데이트 메시지 전송 (게임 종료됨)
		ChatMessage lobbyUpdateMessage = new ChatMessage("SYSTEM", message.roomName(), "LOBBY", "ROOM_UPDATE");
		messagingTemplate.convertAndSend("/topic/lobby", lobbyUpdateMessage);
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

	// 로비 강제 갱신 요청 처리
	@MessageMapping("/lobby.update")
	public void handleLobbyUpdate(@Payload ChatMessage message) {
		log.info("로비 강제 갱신 요청 수신: 타입={}, 내용={}", message.type(), message.content());
		
		// 메시지 타입에 따라 다른 처리
		if ("PARTICIPANT_LEAVE".equals(message.type())) {
			// 참가자 퇴장 처리 - 바로 로비에 반영
			String[] parts = message.content().split("\\|");
			String leavingUser = parts.length > 0 ? parts[0] : "unknown";
			int remainingCount = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
			
			log.info("참가자 {} 퇴장 처리, 남은 인원: {}", leavingUser, remainingCount);
			
			// 참가자 변경 메시지 전송
			ChatMessage participantChangeMsg = new ChatMessage(
				"SYSTEM",
				message.roomName() + "|" + remainingCount, // 방 이름과 남은 인원 수 전달
				"LOBBY",
				"PARTICIPANT_CHANGE"
			);
			messagingTemplate.convertAndSend("/topic/lobby", participantChangeMsg);
			log.info("로비에 참가자 변경 메시지를 전송했습니다: 방={}, 남은 인원={}", 
				message.roomName(), remainingCount);
			
			return;
		}
		
		// 기본적인 강제 갱신 메시지
		ChatMessage forceRefreshMessage = new ChatMessage(
			"SYSTEM",
			message.content(),
			"LOBBY",
			message.type().equals("FORCE_REFRESH") ? "FORCE_REFRESH" : "ROOM_UPDATE"
		);
		messagingTemplate.convertAndSend("/topic/lobby", forceRefreshMessage);
		log.info("로비 강제 갱신 메시지를 전송했습니다");
	}
}
