package com.chamchamcham.domain.community;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.media.UploadedMedia;
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
        name = "community_post_media",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_community_post_media_uploaded_media", columnNames = {"uploaded_media_id"})
    }
)
public class CommunityPostMedia extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_media_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UploadedMedia uploadedMedia;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected CommunityPostMedia() {}

    public CommunityPostMedia(@Nullable UUID id, CommunityPost post, UploadedMedia uploadedMedia, int displayOrder) {
        Objects.requireNonNull(post, "post");
        Objects.requireNonNull(uploadedMedia, "uploadedMedia");
        this.id = id;
        this.post = post;
        this.uploadedMedia = uploadedMedia;
        this.displayOrder = displayOrder;
    }

    @Nullable
    public UUID getId() { return id; }
    public CommunityPost getPost() { return post; }
    public UploadedMedia getUploadedMedia() { return uploadedMedia; }
    public int getDisplayOrder() { return displayOrder; }

    public CommunityPostMedia(CommunityPost post, UploadedMedia uploadedMedia, int displayOrder) {
        this(null, post, uploadedMedia, displayOrder);
    }

}
