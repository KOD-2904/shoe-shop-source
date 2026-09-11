package com.ttthinh.shoe_shop_basic.promotion.controller.admin;

import com.ttthinh.shoe_shop_basic.promotion.dto.request.ProductDiscountRequest;
import com.ttthinh.shoe_shop_basic.promotion.dto.response.ProductDiscountResponse;
import com.ttthinh.shoe_shop_basic.promotion.service.ProductDiscountService;
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
@RequestMapping("/api/admin/product-discounts")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProductDiscountController {
    private final ProductDiscountService discountService;

    @GetMapping
    public List<ProductDiscountResponse> getAll() {
        return discountService.getAll();
    }

    @PostMapping
    public ProductDiscountResponse create(@RequestBody @Valid ProductDiscountRequest request) {
        return discountService.create(request);
    }

    @PatchMapping("/{id}/active")
    public ProductDiscountResponse setActive(@PathVariable String id, @RequestParam boolean active) {
        return discountService.setActive(id, active);
    }
}
