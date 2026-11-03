package com.chamchamcham.domain.member;

import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "external_identity",
    uniqueConstraints = {
    @UniqueConstraint(
            name = "uk_external_identity_provider_subject",
            columnNames = {"provider", "provider_subject"}
        )
    }
)
public class ExternalIdentity extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AuthProvider provider;

    @Column(name = "provider_subject", nullable = false, length = 128)
    private String providerSubject;

    protected ExternalIdentity() {
    }

    public ExternalIdentity(
        Member member,
        AuthProvider provider,
        String providerSubject
    ) {
        this(null, member, provider, providerSubject);
    }

    public ExternalIdentity(
        @Nullable UUID id,
        Member member,
        AuthProvider provider,
        String providerSubject
    ) {
        this.id = id;
        this.member = Objects.requireNonNull(member, "member");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.providerSubject = Objects.requireNonNull(providerSubject, "providerSubject");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public AuthProvider getProvider() {
        return provider;
    }

    public String getProviderSubject() {
        return providerSubject;
    }

}
