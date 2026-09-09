package com.ttthinh.shoe_shop_basic.controller.promotion;

import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.promotion.VoucherPreviewResponse;
import com.ttthinh.shoe_shop_basic.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/vouchers")
public class VoucherController {
    private final VoucherService voucherService;

    @GetMapping("/preview")
    public ApiResponse<VoucherPreviewResponse> preview(
            @RequestParam String code,
            @RequestParam BigDecimal productTotal
    ) {
        return ApiResponse.<VoucherPreviewResponse>builder()
                .code(200)
                .message("success")
                .result(voucherService.preview(code, productTotal))
                .build();
    }
}
