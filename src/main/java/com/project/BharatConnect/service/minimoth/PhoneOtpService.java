package com.project.BharatConnect.service.minimoth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.project.BharatConnect.error.exception.OtpAlreadySentException;
import com.project.BharatConnect.error.exception.OtpWrongException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Orchestrates phone-based OTP send/verify via MiniMoth.
 * <p>
 * MiniMoth manages the OTP itself on its side; we only need to track whether
 * a send request is still "in flight" (within the 10-minute window) to avoid
 * duplicate sends.  We do this with a local Caffeine cache keyed by phone
 * number — the same pattern as the existing {@code OtpService}.
 */
@Slf4j
@Service
public class PhoneOtpService {

    private final MiniMothService miniMothService;

    /**
     * Tracks phones that have an active OTP in flight.
     * Key   = normalised phone number
     * Value = otp_id returned by MiniMoth (kept for future status-polling)
     */
    private final Cache<String, String> pendingOtpCache;

    public PhoneOtpService(MiniMothService miniMothService) {
        this.miniMothService = miniMothService;
        this.pendingOtpCache = Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Sends a 6-digit OTP to the given phone number via WhatsApp
     * (SMS fallback is automatic).
     *
     * @param phone Indian phone number (+91XXXXXXXXXX or 10 bare digits)
     * @return the otp_id assigned by MiniMoth
     * @throws OtpAlreadySentException if an OTP was already sent in the last
     *         10 minutes and has not yet been verified
     */
    public String sendOtp(String phone) {
        if (pendingOtpCache.getIfPresent(phone) != null) {
            throw new OtpAlreadySentException(
                    "OTP has already been sent to this phone. Please wait 10 minutes."
            );
        }

        String otpId = miniMothService.sendOtp(phone);
        pendingOtpCache.put(phone, otpId);
        return otpId;
    }

    /**
     * Verifies the 6-digit code entered by the user against MiniMoth.
     *
     * @param phone the same phone number used in {@link #sendOtp}
     * @param code  the 6-digit code entered by the user
     * @return {@code true} on success — also invalidates the pending entry so
     *         a fresh OTP can be requested
     * @throws OtpWrongException if the code is incorrect or expired
     */
    public boolean verifyOtp(String phone, String code) {
        boolean valid = miniMothService.verifyOtp(phone, code);

        if (!valid) {
            throw new OtpWrongException(phone);
        }

        // Clear the pending cache so a new OTP can be sent if needed
        pendingOtpCache.invalidate(phone);
        return true;
    }
}
