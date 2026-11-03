package com.chamchamcham.domain.policy;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Repository
public class PolicyRecommendationQueryRepositoryImpl implements PolicyRecommendationQueryRepository {
    private final EntityManager entityManager;

    public PolicyRecommendationQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public SearchResult findPage(SearchCondition condition) {
        Objects.requireNonNull(condition, "condition");
        var where = new ArrayList<>(List.of("r.member.id = :memberId", "p.source = :source", "p.sourceYear = :sourceYear"));
        var params = new LinkedHashMap<String, Object>();
        params.put("memberId", condition.memberId());
        params.put("source", condition.source());
        params.put("sourceYear", condition.sourceYear());

        if (condition.benefitSummary() != null) {
            where.add("p.benefitSummary = :benefitSummary");
            params.put("benefitSummary", condition.benefitSummary());
        }

        var cursor = condition.cursor();
        if (cursor != null) {
            switch (condition.sort()) {
                case RECOMMENDED -> {
                    if (cursor.score() == null) {
                        throw new IllegalArgumentException("Recommended policy cursor requires score");
                    }
                    params.put("cursorScore", cursor.score());
                    if (cursor.applyEndsOn() == null) {
                        where.add("""
                            (
                                r.score < :cursorScore
                                or (r.score = :cursorScore and p.applyEndsOn is null and r.id > :cursorId)
                            )
                            """);
                    } else {
                        where.add("""
                            (
                                r.score < :cursorScore
                                or (r.score = :cursorScore and p.applyEndsOn > :cursorApplyEndsOn)
                                or (r.score = :cursorScore and p.applyEndsOn = :cursorApplyEndsOn and r.id > :cursorId)
                                or (r.score = :cursorScore and p.applyEndsOn is null)
                            )
                            """);
                        params.put("cursorApplyEndsOn", cursor.applyEndsOn());
                    }
                }
                case LATEST -> {
                    if (cursor.applyStartsOn() == null) {
                        where.add("p.applyStartsOn is null and r.id > :cursorId");
                    } else {
                        where.add("""
                            (
                                p.applyStartsOn < :cursorApplyStartsOn
                                or (p.applyStartsOn = :cursorApplyStartsOn and r.id > :cursorId)
                                or p.applyStartsOn is null
                            )
                            """);
                        params.put("cursorApplyStartsOn", cursor.applyStartsOn());
                    }
                }
            }
            params.put("cursorId", cursor.id());
        }

        var orderBy = switch (condition.sort()) {
            case RECOMMENDED -> "r.score desc, case when p.applyEndsOn is null then 1 else 0 end asc, p.applyEndsOn asc, r.id asc";
            case LATEST -> "case when p.applyStartsOn is null then 1 else 0 end asc, p.applyStartsOn desc, r.id asc";
        };
        var query = entityManager.createQuery("""
            select r
            from PolicyRecommendation r
            join fetch r.policyProgram p
            where %s
            order by %s
            """.formatted(String.join(" and ", where), orderBy), PolicyRecommendation.class);
        params.forEach(query::setParameter);
        query.setMaxResults(condition.size());
        return new SearchResult(query.getResultList());
    }
}
