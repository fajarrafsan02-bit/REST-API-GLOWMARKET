package com.projekfajar.produk.service;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public Map<String, Object> uploadImage(MultipartFile file) {
        log.info("Uploading image to Cloudinary: filename={} size={} bytes",
                file.getOriginalFilename(), file.getSize());
        try {
            // Generate unique public ID
            String publicId = "products/" + UUID.randomUUID().toString();
            
            // Upload to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap(
                            "public_id", publicId,
                            "folder", "fajar-gold",
                            "resource_type", "image",
                            "transformation", new com.cloudinary.Transformation()
                                    .width(1000)
                                    .height(1000)
                                    .crop("limit")
                                    .quality("auto")
                                    .fetchFormat("auto")));

            log.info("Image uploaded to Cloudinary: publicId={} url={}",
                    uploadResult.get("public_id"), uploadResult.get("secure_url"));
            return uploadResult;
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary filename={}: {}",
                    file.getOriginalFilename(), e.getMessage(), e);
            throw new RuntimeException("Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }

    public void deleteImage(String publicId) {
        log.info("Deleting image from Cloudinary: publicId={}", publicId);
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Image deleted from Cloudinary: publicId={} result={}", publicId, result.get("result"));
        } catch (IOException e) {
            log.error("Failed to delete image from Cloudinary publicId={}: {}", publicId, e.getMessage(), e);
        }
    }

    public String extractPublicId(String imageUrl) {
        log.debug("Extracting Cloudinary public id from url={}", imageUrl);
        // Extract public ID from Cloudinary URL
        // Example URL: https://res.cloudinary.com/demo/image/upload/v1234567890/fajar-gold/products/uuid.jpg
        try {
            if (imageUrl != null && imageUrl.contains("cloudinary.com")) {
                String[] parts = imageUrl.split("/upload/");
                if (parts.length > 1) {
                    String path = parts[1];
                    // Remove version if exists (v1234567890/)
                    path = path.replaceFirst("v\\d+/", "");
                    // Remove file extension
                    int lastDot = path.lastIndexOf(".");
                    if (lastDot > 0) {
                        path = path.substring(0, lastDot);
                    }
                    log.debug("Extracted publicId={} from url={}", path, imageUrl);
                    return path;
                }
            }
        } catch (Exception e) {
            log.error("Failed to extract public id from url={}: {}", imageUrl, e.getMessage(), e);
        }
        return null;
    }
}
