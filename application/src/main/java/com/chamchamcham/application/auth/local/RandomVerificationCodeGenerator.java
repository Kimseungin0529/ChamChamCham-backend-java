package com.chamchamcham.application.auth.local;

import org.springframework.stereotype.Component;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class RandomVerificationCodeGenerator implements VerificationCodeGenerator {
    @Override public String generate() {
        var code = new StringBuilder(6);
        for (int i = 0; i < 6; i++) code.append(ThreadLocalRandom.current().nextInt(10));
        return code.toString();
    }
}
