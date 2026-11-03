package com.chamchamcham.domain.member;

import com.chamchamcham.domain.common.BaseTimeEntity;
import com.chamchamcham.domain.media.UploadedMedia;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.lang.Nullable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "member")
public class Member extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = true, unique = true)
    private String email;

    @Column(length = 32)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MemberStatus status;

    @Column(length = 64)
    private String name;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(length = 64)
    private String nickname;

    @Column(name = "experience_level")
    private Integer experienceLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "management_type", length = 64)
    private ManagementType managementType;

    @Column(nullable = true)
    private String passwordHash;

    @Column(nullable = false)
    private String role;

    @Column(name = "withdrawn_at")
    private LocalDateTime withdrawnAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_media_id")
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private UploadedMedia profileMedia;

    protected Member() {
    }

    public Member(
        @Nullable String email,
        @Nullable String passwordHash
    ) {
        this(null, email, null, MemberStatus.ACTIVE, null, null, null, null, null, passwordHash, "ROLE_USER", null, null);
    }

    public Member(
        @Nullable UUID id,
        @Nullable String email,
        @Nullable String passwordHash
    ) {
        this(id, email, null, MemberStatus.ACTIVE, null, null, null, null, null, passwordHash, "ROLE_USER", null, null);
    }

    public Member(
        @Nullable UUID id,
        @Nullable String email,
        @Nullable String phone,
        MemberStatus status,
        @Nullable String name,
        @Nullable LocalDate birthDate,
        @Nullable String nickname,
        @Nullable Integer experienceLevel,
        @Nullable ManagementType managementType,
        @Nullable String passwordHash,
        String role,
        @Nullable LocalDateTime withdrawnAt,
        @Nullable UploadedMedia profileMedia
    ) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.status = Objects.requireNonNull(status, "status");
        this.name = name;
        this.birthDate = birthDate;
        this.nickname = nickname;
        this.experienceLevel = experienceLevel;
        this.managementType = managementType;
        this.passwordHash = passwordHash;
        this.role = Objects.requireNonNull(role, "role");
        this.withdrawnAt = withdrawnAt;
        this.profileMedia = profileMedia;
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    @Nullable
    public String getEmail() {
        return email;
    }

    @Nullable
    public String getPhone() {
        return phone;
    }

    public void setPhone(@Nullable String phone) {
        this.phone = phone;
    }

    public MemberStatus getStatus() {
        return status;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    @Nullable
    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(@Nullable LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    @Nullable
    public String getNickname() {
        return nickname;
    }

    public void setNickname(@Nullable String nickname) {
        this.nickname = nickname;
    }

    @Nullable
    public Integer getExperienceLevel() {
        return experienceLevel;
    }

    public void setExperienceLevel(@Nullable Integer experienceLevel) {
        this.experienceLevel = experienceLevel;
    }

    @Nullable
    public ManagementType getManagementType() {
        return managementType;
    }

    public void setManagementType(@Nullable ManagementType managementType) {
        this.managementType = managementType;
    }

    @Nullable
    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRole() {
        return role;
    }

    @Nullable
    public LocalDateTime getWithdrawnAt() {
        return withdrawnAt;
    }

    @Nullable
    public UploadedMedia getProfileMedia() {
        return profileMedia;
    }

    public void setProfileMedia(@Nullable UploadedMedia profileMedia) {
        this.profileMedia = profileMedia;
    }

    public void completeOnboarding(@Nullable String name, @Nullable String phone, @Nullable LocalDate birthDate,
        @Nullable String nickname, int experienceLevel, ManagementType managementType) {
        Objects.requireNonNull(managementType, "managementType");
        this.name = name;
        this.phone = phone;
        this.birthDate = birthDate;
        this.nickname = nickname;
        this.experienceLevel = experienceLevel;
        this.managementType = managementType;
    }

    public void prefillProfile(@Nullable String name, @Nullable String phone, @Nullable LocalDate birthDate) {
        if (isNullOrBlank(this.name) && !isNullOrBlank(name)) this.name = name;
        if (isNullOrBlank(this.phone) && !isNullOrBlank(phone)) this.phone = phone;
        if (this.birthDate == null && birthDate != null) this.birthDate = birthDate;
    }

    private boolean isNullOrBlank(@Nullable String value) {
        return value == null || value.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }

    public void updateProfileMedia(@Nullable UploadedMedia media) {
        this.profileMedia = media;
    }
}
