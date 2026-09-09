package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.entity.promotion.Voucher;
import com.ttthinh.shoe_shop_basic.enums.VoucherType;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.VoucherRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.VoucherUsageRepository;
import com.ttthinh.shoe_shop_basic.service.VoucherService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class VoucherServiceIntegrationTest {
    @Autowired
    private VoucherService voucherService;

    @Autowired
    private VoucherRepository voucherRepository;

    @Autowired
    private VoucherUsageRepository voucherUsageRepository;

    @Test
    void markAndReleaseWithOrderIdIsIdempotent() {
        String code = uniqueCode("LEDGER");
        Voucher voucher = saveVoucher(code, 2);

        voucherService.markUsed(code, "order-1", "user-1");
        voucherService.markUsed(code, "order-1", "user-1");

        assertEquals(1, voucherRepository.findById(voucher.getId()).orElseThrow().getUsedCount());
        assertEquals(1, voucherUsageRepository.findAll().stream()
                .filter(usage -> "order-1".equals(usage.getOrderId()) && code.equals(usage.getVoucherCode()))
                .count());

        voucherService.releaseUsed(code, "order-1");
        voucherService.releaseUsed(code, "order-1");

        assertEquals(0, voucherRepository.findById(voucher.getId()).orElseThrow().getUsedCount());
    }

    @Test
    void markUsedRejectsUsageLimit() {
        String code = uniqueCode("LIMIT");
        saveVoucher(code, 1);

        voucherService.markUsed(code, "order-1", "user-1");

        AppException exception = assertThrows(AppException.class,
                () -> voucherService.markUsed(code, "order-2", "user-2"));
        assertEquals(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED, exception.getErrorCode());
    }

    private Voucher saveVoucher(String code, int usageLimit) {
        return voucherRepository.save(Voucher.builder()
                .code(code)
                .name(code)
                .type(VoucherType.FIXED)
                .value(BigDecimal.valueOf(10000))
                .minOrderAmount(BigDecimal.ZERO)
                .usageLimit(usageLimit)
                .usedCount(0)
                .active(true)
                .build());
    }

    private String uniqueCode(String prefix) {
        return prefix + System.nanoTime();
    }
}
