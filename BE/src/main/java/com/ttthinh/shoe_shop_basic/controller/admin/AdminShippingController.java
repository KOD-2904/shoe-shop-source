package com.ttthinh.shoe_shop_basic.controller.admin;

import com.ttthinh.shoe_shop_basic.dto.request.shipping.GHNSettingsRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shipping.GHNSettingsResponse;
import com.ttthinh.shoe_shop_basic.service.shipping.GHNShippingModeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/shipping/ghn")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminShippingController {
    private final GHNShippingModeService shippingModeService;

    @GetMapping("/settings")
    public GHNSettingsResponse getSettings() {
        return shippingModeService.getSettings();
    }

    @PutMapping("/settings")
    public GHNSettingsResponse updateSettings(@Valid @RequestBody GHNSettingsRequest request) {
        return shippingModeService.update(request);
    }
}
