package com.chamchamcham.domain.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface ExternalIdentityRepository extends JpaRepository<ExternalIdentity, UUID> {
    @Nullable
    ExternalIdentity findByProviderAndProviderSubject(@Param("provider") @NonNull AuthProvider provider, @Param("providerSubject") @NonNull String providerSubject);
}
