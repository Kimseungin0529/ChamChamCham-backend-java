package com.chamchamcham.domain.crop;

import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmBoundaryCoordinate;
import com.chamchamcham.domain.farm.FarmDataSource;
import com.chamchamcham.domain.farm.FarmRepository;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import org.hibernate.SessionFactory;
import com.chamchamcham.domain.common.BaseTimeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@ContextConfiguration(classes = MemberCropRepositoryTest.JpaConfiguration.class)
class MemberCropRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MemberCropRepository memberCropRepository;

    @Autowired
    private FarmRepository farmRepository;

    @Test
    void policyProfileQueriesFetchMultipleFarmsAndCropsWithoutAdditionalSelect() {
        var member = persistFlushFind(new Member(
            null, "policy-query@example.com", null, MemberStatus.ACTIVE, null, null,
            null, null, null, null, "ROLE_USER", null, null
        ));
        var farmNames = List.of("제천 참당귀 농장", "영주 황기 농장", "의성 작약 농장");
        var roadAddresses = List.of("충청북도 제천시 정책로 1", "경상북도 영주시 정책로 2", "경상북도 의성군 정책로 3");
        var crops = List.of(
            new Crop(null, 9001, "참당귀", CropUsePartCategory.ROOT_BARK),
            new Crop(null, 9002, "황기", CropUsePartCategory.ROOT_BARK),
            new Crop(null, 9003, "작약", CropUsePartCategory.ROOT_BARK)
        );
        for (int index = 0; index < farmNames.size(); index++) {
            var roadAddress = roadAddresses.get(index);
            var farm = persistFlushFind(new Farm(
                null, member, farmNames.get(index), roadAddress, roadAddress.replace("정책로", "정책길"),
                null, null, null, null, null, false,
                new ArrayList<>(List.of(new FarmBoundaryCoordinate(36.0 + index, 127.0 + index))),
                new FarmDataSource(null, null, null, null)
            ));
            var savedCrop = persistFlushFind(crops.get(index));
            entityManager.persist(withTimestamps(new MemberCrop(null, member, farm, savedCrop)));
        }
        entityManager.flush();
        entityManager.clear();

        var statistics = entityManager.getEntityManager().getEntityManagerFactory()
            .unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var loadedMember = entityManager.find(Member.class, Objects.requireNonNull(member.getId()));
        var farms = farmRepository.findAllWithBoundaryCoordinatesByOwnerId(Objects.requireNonNull(loadedMember.getId()));
        var memberCrops = memberCropRepository.findAllWithCropByMemberId(Objects.requireNonNull(loadedMember.getId()));
        var afterRepositoryQuery = statistics.getPrepareStatementCount();
        var farmValues = farms.stream().map(farm -> {
            assertThat(farm.getBoundaryCoordinates()).hasSize(1);
            var coordinate = farm.getBoundaryCoordinates().getFirst();
            return tuple(farm.getRoadAddress(), farm.getJibunAddress(), coordinate.getLatitude(), coordinate.getLongitude());
        }).toList();
        var cropValues = memberCrops.stream()
            .map(memberCrop -> tuple(memberCrop.getCrop().getName(), memberCrop.getCrop().getUsePartCategory()))
            .toList();
        var afterProfileFieldAccess = statistics.getPrepareStatementCount();

        assertThat(farms).hasSize(3);
        assertThat(memberCrops).hasSize(3);
        assertThat(farmValues).containsExactlyInAnyOrder(
            tuple("충청북도 제천시 정책로 1", "충청북도 제천시 정책길 1", 36.0, 127.0),
            tuple("경상북도 영주시 정책로 2", "경상북도 영주시 정책길 2", 37.0, 128.0),
            tuple("경상북도 의성군 정책로 3", "경상북도 의성군 정책길 3", 38.0, 129.0)
        );
        assertThat(cropValues).containsExactlyInAnyOrder(
            tuple("참당귀", CropUsePartCategory.ROOT_BARK),
            tuple("황기", CropUsePartCategory.ROOT_BARK),
            tuple("작약", CropUsePartCategory.ROOT_BARK)
        );
        assertThat(afterRepositoryQuery).isLessThanOrEqualTo(3L);
        assertThat(afterProfileFieldAccess).isEqualTo(afterRepositoryQuery);
    }

    private <T extends BaseTimeEntity> T persistFlushFind(T entity) {
        return entityManager.persistFlushFind(withTimestamps(entity));
    }

    private <T extends BaseTimeEntity> T withTimestamps(T entity) {
        var now = LocalDateTime.of(2026, 7, 16, 12, 0);
        ReflectionTestUtils.setField(entity, "createdAt", now);
        ReflectionTestUtils.setField(entity, "updatedAt", now);
        return entity;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.chamchamcham.domain")
    @EnableJpaRepositories("com.chamchamcham.domain")
    static class JpaConfiguration {
    }
}
