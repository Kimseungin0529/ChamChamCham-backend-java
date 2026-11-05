package com.chamchamcham.application.pesticide;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.Pesticide;
import com.chamchamcham.domain.pesticide.PesticideApplicationRepository;
import com.chamchamcham.domain.pesticide.PesticideQueryRepository;
import com.chamchamcham.domain.pesticide.PesticideRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class PesticideCatalogServiceTest {
    @Mock private PesticideRepository pesticideRepository;
    @Mock private PesticideQueryRepository pesticideQueryRepository;
    @Mock private PesticideApplicationRepository pesticideApplicationRepository;
    private PesticideCatalogService service;

    @BeforeEach
    void setUp() throws Exception {
        service = new PesticideCatalogService(pesticideRepository, pesticideQueryRepository, pesticideApplicationRepository, new OpaqueCursorCodec());
    }

    @Test
    @DisplayName("search returns no next cursor when results fit within the page")
    void search_returns_no_next_cursor_when_results_fit_within_the_page() throws Exception {
        var pesticide = new Pesticide(UUID.randomUUID(), "만코제브 수화제", "가가방");
        when(pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(null, null, 21))).thenReturn(java.util.Arrays.asList(pesticide));
        var result = service.search(null, null, 20);
        assertEquals(1, result.getItems().size());
        assertEquals("가가방", result.getItems().get(0).getBrandName());
        assertNull(result.getNextCursor());
    }

    @Test
    @DisplayName("search returns a next cursor when more rows exist than the page size")
    void search_returns_a_next_cursor_when_more_rows_exist_than_the_page_size() throws Exception {
        var pesticides = java.util.stream.IntStream.rangeClosed(1, 3).boxed().map(it -> new Pesticide(UUID.randomUUID(), "품목" + it + "", "상표" + it + "")).toList();
        when(pesticideQueryRepository.search(new PesticideQueryRepository.SearchCondition(null, null, 3))).thenReturn(pesticides);
        var result = service.search(null, null, 2);
        assertEquals(2, result.getItems().size());
        assertEquals(false, (result.getNextCursor() == null || result.getNextCursor().isBlank()));
    }

    @Test
    @DisplayName("search rejects a non-positive size")
    void search_rejects_a_non_positive_size() throws Exception {
        var exception = assertThrows(BusinessException.class, () -> service.search(null, null, 0));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    @Test
    @DisplayName("listPestsByPesticide throws when the pesticide does not exist")
    void listPestsByPesticide_throws_when_the_pesticide_does_not_exist() throws Exception {
        var pesticideId = UUID.randomUUID();
        when(pesticideRepository.existsById(pesticideId)).thenReturn(false);
        var exception = assertThrows(BusinessException.class, () -> service.listPestsByPesticide(pesticideId));
        assertEquals(ErrorCode.PESTICIDE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("listPestsByPesticide returns pests treatable by the pesticide")
    void listPestsByPesticide_returns_pests_treatable_by_the_pesticide() throws Exception {
        var pesticideId = UUID.randomUUID();
        when(pesticideRepository.existsById(pesticideId)).thenReturn(true);
        when(pesticideApplicationRepository.findDistinctPestsByPesticideId(pesticideId)).thenReturn(java.util.Arrays.asList(new Pest(UUID.randomUUID(), "역병")));
        var result = service.listPestsByPesticide(pesticideId);
        assertEquals(1, result.size());
        assertEquals("역병", result.get(0).getName());
    }
}
