package com.project.BharatConnect.service.minimoth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Thin wrapper around the MiniMoth REST API.
 * <p>
 * Base URL : https://api.minimoth.dev
 * Auth     : X-Api-Key header on every request
 * <p>
 * Endpoints used:
 *   POST /v1/otp/send   – send OTP via WhatsApp (SMS fallback automatic)
 *   POST /v1/otp/verify – verify the 6-digit code entered by the user
 */
@Slf4j
@Service
public class MiniMothService {

    private static final String BASE_URL = "https://api.minimoth.dev";

    private final RestClient restClient;

    public MiniMothService(@Value("${minimoth.api-key}") String apiKey) {
        this.restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .defaultHeader("X-Api-Key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    // ------------------------------------------------------------------ //
    //  Send OTP                                                           //
    // ------------------------------------------------------------------ //

    /**
     * Sends a 6-digit OTP to {@code phone} via WhatsApp first,
     * falling back to SMS automatically.
     *
     * @param phone Indian phone number — accepted formats:
     *              {@code +919876543210} or bare 10 digits {@code 9876543210}
     * @return the {@code otp_id} assigned by MiniMoth (can be used to poll
     *         delivery status via {@code GET /v1/otp/status/:otp_id})
     */
    public String sendOtp(String phone) {
        log.info("MiniMoth: sending OTP to {}", maskPhone(phone));

        SendOtpResponse response = restClient.post()
                .uri("/v1/otp/send")
                .body(Map.of("phone", normalise(phone)))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());
                    log.error("MiniMoth sendOtp error {} – {}", res.getStatusCode(), body);
                    throw new MiniMothException("Failed to send OTP via MiniMoth: " + res.getStatusCode());
                })
                .body(SendOtpResponse.class);

        if (response == null || response.getOtpId() == null) {
            throw new MiniMothException("MiniMoth returned an empty response for sendOtp");
        }

        log.info("MiniMoth: OTP dispatched, otp_id={}", response.getOtpId());
        return response.getOtpId();
    }

    // ------------------------------------------------------------------ //
    //  Verify OTP                                                         //
    // ------------------------------------------------------------------ //

    /**
     * Verifies the code entered by the user.
     *
     * @param phone the same phone number used in {@link #sendOtp}
     * @param code  the 6-digit code entered by the user
     * @return {@code true} if the code is correct and has not expired
     * @throws MiniMothException on network error or if MiniMoth returns an
     *         HTTP error status (e.g. 401 INVALID_OTP, 429 rate-limited)
     */
    public boolean verifyOtp(String phone, String code) {
        log.info("MiniMoth: verifying OTP for {}", maskPhone(phone));

        VerifyOtpResponse response = restClient.post()
                .uri("/v1/otp/verify")
                .body(Map.of("phone", normalise(phone), "code", code))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());
                    log.warn("MiniMoth verifyOtp error {} – {}", res.getStatusCode(), body);
                    // 4xx means wrong/expired OTP — surface as a domain error
                    throw new MiniMothException("OTP verification failed: " + res.getStatusCode());
                })
                .body(VerifyOtpResponse.class);

        boolean valid = response != null && response.getAccessToken() != null;
        log.info("MiniMoth: verify result for {} = {}", maskPhone(phone), valid);
        return valid;
    }

    // ------------------------------------------------------------------ //
    //  Helpers                                                            //
    // ------------------------------------------------------------------ //

    /**
     * Ensures the phone number is in {@code +91XXXXXXXXXX} format.
     * Accepts bare 10-digit numbers and strips any leading country code.
     */
    private String normalise(String phone) {
        if (phone == null) return phone;
        String digits = phone.replaceAll("[^\\d]", "");
        if (digits.length() == 10) return "+91" + digits;
        if (digits.length() == 12 && digits.startsWith("91")) return "+" + digits;
        return phone; // let MiniMoth validate and reject if malformed
    }

    /** Returns a masked version of the phone number for safe logging. */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return phone.substring(0, phone.length() - 4) + "****";
    }

    // ------------------------------------------------------------------ //
    //  Response DTOs                                                      //
    // ------------------------------------------------------------------ //

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SendOtpResponse {
        @JsonProperty("otp_id")
        private String otpId;

        @JsonProperty("expires_at")
        private String expiresAt;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class VerifyOtpResponse {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("refresh_token")
        private String refreshToken;

        @JsonProperty("expires_at")
        private String expiresAt;

        @JsonProperty("identity_id")
        private String identityId;
    }
}
