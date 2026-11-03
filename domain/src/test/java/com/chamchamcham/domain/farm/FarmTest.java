package com.chamchamcham.domain.farm;

import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FarmTest {
    @Test
    void updateProfileReplacesEditableFarmFields() {
        var owner = new Member(
            UUID.randomUUID(), "owner@example.com", null, MemberStatus.ACTIVE, null, null,
            null, null, null, null, "ROLE_USER", null, null
        );
        var farm = new Farm(
            UUID.randomUUID(), owner, "기존 농장", "강원특별자치도 횡성군 기존로 1",
            "강원특별자치도 횡성군 기존리 1", 37.1, 128.1, "old-pnu", "전",
            new BigDecimal("100.00"), false,
            new ArrayList<>(List.of(new FarmBoundaryCoordinate(37.1, 128.1))),
            new FarmDataSource("OLD", "OLD", null, null)
        );

        farm.updateProfile(
            "수정 농장", "강원특별자치도 평창군 새로 2", null, 37.2, 128.2,
            "new-pnu", "답", new BigDecimal("200.50"), true,
            List.of(new FarmBoundaryCoordinate(37.2, 128.2), new FarmBoundaryCoordinate(37.3, 128.3)),
            new FarmDataSource("KAKAO", "KAKAO", "PUBLIC_DATA", null)
        );

        assertThat(farm.getName()).isEqualTo("수정 농장");
        assertThat(farm.getRoadAddress()).isEqualTo("강원특별자치도 평창군 새로 2");
        assertThat(farm.getJibunAddress()).isNull();
        assertThat(farm.getLatitude()).isEqualTo(37.2);
        assertThat(farm.getLongitude()).isEqualTo(128.2);
        assertThat(farm.getPnu()).isEqualTo("new-pnu");
        assertThat(farm.getLandCategory()).isEqualTo("답");
        assertThat(farm.getAreaSqm()).isEqualByComparingTo("200.50");
        assertThat(farm.getAreaIsManualEntry()).isTrue();
        assertThat(farm.getBoundaryCoordinates()).hasSize(2);
        assertThat(farm.getDataSource().getAddress()).isEqualTo("KAKAO");
        assertThat(farm.getDataSource().getCoordinate()).isEqualTo("KAKAO");
        assertThat(farm.getDataSource().getParcel()).isEqualTo("PUBLIC_DATA");
        assertThat(farm.getDataSource().getLandCharacteristic()).isNull();
    }
}
