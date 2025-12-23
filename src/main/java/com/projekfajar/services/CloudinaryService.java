package com.projekfajar.services;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private static final Logger logger = LoggerFactory.getLogger(CloudinaryService.class);
    
    private final Cloudinary cloudinary;

    public Map<String, Object> uploadImage(MultipartFile file) {
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

            logger.info("Image uploaded successfully to Cloudinary: {}", uploadResult.get("url"));
            return uploadResult;
        } catch (IOException e) {
            logger.error("Error uploading image to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image to Cloudinary: " + e.getMessage());
        }
    }

    public void deleteImage(String publicId) {
        try {
            Map<String, Object> result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            logger.info("Image deleted from Cloudinary: {}", result);
        } catch (IOException e) {
            logger.error("Error deleting image from Cloudinary: {}", e.getMessage());
        }
    }

    public String extractPublicId(String imageUrl) {
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
                    return path;
                }
            }
        } catch (Exception e) {
            logger.error("Error extracting public ID from URL: {}", e.getMessage());
        }
        return null;
    }
}
