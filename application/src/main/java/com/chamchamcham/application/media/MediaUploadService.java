package com.chamchamcham.application.media;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.media.UploadedMedia;
import com.chamchamcham.domain.media.UploadedMediaRepository;
import com.chamchamcham.domain.media.UploadedMediaType;
import com.chamchamcham.domain.member.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Base64;
import java.util.Objects;

@Service
@Transactional
public class MediaUploadService {
    private final MemberRepository memberRepository;
    private final UploadedMediaRepository uploadedMediaRepository;
    private final ImageUploader imageUploader;
    private final int maxImageBytes;

    public MediaUploadService(MemberRepository memberRepository, UploadedMediaRepository uploadedMediaRepository,
            ImageUploader imageUploader, @Value("${app.media.max-image-bytes:10485760}") int maxImageBytes) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.uploadedMediaRepository = Objects.requireNonNull(uploadedMediaRepository, "uploadedMediaRepository");
        this.imageUploader = Objects.requireNonNull(imageUploader, "imageUploader");
        this.maxImageBytes = maxImageBytes;
    }

    public MediaResult.UploadedImage upload(MediaCommand.UploadImage command) {
        return upload(command, null);
    }

    public MediaResult.UploadedImage upload(MediaCommand.UploadImage command, @Nullable byte[] decodedBytesForTest) {
        Objects.requireNonNull(command, "command");
        var member = memberRepository.findById(command.memberId()).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
        var decoded = decodedBytesForTest != null ? decodedBytesForTest : decodeBase64(command.base64Image());
        if (decoded.length > maxImageBytes) throw new BusinessException(ErrorCode.MEDIA_TOO_LARGE);
        var folder = switch (command.usageType()) {
            case PROFILE -> "profiles";
            case COMMUNITY_POST -> "community-posts";
            case FARMING_RECORD -> "farming-records";
        };
        var uploaded = imageUploader.upload(new ImageUploader.UploadCommand(decoded, command.originalFilename(), command.contentType(), folder));
        var media = uploadedMediaRepository.save(new UploadedMedia(member, UploadedMediaType.IMAGE, command.usageType(),
            uploaded.imageUrl(), uploaded.cloudinaryPublicId()));
        return MediaResult.UploadedImage.from(media);
    }

    private byte[] decodeBase64(String value) {
        try {
            var payload = value.substring(value.indexOf(',') + 1);
            return Base64.getDecoder().decode(payload);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}
