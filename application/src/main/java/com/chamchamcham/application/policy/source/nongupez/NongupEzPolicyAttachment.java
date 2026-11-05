package com.chamchamcham.application.policy.source.nongupez;

import org.springframework.lang.Nullable;

public record NongupEzPolicyAttachment(
    @Nullable String fileName,
    @Nullable String extension,
    @Nullable Long sizeBytes,
    @Nullable String url
) {
    @Nullable
    public String getFileName() { return fileName; }
    @Nullable
    public String getExtension() { return extension; }
    @Nullable
    public Long getSizeBytes() { return sizeBytes; }
    @Nullable
    public String getUrl() { return url; }
}
