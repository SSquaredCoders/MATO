package com.lshzzz.mato.model.v2;

import java.util.List;

public record V2RoomSnapshot(
    String roomName,
    String hostNickname,
    V2GamePhase phase,
    V2MapSummary map,
    int maxParticipants,
    int round,
    int totalRounds,
    String songOrderMode,
    String answerMode,
    String roundFlowMode,
    int roundTimeLimitSeconds,
    int skipVotesRequired,
    int configuredSkipVotesRequired,
    int hintRevealDelaySeconds,
    int currentSkipVotes,
    List<String> skipVoterNicknames,
    String currentPrompt,
    String currentHint,
    String hintRevealAt,
    String roundEndsAt,
    String lastEvent,
    String currentReveal,
    boolean showMediaControls,
    String currentAudioSourceType,
    String currentAudioSourceValue,
    String currentAudioSourceLabel,
    Integer currentClipStartSeconds,
    Integer currentClipEndSeconds,
    List<V2RoomParticipant> participants
) {
}
