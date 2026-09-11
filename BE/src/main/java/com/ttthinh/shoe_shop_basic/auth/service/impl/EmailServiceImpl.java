package com.ttthinh.shoe_shop_basic.auth.service.impl;

import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.auth.entity.EmailVerifyToken;
import com.ttthinh.shoe_shop_basic.auth.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.auth.repository.EmailVerifyRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.auth.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements MailService {
    private final JavaMailSender mailSender;
    private final EmailVerifyRepository emailVerifyRepository;
    private final UserAccountRepository userAccountRepository;
    @Override
    public void sendMail(String to, String verifyLink) {
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Xác nhận đăng ký tài khoản");
        msg.setText("""
                Chào bạn,
                
                Vui lòng bấm link sau để xác nhận email:
                %s
                
                Link sẽ hết hạn sau một thời gian.
                """.formatted(verifyLink));

        mailSender.send(msg);
    }

    @Override
    public void sendPasswordResetMail(String to, String resetLink) {
        log.warn("Sending password reset mail to={}", to);
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(to);
        msg.setSubject("Reset password");
        msg.setText("""
                Hello,
                
                Please open this link to reset your password:
                %s
                
                This link will expire soon. If you did not request this, please ignore this email.
                """.formatted(resetLink));

        mailSender.send(msg);
        log.warn("Sent password reset mail to={}", to);
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "userProfile", allEntries = true)
    })
    public void verifyEmail(String token) {
        EmailVerifyToken evt = emailVerifyRepository.findByToken(token)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_VALID_TOKEN));
        if (evt.isUsed()) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }
        if (evt.getExpiresAt().isBefore(Instant.now())) {
            throw new AppException(ErrorCode.NOT_VALID_TOKEN);
        }

        UserAccount user = evt.getUser();
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        userAccountRepository.save(user);

        evt.setUsed(true);
        evt.setUsedAt(Instant.now());
        emailVerifyRepository.save(evt);
    }
}
