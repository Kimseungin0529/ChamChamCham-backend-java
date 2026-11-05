package com.chamchamcham.application.pesticide;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.pesticide.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class PesticideCatalogService {
    private final PesticideRepository pesticideRepository;
    private final PesticideQueryRepository pesticideQueryRepository;
    private final PesticideApplicationRepository pesticideApplicationRepository;
    private final OpaqueCursorCodec cursorCodec;
    public PesticideCatalogService(PesticideRepository pesticideRepository, PesticideQueryRepository pesticideQueryRepository,
        PesticideApplicationRepository pesticideApplicationRepository, OpaqueCursorCodec cursorCodec) {
        this.pesticideRepository = Objects.requireNonNull(pesticideRepository, "pesticideRepository");
        this.pesticideQueryRepository = Objects.requireNonNull(pesticideQueryRepository, "pesticideQueryRepository");
        this.pesticideApplicationRepository = Objects.requireNonNull(pesticideApplicationRepository, "pesticideApplicationRepository");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }
    public PesticideResult.Page search(String keyword, String cursor, int size) throws JsonProcessingException {
        if (size <= 0 || size == Integer.MAX_VALUE) throw new BusinessException(ErrorCode.INVALID_INPUT);
        var decodedCursor = decodeCursor(cursor);
        var rows = pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(keyword, decodedCursor, size + 1));
        var visibleRows = new ArrayList<>(rows.subList(0, Math.min(size, rows.size())));
        String nextCursor = null;
        if (rows.size() > size && !visibleRows.isEmpty()) nextCursor = encodeCursor(visibleRows.getLast());
        var items = new ArrayList<PesticideResult.Summary>(visibleRows.size());
        for (var row : visibleRows) items.add(PesticideResult.Summary.from(row));
        return new PesticideResult.Page(items, nextCursor);
    }
    public List<PesticideResult.PestSummary> listPestsByPesticide(UUID pesticideId) {
        Objects.requireNonNull(pesticideId, "pesticideId");
        if (!pesticideRepository.existsById(pesticideId)) throw new BusinessException(ErrorCode.PESTICIDE_NOT_FOUND);
        var result = new ArrayList<PesticideResult.PestSummary>();
        for (var pest : pesticideApplicationRepository.findDistinctPestsByPesticideId(pesticideId)) result.add(PesticideResult.PestSummary.from(pest));
        return result;
    }
    private PesticideQueryRepository.Cursor decodeCursor(String cursor) {
        if (cursor == null || trim(cursor).isEmpty()) return null;
        var payload = cursorCodec.decode(cursor, PesticideCursorPayload.class);
        return new PesticideQueryRepository.Cursor(payload.getBrandName(), payload.getId());
    }
    private String encodeCursor(Pesticide pesticide) throws JsonProcessingException {
        if (pesticide.getId() == null) throw new IllegalArgumentException("Persisted pesticide id is required");
        return cursorCodec.encode(new PesticideCursorPayload(pesticide.getBrandName(), pesticide.getId()));
    }
    private static String trim(String value) {
        int start = 0, end = value.length();
        while (start < end && whitespace(value.charAt(start))) start++;
        while (end > start && whitespace(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }
    private static boolean whitespace(char value) {
        return Character.isWhitespace(value) || Character.isSpaceChar(value);
    }
}
