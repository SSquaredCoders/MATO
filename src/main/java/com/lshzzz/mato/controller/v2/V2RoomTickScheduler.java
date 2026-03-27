package com.lshzzz.mato.controller.v2;

import com.lshzzz.mato.model.v2.V2RoomEventPayload;
import com.lshzzz.mato.model.v2.V2ServerEnvelope;
import com.lshzzz.mato.service.v2.V2RoomRuntimeService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class V2RoomTickScheduler {

    private final SimpMessagingTemplate messagingTemplate;
    private final V2RoomRuntimeService roomRuntimeService;

    @Scheduled(fixedDelay = 1000)
    public void publishScheduledRoomUpdates() {
        for (V2RoomRuntimeService.EventResult result : roomRuntimeService.collectScheduledEvents()) {
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
}
