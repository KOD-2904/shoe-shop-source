package com.ttthinh.shoe_shop_basic.promotion.controller.admin;

import com.ttthinh.shoe_shop_basic.promotion.dto.request.VoucherRequest;
import com.ttthinh.shoe_shop_basic.promotion.dto.response.VoucherResponse;
import com.ttthinh.shoe_shop_basic.promotion.service.VoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/vouchers")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVoucherController {
    private final VoucherService voucherService;

    @GetMapping
    public List<VoucherResponse> getAll() {
        return voucherService.getAll();
    }

    @PostMapping
    public VoucherResponse create(@RequestBody @Valid VoucherRequest request) {
        return voucherService.create(request);
    }

    @PatchMapping("/{id}/active")
    public VoucherResponse setActive(@PathVariable String id, @RequestParam boolean active) {
        return voucherService.setActive(id, active);
    }
}
