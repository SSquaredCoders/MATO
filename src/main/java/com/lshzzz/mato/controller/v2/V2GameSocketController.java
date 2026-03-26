package com.lshzzz.mato.controller.v2;

import com.lshzzz.mato.model.v2.V2ClientEnvelope;
import com.lshzzz.mato.model.v2.V2RoomEventPayload;
import com.lshzzz.mato.model.v2.V2ServerEnvelope;
import com.lshzzz.mato.service.v2.V2RoomRuntimeService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Controller
@RequiredArgsConstructor
public class V2GameSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final V2RoomRuntimeService roomRuntimeService;

    @MessageMapping("/v2/game.send")
    public void handleEvent(
        @Payload V2ClientEnvelope envelope,
        SimpMessageHeaderAccessor headerAccessor
    ) {
        V2RoomRuntimeService.EventResult result;
        try {
            result = roomRuntimeService.handleEvent(headerAccessor.getSessionId(), envelope);
        } catch (ResponseStatusException exception) {
            result = new V2RoomRuntimeService.EventResult(
                "error",
                envelope.roomName(),
                null,
                exception.getReason(),
                null,
                false,
                null
            );
        }

        broadcast(result);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        V2RoomRuntimeService.EventResult result = roomRuntimeService.handleDisconnect(sessionId);
        if (result != null) {
            broadcast(result);
        }
    }

    private void broadcast(V2RoomRuntimeService.EventResult result) {
        V2ServerEnvelope response = new V2ServerEnvelope(
            result.type(),
            result.roomName(),
            new V2RoomEventPayload(
                result.snapshot(),
                result.message(),
                result.actorNickname(),
                result.accepted(),
                result.chatMessage()
            ),
            Instant.now().toString()
        );

        messagingTemplate.convertAndSend("/topic/v2/rooms/" + result.roomName(), response);
    }
}
