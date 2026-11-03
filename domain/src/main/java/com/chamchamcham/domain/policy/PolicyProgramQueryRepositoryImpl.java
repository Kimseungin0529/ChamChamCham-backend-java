package com.chamchamcham.domain.policy;

import jakarta.persistence.EntityManager;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Repository
public class PolicyProgramQueryRepositoryImpl implements PolicyProgramQueryRepository {
    private final EntityManager entityManager;

    public PolicyProgramQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<PolicyProgram> search(SearchCondition condition) {
        Objects.requireNonNull(condition, "condition");
        var filter = buildFilterPredicates(condition.keyword());
        if (condition.cursorCreatedAt() != null && condition.cursorId() != null) {
            filter.where().add("(p.createdAt < :cCreatedAt or (p.createdAt = :cCreatedAt and p.id < :cId))");
            filter.params().put("cCreatedAt", condition.cursorCreatedAt());
            filter.params().put("cId", condition.cursorId());
        }

        var query = entityManager.createQuery("""
            select p
            from PolicyProgram p
            where %s
            order by p.createdAt desc, p.id desc
            """.formatted(String.join(" and ", filter.where())), PolicyProgram.class);
        filter.params().forEach(query::setParameter);
        query.setMaxResults(condition.size());
        return query.getResultList();
    }

    @Override
    public long count(@Nullable String keyword) {
        var filter = buildFilterPredicates(keyword);
        var query = entityManager.createQuery("""
            select count(p)
            from PolicyProgram p
            where %s
            """.formatted(String.join(" and ", filter.where())), Long.class);
        filter.params().forEach(query::setParameter);
        return query.getSingleResult();
    }

    private Filter buildFilterPredicates(@Nullable String keyword) {
        var where = new ArrayList<>(List.of("p.detailSynced = true", "p.recommendable = true"));
        var params = new LinkedHashMap<String, Object>();
        if (keyword != null) {
            int start = 0;
            int end = keyword.length();
            while (start < end && isWhitespace(keyword.charAt(start))) start++;
            while (end > start && isWhitespace(keyword.charAt(end - 1))) end--;
            var trimmed = keyword.substring(start, end).toLowerCase(Locale.ROOT);
            if (!trimmed.isEmpty()) {
                where.add("(lower(p.title) like :keyword or lower(p.agencyName) like :keyword or lower(p.summary) like :keyword)");
                params.put("keyword", "%" + trimmed + "%");
            }
        }
        return new Filter(where, params);
    }

    private boolean isWhitespace(char character) {
        return Character.isWhitespace(character) || Character.isSpaceChar(character);
    }

    private record Filter(List<String> where, Map<String, Object> params) {
    }
}
