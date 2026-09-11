package com.ttthinh.shoe_shop_basic.auth.service;

public interface MailService {
    void sendMail(String to, String verifyLink);

    void sendPasswordResetMail(String to, String resetLink);

    void verifyEmail(String token);
}
