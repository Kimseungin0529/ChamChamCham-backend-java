package com.chamchamcham.domain.legal;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "member_consent",
    uniqueConstraints = {
    @UniqueConstraint(
            name = "uk_member_consent_member_document",
            columnNames = {"member_id", "legal_document_id"}
        )
    }
)
public class MemberConsent extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_document_id", nullable = false)
    private LegalDocument legalDocument;

    @Column(nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private LocalDateTime agreedAt;

    protected MemberConsent() {
    }

    public MemberConsent(
        Member member,
        LegalDocument legalDocument,
        boolean agreed,
        LocalDateTime agreedAt
    ) {
        this(null, member, legalDocument, agreed, agreedAt);
    }

    public MemberConsent(
        @Nullable UUID id,
        Member member,
        LegalDocument legalDocument,
        boolean agreed,
        LocalDateTime agreedAt
    ) {
        this.id = id;
        this.member = Objects.requireNonNull(member, "member");
        this.legalDocument = Objects.requireNonNull(legalDocument, "legalDocument");
        this.agreed = agreed;
        this.agreedAt = Objects.requireNonNull(agreedAt, "agreedAt");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public LegalDocument getLegalDocument() {
        return legalDocument;
    }

    public boolean getAgreed() {
        return agreed;
    }

    public LocalDateTime getAgreedAt() {
        return agreedAt;
    }
}
