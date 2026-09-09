package com.ttthinh.shoe_shop_basic.controller.shipping;

import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.service.shipping.GHNLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ghn")
public class GHNLocationController {
    private final GHNLocationService ghnLocationService;

    @GetMapping("/provinces")
    public ApiResponse<Object> getProvinces() {
        return ApiResponse.<Object>builder()
                .code(200)
                .message("Loaded GHN provinces")
                .result(ghnLocationService.getProvinces())
                .build();
    }

    @GetMapping("/districts")
    public ApiResponse<Object> getDistricts(@RequestParam Integer provinceId) {
        return ApiResponse.<Object>builder()
                .code(200)
                .message("Loaded GHN districts")
                .result(ghnLocationService.getDistricts(provinceId))
                .build();
    }

    @GetMapping("/wards")
    public ApiResponse<Object> getWards(@RequestParam Integer districtId) {
        return ApiResponse.<Object>builder()
                .code(200)
                .message("Loaded GHN wards")
                .result(ghnLocationService.getWards(districtId))
                .build();
    }
}
