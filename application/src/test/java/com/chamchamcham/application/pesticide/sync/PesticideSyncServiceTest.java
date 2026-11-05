package com.chamchamcham.application.pesticide.sync;

import com.chamchamcham.domain.pesticide.Pest;
import com.chamchamcham.domain.pesticide.PestRepository;
import com.chamchamcham.domain.pesticide.Pesticide;
import com.chamchamcham.domain.pesticide.PesticideApplication;
import com.chamchamcham.domain.pesticide.PesticideApplicationRepository;
import com.chamchamcham.domain.pesticide.PesticideRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.UUID;
import java.util.*;
import org.junit.jupiter.api.DisplayName;

@ExtendWith({MockitoExtension.class})
class PesticideSyncServiceTest {
    @Mock private PsisPesticideHttpTransport transport;
    @Mock private PesticideRepository pesticideRepository;
    @Mock private PestRepository pestRepository;
    @Mock private PesticideApplicationRepository pesticideApplicationRepository;
    private PesticideSyncService service;
    private final UUID pesticideId = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private final UUID pestId1 = UUID.fromString("00000000-0000-0000-0000-0000000000b1");
    private final UUID pestId2 = UUID.fromString("00000000-0000-0000-0000-0000000000b2");
    private final Pesticide savedPesticide = new Pesticide(pesticideId, "만코제브 수화제", "가가방");
    private final Pest savedPest1 = new Pest(pestId1, "역병");
    private final Pest savedPest2 = new Pest(pestId2, "탄저병");

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(pesticideRepository.save(any(Pesticide.class))).thenReturn(savedPesticide);
        lenient().when(pestRepository.save(any(Pest.class))).thenAnswer(invocation -> {
        return ((Pest) invocation.getArgument(0)).getName().equals("역병") ? savedPest1 : savedPest2;
        });
        lenient().when(pesticideApplicationRepository.save(any(PesticideApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new PesticideSyncService(transport, new PsisPesticideResponseParser(), new PsisPesticideRowMapper(), pesticideRepository, pestRepository, pesticideApplicationRepository, new TransactionTemplate(new TestTransactionManager()));
    }

    @Test
    @DisplayName("dedupes the same pesticide across rows within one page")
    void dedupes_the_same_pesticide_across_rows_within_one_page() throws Exception {
        when(pesticideRepository.findByItemNameAndBrandName("만코제브 수화제", "가가방")).thenReturn(null, savedPesticide);
        when(pestRepository.findByName("역병")).thenReturn(null);
        when(pestRepository.findByName("탄저병")).thenReturn(null);
        when(pesticideApplicationRepository.findByPesticide_IdAndPest_IdAndCropName(pesticideId, pestId1, "감자")).thenReturn(null);
        when(pesticideApplicationRepository.findByPesticide_IdAndPest_IdAndCropName(pesticideId, pestId2, "강낭콩")).thenReturn(null);
        when(transport.get(anyMap())).thenReturn(twoRowPageXml());
        var result = service.sync(100);
        assertEquals(2, result.getFetchedRowCount());
        assertEquals(2, result.getCreatedApplicationCount());
    }

    @Test
    @DisplayName("stops paginating once a page returns fewer rows than the page size")
    void stops_paginating_once_a_page_returns_fewer_rows_than_the_page_size() throws Exception {
        when(pesticideRepository.findByItemNameAndBrandName("만코제브 수화제", "가가방")).thenReturn(null, savedPesticide);
        when(pestRepository.findByName("역병")).thenReturn(null);
        when(pestRepository.findByName("탄저병")).thenReturn(null);
        when(pesticideApplicationRepository.findByPesticide_IdAndPest_IdAndCropName(pesticideId, pestId1, "감자")).thenReturn(null);
        when(pesticideApplicationRepository.findByPesticide_IdAndPest_IdAndCropName(pesticideId, pestId2, "강낭콩")).thenReturn(null);
        when(transport.get(anyMap())).thenReturn(twoRowPageXml());
        var result = service.sync(100);
        assertEquals(1, result.getPageCount());
    }

    @Test
    @DisplayName("does not duplicate an application already synced in a prior run")
    void does_not_duplicate_an_application_already_synced_in_a_prior_run() throws Exception {
        when(pesticideRepository.findByItemNameAndBrandName("만코제브 수화제", "가가방")).thenReturn(savedPesticide);
        when(pestRepository.findByName("역병")).thenReturn(savedPest1);
        when(pesticideApplicationRepository.findByPesticide_IdAndPest_IdAndCropName(pesticideId, pestId1, "감자")).thenReturn(null, new PesticideApplication(savedPesticide, savedPest1, "감자"));
        when(transport.get(anyMap())).thenReturn(oneRowPageXml());
        service.sync(100);
        var secondResult = service.sync(100);
        assertEquals(0, secondResult.getCreatedApplicationCount());
    }
    private String oneRowPageXml() {
        return "<response><body><items>\n  <item>\n    <prdtNm>만코제브 수화제</prdtNm>\n    <trdmrkNm>가가방</trdmrkNm>\n    <cropNm>감자</cropNm>\n    <aplyPestNm>역병</aplyPestNm>\n    <dltnMag>500배</dltnMag>\n  </item>\n</items></body></response>";
    }
    private String twoRowPageXml() {
        return "<response><body><items>\n  <item>\n    <prdtNm>만코제브 수화제</prdtNm>\n    <trdmrkNm>가가방</trdmrkNm>\n    <cropNm>감자</cropNm>\n    <aplyPestNm>역병</aplyPestNm>\n    <dltnMag>500배</dltnMag>\n  </item>\n  <item>\n    <prdtNm>만코제브 수화제</prdtNm>\n    <trdmrkNm>가가방</trdmrkNm>\n    <cropNm>강낭콩</cropNm>\n    <aplyPestNm>탄저병</aplyPestNm>\n    <dltnMag>500배</dltnMag>\n  </item>\n</items></body></response>";
    }
    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, org.springframework.transaction.TransactionDefinition definition) {}
        @Override protected void doCommit(DefaultTransactionStatus status) {}
        @Override protected void doRollback(DefaultTransactionStatus status) {}
    }
}
