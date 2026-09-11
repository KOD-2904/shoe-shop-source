package com.ttthinh.shoe_shop_basic.promotion.service;

import com.ttthinh.shoe_shop_basic.promotion.dto.request.VoucherRequest;
import com.ttthinh.shoe_shop_basic.promotion.dto.response.VoucherPreviewResponse;
import com.ttthinh.shoe_shop_basic.promotion.dto.response.VoucherResponse;

import java.math.BigDecimal;
import java.util.List;

public interface VoucherService {
    List<VoucherResponse> getAll();

    VoucherResponse create(VoucherRequest request);

    VoucherResponse setActive(String id, boolean active);

    VoucherPreviewResponse preview(String code, BigDecimal productTotal);

    BigDecimal calculateDiscount(String code, BigDecimal productTotal);

    void markUsed(String code);

    void markUsed(String code, String orderId, String userId);

    void releaseUsed(String code);

    void releaseUsed(String code, String orderId);
}
