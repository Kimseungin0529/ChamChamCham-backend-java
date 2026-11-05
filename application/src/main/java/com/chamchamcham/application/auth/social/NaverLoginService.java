package com.chamchamcham.application.auth.social;

import com.chamchamcham.application.auth.common.AuthCommand;
import com.chamchamcham.application.auth.common.AuthResult;
import com.chamchamcham.domain.member.AuthProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@Transactional
public class NaverLoginService {
    private final NaverProfileClient naverProfileClient;
    private final SocialLoginSupport socialLoginSupport;

    public NaverLoginService(NaverProfileClient naverProfileClient, SocialLoginSupport socialLoginSupport) {
        this.naverProfileClient = Objects.requireNonNull(naverProfileClient, "naverProfileClient");
        this.socialLoginSupport = Objects.requireNonNull(socialLoginSupport, "socialLoginSupport");
    }
    public AuthResult.Login login(AuthCommand.NaverLogin command) {
        Objects.requireNonNull(command, "command");
        var profile = naverProfileClient.fetch(command.accessToken());
        return socialLoginSupport.login(AuthProvider.NAVER, profile.subject(), profile.email(), profile.name(), profile.phone(), profile.birthDate(), () -> {});
    }

}
