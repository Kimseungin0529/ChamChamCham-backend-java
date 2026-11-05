package com.chamchamcham.api.member;
import com.chamchamcham.ApiApplication;
import com.chamchamcham.application.media.ImageUploader;
import com.chamchamcham.application.member.MemberProfileService;
import com.chamchamcham.application.redis.RefreshTokenRepository;
import com.chamchamcham.domain.media.*;
import com.chamchamcham.domain.member.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest(classes = ApiApplication.class, properties = "spring.datasource.url=jdbc:h2:mem:member-withdrawal-integration;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON")
@ActiveProfiles("test")
class MemberWithdrawalIntegrationTest {
    @Autowired private MemberProfileService memberProfileService;
    @Autowired private MemberRepository memberRepository;
    @Autowired private UploadedMediaRepository uploadedMediaRepository;
    @MockBean private RefreshTokenRepository refreshTokenRepository;
    @MockBean private ImageUploader imageUploader;
    private Member member;
    private UUID memberId() { return java.util.Objects.requireNonNull(member.getId(), "Persisted member id is required"); }
    @BeforeEach void setUp() {
        uploadedMediaRepository.deleteAllInBatch(); memberRepository.deleteAllInBatch();
        member = memberRepository.save(new Member("withdrawal-member@example.com", null));
        var profileMedia = uploadedMediaRepository.save(new UploadedMedia(member, UploadedMediaType.IMAGE,
            UploadedMediaUsageType.PROFILE, "https://example.test/profile.jpg", "profile/withdrawal-member"));
        member.updateProfileMedia(profileMedia); memberRepository.save(member);
    }
    @Test @DisplayName("withdraw removes a member with profile media")
    void withdrawRemovesAMemberWithProfileMedia() {
        memberProfileService.withdraw(memberId());
        assertThat(memberRepository.existsById(memberId())).isFalse();
        assertThat(uploadedMediaRepository.count()).isZero();
    }
}
