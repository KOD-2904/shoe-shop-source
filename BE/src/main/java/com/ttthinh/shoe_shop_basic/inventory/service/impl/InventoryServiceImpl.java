package com.ttthinh.shoe_shop_basic.inventory.service.impl;

import com.ttthinh.shoe_shop_basic.inventory.dto.response.InventoryResponse;
import com.ttthinh.shoe_shop_basic.inventory.dto.response.InventoryListItemResponse;
import com.ttthinh.shoe_shop_basic.inventory.entity.Inventory;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.inventory.mapper.InventoryMapper;
import com.ttthinh.shoe_shop_basic.inventory.repository.InventoryRepository;
import com.ttthinh.shoe_shop_basic.inventory.service.InventoryService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {
     private final InventoryRepository inventoryRepository;
     private final InventoryMapper inventoryMapper;

    @Override
    public InventoryResponse getInventory(String variantSizeId) {
        var result =  inventoryRepository.getInventoryByVariantSize_Id(variantSizeId);
        return inventoryMapper.inventoryToInventory(result);
    }

    @Override
    public List<InventoryListItemResponse> getAllInventory() {
        return inventoryRepository.findAllWithCatalog().stream()
                .map(inventory -> toListItem(inventory, 5))
                .toList();
    }

    @Override
    public List<InventoryListItemResponse> getLowStockInventory(int threshold) {
        int normalizedThreshold = Math.max(0, threshold);
        return inventoryRepository.findLowStockWithCatalog(normalizedThreshold).stream()
                .map(inventory -> toListItem(inventory, normalizedThreshold))
                .toList();
    }
    @Transactional
    @Override
    public InventoryResponse increaseStock(String variantSizeId, int amount) {
        if (amount <= 0) throw new AppException(ErrorCode.QUANTITY_NOT_VALID);

        Inventory inventory = inventoryRepository
                .findByVariantSizeId(variantSizeId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        inventory.setQuantity(inventory.getQuantity() + amount);
        return inventoryMapper.inventoryToInventory(inventory);
    }
    @Transactional
    @Override
    public InventoryResponse decreaseStock(String variantSizeId, int amount) {
        try{
            if (amount <= 0) throw new AppException(ErrorCode.QUANTITY_NOT_VALID);

            Inventory inventory = inventoryRepository
                    .findByVariantSizeId(variantSizeId)
                    .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

            if (inventory.getQuantity() < amount) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            inventory.setQuantity(inventory.getQuantity() - amount);
            return inventoryMapper.inventoryToInventory(inventory);
        }
        catch (OptimisticLockException e) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }
    }
    @Transactional
    @Override
    public InventoryResponse setStock(String variantSizeId, int quantity) {
        if (quantity < 0) {
            throw new AppException(ErrorCode.QUANTITY_NOT_VALID);
        }

        Inventory inventory = inventoryRepository
                .findByVariantSizeId(variantSizeId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        inventory.setQuantity(quantity);
        return inventoryMapper.inventoryToInventory(inventory);
    }

    private InventoryListItemResponse toListItem(Inventory inventory, int threshold) {
        var size = inventory.getVariantSize();
        var variant = size.getVariant();
        var product = variant.getProduct();
        int quantity = inventory.getQuantity() != null ? inventory.getQuantity() : 0;
        int locked = inventory.getQuantityLocked() != null ? inventory.getQuantityLocked() : 0;
        int available = Math.max(0, quantity - locked);
        return InventoryListItemResponse.builder()
                .inventoryId(inventory.getId())
                .productId(product.getId())
                .productName(product.getName())
                .variantId(variant.getId())
                .color(variant.getColor())
                .variantSizeId(size.getId())
                .size(size.getSize())
                .sku(size.getSku())
                .price(size.getPrice())
                .quantity(quantity)
                .quantityLocked(locked)
                .availableQuantity(available)
                .lowStock(available <= threshold)
                .outOfStock(available <= 0)
                .build();
    }
}
