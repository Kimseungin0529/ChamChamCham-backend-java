package com.chamchamcham.api.media;

import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.application.media.ImageUploader;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.stereotype.Component;

@Component
public class CloudinaryImageUploader implements ImageUploader {
    private final Cloudinary cloudinary;
    public CloudinaryImageUploader(Cloudinary cloudinary) { this.cloudinary = cloudinary; }
    @Override
    public UploadedImage upload(UploadCommand command) {
        java.util.Objects.requireNonNull(command, "command");
        try {
            var result = cloudinary.uploader().upload(command.bytes(), ObjectUtils.asMap("folder", command.usageFolder(), "resource_type", "image"));
            return new UploadedImage((String) result.get("secure_url"), (String) result.get("public_id"));
        } catch (Exception exception) { throw new BusinessException(ErrorCode.MEDIA_UPLOAD_FAILED); }
    }
    @Override
    public void delete(String cloudinaryPublicId) {
        java.util.Objects.requireNonNull(cloudinaryPublicId, "cloudinaryPublicId");
        try {
            var value = cloudinary.uploader().destroy(cloudinaryPublicId, ObjectUtils.asMap("resource_type", "image", "invalidate", true)).get("result");
            var result = value instanceof String string ? string : null;
            if (!"ok".equals(result) && !"not found".equals(result)) throw new BusinessException(ErrorCode.MEDIA_DELETE_FAILED);
        } catch (BusinessException exception) { throw exception; }
        catch (Exception exception) { throw new BusinessException(ErrorCode.MEDIA_DELETE_FAILED); }
    }
}
