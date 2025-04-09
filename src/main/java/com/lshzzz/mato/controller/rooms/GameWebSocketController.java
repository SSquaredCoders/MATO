package com.lshzzz.mato.controller.rooms;

import com.lshzzz.mato.model.chat.dto.ChatMessage;
import com.lshzzz.mato.model.game.dto.GameStatusResponse;
import com.lshzzz.mato.model.room.dto.RoomsResponse;
import com.lshzzz.mato.service.game.GameService;
import com.lshzzz.mato.service.rooms.RedisRoomsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.HashSet;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GameWebSocketController {
	private final SimpMessagingTemplate messagingTemplate;
	private final GameService gameService;
	private final RedisRoomsService roomsService;
	private final com.lshzzz.mato.service.rooms.SessionTracker sessionTracker;

	@MessageMapping("/chat.join")
	public void handleJoin(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
		log.info("입장 메시지 수신: {} 사용자가 {} 방에 입장 시도", message.sender(), message.roomName());
		
		try {
			// 세션에 사용자 정보와 방 정보 저장
			if (headerAccessor.getSessionAttributes() != null) {
				String sessionId = headerAccessor.getSessionId();
				// 클라이언트에서 전송한 sender 값을 그대로 사용
				String username = message.sender();
				
				headerAccessor.getSessionAttributes().put("username", username);
				headerAccessor.getSessionAttributes().put("roomName", message.roomName());
				headerAccessor.getSessionAttributes().put("sessionId", sessionId);
				log.info("세션에 사용자 정보 저장: 사용자={}, 방={}, 세션={}", 
					username, message.roomName(), sessionId);
					
				// 세션 추적기에 세션 등록
				sessionTracker.registerSession(sessionId, username, message.roomName());
				
				// 같은 방에 동일 사용자의 다른 세션이 있는지 확인
				boolean alreadyInRoom = sessionTracker.isUserInRoom(username, message.roomName());
				if (alreadyInRoom) {
					log.info("사용자 {}는 이미 다른 세션으로 방 {}에 접속 중입니다.", 
							username, message.roomName());
				}
			}
			
			// 방에 한 명만 있고, 그 사람이 방장인 경우 중복 가능성 확인
			try {
				RoomsResponse room = roomsService.findByName(message.roomName());
				Set<String> participants = roomsService.getParticipants(message.roomName());
				log.info("방 {} 현재 참가자 목록: {}", message.roomName(), participants);
				
				// 방에 한 명만 있고 입장 메시지와 호스트가 같은 IP나 세션 특성을 가지는지 확인
				if (participants.size() == 1 && !participants.contains(message.sender())) {
					String hostNickname = room.host();
					if (hostNickname != null && !participants.contains(hostNickname)) {
						// 호스트가 참가자 목록에 없으면 뭔가 잘못된 것 - 정리 필요
						log.warn("방 {} 호스트 {}가 참가자 목록에 없습니다. 데이터 정리가 필요할 수 있습니다.", 
							message.roomName(), hostNickname);
					}
					
					// 특수 처리: 방장과 현재 유저만 있는 경우 처리 (낮은 가능성)
					if (participants.size() == 1) {
						String existingParticipant = participants.iterator().next();
						
						// 방장 이름과 참가자 이름이 다르고, 입장 시도자가 방장 이름과 같으면 중복 가능성 높음
						if (!existingParticipant.equals(hostNickname) && message.sender().equals(hostNickname)) {
							log.info("방 {}에 방장 재입장으로 판단. 참가자 목록 정리 중...", message.roomName());
							// 이 경우 방장이 다시 입장하는 것으로 간주하고 기존 세션 정리
							roomsService.removeParticipant(message.roomName(), existingParticipant);
							log.info("방 {}에서 기존 참가자 {} 제거 완료", message.roomName(), existingParticipant);
						}
						// 방장 이름과 참가자 이름이 같고, 입장 시도자가 다른 이름이면 세션 변경으로 간주
						else if (existingParticipant.equals(hostNickname) && !message.sender().equals(hostNickname)) {
							log.info("방 {}에 방장의 다른 세션으로 판단. 참가자로 간주.", message.roomName());
							// 여기서는 새 참가자로 추가하지만 나중에 한쪽이 연결 종료되면 자동 정리됨
						}
					}
				}
			} catch (Exception e) {
				log.warn("방 {} 정보 조회 중 오류: {}", message.roomName(), e.getMessage());
				// 오류가 있어도 진행 - 단순 최적화 로직
			}
			
			// 이미 참가자인지 확인 (중복 참가 방지)
			Set<String> participants = roomsService.getParticipants(message.roomName());
			log.info("방 {} 현재 참가자 목록: {}", message.roomName(), participants);
			
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
			log.info("{} 사용자는 {} 방의 신규 참가자입니다. 추가합니다.", message.sender(), message.roomName());
			roomsService.addParticipant(message.roomName(), message.sender());
			log.info("{} 사용자를 {} 방에 추가 완료", message.sender(), message.roomName());
			
			// 업데이트 후 참가자 목록 다시 확인
			participants = roomsService.getParticipants(message.roomName());
			log.info("방 {} 업데이트 후 참가자 목록: {} (총 {}명)", 
					message.roomName(), participants, participants.size());
			
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

	// 두 사용자 식별자가 동일 인물일 가능성 체크
	private boolean isPossiblyRelatedUser(String user1, String user2) {
		// 완전히 같은 경우
		if (user1.equals(user2)) {
			return true;
		}
		
		// 문자열 패턴 분석 (예: "test1"과 "zxczxc"처럼 전혀 관계없는 경우는 false)
		
		// 게스트 사용자인 경우, 숫자만 다른지 체크
		if (user1.startsWith("게스트") && user2.startsWith("게스트")) {
			return true; // 둘 다 게스트면 동일 사용자로 간주
		}
		
		// 다른 특수한 패턴이 있다면 여기에 추가
		
		// 방장 정보 기반 확인 - 방에 인원이 한 명뿐이고, 그게 방장이면
		// 새로 들어오는 사용자는 동일 인물일 가능성이 높음
		return false;
	}

	@MessageMapping("/chat.leave")
	public void handleLeave(@Payload ChatMessage message, SimpMessageHeaderAccessor headerAccessor) {
		log.info("퇴장 메시지 수신: {} 사용자가 {} 방에서 퇴장", message.sender(), message.roomName());
		
		// 세션 정보 처리
		String sessionId = headerAccessor.getSessionId();
		if (headerAccessor.getSessionAttributes() != null) {
			headerAccessor.getSessionAttributes().remove("username");
			headerAccessor.getSessionAttributes().remove("roomName");
			log.info("세션에서 사용자 정보 제거: 사용자={}, 방={}, 세션={}", 
				message.sender(), message.roomName(), sessionId);
		}
		
		// 세션 추적기에서 세션 제거
		sessionTracker.removeSession(sessionId);
		
		// 방이 실제로 존재하는지 먼저 확인
		try {
			roomsService.findByName(message.roomName());
		} catch (Exception e) {
			log.info("방 {}이 이미 존재하지 않습니다. 퇴장 처리 무시", message.roomName());
			return;
		}
		
		// 참가자 목록에서 제거 요청 (별도 HTTP 요청도 있지만 추가 안전장치)
		try {
			// 제거 전에 남은 참가자 수와 목록을 확인 (지금 퇴장하는 참가자 포함)
			Set<String> participants = roomsService.getParticipants(message.roomName());
			int participantsCount = participants.size();
			log.info("퇴장 전 {} 방 참가자 수: {}명, 참가자 목록: {}", 
					message.roomName(), participantsCount, participants);
			
			// 이미 참가자가 HTTP 요청으로 제거되었을 수 있으므로 다시 확인
			boolean isStillParticipant = participants.contains(message.sender());
			
			// 아직 참가자인 경우에만 제거 처리
			if (isStillParticipant) {
				// 참가자 제거 처리 (이 메서드 내에서 0명일 경우 방 삭제 로직 포함)
				log.info("{} 사용자가 {} 방의 참가자 목록에 있습니다. 제거합니다.", message.sender(), message.roomName());
				roomsService.removeParticipant(message.roomName(), message.sender());
			} else {
				log.info("{} 사용자는 이미 {} 방의 참가자 목록에 없습니다.", message.sender(), message.roomName());
				// 참가자가 아직 제거되지 않았을 수 있으므로 강제로 한번 더 시도
				try {
					log.info("{} 사용자를 {} 방에서 강제 제거 시도합니다", message.sender(), message.roomName());
					roomsService.removeParticipant(message.roomName(), message.sender());
				} catch (Exception e) {
					log.warn("강제 제거 중 오류: {}", e.getMessage());
				}
			}
			
			// 사용자가 다른 세션으로 여전히 방에 접속 중인지 확인
			boolean stillInRoom = sessionTracker.isUserInRoom(message.sender(), message.roomName());
			if (stillInRoom) {
				log.info("사용자 {}가 다른 세션으로 방 {}에 남아있어 퇴장 처리하지 않습니다.", 
						message.sender(), message.roomName());
				return;
			}
			
			// 퇴장 후 남은 참가자 수 확인 - 실제 Redis에서 다시 조회
			Set<String> remainingParticipants;
			try {
				remainingParticipants = roomsService.getParticipants(message.roomName());
				int remainingCount = remainingParticipants.size();
				log.info("퇴장 후 {} 방 남은 참가자 수: {}명, 남은 참가자: {}", 
						message.roomName(), remainingCount, remainingParticipants);
				
				// 남은 참가자가 0명이면 방 삭제
				if (remainingCount == 0) {
					log.info("방 {}에 참가자가 없으므로 방을 삭제합니다.", message.roomName());
					try {
						roomsService.deleteRoomByName(message.roomName());
						log.info("방 {} 삭제 성공", message.roomName());
						
						// 방이 실제로 삭제되었는지 한번 더 확인
						try {
							roomsService.findByName(message.roomName());
							log.warn("방 {} 삭제 명령 후에도 여전히 존재합니다. 다시 삭제 시도", message.roomName());
							roomsService.deleteRoomByName(message.roomName());
						} catch (Exception ex) {
							// 방을 찾을 수 없으면 정상적으로 삭제된 것
							log.info("방 {} 삭제 확인 완료", message.roomName());
						}
					} catch (Exception e) {
						log.info("방 {}이 이미 삭제되었습니다.", message.roomName());
					}
				} else if (remainingCount == 1) {
					log.info("방 {} 남은 참가자가 1명 남았습니다.", message.roomName());
					// 혹시 자기 자신이 아직 목록에 있는지 확인
					if (remainingParticipants.contains(message.sender())) {
						log.warn("참가자 {} 퇴장 처리 실패. 아직 목록에 존재합니다. 다시 제거 시도", message.sender());
						roomsService.removeParticipant(message.roomName(), message.sender());
						// 다시 남은 인원 확인
						Set<String> finalParticipants = roomsService.getParticipants(message.roomName());
						if (finalParticipants.isEmpty()) {
							log.info("방 {} 최종 점검: 참가자 없음, 방 삭제 실행", message.roomName());
							roomsService.deleteRoomByName(message.roomName());
						}
					}
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

	/**
	 * 웹소켓 연결 종료 이벤트를 처리하는 메서드
	 * @param event 세션 구독 이벤트
	 */
	@org.springframework.context.event.EventListener
	public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
		org.springframework.messaging.simp.stomp.StompHeaderAccessor headerAccessor = org.springframework.messaging.simp.stomp.StompHeaderAccessor.wrap(event.getMessage());
		
		// 연결이 끊어진 사용자 정보 추출
		String sessionId = headerAccessor.getSessionId();
		log.info("웹소켓 연결 종료: 세션 ID={}", sessionId);
		
		// 세션 추적기에서 사용자 정보 가져오기
		String username = sessionTracker.getUserBySession(sessionId);
		String roomName = sessionTracker.getRoomBySession(sessionId);
		
		// 세션 추적기에서 세션 제거
		sessionTracker.removeSession(sessionId);
		
		// 세션에 저장된 사용자 정보와 방 정보 추출 (백업)
		if (username == null || roomName == null) {
			username = (String) headerAccessor.getSessionAttributes().get("username");
			roomName = (String) headerAccessor.getSessionAttributes().get("roomName");
		}
		
		// 같은 사용자가 다른 세션으로 여전히 방에 남아있는지 확인
		if (username != null && roomName != null) {
			boolean stillInRoom = sessionTracker.isUserInRoom(username, roomName);
			if (stillInRoom) {
				log.info("사용자 {}가 다른 세션으로 방 {}에 남아있어 퇴장 처리하지 않습니다.", username, roomName);
				return;
			}
		}
		
		// username과 roomName 정보가 있으면 방에서 자동 퇴장 처리
		if (username != null && roomName != null) {
			log.info("자동 퇴장 처리: 사용자={}, 방={}, 세션={}", username, roomName, sessionId);
			
			try {
				// 방에서 사용자 제거
				roomsService.removeParticipant(roomName, username);
				
				// 퇴장 메시지 생성 및 전송
				String content = username + "님이 연결이 끊어져 퇴장했습니다.";
				ChatMessage leaveMessage = new ChatMessage("SYSTEM", content, roomName, "LEAVE");
				messagingTemplate.convertAndSend("/topic/rooms/" + roomName, leaveMessage);
				
				// 남은 참가자 수 확인
				Set<String> remainingParticipants = roomsService.getParticipants(roomName);
				int remainingCount = remainingParticipants.size();
				log.info("연결 종료 후 {} 방 남은 참가자 수: {}명, 남은 참가자: {}", 
						roomName, remainingCount, remainingParticipants);
				
				// 남은 참가자가 0명이면 방 삭제
				if (remainingCount == 0) {
					log.info("방 {}에 참가자가 없으므로 방을 삭제합니다.", roomName);
					try {
						roomsService.deleteRoomByName(roomName);
						log.info("방 {} 삭제 성공", roomName);
					} catch (Exception e) {
						log.info("방 {}이 이미 삭제되었습니다.", roomName);
					}
				}
				
				// 로비에 방 업데이트 메시지 전송
				String eventType = remainingCount == 0 ? "ROOM_DELETE" : "PARTICIPANT_CHANGE";
				ChatMessage lobbyUpdateMessage = new ChatMessage(
					"SYSTEM", 
					roomName, 
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
						roomName, 
						"UPDATE_PARTICIPANTS"
					);
					messagingTemplate.convertAndSend("/topic/rooms/" + roomName, roomUpdateMessage);
					log.info("방 {}에 참가자 목록 업데이트 메시지를 전송했습니다", roomName);
				}
			} catch (Exception e) {
				log.error("자동 퇴장 처리 중 오류 발생: {}", e.getMessage(), e);
			}
		}
	}
}
