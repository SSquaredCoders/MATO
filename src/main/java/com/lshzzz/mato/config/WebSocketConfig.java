package com.lshzzz.mato.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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

}

