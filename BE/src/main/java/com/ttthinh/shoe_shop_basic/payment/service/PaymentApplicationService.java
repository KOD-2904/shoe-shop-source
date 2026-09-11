package com.ttthinh.shoe_shop_basic.payment.service;

import com.ttthinh.shoe_shop_basic.payment.dto.response.VNPayProcessResult;
import com.ttthinh.shoe_shop_basic.payment.dto.response.VNPayUrlResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentApplicationService {
    VNPayUrlResponse createVNPayPayment(UserAccount user, String orderId, HttpServletRequest request);

    VNPayProcessResult inspectVNPayReturn(Map<String, String> params);

    VNPayProcessResult handleVNPayIpn(Map<String, String> params);
}
