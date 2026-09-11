package com.ttthinh.shoe_shop_basic.inventory.service;

import com.ttthinh.shoe_shop_basic.inventory.dto.response.InventoryResponse;
import com.ttthinh.shoe_shop_basic.inventory.dto.response.InventoryListItemResponse;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface InventoryService {
    InventoryResponse increaseStock(String variantSizeId, int amount);
    InventoryResponse getInventory(String variantSizeId);
    List<InventoryListItemResponse> getAllInventory();
    List<InventoryListItemResponse> getLowStockInventory(int threshold);

    @Transactional
    InventoryResponse decreaseStock(String variantSizeId, int amount);

    @Transactional
    InventoryResponse setStock(String variantSizeId, int quantity);
}
