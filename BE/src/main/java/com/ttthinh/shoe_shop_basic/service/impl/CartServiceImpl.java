package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.cart.AddCartItemRequest;
import com.ttthinh.shoe_shop_basic.dto.response.cart.CartItemResponse;
import com.ttthinh.shoe_shop_basic.dto.response.cart.CartResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.cart.Cart;
import com.ttthinh.shoe_shop_basic.entity.cart.CartItem;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.CartItemMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.CartItemRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.CartRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.InventoryRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.VariantSizeRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.service.CartService;
import com.ttthinh.shoe_shop_basic.service.ProductDiscountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final VariantSizeRepository variantSizeRepository;
    private final InventoryRepository inventoryRepository;
    private final UserAccountRepository userAccountRepository;
    private final CartItemMapper cartItemMapper;
    private final ProductDiscountService productDiscountService;

    @Transactional
    @Override
    public CartResponse getMyCart(UserAccount user) {
        log.warn("UserId = {}", user.getId());

        Cart cart = getOrCreateCart(user);
        List<CartItem> items = cartItemRepository.findByCart(cart);

        List<CartItemResponse> itemResponses = items.stream()
                .map(this::toPricedResponse)
                .toList();

        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .cartId(cart.getId())
                .items(itemResponses)
                .subtotal(subtotal)
                .build();
    }

    private Cart getOrCreateCart(UserAccount user) {
        return cartRepository.findByUser(user)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);
                    return cartRepository.save(cart);
                });
    }


    @Override
    @Transactional
    public CartItemResponse addItem(UserAccount user, AddCartItemRequest request) {

        Cart cart = getOrCreateCart(user);

        VariantSize variantSize = variantSizeRepository.findById(request.getVariantSizeId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        validateSellable(variantSize);

        Inventory inventory = inventoryRepository.findByVariantSize(variantSize)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        if (inventory.getAvailableQuantity() < request.getQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        CartItem item = cartItemRepository
                .findByCartAndVariantSize(cart, variantSize)
                .orElse(null);

        if (item != null) {
            int newQty = item.getQuantity() + request.getQuantity();
            if (newQty > inventory.getAvailableQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
            item.setQuantity(newQty);
        } else {
            item = CartItem.builder()
                    .cart(cart)
                    .variantSize(variantSize)
                    .quantity(request.getQuantity())
                    .build();
        }

        return toPricedResponse(cartItemRepository.save(item));
    }

    @Override
    @Transactional
    public CartItemResponse updateItem(UserAccount user, String cartItemId, int quantity) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        // quantity <= 0 → remove item
        if (quantity <= 0) {
            CartItemResponse response = toPricedResponse(item);
            cartItemRepository.delete(item);
            return response;
        }

        Inventory inventory = inventoryRepository.findByVariantSize(item.getVariantSize())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        validateSellable(item.getVariantSize());

        if (quantity > inventory.getAvailableQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        item.setQuantity(quantity);
        return toPricedResponse(item);
    }


    @Override
    @Transactional
    public CartItemResponse removeItem(UserAccount user, String cartItemId) {

        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> new AppException(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }

        CartItemResponse response = toPricedResponse(item);
        cartItemRepository.delete(item);

        return response;
    }


    @Override
    @Transactional
    public void clearCart(UserAccount user) {
        Cart cart = getOrCreateCart(user);

        List<CartItem> items = cartItemRepository.findByCart(cart);

        cartItemRepository.deleteAll(items);
    }

    private void validateSellable(VariantSize variantSize) {
        if (variantSize == null
                || variantSize.getVariant() == null
                || !Boolean.TRUE.equals(variantSize.getVariant().getActive())
                || variantSize.getVariant().getProduct() == null
                || variantSize.getVariant().getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
    }

    private CartItemResponse toPricedResponse(CartItem item) {
        CartItemResponse response = cartItemMapper.toResponse(item);
        BigDecimal original = item.getVariantSize().getPrice();
        BigDecimal sale = productDiscountService.effectivePrice(item.getVariantSize());
        response.setOriginalPrice(original);
        response.setDiscountPrice(original.subtract(sale).max(BigDecimal.ZERO));
        response.setPrice(sale);
        response.setLineTotal(sale.multiply(BigDecimal.valueOf(item.getQuantity())));
        return response;
    }
}
