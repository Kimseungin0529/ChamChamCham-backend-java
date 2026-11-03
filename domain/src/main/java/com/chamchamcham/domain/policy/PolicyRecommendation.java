package com.chamchamcham.domain.policy;

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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "policy_recommendation")
public class PolicyRecommendation extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_program_id", nullable = false)
    private PolicyProgram policyProgram;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal score;

    @Column(nullable = false, length = 1000)
    private String reason;

    protected PolicyRecommendation() {
    }

    public PolicyRecommendation(Member member, PolicyProgram policyProgram, BigDecimal score, String reason) {
        this(null, member, policyProgram, score, reason);
    }

    public PolicyRecommendation(@Nullable UUID id, Member member, PolicyProgram policyProgram, BigDecimal score, String reason) {
        this.id = id;
        this.member = Objects.requireNonNull(member, "member");
        this.policyProgram = Objects.requireNonNull(policyProgram, "policyProgram");
        this.score = Objects.requireNonNull(score, "score");
        this.reason = Objects.requireNonNull(reason, "reason");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public PolicyProgram getPolicyProgram() {
        return policyProgram;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getReason() {
        return reason;
    }
}
