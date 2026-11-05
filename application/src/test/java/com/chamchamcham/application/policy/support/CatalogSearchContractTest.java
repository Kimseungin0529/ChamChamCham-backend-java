package com.chamchamcham.application.policy.support;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.pesticide.*;
import com.chamchamcham.application.pesticide.sync.JavaNetPsisPesticideHttpTransport;
import com.chamchamcham.application.policy.recommendation.*;
import com.chamchamcham.application.search.*;
import com.chamchamcham.domain.pesticide.*;
import com.chamchamcham.domain.crop.CropRepository;
import com.chamchamcham.domain.policy.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CatalogSearchContractTest {
    @Test
    void textListCodecKeepsOrderAndOriginalInputSizeSetMutability() throws Exception {
        var codec = new TextListJsonCodec();
        assertThat(codec.encode(Arrays.asList("\u00a0약제\u00a0", "", "약제", "정책"))).isEqualTo("[\"약제\",\"정책\"]");
        var single = codec.decode("[\"약제\"]");
        assertThatThrownBy(() -> single.add("정책")).isInstanceOf(UnsupportedOperationException.class);
        var duplicate = codec.decode("[\"약제\",\"약제\"]");
        assertThat(duplicate.add("정책")).isTrue();
        assertThat(duplicate).containsExactly("약제", "정책");
    }

    @Test
    void textListCodecKeepsNullElementsAndMalformedFailure() throws Exception {
        var codec = new TextListJsonCodec();
        assertThat(codec.decode("[null,\"약제\"]")).containsExactly(null, "약제");
        assertThat(codec.decode("\u00a0")).isEmpty();
        assertThatThrownBy(() -> codec.decode("{bad")).isInstanceOf(IllegalArgumentException.class).hasMessage("Malformed text list JSON").hasCauseInstanceOf(Exception.class);
    }

    @Test
    void modelsRetainCollectionAliasesAndValueEquality() throws Exception {
        var items = new ArrayList<PesticideResult.Summary>();
        var page = new PesticideResult.Page(items, null);
        assertThat(page).isEqualTo(new PesticideResult.Page(new ArrayList<>(), null));
        items.add(new PesticideResult.Summary(UUID.randomUUID(), "품목", "상표", null, null));
        assertThat(page.getItems()).isSameAs(items).hasSize(1);
    }

    @Test
    void suggestionTrimUsesUnicodeWhitespaceAndCaseNormalizationIsLocaleIndependent() throws Exception {
        var crops = mock(CropRepository.class);
        var pesticides = mock(PesticideQueryRepository.class);
        var policies = mock(PolicyProgramRepository.class);
        var service = new SearchSuggestionService(crops, pesticides, policies);
        assertThat(service.suggest("\u00a0\u2007\u202f").getKeywords()).isEmpty();
        verifyNoInteractions(crops, pesticides, policies);
    }

    @Test
    void catalogRejectsOnlyNonPositiveAndOverflowPageSizes() throws Exception {
        var repository = mock(PesticideRepository.class);
        var query = mock(PesticideQueryRepository.class);
        var applications = mock(PesticideApplicationRepository.class);
        var service = new PesticideCatalogService(repository, query, applications, new OpaqueCursorCodec());
        for (int size : new int[]{-1, 0, Integer.MAX_VALUE}) {
            assertThatThrownBy(() -> service.search(null, null, size)).isInstanceOfSatisfying(BusinessException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }
        when(query.search(new PesticideQueryRepository.SearchCondition(null, null, 52))).thenReturn(List.of());
        assertThat(service.search(null, "\u00a0", 51).getItems()).isEmpty();
        verify(query).search(new PesticideQueryRepository.SearchCondition(null, null, 52));
    }

    @Test
    void pesticideTransportKeepsLazyInitializationAndConfigurationFailureOrder() throws Exception {
        var transport = new JavaNetPsisPesticideHttpTransport("", "", -1);
        assertThatThrownBy(() -> transport.get(Map.of())).isInstanceOf(IllegalStateException.class).hasMessage("psis.pesticide.base-url is not configured");
    }

    @Test
    void cursorRequiredReferencesFailWhileNullableOrderingFieldsRemainOptional() throws Exception {
        var codec = new OpaqueCursorCodec();
        var missing = codec.encode(Map.of("sourceYear", "2026", "sort", "LATEST", "id", UUID.randomUUID()));
        assertThatThrownBy(() -> codec.decode(missing, PolicyRecommendationCursorPayload.class)).isInstanceOfSatisfying(BusinessException.class,
            e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_CURSOR));
        var cursor = codec.encode(Map.of("source", "NONGUP_EZ", "sourceYear", "2026", "sort", "LATEST", "id", UUID.randomUUID()));
        assertThat(codec.decode(cursor, PolicyRecommendationCursorPayload.class).getApplyStartsOn()).isNull();
    }

    @Test
    void periodTrimsUnicodeAndTruncatesUtf16Units() throws Exception {
        var generator = new PolicyCardTextGenerator();
        assertThat(generator.periodLabel(null, null, "\u00a0접수기관문의\u00a0")).isEqualTo("접수기관문의");
        var notice = "가".repeat(18) + "😀";
        var result = generator.periodLabel(null, null, notice);
        assertThat(result).hasSize(19);
        assertThat(Character.isHighSurrogate(result.charAt(18))).isTrue();
        assertThat(generator.periodLabel(LocalDate.of(2026, 3, 25), LocalDate.of(2026, 6, 30), "무시")).isEqualTo("2026.03.25~06.30");
    }
    @Test
    void emptyResultsRejectClearAndAbsentRemoval() {
        var codec = new TextListJsonCodec();
        assertThatThrownBy(() -> codec.decode(null).clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> codec.decode("[]").remove("없음")).isInstanceOf(UnsupportedOperationException.class);
        var service = new SearchSuggestionService(mock(CropRepository.class), mock(PesticideQueryRepository.class), mock(PolicyProgramRepository.class));
        assertThatThrownBy(() -> service.suggest(null).getKeywords().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> service.suggest(" ").getKeywords().remove("없음")).isInstanceOf(UnsupportedOperationException.class);
    }
    @Test
    void nullStoredTagsAreIgnoredByMatchingRequirements() {
        var scorer = new PolicyRecommendationScorer(new PolicyRegionMatcher());
        var profile = new PolicyMemberProfile(null, null, com.chamchamcham.domain.member.ManagementType.AGRICULTURAL_INDIVIDUAL, Set.of(), Set.of(), Set.of());
        var targets = new LinkedHashSet<>(Arrays.asList(null, "REGISTERED_FARMER"));
        var crops = Collections.<String>singleton(null);
        var score = scorer.score(profile, targets, crops, Set.of("전국"), false, LocalDate.of(2026, 4, 1));
        assertThat(score.getEligible()).isTrue();
        assertThat(score.getScore()).isEqualByComparingTo("55");
        var generic = scorer.score(profile, Collections.singleton(null), crops, Set.of("전국"), false, LocalDate.of(2026, 4, 1));
        assertThat(generic.getEligible()).isFalse();
        assertThat(generic.getScore()).isEqualByComparingTo("0");
    }

    @Test
    void catalogPageRetainsSelectedRowsWhenSourceListChangesDuringCursorEncoding() throws Exception {
        var query = mock(PesticideQueryRepository.class);
        var codec = mock(OpaqueCursorCodec.class);
        var rows = new ArrayList<Pesticide>();
        rows.add(new Pesticide(UUID.randomUUID(), "품목1", "상표1"));
        rows.add(new Pesticide(UUID.randomUUID(), "품목2", "상표2"));
        rows.add(new Pesticide(UUID.randomUUID(), "품목3", "상표3"));
        when(query.search(new PesticideQueryRepository.SearchCondition(null, null, 3))).thenReturn(rows);
        when(codec.encode(any())).thenAnswer(invocation -> { rows.clear(); return "next"; });
        var service = new PesticideCatalogService(mock(PesticideRepository.class), query, mock(PesticideApplicationRepository.class), codec);
        var page = service.search(null, null, 2);
        assertThat(page.getItems()).extracting(PesticideResult.Summary::getBrandName).containsExactly("상표1", "상표2");
        assertThat(page.getNextCursor()).isEqualTo("next");
    }
}
