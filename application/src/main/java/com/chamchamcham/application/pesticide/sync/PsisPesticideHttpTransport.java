package com.chamchamcham.application.pesticide.sync;

import java.util.Map;
import java.io.IOException;

@FunctionalInterface
public interface PsisPesticideHttpTransport {
    String get(Map<String, String> queryParams) throws IOException, InterruptedException;
}
