package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryLockService {
    private final InventoryRepository inventoryRepository;

    @Transactional
    public void lockCartItems(List<CartItem> items) {
        for (CartItem item : items) {
            lockVariantSize(item.getVariantSize().getId(), item.getQuantity());
        }
    }

    @Transactional
    public void lockVariantSize(String variantSizeId, int quantity) {
        Inventory inventory = getLockedInventory(variantSizeId);
        if (quantity <= 0 || inventory.getAvailableQuantity() < quantity) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }
        inventory.setQuantityLocked(nullToZero(inventory.getQuantityLocked()) + quantity);
        inventoryRepository.save(inventory);
    }

    @Transactional
    public void deductLocked(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = getLockedInventory(item.getVariantSize().getId());
            int quantity = item.getQuantity();
            int currentQuantity = nullToZero(inventory.getQuantity());
            int currentLocked = nullToZero(inventory.getQuantityLocked());

            if (currentQuantity < quantity || currentLocked < quantity) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            inventory.setQuantity(currentQuantity - quantity);
            inventory.setQuantityLocked(currentLocked - quantity);
            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    public void releaseLocked(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = getLockedInventory(item.getVariantSize().getId());
            int quantity = item.getQuantity();
            int currentLocked = nullToZero(inventory.getQuantityLocked());

            if (currentLocked < quantity) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            inventory.setQuantityLocked(currentLocked - quantity);
            inventoryRepository.save(inventory);
        }
    }

    @Transactional
    public void restoreDeducted(Order order) {
        for (OrderItem item : order.getItems()) {
            Inventory inventory = getLockedInventory(item.getVariantSize().getId());
            int quantity = item.getQuantity();
            int currentQuantity = nullToZero(inventory.getQuantity());

            inventory.setQuantity(currentQuantity + quantity);
            inventoryRepository.save(inventory);
        }
    }

    private Inventory getLockedInventory(String variantSizeId) {
        return inventoryRepository.findLockedByVariantSizeId(variantSizeId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
    }

    private int nullToZero(Integer value) {
        return value == null ? 0 : value;
    }
}
