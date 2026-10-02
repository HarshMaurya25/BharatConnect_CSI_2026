package com.project.BharatConnect.notification;

import com.project.BharatConnect.dto.notification.MailDto;
import com.project.BharatConnect.dto.notification.OtpCodeDto;
import com.project.BharatConnect.mail.MailSenderService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@AllArgsConstructor
public class OtpSenderService {
    private final MailSenderService sender;

    @EventListener
    public void sendCode(OtpCodeDto otpCodeDto){
        MailDto mail = MailDto.builder()
                .email(otpCodeDto.getEmail())
                .subject("BharatConnect – Your OTP Verification Code")
                .text(sendOtpText(otpCodeDto.getOTP()))
                .build();
        sender.sendMail(mail);

        log.info("Mail is send to {} with code : {}", otpCodeDto.getEmail() , otpCodeDto.getOTP());
    }

    public String sendOtpText( String otp) {

        String subject = "BharatConnect – Your OTP Verification Code";

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta charset="UTF-8">
                    <title>BharatConnect OTP Verification</title>
                </head>
                
                <body style="margin:0; padding:0; background-color:#f4f6f8;
                             font-family:Arial, sans-serif;">
                
                    <div style="max-width:600px; margin:40px auto;
                                background:#ffffff; border-radius:10px;
                                padding:35px;">
                
                        <h2 style="color:#1f2937;">
                            BharatConnect
                        </h2>
                
                        <p style="color:#4b5563; font-size:15px;">
                            Hello,
                        </p>
                
                        <p style="color:#4b5563; font-size:15px;">
                            Use the following One-Time Password (OTP) to verify
                            your email address and continue with your
                            BharatConnect account.
                        </p>
                
                        <div style="text-align:center; margin:30px 0;">
                            <div style="
                                display:inline-block;
                                background:#f3f4f6;
                                padding:15px 30px;
                                border-radius:8px;
                                font-size:30px;
                                font-weight:bold;
                                letter-spacing:8px;
                                color:#111827;
                            ">
                                %s
                            </div>
                        </div>
                
                        <p style="color:#6b7280; font-size:14px;
                                  text-align:center;">
                            This OTP is valid for <strong>10 minutes</strong>.
                        </p>
                
                        <p style="color:#4b5563; font-size:14px;">
                            If you did not request this verification code,
                            you can safely ignore this email.
                            Do not share your OTP with anyone.
                        </p>
                
                        <hr style="border:none;
                                   border-top:1px solid #e5e7eb;
                                   margin:30px 0;">
                
                        <p style="color:#9ca3af; font-size:12px;
                                  text-align:center;">
                            © 2026 BharatConnect. All rights reserved.
                        </p>
                
                    </div>
                
                </body>
                </html>
                """.formatted(otp);
    }
}
