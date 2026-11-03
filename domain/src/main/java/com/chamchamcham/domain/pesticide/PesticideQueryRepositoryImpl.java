package com.chamchamcham.domain.pesticide;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Repository
public class PesticideQueryRepositoryImpl implements PesticideQueryRepository {
    private final EntityManager entityManager;

    public PesticideQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = Objects.requireNonNull(entityManager, "entityManager");
    }

    @Override
    public List<Pesticide> search(SearchCondition condition) {
        Objects.requireNonNull(condition, "condition");
        var where = new ArrayList<String>();
        var params = new LinkedHashMap<String, Object>();
        var keyword = condition.keyword();
        if (keyword != null && keyword.chars().anyMatch(c -> !Character.isWhitespace(c) && !Character.isSpaceChar(c))) {
            where.add("(p.brandName like concat('%', :keyword, '%') or p.itemName like concat('%', :keyword, '%'))");
            params.put("keyword", keyword);
        }
        if (condition.cursor() != null) {
            var cursor = condition.cursor();
            where.add("(p.brandName > :cursorBrandName or (p.brandName = :cursorBrandName and p.id > :cursorId))");
            params.put("cursorBrandName", cursor.brandName());
            params.put("cursorId", cursor.id());
        }
        var whereClause = where.isEmpty() ? "" : "where " + String.join(" and ", where);
        var query = entityManager.createQuery("""
            select p
            from Pesticide p
            %s
            order by p.brandName asc, p.id asc
            """.formatted(whereClause), Pesticide.class);
        params.forEach(query::setParameter);
        query.setMaxResults(condition.size());
        return query.getResultList();
    }

    @Override
    public List<VoiceCatalogRow> findByCropNames(List<String> cropNames, int maxRows) {
        Objects.requireNonNull(cropNames, "cropNames");
        if (cropNames.isEmpty()) return List.of();
        var query = entityManager.createQuery("""
            select distinct pa.pesticide.itemName, pa.pesticide.brandName, pa.pest.name
            from PesticideApplication pa
            where pa.cropName in :cropNames
            order by pa.pesticide.itemName asc, pa.pest.name asc
            """, Object[].class);
        query.setParameter("cropNames", cropNames);
        query.setMaxResults(maxRows);
        return query.getResultList().stream()
            .map(row -> new VoiceCatalogRow((String) row[0], (String) row[1], (String) row[2]))
            .toList();
    }
}
