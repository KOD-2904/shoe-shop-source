package com.ttthinh.shoe_shop_basic.checkout.service;

import com.ttthinh.shoe_shop_basic.checkout.dto.request.CheckoutRequest;
import com.ttthinh.shoe_shop_basic.checkout.dto.request.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.order.dto.request.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.checkout.dto.response.CheckoutPreviewResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.cart.entity.CartItem;
import com.ttthinh.shoe_shop_basic.checkout.entity.ShippingFeeSnapshot;
import com.ttthinh.shoe_shop_basic.customer.entity.Address;
import com.ttthinh.shoe_shop_basic.inventory.entity.Inventory;
import com.ttthinh.shoe_shop_basic.catalog.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.cart.repository.CartItemRepository;
import com.ttthinh.shoe_shop_basic.customer.service.AddressService;
import com.ttthinh.shoe_shop_basic.inventory.repository.InventoryRepository;
import com.ttthinh.shoe_shop_basic.checkout.repository.ShippingFeeSnapshotRepository;
import com.ttthinh.shoe_shop_basic.promotion.service.ProductDiscountService;
import com.ttthinh.shoe_shop_basic.promotion.service.VoucherService;
import com.ttthinh.shoe_shop_basic.shipping.service.GHNShippingService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CheckoutService {
    private final CartItemRepository cartItemRepository;
    private final InventoryRepository inventoryRepository;
    private final AddressService addressService;
    private final GHNShippingService ghnShippingService;
    private final ShippingFeeSnapshotRepository shippingFeeSnapshotRepository;
    private final VoucherService voucherService;
    private final ProductDiscountService productDiscountService;

    @Value("${checkout.shipping-fee-snapshot-ttl-seconds:300}")
    private long snapshotTtlSeconds;

    @Transactional
    public CheckoutPreviewResponse preview(UserAccount user, CheckoutRequest request) {
        Address address = resolveAddress(user, request.getAddressId());
        List<CartItem> selectedItems = validateAndGetSelectedItems(user, request.getCartItemIds());
        CheckoutTotals totals = calculateTotals(selectedItems);
        BigDecimal shippingFee = calculateShippingFee(address, totals);
        BigDecimal discountAmount = voucherService.calculateDiscount(request.getVoucherCode(), totals.getProductTotal());

        ShippingFeeSnapshot snapshot = ShippingFeeSnapshot.builder()
                .userId(user.getId())
                .addressId(address.getId())
                .cartSignature(buildCartSignature(selectedItems))
                .productTotal(totals.getProductTotal())
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .voucherCode(normalizeVoucherCode(request.getVoucherCode()))
                .totalAmount(totals.getProductTotal().add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO))
                .weight(totals.getTotalWeight())
                .length(totals.getMaxLength())
                .width(totals.getMaxWidth())
                .height(totals.getTotalHeight())
                .expiresAt(LocalDateTime.now().plusSeconds(snapshotTtlSeconds))
                .build();
        shippingFeeSnapshotRepository.save(snapshot);

        return CheckoutPreviewResponse.builder()
                .shippingFeeSnapshotId(snapshot.getId())
                .addressId(address.getId())
                .productTotal(snapshot.getProductTotal())
                .shippingFee(snapshot.getShippingFee())
                .discountAmount(snapshot.getDiscountAmount())
                .voucherCode(snapshot.getVoucherCode())
                .totalAmount(snapshot.getTotalAmount())
                .expiresAt(toInstant(snapshot.getExpiresAt()))
                .build();
    }

    @Transactional
    public CheckoutPreviewResponse previewBuyNow(UserAccount user, BuyNowRequest request) {
        Address address = resolveAddress(user, request.getAddressId());
        Inventory inventory = inventoryRepository
                .findLockedByVariantSizeId(request.getVariantSizeId())
                .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
        validateSellable(inventory);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.QUANTITY_NOT_VALID);
        }
        if (request.getQuantity() > inventory.getAvailableQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        CheckoutTotals totals = CheckoutTotals.builder()
                .productTotal(productDiscountService.effectivePrice(inventory.getVariantSize()).multiply(BigDecimal.valueOf(request.getQuantity())))
                .totalWeight(200 * request.getQuantity())
                .maxLength(0)
                .maxWidth(0)
                .totalHeight(0)
                .build();
        BigDecimal shippingFee = calculateShippingFee(address, totals);
        BigDecimal discountAmount = voucherService.calculateDiscount(request.getVoucherCode(), totals.getProductTotal());

        ShippingFeeSnapshot snapshot = ShippingFeeSnapshot.builder()
                .userId(user.getId())
                .addressId(address.getId())
                .cartSignature(buildBuyNowSignature(request.getVariantSizeId(), request.getQuantity()))
                .productTotal(totals.getProductTotal())
                .shippingFee(shippingFee)
                .discountAmount(discountAmount)
                .voucherCode(normalizeVoucherCode(request.getVoucherCode()))
                .totalAmount(totals.getProductTotal().add(shippingFee).subtract(discountAmount).max(BigDecimal.ZERO))
                .weight(totals.getTotalWeight())
                .length(totals.getMaxLength())
                .width(totals.getMaxWidth())
                .height(totals.getTotalHeight())
                .expiresAt(LocalDateTime.now().plusSeconds(snapshotTtlSeconds))
                .build();
        shippingFeeSnapshotRepository.save(snapshot);

        return CheckoutPreviewResponse.builder()
                .shippingFeeSnapshotId(snapshot.getId())
                .addressId(address.getId())
                .productTotal(snapshot.getProductTotal())
                .shippingFee(snapshot.getShippingFee())
                .discountAmount(snapshot.getDiscountAmount())
                .voucherCode(snapshot.getVoucherCode())
                .totalAmount(snapshot.getTotalAmount())
                .expiresAt(toInstant(snapshot.getExpiresAt()))
                .build();
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(ZoneId.systemDefault()).toInstant();
    }

    public Address resolveAddress(UserAccount user, String addressId) {
        Address address;
        if (StringUtils.hasText(addressId)) {
            address = addressService.getAddressById(addressId);
        } else {
            address = addressService.getDefaultAddress(user);
        }
        if (address == null) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        if (!address.getUserId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        if (address.getDistrictId() == null
                || !StringUtils.hasText(address.getWardCode())
                || !StringUtils.hasText(address.getReceiverName())
                || !StringUtils.hasText(address.getPhoneNumber())
                || (!StringUtils.hasText(address.getFullAddress()) && !StringUtils.hasText(address.getDetailAddress()))) {
            throw new AppException(ErrorCode.ADDRESS_NOT_FOUND);
        }
        return address;
    }

    public List<CartItem> validateAndGetSelectedItems(UserAccount user, List<String> cartItemIds) {
        if (cartItemIds == null || cartItemIds.isEmpty()) {
            throw new AppException(ErrorCode.CART_EMPTY);
        }

        List<CartItem> selectedItems = cartItemRepository.findAllById(cartItemIds);
        if (selectedItems.size() != cartItemIds.size()) {
            throw new AppException(ErrorCode.CART_ITEM_NOT_FOUND);
        }

        for (CartItem item : selectedItems) {
            if (!item.getCart().getUser().getId().equals(user.getId())) {
                throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
            }
        }
        return selectedItems;
    }

    public CheckoutTotals calculateTotals(List<CartItem> items) {
        BigDecimal productTotal = BigDecimal.ZERO;
        int totalWeight = 0;

        for (CartItem item : items) {
            Inventory inventory = inventoryRepository
                    .findLockedByVariantSizeId(item.getVariantSize().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
            validateSellable(inventory);

            if (item.getQuantity() > inventory.getAvailableQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }

            productTotal = productTotal.add(
                    productDiscountService.effectivePrice(item.getVariantSize()).multiply(BigDecimal.valueOf(item.getQuantity()))
            );
            totalWeight += 200 * item.getQuantity();
        }

        return CheckoutTotals.builder()
                .productTotal(productTotal)
                .totalWeight(totalWeight)
                .maxLength(0)
                .maxWidth(0)
                .totalHeight(0)
                .build();
    }

    public String buildCartSignature(List<CartItem> items) {
        return items.stream()
                .sorted(Comparator.comparing(CartItem::getId))
                .map(item -> item.getId() + ":" + item.getVariantSize().getId() + ":" + item.getQuantity())
                .collect(Collectors.joining("|"));
    }

    public String buildBuyNowSignature(String variantSizeId, Integer quantity) {
        return "BUY_NOW:" + variantSizeId + ":" + quantity;
    }

    private BigDecimal calculateShippingFee(Address address, CheckoutTotals totals) {
        if (totals.getTotalWeight() == 0) {
            return BigDecimal.ZERO;
        }
        ShippingFeeRequest request = ShippingFeeRequest.builder()
                .toDistrictId(address.getDistrictId())
                .toWardCode(address.getWardCode())
                .weight(totals.getTotalWeight())
                .length(totals.getMaxLength())
                .width(totals.getMaxWidth())
                .height(totals.getTotalHeight())
                .insuranceValue(0)
                .build();
        try {
            return BigDecimal.valueOf(ghnShippingService.calculateShippingFee(request));
        } catch (Exception exception) {
            log.error("Failed to calculate GHN shipping fee, using fallback", exception);
            int baseFee = 30000;
            int extraWeight = Math.max(0, totals.getTotalWeight() - 1000);
            int extraFee = (extraWeight / 500) * 5000;
            return BigDecimal.valueOf(baseFee + extraFee);
        }
    }

    private String normalizeVoucherCode(String voucherCode) {
        return StringUtils.hasText(voucherCode) ? voucherCode.trim().toUpperCase() : null;
    }

    private void validateSellable(Inventory inventory) {
        if (inventory == null
                || inventory.getVariantSize() == null
                || inventory.getVariantSize().getVariant() == null
                || !Boolean.TRUE.equals(inventory.getVariantSize().getVariant().getActive())
                || inventory.getVariantSize().getVariant().getProduct() == null
                || inventory.getVariantSize().getVariant().getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class CheckoutTotals {
        BigDecimal productTotal;
        Integer totalWeight;
        Integer maxLength;
        Integer maxWidth;
        Integer totalHeight;
    }
}
