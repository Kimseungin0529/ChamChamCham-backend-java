package com.chamchamcham.domain.community;

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
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

@Entity
@Table(
        name = "community_post_like",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_community_post_like_post_member", columnNames = {"post_id", "member_id"})
    }
)
public class CommunityPostLike extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    protected CommunityPostLike() {}

    public CommunityPostLike(@Nullable UUID id, CommunityPost post, Member member) {
        Objects.requireNonNull(post, "post");
        Objects.requireNonNull(member, "member");
        this.id = id;
        this.post = post;
        this.member = member;
    }

    @Nullable
    public UUID getId() { return id; }
    public CommunityPost getPost() { return post; }
    public Member getMember() { return member; }

    public CommunityPostLike(CommunityPost post, Member member) {
        this(null, post, member);
    }

}
