package com.chamchamcham.api.member.controller;
import com.chamchamcham.api.exception.GlobalExceptionHandler;
import com.chamchamcham.application.security.TokenProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.List;
import java.util.UUID;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.chamchamcham.application.crop.CropResult;
import com.chamchamcham.application.member.*;
import com.chamchamcham.domain.crop.CropUsePartCategory;
import com.chamchamcham.domain.member.ManagementType;
import java.time.LocalDate;
@WebMvcTest(MemberController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MemberControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID requesterId = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private final UUID farmId = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final UUID cropId = UUID.fromString("00000000-0000-0000-0000-000000000201");
    @MockBean private MemberProfileService memberProfileService;
    @MockBean private TokenProvider tokenProvider;
    @Test @DisplayName("get my profile keeps farm summaries as read model")
    void getMyProfileKeepsFarmSummariesAsReadModel() throws Exception {
        given(memberProfileService.getMyProfile(memberId)).willReturn(myProfile());
        mockMvc.perform(get("/api/v1/members/me").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.memberId", equalTo(memberId.toString())))
            .andExpect(jsonPath("$.data.farms[0].farmId", equalTo(farmId.toString())))
            .andExpect(jsonPath("$.data.crops[0].cropName", equalTo("황기")));
    }
    @Test @DisplayName("delete me delegates authenticated member hard withdrawal")
    void deleteMeDelegatesAuthenticatedMemberHardWithdrawal() throws Exception {
        mockMvc.perform(delete("/api/v1/members/me").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success", equalTo(true)));
        verify(memberProfileService).withdraw(memberId);
    }
    @Test @DisplayName("get farm crops keeps nested crop read model")
    void getFarmCropsKeepsNestedCropReadModel() throws Exception {
        given(memberProfileService.getMyFarmCrops(memberId)).willReturn(List.of(new MemberProfileResult.FarmCrops(
            farmId, "횡성 황기밭", List.of(cropSummary()))));
        mockMvc.perform(get("/api/v1/members/me/farm-crops").with(authenticatedMember(memberId.toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].crops[0].id", equalTo(cropId.toString())));
    }
    @Test @DisplayName("update profile maps profile fields without farms")
    void updateProfileMapsProfileFieldsWithoutFarms() throws Exception {
        given(memberProfileService.updateMyProfile(updateCommand())).willReturn(myProfile());
        mockMvc.perform(put("/api/v1/members/me/profile").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(profileJson()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.name", equalTo("이황기")));
        verify(memberProfileService).updateMyProfile(updateCommand());
    }
    @Test @DisplayName("update profile succeeds without name phone birthDate or nickname")
    void updateProfileSucceedsWithoutNamePhoneBirthDateOrNickname() throws Exception {
        given(memberProfileService.updateMyProfile(updateCommand(null, null, null, null, 2))).willReturn(myProfile());
        mockMvc.perform(put("/api/v1/members/me/profile").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(profileJson().replace("\"name\":\"이황기\",", "")
                .replace("\"phone\":\"010-1000-0001\",", "").replace("\"birthDate\":\"1986-03-12\",", "")
                .replace("\"nickname\":\"황기농부\",", "")))
            .andExpect(status().isOk());
    }
    @Test @DisplayName("update profile rejects experience level above requester age")
    void updateProfileRejectsExperienceLevelAboveRequesterAge() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/profile").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(profileJson().replace("\"birthDate\":\"1986-03-12\"", "\"birthDate\":\"" + LocalDate.now().minusYears(5) + "\"")
                .replace("\"experienceLevel\":2", "\"experienceLevel\":50")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_001")));
        verifyNoInteractions(memberProfileService);
    }
    @Test @DisplayName("update profile reports experience level field above one hundred")
    void updateProfileReportsExperienceLevelFieldAboveOneHundred() throws Exception {
        mockMvc.perform(put("/api/v1/members/me/profile").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(profileJson().replace("\"experienceLevel\":2", "\"experienceLevel\":101")))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code", equalTo("COMMON_001")))
            .andExpect(jsonPath("$.error.detail.field", equalTo("experienceLevel")));
        verifyNoInteractions(memberProfileService);
    }
    @Test @DisplayName("update profile allows experience level up to one hundred when birthDate is absent")
    void updateProfileAllowsExperienceLevelUpToOneHundredWhenBirthDateIsAbsent() throws Exception {
        given(memberProfileService.updateMyProfile(updateCommand("이황기", "010-1000-0001", null, "황기농부", 90))).willReturn(myProfile());
        mockMvc.perform(put("/api/v1/members/me/profile").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content(profileJson().replace("\"birthDate\":\"1986-03-12\",", "")
                .replace("\"experienceLevel\":2", "\"experienceLevel\":90"))).andExpect(status().isOk());
    }
    @Test @DisplayName("get public profile keeps safe fields")
    void getPublicProfileKeepsSafeFields() throws Exception {
        given(memberProfileService.getPublicProfile(memberId)).willReturn(new MemberProfileResult.PublicProfile(
            memberId, "황기농부", 2, ManagementType.AGRICULTURAL_INDIVIDUAL.name(), null,
            List.of(new MemberProfileResult.PublicFarm(farmId, "강원특별자치도 횡성군")), List.of(new MemberProfileResult.CropProfile(cropId, "황기"))));
        mockMvc.perform(get("/api/v1/members/{memberId}/profile", memberId).with(authenticatedMember(requesterId.toString())))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.email").doesNotExist())
            .andExpect(jsonPath("$.data.farms[0].roadAddress").doesNotExist());
    }
    private RequestPostProcessor authenticatedMember(String id) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }
    private MemberProfileResult.MyProfile myProfile() {
        return new MemberProfileResult.MyProfile(memberId, "hwanggi@example.com", "이황기", "010-1000-0001", LocalDate.of(1986, 3, 12),
            "황기농부", 2, ManagementType.AGRICULTURAL_INDIVIDUAL.name(), null,
            List.of(new MemberProfileResult.MyFarm(farmId, "횡성 황기밭", "강원특별자치도 횡성군 둔내면 1", null, "강원특별자치도 횡성군")),
            List.of(new MemberProfileResult.CropProfile(cropId, "황기")));
    }
    private CropResult.CropSummary cropSummary() { return new CropResult.CropSummary(cropId, 422, "황기", CropUsePartCategory.ROOT_BARK.name(), CropUsePartCategory.ROOT_BARK.getLabel()); }
    private MemberProfileCommand.UpdateMyProfile updateCommand() { return updateCommand("이황기", "010-1000-0001", LocalDate.of(1986, 3, 12), "황기농부", 2); }
    private MemberProfileCommand.UpdateMyProfile updateCommand(String name, String phone, LocalDate birthDate, String nickname, int experienceLevel) {
        return new MemberProfileCommand.UpdateMyProfile(memberId, name, phone, birthDate, nickname, experienceLevel,
            ManagementType.AGRICULTURAL_INDIVIDUAL, null, List.of(new MemberProfileCommand.Farm(farmId, "횡성 황기밭", "강원특별자치도 횡성군 둔내면 1", null,
                37.5, 128.5, null, null, null, false, List.of(), new MemberProfileCommand.FarmDataSource(null, null, null, null), List.of(cropId))));
    }
    private String profileJson() {
        return """
            {
              "name":"이황기",
              "phone":"010-1000-0001",
              "birthDate":"1986-03-12",
              "nickname":"황기농부",
              "experienceLevel":2,
              "managementType":"AGRICULTURAL_INDIVIDUAL",
              "profileMediaId":null,
              "farms":[
                {
                  "farmId":"%s",
                  "name":"횡성 황기밭",
                  "roadAddress":"강원특별자치도 횡성군 둔내면 1",
                  "latitude":37.5,
                  "longitude":128.5,
                  "cropIds":["%s"]
                }
              ]
            }
            """.formatted(farmId, cropId).stripTrailing();
    }
}
