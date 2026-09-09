package com.ttthinh.shoe_shop_basic.service.impl.auth;

import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.auth.EmailVerifyToken;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.EmailVerifyRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.service.auth.MailService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@RequiredArgsConstructor
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
