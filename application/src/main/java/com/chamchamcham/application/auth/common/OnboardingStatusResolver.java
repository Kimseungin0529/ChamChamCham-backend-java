package com.chamchamcham.application.auth.common;

import com.chamchamcham.domain.member.Member;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class OnboardingStatusResolver {
    public AuthResult.Onboarding resolve(Member member) {
        Objects.requireNonNull(member, "member");
        var missing = new ArrayList<AuthResult.OnboardingField>();
        if (member.getExperienceLevel() == null) missing.add(AuthResult.OnboardingField.EXPERIENCE_LEVEL);
        if (member.getManagementType() == null) missing.add(AuthResult.OnboardingField.MANAGEMENT_TYPE);
        return new AuthResult.Onboarding(missing.isEmpty() ? AuthResult.OnboardingStatus.COMPLETE : AuthResult.OnboardingStatus.REQUIRED, Collections.unmodifiableList(missing));
    }
}
