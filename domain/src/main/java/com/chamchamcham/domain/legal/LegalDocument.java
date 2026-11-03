package com.chamchamcham.domain.legal;

import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "legal_document",
    uniqueConstraints = {
    @UniqueConstraint(
            name = "uk_legal_document_type_version",
            columnNames = {"document_type", "version"}
        )
    }
)
public class LegalDocument extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "document_type", nullable = false, length = 64)
    private String documentType;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 32)
    private String version;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    protected LegalDocument() {
    }

    public LegalDocument(
        String documentType,
        String title,
        String version,
        String body,
        LocalDateTime publishedAt
    ) {
        this(null, documentType, title, version, body, publishedAt);
    }

    public LegalDocument(
        @Nullable UUID id,
        String documentType,
        String title,
        String version,
        String body,
        LocalDateTime publishedAt
    ) {
        this.id = id;
        this.documentType = Objects.requireNonNull(documentType, "documentType");
        this.title = Objects.requireNonNull(title, "title");
        this.version = Objects.requireNonNull(version, "version");
        this.body = Objects.requireNonNull(body, "body");
        this.publishedAt = Objects.requireNonNull(publishedAt, "publishedAt");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public String getDocumentType() {
        return documentType;
    }

    public String getTitle() {
        return title;
    }

    public String getVersion() {
        return version;
    }

    public String getBody() {
        return body;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }
}
