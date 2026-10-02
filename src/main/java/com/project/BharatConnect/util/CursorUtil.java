package com.project.BharatConnect.util;

import com.project.BharatConnect.error.exception.InvalidRequestException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

public class CursorUtil {

    private CursorUtil() {}

    public record Cursor(LocalDateTime createdAt, UUID id) {}

    public static String encode(LocalDateTime createdAt, UUID id) {
        if (createdAt == null || id == null) {
            return null;
        }
        String raw = createdAt.toString() + "|" + id.toString();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public static Cursor decode(String cursorStr) {
        if (cursorStr == null || cursorStr.isBlank()) {
            return null;
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(cursorStr);
            String raw = new String(decodedBytes, StandardCharsets.UTF_8);
            String[] parts = raw.split("\\|", 2);
            if (parts.length != 2) {
                throw new InvalidRequestException("Invalid pagination cursor format");
            }
            LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
            UUID id = UUID.fromString(parts[1]);
            return new Cursor(createdAt, id);
        } catch (IllegalArgumentException | DateTimeParseException e) {
            throw new InvalidRequestException("Invalid pagination cursor");
        }
    }
}
