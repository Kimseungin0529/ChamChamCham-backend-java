package com.chamchamcham.domain.community;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.farming.FarmingRecord;
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
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

@Entity
@Table(name = "community_post")
public class CommunityPost extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "farming_record_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private FarmingRecord farmingRecord;

    @Enumerated(EnumType.STRING)
    @Column(name = "post_type", nullable = false, length = 32)
    private CommunityPostType postType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted;

    protected CommunityPost() {}

    public CommunityPost(
            @Nullable UUID id,
            Member author,
            Crop crop,
            @Nullable FarmingRecord farmingRecord,
            CommunityPostType postType,
            String title,
            String body,
            boolean isDeleted
    ) {
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(crop, "crop");
        Objects.requireNonNull(postType, "postType");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        this.id = id;
        this.author = author;
        this.crop = crop;
        this.farmingRecord = farmingRecord;
        this.postType = postType;
        this.title = title;
        this.body = body;
        this.isDeleted = isDeleted;
    }

    @Nullable
    public UUID getId() { return id; }
    public Member getAuthor() { return author; }
    public Crop getCrop() { return crop; }
    public void setCrop(Crop crop) { this.crop = Objects.requireNonNull(crop, "crop"); }
    @Nullable
    public FarmingRecord getFarmingRecord() { return farmingRecord; }
    public void setFarmingRecord(@Nullable FarmingRecord farmingRecord) { this.farmingRecord = farmingRecord; }
    public CommunityPostType getPostType() { return postType; }
    public void setPostType(CommunityPostType postType) { this.postType = Objects.requireNonNull(postType, "postType"); }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = Objects.requireNonNull(title, "title"); }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = Objects.requireNonNull(body, "body"); }
    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean isDeleted) { this.isDeleted = isDeleted; }

    public CommunityPost(Member author, Crop crop, CommunityPostType postType, String title, String body) {
        this(null, author, crop, null, postType, title, body, false);
    }

    public CommunityPost(@Nullable UUID id, Member author, Crop crop, CommunityPostType postType, String title, String body) {
        this(id, author, crop, null, postType, title, body, false);
    }

    public void update(Crop crop, @Nullable FarmingRecord farmingRecord, CommunityPostType postType, String title, String body) {
        Objects.requireNonNull(crop, "crop");
        Objects.requireNonNull(postType, "postType");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(body, "body");
        this.crop = crop;
        this.farmingRecord = farmingRecord;
        this.postType = postType;
        this.title = title;
        this.body = body;
    }

    public void softDelete() { isDeleted = true; }
}
