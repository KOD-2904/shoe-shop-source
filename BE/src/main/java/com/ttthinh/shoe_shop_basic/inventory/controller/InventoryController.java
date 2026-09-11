package com.ttthinh.shoe_shop_basic.inventory.controller;

import com.ttthinh.shoe_shop_basic.common.dto.ApiResponse;
import com.ttthinh.shoe_shop_basic.inventory.dto.response.InventoryListItemResponse;
import com.ttthinh.shoe_shop_basic.inventory.dto.response.InventoryResponse;
import com.ttthinh.shoe_shop_basic.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {
    private final InventoryService inventoryService;

    @GetMapping
    public ApiResponse<List<InventoryListItemResponse>> getAllInventory() {
        return ApiResponse.<List<InventoryListItemResponse>>builder()
                .result(inventoryService.getAllInventory())
                .code(200)
                .message("success")
                .build();
    }

    @GetMapping("/low-stock")
    public ApiResponse<List<InventoryListItemResponse>> getLowStockInventory(
            @RequestParam(defaultValue = "5") int threshold
    ) {
        return ApiResponse.<List<InventoryListItemResponse>>builder()
                .result(inventoryService.getLowStockInventory(threshold))
                .code(200)
                .message("success")
                .build();
    }

    @GetMapping("/{variantSizeId}")
    public ApiResponse<InventoryResponse> getInventory(@PathVariable String variantSizeId) {
        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.getInventory(variantSizeId))
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping("/increase")
    public ApiResponse<InventoryResponse> increase(
            @RequestParam String variantSizeId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.increaseStock(variantSizeId, quantity))
                .code(200)
                .message("success")
                .build();
    }

    @PostMapping("/decrease")
    public ApiResponse<InventoryResponse> decrease(
            @RequestParam String variantSizeId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.decreaseStock(variantSizeId, quantity))
                .code(200)
                .message("success")
                .build();
    }

    @PutMapping("/set")
    public ApiResponse<InventoryResponse> set(
            @RequestParam String variantSizeId,
            @RequestParam int quantity
    ) {

        return ApiResponse.<InventoryResponse>builder()
                .result(inventoryService.setStock(variantSizeId, quantity))
                .code(200)
                .message("success")
                .build();
    }
}
