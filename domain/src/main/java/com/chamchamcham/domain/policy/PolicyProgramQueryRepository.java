package com.chamchamcham.domain.policy;

import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface PolicyProgramQueryRepository {
    List<PolicyProgram> search(SearchCondition condition);

    long count(@Nullable String keyword);

    record SearchCondition(@Nullable String keyword, @Nullable LocalDateTime cursorCreatedAt, @Nullable UUID cursorId, int size) {
    }
}
