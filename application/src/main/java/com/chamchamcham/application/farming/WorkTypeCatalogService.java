package com.chamchamcham.application.farming;

import com.chamchamcham.domain.farming.WorkType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class WorkTypeCatalogService {
    public List<WorkTypeResult.WorkTypeSummary> listWorkTypes() {
        return Arrays.stream(WorkType.values()).map(workType -> new WorkTypeResult.WorkTypeSummary(
            workType.name(), workType.getLabel(), workType.getDetailRequired(), WorkTypeFieldCatalog.fieldsFor(workType))).toList();
    }
}
