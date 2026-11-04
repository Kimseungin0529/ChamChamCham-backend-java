package com.chamchamcham.application.coaching.chat;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.farming.FarmingRecordRepository;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public record CoachingContext(
    @NonNull String text
) {
    public CoachingContext {
        Objects.requireNonNull(text, "text");
    }
    @NonNull public String getText() { return text; }
}
