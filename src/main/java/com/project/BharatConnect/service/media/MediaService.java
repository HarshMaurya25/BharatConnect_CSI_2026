package com.project.BharatConnect.service.media;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.project.BharatConnect.error.exception.InvalidMediaException;
import com.project.BharatConnect.error.exception.MediaProcessingException;
import com.project.BharatConnect.error.exception.MediaTooLargeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final Cloudinary cloudinary;

    // ---------------- LIMITS ----------------

    private static final long POST_IMAGE_MAX_SIZE = 10 * 1024 * 1024;     // 10 MB
    private static final long PROFILE_IMAGE_MAX_SIZE = 5 * 1024 * 1024;  // 5 MB
    private static final long VIDEO_MAX_SIZE = 100 * 1024 * 1024;        // 100 MB

    private static final int POST_MAX_WIDTH = 1920;
    private static final int POST_MAX_HEIGHT = 1080;

    private static final int PROFILE_SIZE = 512;

    // ---------------- CONTENT TYPES ----------------

    private static final Set<String> IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private static final Set<String> VIDEO_TYPES = Set.of(
            "video/mp4",
            "video/webm",
            "video/quicktime"
    );

    public Map<String, Object> uploadPostImage(
            MultipartFile file,
            UUID postId
    ) {

        validateFile(
                file,
                POST_IMAGE_MAX_SIZE,
                IMAGE_TYPES,
                "Post image"
        );

        byte[] imageBytes = processPostImage(file);

        Map params = ObjectUtils.asMap(
                "public_id", "posts/" + postId,
                "overwrite", true,
                "resource_type", "image"
        );

        return upload(imageBytes, params);
    }


    public Map uploadVideo(
            MultipartFile file,
            UUID postId
    ) {

        validateFile(
                file,
                VIDEO_MAX_SIZE,
                VIDEO_TYPES,
                "Video"
        );

        try {

            Map params = ObjectUtils.asMap(
                    "public_id", "videos/" + postId,
                    "overwrite", true,
                    "resource_type", "video"
            );

            return cloudinary.uploader()
                    .upload(file.getBytes(), params);

        } catch (IOException e) {

            log.error(
                    "Video upload failed for post: {}",
                    postId,
                    e
            );

            throw new MediaProcessingException(
                    "Failed to upload video"
            );
        }
    }


    public Map<String, Object> uploadProfileImage(
            MultipartFile file,
            UUID userId
    ) {

        validateFile(
                file,
                PROFILE_IMAGE_MAX_SIZE,
                IMAGE_TYPES,
                "Profile image"
        );

        byte[] imageBytes = processProfileImage(file);

        Map<String, Object> params = ObjectUtils.asMap(
                "public_id", "profiles/" + userId,
                "overwrite", true,
                "resource_type", "image"
        );

        return upload(imageBytes, params);
    }

    public String getPostImage(UUID postId) {

        return cloudinary
                .url()
                .resourceType("image")
                .generate("posts/" + postId);
    }


    public String getProfileImage(UUID userId) {

        return cloudinary
                .url()
                .resourceType("image")
                .generate("profiles/" + userId);
    }


    public String getVideo(UUID postId) {

        return cloudinary
                .url()
                .resourceType("video")
                .generate("videos/" + postId);
    }

    private void validateFile(
            MultipartFile file,
            long maxSize,
            Set<String> allowedTypes,
            String fileType
    ) {

        if (file == null || file.isEmpty()) {

            throw new InvalidMediaException(
                    fileType + " is required"
            );
        }

        if (file.getSize() > maxSize) {

            throw new MediaTooLargeException(
                    fileType + " exceeds the maximum allowed size"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null ||
                !allowedTypes.contains(contentType.toLowerCase())) {

            throw new InvalidMediaException(
                    "Unsupported " + fileType.toLowerCase() + " format"
            );
        }
    }

    private byte[] processPostImage(MultipartFile file) {

        try {

            BufferedImage original =
                    ImageIO.read(file.getInputStream());

            if (original == null) {

                throw new InvalidMediaException(
                        "Uploaded file is not a valid image"
                );
            }

            int width = original.getWidth();
            int height = original.getHeight();

            // No resize required
            if (width <= POST_MAX_WIDTH &&
                    height <= POST_MAX_HEIGHT) {

                return file.getBytes();
            }

            double widthRatio =
                    (double) POST_MAX_WIDTH / width;

            double heightRatio =
                    (double) POST_MAX_HEIGHT / height;

            double ratio =
                    Math.min(widthRatio, heightRatio);

            int newWidth =
                    (int) Math.round(width * ratio);

            int newHeight =
                    (int) Math.round(height * ratio);

            BufferedImage resized =
                    new BufferedImage(
                            newWidth,
                            newHeight,
                            BufferedImage.TYPE_INT_RGB
                    );

            Graphics2D graphics =
                    resized.createGraphics();

            graphics.drawImage(
                    original,
                    0,
                    0,
                    newWidth,
                    newHeight,
                    null
            );

            graphics.dispose();

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    resized,
                    "jpg",
                    output
            );

            return output.toByteArray();

        } catch (IOException e) {

            log.error("Post image processing failed", e);

            throw new MediaProcessingException(
                    "Failed to process post image"
            );
        }
    }

    private byte[] processProfileImage(MultipartFile file) {

        try {

            BufferedImage original =
                    ImageIO.read(file.getInputStream());

            if (original == null) {

                throw new InvalidMediaException(
                        "Uploaded file is not a valid image"
                );
            }

            int width = original.getWidth();
            int height = original.getHeight();

            int cropSize = Math.min(width, height);

            int x = (width - cropSize) / 2;
            int y = (height - cropSize) / 2;

            BufferedImage cropped =
                    original.getSubimage(
                            x,
                            y,
                            cropSize,
                            cropSize
                    );

            BufferedImage resized =
                    new BufferedImage(
                            PROFILE_SIZE,
                            PROFILE_SIZE,
                            BufferedImage.TYPE_INT_RGB
                    );

            Graphics2D graphics =
                    resized.createGraphics();

            graphics.drawImage(
                    cropped,
                    0,
                    0,
                    PROFILE_SIZE,
                    PROFILE_SIZE,
                    null
            );

            graphics.dispose();

            ByteArrayOutputStream output =
                    new ByteArrayOutputStream();

            ImageIO.write(
                    resized,
                    "jpg",
                    output
            );

            return output.toByteArray();

        } catch (IOException e) {

            log.error(
                    "Profile image processing failed",
                    e
            );

            throw new MediaProcessingException(
                    "Failed to process profile image"
            );
        }
    }

    private Map upload(
            byte[] data,
            Map<String, Object> params
    ) {

        try {

            return cloudinary
                    .uploader()
                    .upload(data, params);

        } catch (IOException e) {

            log.error("Cloudinary upload failed", e);

            throw new MediaProcessingException(
                    "Failed to upload media"
            );
        }
    }
}