package com.lshzzz.mato.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
		registry.enableSimpleBroker("/topic"); // 메시지를 구독하는 엔드포인트 (클라이언트가 구독)
		registry.setApplicationDestinationPrefixes("/app"); // 클라이언트가 서버로 보낼 때 사용하는 prefix
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/ws")
			.setAllowedOrigins("http://localhost:8080", "http://localhost:5173", "http://112.159.76.59:15173", "http://112.159.76.59:18080"
			) // 특정 도메인만 허용
			.withSockJS();
	}
	
	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new ChannelInterceptor() {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
				
				// 연결 시작, 구독 또는 SEND 메시지일 때 처리
				if (StompCommand.CONNECT.equals(accessor.getCommand())) {
					// 연결 시 세션 정보 초기화
					log.info("웹소켓 연결 요청: 세션={}", accessor.getSessionId());
				}
				else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
					// 구독 정보에서 방 이름 추출해서 세션에 저장
					String destination = accessor.getDestination();
					if (destination != null && destination.startsWith("/topic/rooms/")) {
						String roomName = destination.substring("/topic/rooms/".length());
						if (accessor.getSessionAttributes() != null) {
							accessor.getSessionAttributes().put("subscribedRoom", roomName);
							log.info("방 {} 구독: 세션={}", roomName, accessor.getSessionId());
						}
					}
				}
				else if (StompCommand.SEND.equals(accessor.getCommand())) {
					try {
						// 목적지(destination) 정보 확인
						String destination = accessor.getDestination();
						if (destination != null) {
							// '/app/chat.join' 목적지일 때 (방 입장 메시지)
							if (destination.startsWith("/app/chat.join")) {
								// 메시지 본문 추출 시도
								try {
									// 페이로드에서 누가 어떤 방에 들어가는지 정보 추출 시도
									Object payload = message.getPayload();
									if (payload != null) {
										log.info("chat.join 메시지 감지: 세션={}, 페이로드={}", 
												accessor.getSessionId(), payload.toString());
									}
								} catch (Exception e) {
									log.warn("메시지 페이로드 추출 오류: {}", e.getMessage());
								}
								
								log.info("chat.join 메시지 감지: 세션={}", accessor.getSessionId());
							}
							// '/app/chat.leave' 목적지일 때 (방 퇴장 메시지)
							else if (destination.startsWith("/app/chat.leave")) {
								log.info("chat.leave 메시지 감지: 세션={}", accessor.getSessionId());
								// 세션에서 해당 정보 제거 (GameWebSocketController에서 처리)
							}
						}
					} catch (Exception e) {
						log.error("메시지 처리 중 오류: {}", e.getMessage(), e);
					}
				}
				else if (StompCommand.DISCONNECT.equals(accessor.getCommand())) {
					// 연결 종료 시 로그 남기기
					log.info("웹소켓 연결 종료 요청: 세션={}", accessor.getSessionId());
					// 실제 세션 정보 정리는 SessionDisconnectEvent에서 처리
				}
				
				return message;
			}
		});
	}
}

