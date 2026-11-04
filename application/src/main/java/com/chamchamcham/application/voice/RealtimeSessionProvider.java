package com.chamchamcham.application.voice;

public interface RealtimeSessionProvider {
    RealtimeSessionResult createEphemeralSession(RealtimeSessionRequest request);
}
