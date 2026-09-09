package com.ttthinh.shoe_shop_basic.service.auth;

public interface MailService {
    public void sendMail(String to, String verifyLink);
    public void verifyEmail(String token);
}
