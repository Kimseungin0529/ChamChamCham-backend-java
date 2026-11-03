package com.chamchamcham.domain.notification;

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

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "notification_preference",
    uniqueConstraints = {
    @UniqueConstraint(
            name = "uk_notification_preference_member_channel_topic",
            columnNames = {"member_id", "channel", "topic"}
        )
    }
)
public class NotificationPreference extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;

    @Column(nullable = false, length = 32)
    private String channel;

    @Column(nullable = false, length = 64)
    private String topic;

    @Column(nullable = false)
    private boolean enabled;

    protected NotificationPreference() {
    }

    public NotificationPreference(
        Member member,
        String channel,
        String topic
    ) {
        this(null, member, channel, topic, true);
    }

    public NotificationPreference(
        @Nullable UUID id,
        Member member,
        String channel,
        String topic
    ) {
        this(id, member, channel, topic, true);
    }

    public NotificationPreference(
        @Nullable UUID id,
        Member member,
        String channel,
        String topic,
        boolean enabled
    ) {
        this.id = id;
        this.member = Objects.requireNonNull(member, "member");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.topic = Objects.requireNonNull(topic, "topic");
        this.enabled = enabled;
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public String getChannel() {
        return channel;
    }

    public String getTopic() {
        return topic;
    }

    public boolean getEnabled() {
        return enabled;
    }
}
