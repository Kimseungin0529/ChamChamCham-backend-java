package com.chamchamcham.domain.media;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.member.Member;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "uploaded_media")
public class UploadedMedia extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member owner;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 32)
    private UploadedMediaType mediaType;

    @Enumerated(EnumType.STRING)
    @Column(name = "usage_type", nullable = false, length = 32)
    private UploadedMediaUsageType usageType;

    @Column(name = "file_url", nullable = false, length = 2048)
    private String fileUrl;

    @Column(name = "cloudinary_public_id", nullable = false, length = 255)
    private String cloudinaryPublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UploadedMediaStatus status;

    protected UploadedMedia() {
    }

    public UploadedMedia(
        Member owner,
        UploadedMediaType mediaType,
        UploadedMediaUsageType usageType,
        String fileUrl,
        String cloudinaryPublicId
    ) {
        this(null, owner, mediaType, usageType, fileUrl, cloudinaryPublicId, UploadedMediaStatus.TEMP);
    }

    public UploadedMedia(
        @Nullable UUID id,
        Member owner,
        UploadedMediaType mediaType,
        UploadedMediaUsageType usageType,
        String fileUrl,
        String cloudinaryPublicId
    ) {
        this(id, owner, mediaType, usageType, fileUrl, cloudinaryPublicId, UploadedMediaStatus.TEMP);
    }

    public UploadedMedia(
        @Nullable UUID id,
        Member owner,
        UploadedMediaType mediaType,
        UploadedMediaUsageType usageType,
        String fileUrl,
        String cloudinaryPublicId,
        UploadedMediaStatus status
    ) {
        this.id = id;
        this.owner = Objects.requireNonNull(owner, "owner");
        this.mediaType = Objects.requireNonNull(mediaType, "mediaType");
        this.usageType = Objects.requireNonNull(usageType, "usageType");
        this.fileUrl = Objects.requireNonNull(fileUrl, "fileUrl");
        this.cloudinaryPublicId = Objects.requireNonNull(cloudinaryPublicId, "cloudinaryPublicId");
        this.status = Objects.requireNonNull(status, "status");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Member getOwner() {
        return owner;
    }

    public UploadedMediaType getMediaType() {
        return mediaType;
    }

    public UploadedMediaUsageType getUsageType() {
        return usageType;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public String getCloudinaryPublicId() {
        return cloudinaryPublicId;
    }

    public UploadedMediaStatus getStatus() {
        return status;
    }

    public void setStatus(UploadedMediaStatus status) {
        this.status = Objects.requireNonNull(status, "status");
    }

    public void markAttached() {
        status = UploadedMediaStatus.ATTACHED;
    }

    public void markDeleted() {
        status = UploadedMediaStatus.DELETED;
    }

    public boolean isAttachable() {
        return status == UploadedMediaStatus.TEMP;
    }
}
