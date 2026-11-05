package com.chamchamcham.application.policy.source.nongupez;

import java.util.Map;
import java.io.IOException;

@FunctionalInterface
public interface NongupEzHttpTransport {
    String post(String path, Map<String, ?> body) throws IOException, InterruptedException;
}
