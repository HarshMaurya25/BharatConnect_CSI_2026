package com.project.BharatConnect.service.user;

import com.github.benmanes.caffeine.cache.Cache;
import com.project.BharatConnect.error.exception.OtpAlreadySentException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;

@Service
@AllArgsConstructor
public class OtpService {

    private final Cache<String, String> otpCache;

    public String generateOtp(String email) {
        String existingOtp = otpCache.getIfPresent(email);

        if (existingOtp != null) {
            throw new OtpAlreadySentException(
                    "OTP has already been sent to this email. Please wait 10 minutes."
            );
        }

        SecureRandom random = new SecureRandom();
        Integer code = 100_000 + random.nextInt(900_000);
        String otp = String.valueOf(code);
        otpCache.put(email, otp);

        return otp;
    }

    public boolean verifyOtp(String email, String otp) {

        String storedOtp = otpCache.getIfPresent(email);
        if (storedOtp == null) {
            return false;
        }
        if (!storedOtp.equals(otp)) {
            return false;
        }
        otpCache.invalidate(email);

        return true;
    }
}
