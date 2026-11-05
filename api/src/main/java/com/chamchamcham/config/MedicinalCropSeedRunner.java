package com.chamchamcham.config;

import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

@Component
@Profile({"local", "dev", "prod"})
public class MedicinalCropSeedRunner implements ApplicationRunner {
    private static final Logger logger = LoggerFactory.getLogger(MedicinalCropSeedRunner.class.getName() + "Kt");
    private static final String SEED_RESOURCE = "seed/medicinal-crops.json";
    private final CropRepository cropRepository;
    private final ObjectMapper objectMapper;
    public MedicinalCropSeedRunner(CropRepository cropRepository, ObjectMapper objectMapper) {
        this.cropRepository = cropRepository; this.objectMapper = objectMapper;
    }
    @Override @Transactional
    public void run(ApplicationArguments args) throws java.io.IOException {
        java.util.List<MedicinalCropSeedRow> seedRows;
        try (var input = new ClassPathResource(SEED_RESOURCE).getInputStream()) {
            seedRows = Arrays.asList(objectMapper.readValue(input, MedicinalCropSeedRow[].class));
        }
        var existingByExternalNo = new LinkedHashMap<Integer, Crop>();
        for (var crop : cropRepository.findByExternalNoIn(seedRows.stream().map(MedicinalCropSeedRow::externalNo).collect(Collectors.toList()))) {
            existingByExternalNo.put(crop.getExternalNo(), crop);
        }
        var crops = seedRows.stream().map(row -> {
            var existing = existingByExternalNo.get(row.externalNo());
            if (existing != null) { existing.updateCatalogData(row.name(), row.usePartCategory()); return existing; }
            return new Crop(row.externalNo(), row.name(), row.usePartCategory());
        }).collect(Collectors.toList());
        cropRepository.saveAll(crops);
        logger.info("Medicinal crop seed synchronized: {}", crops.size());
    }
}
