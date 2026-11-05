package com.chamchamcham.application.pesticide.sync;

import com.chamchamcham.domain.pesticide.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;

/**
 * PSIS 원본 데이터(작물 x 병해충 x 약제 사실 테이블, 약 137,877건)를 전량 페이지네이션 순회하며
 * Pesticide/Pest/PesticideApplication으로 dedup·upsert한다.
 *
 * 매 서버 기동마다 자동 실행되지 않는다 — 관리자 트리거(AdminPesticideSyncController)로만 호출된다.
 * 행 단위 findBy 쿼리를 쓰므로 137k건 전체 동기화는 느릴 수 있다(1회성 관리 작업이라 우선 정확성을
 * 우선하고, 실제 실행해보고 느리면 배치 upsert로 최적화한다 - YAGNI).
 */
@Service
public class PesticideSyncService {
    private static final int DEFAULT_PAGE_SIZE = 1000;
    private final PsisPesticideHttpTransport transport;
    private final PsisPesticideResponseParser responseParser;
    private final PsisPesticideRowMapper rowMapper;
    private final PesticideRepository pesticideRepository;
    private final PestRepository pestRepository;
    private final PesticideApplicationRepository pesticideApplicationRepository;
    private final TransactionTemplate transactionTemplate;
    public PesticideSyncService(PsisPesticideHttpTransport transport, PsisPesticideResponseParser responseParser, PsisPesticideRowMapper rowMapper,
        PesticideRepository pesticideRepository, PestRepository pestRepository, PesticideApplicationRepository pesticideApplicationRepository, TransactionTemplate transactionTemplate) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.responseParser = Objects.requireNonNull(responseParser, "responseParser");
        this.rowMapper = Objects.requireNonNull(rowMapper, "rowMapper");
        this.pesticideRepository = Objects.requireNonNull(pesticideRepository, "pesticideRepository");
        this.pestRepository = Objects.requireNonNull(pestRepository, "pestRepository");
        this.pesticideApplicationRepository = Objects.requireNonNull(pesticideApplicationRepository, "pesticideApplicationRepository");
        this.transactionTemplate = Objects.requireNonNull(transactionTemplate, "transactionTemplate");
    }
    public PesticideSyncResult sync() throws Exception { return sync(DEFAULT_PAGE_SIZE); }
    public PesticideSyncResult sync(int pageSize) throws Exception {
        int pageNo = 1, pagesFetched = 0, fetchedRowCount = 0, createdApplicationCount = 0;
        while (true) {
            var params = new LinkedHashMap<String, String>();
            params.put("pageNo", Integer.toString(pageNo));
            params.put("numOfRows", Integer.toString(pageSize));
            params.put("type", "xml");
            String body = transport.get(params);
            var rawRows = responseParser.parse(body);
            if (rawRows.isEmpty()) break;
            pagesFetched++;
            var rows = new ArrayList<PsisPesticideRow>();
            for (var raw : rawRows) {
                var row = rowMapper.map(raw);
                if (row != null) rows.add(row);
            }
            fetchedRowCount += rawRows.size();
            Integer created = transactionTemplate.execute(status -> upsertRows(rows));
            createdApplicationCount += created == null ? 0 : created;
            if (rawRows.size() < pageSize) break;
            pageNo++;
        }
        return new PesticideSyncResult(fetchedRowCount, createdApplicationCount, pagesFetched);
    }
    private int upsertRows(List<PsisPesticideRow> rows) {
        int created = 0;
        for (var row : rows) {
            var pesticide = upsertPesticide(row);
            var pest = upsertPest(row.getPestName());
            if (upsertApplication(pesticide, pest, row)) created++;
        }
        return created;
    }
    private Pesticide upsertPesticide(PsisPesticideRow row) {
        var existing = pesticideRepository.findByItemNameAndBrandName(row.getItemName(), row.getBrandName());
        var pesticide = existing == null ? new Pesticide(row.getItemName(), row.getBrandName()) : existing;
        pesticide.updateCatalogData(row.getActiveIngredient(), row.getFormulation(), row.getUsageCategory(), row.getHumanToxicity(), row.getFishToxicity(), row.getManufacturer());
        return pesticideRepository.save(pesticide);
    }
    private Pest upsertPest(String name) {
        var existing = pestRepository.findByName(name);
        return existing != null ? existing : pestRepository.save(new Pest(name));
    }
    private boolean upsertApplication(Pesticide pesticide, Pest pest, PsisPesticideRow row) {
        if (pesticide.getId() == null) throw new IllegalArgumentException("Persisted pesticide id is required");
        if (pest.getId() == null) throw new IllegalArgumentException("Persisted pest id is required");
        var existing = pesticideApplicationRepository.findByPesticide_IdAndPest_IdAndCropName(pesticide.getId(), pest.getId(), row.getCropName());
        if (existing != null) {
            existing.updateUsageDetails(row.getDilutionRate(), row.getUsageAmount(), row.getUsageTiming(), row.getMaxUsageCount());
            return false;
        }
        var application = new PesticideApplication(pesticide, pest, row.getCropName());
        application.setDilutionRate(row.getDilutionRate());
        application.setUsageAmount(row.getUsageAmount());
        application.setUsageTiming(row.getUsageTiming());
        application.setMaxUsageCount(row.getMaxUsageCount());
        pesticideApplicationRepository.save(application);
        return true;
    }
}
