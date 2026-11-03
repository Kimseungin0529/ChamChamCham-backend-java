package com.chamchamcham.domain.member;

import com.chamchamcham.domain.community.CommunityComment;
import com.chamchamcham.domain.community.CommunityPost;
import com.chamchamcham.domain.community.CommunityPostLike;
import com.chamchamcham.domain.community.CommunityPostMedia;
import com.chamchamcham.domain.community.CommunityPostType;
import com.chamchamcham.domain.crop.Crop;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.crop.MemberCrop;
import com.chamchamcham.domain.farm.Farm;
import com.chamchamcham.domain.farm.FarmBoundaryCoordinate;
import com.chamchamcham.domain.farm.FarmDataSource;
import com.chamchamcham.domain.farming.EntryMode;
import com.chamchamcham.domain.farming.FarmingRecord;
import com.chamchamcham.domain.farming.FarmingRecordMedia;
import com.chamchamcham.domain.farming.PlantingMethod;
import com.chamchamcham.domain.farming.PlantingRecord;
import com.chamchamcham.domain.farming.WorkType;
import com.chamchamcham.domain.legal.LegalDocument;
import com.chamchamcham.domain.legal.MemberConsent;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaStatus;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.media.UploadedMediaUsageType;
import com.chamchamcham.domain.notification.NotificationPreference;
import com.chamchamcham.domain.common.BaseTimeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@ContextConfiguration(classes = MemberHardDeleteSchemaTest.JpaConfiguration.class)
class MemberHardDeleteSchemaTest {
    private final TestEntityManager entityManager;
    private final JdbcTemplate jdbcTemplate;
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 16, 12, 0);

    @Autowired
    MemberHardDeleteSchemaTest(TestEntityManager entityManager, DataSource dataSource) {
        this.entityManager = entityManager;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Test
    void memberOwnedForeignKeysDeclareRequiredDeleteActions() throws SQLException {
        var deleteRules = importedDeleteRules();

        expectedCascade.forEach(key -> assertThat(deleteRules.get(key))
            .describedAs("%s delete action", key)
            .isEqualTo((short) DatabaseMetaData.importedKeyCascade));
        expectedSetNull.forEach(key -> assertThat(deleteRules.get(key))
            .describedAs("%s delete action", key)
            .isEqualTo((short) DatabaseMetaData.importedKeySetNull));
        sharedReferences.forEach(key -> assertThat(deleteRules.get(key))
            .describedAs("%s must preserve shared rows", key)
            .isNotEqualTo((short) DatabaseMetaData.importedKeyCascade));
    }

    @Test
    void physicalMemberDeleteRemovesOwnedGraphAndPreservesSharedRows() {
        var crop = persist(new Crop(null, 422, "황기", CropUsePartCategory.ROOT_BARK));
        var legalDocument = persist(new LegalDocument(null, "TERMS", "이용약관", "1.0", "약관", now));
        var member = persist(new Member(
            null, "withdraw@example.com", null, MemberStatus.ACTIVE, null, null,
            null, null, null, null, "ROLE_USER", null, null
        ));
        var farm = persist(new Farm(
            null, member, "탈퇴 테스트 농장", "충북 제천시 테스트로 1", null,
            null, null, null, null, null, false,
            new ArrayList<>(List.of(new FarmBoundaryCoordinate(37.0, 128.0))),
            new FarmDataSource(null, null, null, null)
        ));
        persist(new MemberCrop(null, member, farm, crop));

        var record = persist(new FarmingRecord(
            null, member, farm, crop, WorkType.PLANTING, now, "맑음", 25, "파종", EntryMode.MANUAL, false
        ));
        persist(new PlantingRecord(null, record, PlantingMethod.SEED, null, null, null, null, null));

        var media = persist(new UploadedMedia(
            null, member, UploadedMediaType.IMAGE, UploadedMediaUsageType.COMMUNITY_POST,
            "https://example.test/member-image.jpg", "community/member-image", UploadedMediaStatus.ATTACHED
        ));
        member.updateProfileMedia(media);
        persist(new FarmingRecordMedia(null, record, media, 0));

        var post = persist(new CommunityPost(
            null, member, crop, record, CommunityPostType.GENERAL, "탈퇴할 글", "본문", false
        ));
        persist(new CommunityPostMedia(null, post, media, 0));
        var comment = persist(new CommunityComment(null, post, null, member, "댓글", media, false));
        persist(new CommunityComment(null, post, comment, member, "대댓글", null, false));
        persist(new CommunityPostLike(null, post, member));
        persist(new ExternalIdentity(null, member, AuthProvider.KAKAO, "withdraw-subject"));
        persist(new MemberConsent(null, member, legalDocument, true, now));
        persist(new NotificationPreference(null, member, "PUSH", "WEATHER", true));
        entityManager.flush();
        var memberId = Objects.requireNonNull(member.getId());

        jdbcTemplate.update("delete from member where id = ?", memberId);
        entityManager.clear();

        ownedTables.forEach(table -> assertThat(rowCount(table))
            .describedAs("%s rows after member delete", table)
            .isZero());
        assertThat(rowCount("crop")).isEqualTo(1);
        assertThat(rowCount("legal_document")).isEqualTo(1);
    }

    private Map<String, Short> importedDeleteRules() throws SQLException {
        var tables = Stream.of(expectedCascade, expectedSetNull, sharedReferences)
            .flatMap(Set::stream)
            .map(key -> key.substring(0, key.indexOf('.')))
            .collect(Collectors.toSet());
        var deleteRules = new HashMap<String, Short>();
        try (var connection = Objects.requireNonNull(jdbcTemplate.getDataSource()).getConnection()) {
            for (var table : tables) {
                try (var keys = connection.getMetaData().getImportedKeys(
                    connection.getCatalog(), connection.getSchema(), table.toLowerCase(Locale.ROOT)
                )) {
                    while (keys.next()) {
                        var key = (keys.getString("FKTABLE_NAME") + "." + keys.getString("FKCOLUMN_NAME"))
                            .toUpperCase(Locale.ROOT);
                        deleteRules.put(key, keys.getShort("DELETE_RULE"));
                    }
                }
            }
        }
        return deleteRules;
    }

    private long rowCount(String table) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject("select count(*) from " + table, Long.class));
    }

    private static final Set<String> expectedCascade = Set.of(
        "EXTERNAL_IDENTITY.MEMBER_ID",
        "MEMBER_CONSENT.MEMBER_ID",
        "NOTIFICATION_PREFERENCE.MEMBER_ID",
        "FARM.OWNER_MEMBER_ID",
        "MEMBER_CROP.MEMBER_ID",
        "FARMING_RECORD.MEMBER_ID",
        "FARMING_RECORD.FARM_ID",
        "FARMING_CYCLE_REPORT.MEMBER_ID",
        "FARMING_CYCLE_REPORT.FARM_ID",
        "RECORD_FEEDBACK.MEMBER_ID",
        "REPORT_FEEDBACK.MEMBER_ID",
        "POLICY_RECOMMENDATION.MEMBER_ID",
        "VOICE_RECORD_SESSION.MEMBER_ID",
        "COMMUNITY_POST.AUTHOR_MEMBER_ID",
        "COMMUNITY_COMMENT.AUTHOR_MEMBER_ID",
        "COMMUNITY_POST_LIKE.MEMBER_ID",
        "UPLOADED_MEDIA.OWNER_MEMBER_ID",
        "FARM_BOUNDARY_COORDINATE.FARM_ID",
        "MEMBER_CROP.FARM_ID",
        "PLANTING_RECORD.RECORD_ID",
        "WATERING_RECORD.RECORD_ID",
        "WEEDING_RECORD.RECORD_ID",
        "FERTILIZING_RECORD.RECORD_ID",
        "PEST_CONTROL_RECORD.RECORD_ID",
        "HARVEST_RECORD.RECORD_ID",
        "FARMING_RECORD_MEDIA.RECORD_ID",
        "RECORD_FEEDBACK.RECORD_ID",
        "RECORD_FEEDBACK_NEXT_ACTION.RECORD_FEEDBACK_ID",
        "REPORT_FEEDBACK.REPORT_ID",
        "REPORT_FEEDBACK_ITEM.REPORT_FEEDBACK_ID",
        "VOICE_RECORD_TURN.SESSION_ID",
        "COMMUNITY_POST_MEDIA.POST_ID",
        "COMMUNITY_COMMENT.POST_ID",
        "COMMUNITY_POST_LIKE.POST_ID",
        "COMMUNITY_COMMENT.PARENT_COMMENT_ID",
        "COMMUNITY_POST_MEDIA.UPLOADED_MEDIA_ID",
        "FARMING_RECORD_MEDIA.UPLOADED_MEDIA_ID"
    );

    private static final Set<String> expectedSetNull = Set.of(
        "MEMBER.PROFILE_MEDIA_ID",
        "COMMUNITY_COMMENT.MEDIA_ID",
        "COMMUNITY_POST.FARMING_RECORD_ID",
        "VOICE_RECORD_SESSION.DRAFT_RECORD_ID",
        "FARMING_CYCLE_REPORT.FINAL_HARVEST_RECORD_ID"
    );

    private static final Set<String> sharedReferences = Set.of(
        "COMMUNITY_POST.CROP_ID",
        "MEMBER_CONSENT.LEGAL_DOCUMENT_ID",
        "POLICY_RECOMMENDATION.POLICY_PROGRAM_ID"
    );

    private static final Set<String> ownedTables = Set.of(
        "member",
        "external_identity",
        "member_consent",
        "notification_preference",
        "farm",
        "farm_boundary_coordinate",
        "member_crop",
        "farming_record",
        "planting_record",
        "farming_record_media",
        "uploaded_media",
        "community_post",
        "community_post_media",
        "community_comment",
        "community_post_like"
    );

    private <T extends BaseTimeEntity> T persist(T entity) {
        ReflectionTestUtils.setField(entity, "createdAt", now);
        ReflectionTestUtils.setField(entity, "updatedAt", now);
        return entityManager.persist(entity);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.chamchamcham.domain")
    @EnableJpaRepositories("com.chamchamcham.domain")
    static class JpaConfiguration {
    }
}
