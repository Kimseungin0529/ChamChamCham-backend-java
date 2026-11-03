package com.chamchamcham.domain.pesticide;

import com.chamchamcham.domain.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.lang.Nullable;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
    name = "pest",
    uniqueConstraints = {
    @UniqueConstraint(name = "uk_pest_name", columnNames = {"name"})
    }
)
public class Pest extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(nullable = false, length = 128)
    private String name;

    protected Pest() {
    }

    public Pest(
        String name
    ) {
        this(null, name);
    }

    public Pest(
        @Nullable UUID id,
        String name
    ) {
        this.id = id;
        this.name = Objects.requireNonNull(name, "name");
    }

    @Nullable
    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }
}
