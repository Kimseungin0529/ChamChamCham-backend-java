package com.chamchamcham.api.media.controller;
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
import com.chamchamcham.application.media.*;
import com.chamchamcham.domain.media.*;
@WebMvcTest(MediaController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class MediaControllerTest {
    @Autowired private MockMvc mockMvc;
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID mediaId = UUID.fromString("00000000-0000-0000-0000-000000000501");
    @MockBean private MediaUploadService mediaUploadService;
    @MockBean private TokenProvider tokenProvider;
    @Test @DisplayName("upload image returns uploaded media response")
    void uploadImageReturnsUploadedMediaResponse() throws Exception {
        when(mediaUploadService.upload(new MediaCommand.UploadImage(memberId, UploadedMediaUsageType.COMMUNITY_POST,
            "aGVsbG8=", "sprout.jpg", "image/jpeg")))
            .thenReturn(new MediaResult.UploadedImage(mediaId, "https://example.test/sprout.jpg", UploadedMediaStatus.TEMP));
        mockMvc.perform(post("/api/v1/media/images").with(authenticatedMember(memberId.toString()))
            .contentType(MediaType.APPLICATION_JSON).content("""
                {
                  "usageType": "COMMUNITY_POST",
                  "base64Image": "aGVsbG8=",
                  "originalFilename": "sprout.jpg",
                  "contentType": "image/jpeg"
                }
                """))
            .andExpect(status().isOk()).andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.mediaId", equalTo(mediaId.toString())))
            .andExpect(jsonPath("$.data.imageUrl", equalTo("https://example.test/sprout.jpg")))
            .andExpect(jsonPath("$.data.status", equalTo("TEMP")));
    }
    private RequestPostProcessor authenticatedMember(String id) {
        return request -> {
            SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            return request;
        };
    }
}
