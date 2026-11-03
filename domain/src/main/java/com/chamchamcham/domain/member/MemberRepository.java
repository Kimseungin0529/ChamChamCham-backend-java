package com.chamchamcham.domain.member;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.UUID;

public interface MemberRepository extends JpaRepository<Member, UUID> {
    @Nullable
    Member findByEmail(@Param("email") @NonNull String email);

    boolean existsByEmail(@Param("email") @NonNull String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Member m where m.id = :memberId")
    @Nullable
    Member findByIdForUpdate(@Param("memberId") @NonNull UUID memberId);

    @Modifying(clearAutomatically = true)
    @Query("delete from Member m where m.id = :memberId")
    int hardDeleteById(@Param("memberId") @NonNull UUID memberId);
}
