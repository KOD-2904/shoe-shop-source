package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.response.payment.VNPayProcessResult;
import com.ttthinh.shoe_shop_basic.dto.response.payment.VNPayUrlResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

public interface PaymentApplicationService {
    VNPayUrlResponse createVNPayPayment(UserAccount user, String orderId, HttpServletRequest request);

    VNPayProcessResult inspectVNPayReturn(Map<String, String> params);

    VNPayProcessResult handleVNPayIpn(Map<String, String> params);
}
