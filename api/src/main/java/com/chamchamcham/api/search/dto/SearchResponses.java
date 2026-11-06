package com.chamchamcham.api.search.dto;

import com.chamchamcham.api.community.dto.CommunityResponses;
import com.chamchamcham.api.farming.dto.FarmingRecordResponses;
import com.chamchamcham.application.search.SearchResult;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class SearchResponses {
    private SearchResponses() {}

    @Schema(description = "정책 정보 검색 결과 항목")
    public record SearchPolicyItemResponse(
        @Schema(description = "정책 프로그램 ID")
        @NonNull UUID id,
        @Schema(description = "정책 제목", example = "청년 농업인 영농정착 지원사업")
        @NonNull String title,
        @Schema(description = "기관명", example = "농림축산식품부")
        @NonNull String agencyName,
        @Schema(description = "대상자 요약", example = "만 40세 미만 청년 농업인")
        @NonNull String eligibilitySummary,
        @Schema(description = "지원내용 요약", example = "월 최대 110만원 지원")
        @NonNull String benefitSummary,
        @Schema(description = "접수기간 표시 문구", example = "2026-06-01 ~ 2026-06-30")
        @NonNull String applicationPeriodLabel,
        @Schema(description = "접수 시작일. 미확정이면 null")
        @Nullable LocalDate applyStartsOn,
        @Schema(description = "접수 종료일. 상시·미확정이면 null")
        @Nullable LocalDate applyEndsOn,
        @Schema(description = "정책 사이트 URL", example = "https://www.nongupez.kr/policy/1234")
        @Nullable String sourceUrl,
        @Schema(description = "정책 등록 시각 (최신순 정렬 기준)")
        @NonNull LocalDateTime createdAt
    ) {
        public SearchPolicyItemResponse {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
            Objects.requireNonNull(agencyName, "agencyName");
            Objects.requireNonNull(eligibilitySummary, "eligibilitySummary");
            Objects.requireNonNull(benefitSummary, "benefitSummary");
            Objects.requireNonNull(applicationPeriodLabel, "applicationPeriodLabel");
            Objects.requireNonNull(createdAt, "createdAt");
        }

        @NonNull public UUID getId() { return id; }
        @NonNull public String getTitle() { return title; }
        @NonNull public String getAgencyName() { return agencyName; }
        @NonNull public String getEligibilitySummary() { return eligibilitySummary; }
        @NonNull public String getBenefitSummary() { return benefitSummary; }
        @NonNull public String getApplicationPeriodLabel() { return applicationPeriodLabel; }
        @Nullable public LocalDate getApplyStartsOn() { return applyStartsOn; }
        @Nullable public LocalDate getApplyEndsOn() { return applyEndsOn; }
        @Nullable public String getSourceUrl() { return sourceUrl; }
        @NonNull public LocalDateTime getCreatedAt() { return createdAt; }

        public static SearchPolicyItemResponse from(SearchResult.PolicyItem item) {
            return new SearchPolicyItemResponse(
                item.getId(),
                item.getTitle(),
                item.getAgencyName(),
                item.getEligibilitySummary(),
                item.getBenefitSummary(),
                item.getApplicationPeriodLabel(),
                item.getApplyStartsOn(),
                item.getApplyEndsOn(),
                item.getSourceUrl(),
                item.getCreatedAt()
            );
        }
    }

    @Schema(description = "전체 검색의 나의 일지 섹션 (최신순 최대 3건)")
    public record SearchRecordSectionResponse(
        @Schema(description = "일지 미리보기 목록. 기록 목록 조회 응답과 동일한 형태")
        @NonNull List<FarmingRecordResponses.RecordSummaryResponse> items,
        @Schema(description = "검색 조건에 맞는 일지 전체 개수", example = "12")
        long totalCount
    ) {
        public SearchRecordSectionResponse {
            Objects.requireNonNull(items, "items");
        }

        @NonNull public List<FarmingRecordResponses.RecordSummaryResponse> getItems() { return items; }
        public long getTotalCount() { return totalCount; }

        public static SearchRecordSectionResponse from(SearchResult.RecordPage page) {
            return new SearchRecordSectionResponse(
                page.getItems().stream().map(FarmingRecordResponses.RecordSummaryResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                page.getTotalCount()
            );
        }
    }

    @Schema(description = "전체 검색의 정책 정보 섹션 (최신순 최대 3건)")
    public record SearchPolicySectionResponse(
        @Schema(description = "정책 미리보기 목록")
        @NonNull List<SearchPolicyItemResponse> items,
        @Schema(description = "검색 조건에 맞는 정책 전체 개수", example = "7")
        long totalCount
    ) {
        public SearchPolicySectionResponse {
            Objects.requireNonNull(items, "items");
        }

        @NonNull public List<SearchPolicyItemResponse> getItems() { return items; }
        public long getTotalCount() { return totalCount; }

        public static SearchPolicySectionResponse from(SearchResult.PolicyPage page) {
            return new SearchPolicySectionResponse(
                page.getItems().stream().map(SearchPolicyItemResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                page.getTotalCount()
            );
        }
    }

    @Schema(description = "전체 검색의 게시물 섹션 (최신순 최대 3건)")
    public record SearchPostSectionResponse(
        @Schema(description = "게시물 미리보기 목록. 게시물 목록 조회 응답과 동일한 형태")
        @NonNull List<CommunityResponses.PostSummaryResponse> items,
        @Schema(description = "검색 조건에 맞는 게시물 전체 개수", example = "5")
        long totalCount
    ) {
        public SearchPostSectionResponse {
            Objects.requireNonNull(items, "items");
        }

        @NonNull public List<CommunityResponses.PostSummaryResponse> getItems() { return items; }
        public long getTotalCount() { return totalCount; }

        public static SearchPostSectionResponse from(SearchResult.PostPage page) {
            return new SearchPostSectionResponse(
                page.getItems().stream().map(CommunityResponses.PostSummaryResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                page.getTotalCount()
            );
        }
    }

    @Schema(description = "전체 검색 응답. 나의 일지·정책 정보·게시물 섹션을 각각 최신순 최대 3건씩 담습니다")
    public record SearchAllResponse(
        @Schema(description = "나의 일지 섹션")
        @NonNull SearchRecordSectionResponse records,
        @Schema(description = "정책 정보 섹션")
        @NonNull SearchPolicySectionResponse policies,
        @Schema(description = "게시물 섹션")
        @NonNull SearchPostSectionResponse posts
    ) {
        public SearchAllResponse {
            Objects.requireNonNull(records, "records");
            Objects.requireNonNull(policies, "policies");
            Objects.requireNonNull(posts, "posts");
        }

        @NonNull public SearchRecordSectionResponse getRecords() { return records; }
        @NonNull public SearchPolicySectionResponse getPolicies() { return policies; }
        @NonNull public SearchPostSectionResponse getPosts() { return posts; }

        public static SearchAllResponse from(SearchResult.All result) {
            return new SearchAllResponse(
                SearchRecordSectionResponse.from(result.getRecords()),
                SearchPolicySectionResponse.from(result.getPolicies()),
                SearchPostSectionResponse.from(result.getPosts())
            );
        }
    }

    @Schema(description = "나의 일지 검색 응답 (커서 기반 무한스크롤)")
    public record SearchRecordPageResponse(
        @Schema(description = "일지 목록. 기록 목록 조회 응답과 동일한 형태")
        @NonNull List<FarmingRecordResponses.RecordSummaryResponse> items,
        @Schema(description = "다음 페이지 커서. 마지막 페이지면 null")
        @Nullable String nextCursor,
        @Schema(description = "검색 조건에 맞는 일지 전체 개수", example = "25")
        long totalCount
    ) {
        public SearchRecordPageResponse {
            Objects.requireNonNull(items, "items");
        }

        @NonNull public List<FarmingRecordResponses.RecordSummaryResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public long getTotalCount() { return totalCount; }

        public static SearchRecordPageResponse from(SearchResult.RecordPage page) {
            return new SearchRecordPageResponse(
                page.getItems().stream().map(FarmingRecordResponses.RecordSummaryResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                page.getNextCursor(),
                page.getTotalCount()
            );
        }
    }

    @Schema(description = "정책 정보 검색 응답 (커서 기반 무한스크롤)")
    public record SearchPolicyPageResponse(
        @Schema(description = "정책 목록")
        @NonNull List<SearchPolicyItemResponse> items,
        @Schema(description = "다음 페이지 커서. 마지막 페이지면 null")
        @Nullable String nextCursor,
        @Schema(description = "검색 조건에 맞는 정책 전체 개수", example = "7")
        long totalCount
    ) {
        public SearchPolicyPageResponse {
            Objects.requireNonNull(items, "items");
        }

        @NonNull public List<SearchPolicyItemResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public long getTotalCount() { return totalCount; }

        public static SearchPolicyPageResponse from(SearchResult.PolicyPage page) {
            return new SearchPolicyPageResponse(
                page.getItems().stream().map(SearchPolicyItemResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                page.getNextCursor(),
                page.getTotalCount()
            );
        }
    }

    @Schema(description = "게시물 검색 응답 (커서 기반 무한스크롤)")
    public record SearchPostPageResponse(
        @Schema(description = "게시물 목록. 게시물 목록 조회 응답과 동일한 형태")
        @NonNull List<CommunityResponses.PostSummaryResponse> items,
        @Schema(description = "다음 페이지 커서. 마지막 페이지면 null")
        @Nullable String nextCursor,
        @Schema(description = "검색 조건에 맞는 게시물 전체 개수", example = "5")
        long totalCount
    ) {
        public SearchPostPageResponse {
            Objects.requireNonNull(items, "items");
        }

        @NonNull public List<CommunityResponses.PostSummaryResponse> getItems() { return items; }
        @Nullable public String getNextCursor() { return nextCursor; }
        public long getTotalCount() { return totalCount; }

        public static SearchPostPageResponse from(SearchResult.PostPage page) {
            return new SearchPostPageResponse(
                page.getItems().stream().map(CommunityResponses.PostSummaryResponse::from).collect(Collectors.toCollection(ArrayList::new)),
                page.getNextCursor(),
                page.getTotalCount()
            );
        }
    }
}
