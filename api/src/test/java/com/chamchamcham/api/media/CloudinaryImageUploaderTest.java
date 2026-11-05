package com.chamchamcham.api.media;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class CloudinaryImageUploaderTest {
    @Mock private Cloudinary cloudinary;
    @Mock private Uploader uploader;
    private CloudinaryImageUploader adapter;
    @BeforeEach void setUp() { when(cloudinary.uploader()).thenReturn(uploader); adapter = new CloudinaryImageUploader(cloudinary); }
    @Test @DisplayName("delete maps ok and invalidates cached image")
    void deleteMapsOkAndInvalidatesCachedImage() throws Exception {
        when(uploader.destroy(eq("community/post-image"), anyMap())).thenReturn(Map.of("result", "ok"));
        adapter.delete("community/post-image");
        verify(uploader).destroy(eq("community/post-image"), argThat(options -> "image".equals(options.get("resource_type")) && Boolean.TRUE.equals(options.get("invalidate"))));
    }
    @Test @DisplayName("delete maps not found as terminal success")
    void deleteMapsNotFoundAsTerminalSuccess() throws Exception {
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result", "not found"));
        adapter.delete("missing");
    }
    @Test @DisplayName("delete wraps provider failure")
    void deleteWrapsProviderFailure() throws Exception {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IllegalStateException("down"));
        var exception = assertThrows(BusinessException.class, () -> adapter.delete("community/post-image"));
        assertEquals(ErrorCode.MEDIA_DELETE_FAILED, exception.getErrorCode());
    }
}
