package com.lshzzz.mato.model.v2;

import java.util.Map;

public record V2ClientEnvelope(
    String type,
    String roomName,
    Map<String, Object> payload,
    String clientTimestamp
) {
}
