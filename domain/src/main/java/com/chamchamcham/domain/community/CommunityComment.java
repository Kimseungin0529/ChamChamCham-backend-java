package com.chamchamcham.domain.community;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.media.UploadedMedia;
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
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "community_comment")
public class CommunityComment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityComment parentComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member author;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private UploadedMedia media;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    protected CommunityComment() {}

    public CommunityComment(
            @Nullable UUID id,
            CommunityPost post,
            @Nullable CommunityComment parentComment,
            Member author,
            String body,
            @Nullable UploadedMedia media,
            boolean isDeleted
    ) {
        Objects.requireNonNull(post, "post");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(body, "body");
        this.id = id;
        this.post = post;
        this.parentComment = parentComment;
        this.author = author;
        this.body = body;
        this.media = media;
        this.isDeleted = isDeleted;
    }

    @Nullable
    public UUID getId() { return id; }
    public CommunityPost getPost() { return post; }
    @Nullable
    public CommunityComment getParentComment() { return parentComment; }
    public Member getAuthor() { return author; }
    public String getBody() { return body; }
    @Nullable
    public UploadedMedia getMedia() { return media; }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }

    public CommunityComment(CommunityPost post, Member author, String body) {
        this(null, post, null, author, body, null, false);
    }

    public CommunityComment(@Nullable UUID id, CommunityPost post, Member author, String body) {
        this(id, post, null, author, body, null, false);
    }

    public void softDelete() { isDeleted = true; }
}
