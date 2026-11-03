package com.chamchamcham.domain.crop;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.farm.Farm;
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

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "member_crop")
public class MemberCrop extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "farm_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Farm farm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "crop_id", nullable = false)
    private Crop crop;

    protected MemberCrop() {
    }

    public MemberCrop(
        Member member,
        Farm farm,
        Crop crop
    ) {
        this(null, member, farm, crop);
    }

    public MemberCrop(
        @Nullable UUID id,
        Member member,
        Farm farm,
        Crop crop
    ) {
        this.id = id;
        this.member = Objects.requireNonNull(member, "member");
        this.farm = Objects.requireNonNull(farm, "farm");
        this.crop = Objects.requireNonNull(crop, "crop");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public Farm getFarm() {
        return farm;
    }

    public Crop getCrop() {
        return crop;
    }
}
