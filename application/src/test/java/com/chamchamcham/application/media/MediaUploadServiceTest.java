package com.chamchamcham.application.media;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.media.*;
import com.chamchamcham.domain.member.Member;
import com.chamchamcham.domain.member.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MediaUploadServiceTest {
    private final UUID memberId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID mediaId = UUID.fromString("00000000-0000-0000-0000-000000000501");
    @Mock private MemberRepository memberRepository;
    @Mock private UploadedMediaRepository uploadedMediaRepository;
    private FakeImageUploader uploader;
    private MediaUploadService service;

    @BeforeEach
    void setUp() {
        uploader = new FakeImageUploader();
        service = new MediaUploadService(memberRepository, uploadedMediaRepository, uploader, 10 * 1024 * 1024);
    }

    @Test
    void uploadStoresTempImageMediaOwnedByMember() {
        var member = member();
        stubSuccessfulUpload(member);
        var result = service.upload(command(UploadedMediaUsageType.COMMUNITY_POST, "aGVsbG8="), null);
        assertEquals(mediaId, result.getMediaId());
        assertEquals("https://example.test/sprout.jpg", result.getImageUrl());
        assertEquals(UploadedMediaStatus.TEMP, result.getStatus());
        var saved = capturedMedia();
        assertEquals(member, saved.getOwner());
        assertEquals(UploadedMediaType.IMAGE, saved.getMediaType());
        assertEquals(UploadedMediaUsageType.COMMUNITY_POST, saved.getUsageType());
        assertEquals("cloud/community/sprout", saved.getCloudinaryPublicId());
        assertEquals(UploadedMediaStatus.TEMP, saved.getStatus());
        assertThat(uploader.lastCommand.getBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        assertEquals("community-posts", uploader.lastCommand.getUsageFolder());
    }

    @Test
    void uploadRejectsMissingMemberBeforeCallingUploader() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.empty());
        var exception = assertThrows(BusinessException.class,
            () -> service.upload(command(UploadedMediaUsageType.PROFILE, "aGVsbG8="), null));
        assertEquals(ErrorCode.MEMBER_NOT_FOUND, exception.getErrorCode());
        assertEquals(0, uploader.calls);
        verifyNoInteractions(uploadedMediaRepository);
    }

    @Test
    void uploadRejectsImageLargerThanConfiguredDecodedByteLimit() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member()));
        var exception = assertThrows(BusinessException.class,
            () -> service.upload(command(UploadedMediaUsageType.COMMUNITY_POST, "aGVsbG8="), new byte[11 * 1024 * 1024]));
        assertEquals(ErrorCode.MEDIA_TOO_LARGE, exception.getErrorCode());
        assertEquals(0, uploader.calls);
        verifyNoInteractions(uploadedMediaRepository);
    }

    @Test
    void uploadAcceptsDataUriAndUsesProfileFolder() {
        stubSuccessfulUpload(member());
        service.upload(command(UploadedMediaUsageType.PROFILE, "data:image/jpeg;base64,aGVsbG8="), null);
        assertThat(uploader.lastCommand.getBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
        assertEquals("profiles", uploader.lastCommand.getUsageFolder());
    }

    @Test
    void uploadRejectsMalformedBase64BeforeCallingUploader() {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member()));
        var exception = assertThrows(BusinessException.class,
            () -> service.upload(command(UploadedMediaUsageType.PROFILE, "%%%"), null));
        assertEquals(ErrorCode.INVALID_INPUT, exception.getErrorCode());
        assertEquals(0, uploader.calls);
        verifyNoInteractions(uploadedMediaRepository);
    }

    @Test
    void uploadAcceptsExactDecodedByteLimitAndFarmingRecordFolder() {
        service = new MediaUploadService(memberRepository, uploadedMediaRepository, uploader, 5);
        stubSuccessfulUpload(member());
        service.upload(command(UploadedMediaUsageType.FARMING_RECORD, "aGVsbG8="), null);
        assertEquals("farming-records", uploader.lastCommand.getUsageFolder());
        assertEquals(5, uploader.lastCommand.getBytes().length);
    }

    private Member member() { return new Member(memberId, "farmer@example.com", null); }
    private MediaCommand.UploadImage command(UploadedMediaUsageType usageType, String base64) {
        return new MediaCommand.UploadImage(memberId, usageType, base64, "sprout.jpg", "image/jpeg");
    }
    private void stubSuccessfulUpload(Member member) {
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(member));
        when(uploadedMediaRepository.save(any(UploadedMedia.class))).thenAnswer(invocation -> {
            UploadedMedia media = invocation.getArgument(0);
            return new UploadedMedia(mediaId, media.getOwner(), media.getMediaType(), media.getUsageType(),
                media.getFileUrl(), media.getCloudinaryPublicId(), media.getStatus());
        });
    }
    private UploadedMedia capturedMedia() {
        var captor = ArgumentCaptor.forClass(UploadedMedia.class);
        verify(uploadedMediaRepository).save(captor.capture()); return captor.getValue();
    }
    private static class FakeImageUploader implements ImageUploader {
        int calls;
        ImageUploader.UploadCommand lastCommand;
        @Override
        public ImageUploader.UploadedImage upload(ImageUploader.UploadCommand command) {
            calls++; lastCommand = command;
            return new ImageUploader.UploadedImage("https://example.test/sprout.jpg", "cloud/community/sprout");
        }
        @Override
        public void delete(String cloudinaryPublicId) {}
    }
}
