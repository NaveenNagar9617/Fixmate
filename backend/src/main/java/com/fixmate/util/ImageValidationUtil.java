package com.fixmate.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;

/**
 * Multi-layer server-side image validation utility.
 * Enforces defense-in-depth:
 * 1. File size limit (<= 5 MB)
 * 2. Case-normalized extension whitelist (.jpg, .jpeg, .png, .webp)
 * 3. MIME Content-Type whitelist (image/jpeg, image/png, image/webp)
 * 4. Magic-byte signature verification (JPEG, PNG, WebP)
 * 5. Image stream decoding check where runtime support is available
 */
@Component
@Slf4j
public class ImageValidationUtil {

    public static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");

    // Magic byte signatures
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };
    private static final byte[] WEBP_RIFF_MAGIC = new byte[]{0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] WEBP_HEADER_MAGIC = new byte[]{0x57, 0x45, 0x42, 0x50}; // "WEBP"

    /**
     * Validates an uploaded image file against all defense-in-depth security checks.
     *
     * @param file the MultipartFile to validate
     * @throws IllegalArgumentException if the file fails any validation check
     */
    public void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Photo file cannot be empty");
        }

        // 1. File Size Check
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Photo size exceeds the maximum allowed limit of 5MB (Actual: "
                    + String.format(Locale.ROOT, "%.2f", file.getSize() / (1024.0 * 1024.0)) + "MB)");
        }

        // 2. Extension Check
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("Photo must have a valid file extension (.jpg, .jpeg, .png, .webp)");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Invalid file extension '" + extension + "'. Allowed formats: " + ALLOWED_EXTENSIONS);
        }

        // 3. MIME Content-Type Check
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Invalid Content-Type '" + contentType + "'. Allowed types: " + ALLOWED_MIME_TYPES);
        }

        // 4. Magic-Byte Header Verification & Content Inspection
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            log.error("Failed to read image bytes for validation", e);
            throw new IllegalArgumentException("Failed to read image content");
        }

        if (fileBytes.length < 12) {
            throw new IllegalArgumentException("File is too small to be a valid image");
        }

        boolean validMagicBytes = checkMagicBytes(fileBytes, extension);
        if (!validMagicBytes) {
            throw new IllegalArgumentException("File content does not match allowed image signatures (Magic-byte verification failed)");
        }

        // 5. Image Decoding Verification
        verifyImageDecodability(fileBytes, extension);
    }

    private boolean checkMagicBytes(byte[] bytes, String extension) {
        if (extension.equals(".jpg") || extension.equals(".jpeg")) {
            return matchesPrefix(bytes, 0, JPEG_MAGIC);
        } else if (extension.equals(".png")) {
            return matchesPrefix(bytes, 0, PNG_MAGIC);
        } else if (extension.equals(".webp")) {
            // WebP files start with "RIFF" at offset 0 and "WEBP" at offset 8
            return matchesPrefix(bytes, 0, WEBP_RIFF_MAGIC) && matchesPrefix(bytes, 8, WEBP_HEADER_MAGIC);
        }
        return false;
    }

    private boolean matchesPrefix(byte[] bytes, int offset, byte[] pattern) {
        if (bytes.length < offset + pattern.length) {
            return false;
        }
        for (int i = 0; i < pattern.length; i++) {
            if (bytes[offset + i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }

    private void verifyImageDecodability(byte[] fileBytes, String extension) {
        if (extension.equals(".jpg") || extension.equals(".jpeg") || extension.equals(".png")) {
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                BufferedImage image = ImageIO.read(is);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new IllegalArgumentException("Corrupt or unreadable image file (" + extension + ")");
                }
            } catch (IOException e) {
                log.warn("ImageIO failed to decode image: {}", e.getMessage());
                throw new IllegalArgumentException("Corrupt or invalid image stream (" + extension + ")");
            }
        } else if (extension.equals(".webp")) {
            try (InputStream is = new ByteArrayInputStream(fileBytes)) {
                BufferedImage image = ImageIO.read(is);
                if (image != null && (image.getWidth() <= 0 || image.getHeight() <= 0)) {
                    throw new IllegalArgumentException("Corrupt or invalid WebP image");
                }
            } catch (Exception e) {
                log.debug("WebP ImageIO reader note: {}", e.getMessage());
            }
        }
    }
}
