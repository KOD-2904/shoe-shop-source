package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.promotion.ProductDiscountRequest;
import com.ttthinh.shoe_shop_basic.dto.response.promotion.ProductDiscountResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import com.ttthinh.shoe_shop_basic.entity.promotion.ProductDiscount;
import com.ttthinh.shoe_shop_basic.enums.PromotionTargetType;
import com.ttthinh.shoe_shop_basic.enums.VoucherType;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductDiscountRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.CategoryRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.VariantSizeRepository;
import com.ttthinh.shoe_shop_basic.service.ProductDiscountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductDiscountServiceImpl implements ProductDiscountService {
    private final ProductDiscountRepository discountRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final VariantSizeRepository variantSizeRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDiscountResponse> getAll() {
        return discountRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ProductDiscountResponse create(ProductDiscountRequest request) {
        ProductDiscount discount = ProductDiscount.builder()
                .name(request.getName())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .type(request.getType())
                .value(request.getValue())
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .active(request.getActive() == null || request.getActive())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();
        validate(discount);
        return toResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional
    public ProductDiscountResponse setActive(String id, boolean active) {
        ProductDiscount discount = discountRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_DISCOUNT_NOT_FOUND));
        discount.setActive(active);
        return toResponse(discountRepository.save(discount));
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal effectivePrice(VariantSize variantSize) {
        if (variantSize == null || variantSize.getPrice() == null) {
            return BigDecimal.ZERO;
        }
        Product product = variantSize.getVariant().getProduct();
        BigDecimal discount = bestDiscountAmount(product, variantSize.getId(), variantSize.getPrice());
        return variantSize.getPrice().subtract(discount).max(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal effectiveProductPrice(Product product) {
        if (product == null || product.getBasePrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getBasePrice().subtract(discountAmount(product, product.getBasePrice())).max(BigDecimal.ZERO);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal discountAmount(Product product, BigDecimal basePrice) {
        return bestDiscountAmount(product, null, basePrice);
    }

    private BigDecimal bestDiscountAmount(Product product, String variantSizeId, BigDecimal basePrice) {
        if (product == null || basePrice == null || basePrice.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        List<String> targetIds = new ArrayList<>();
        List<PromotionTargetType> targetTypes = new ArrayList<>();
        targetIds.add(product.getId());
        targetTypes.add(PromotionTargetType.PRODUCT);
        if (product.getCategory() != null) {
            targetIds.add(product.getCategory().getId());
            targetTypes.add(PromotionTargetType.CATEGORY);
        }
        if (variantSizeId != null) {
            targetIds.add(variantSizeId);
            targetTypes.add(PromotionTargetType.VARIANT_SIZE);
        }
        return discountRepository.findActiveCandidates(targetTypes, targetIds, LocalDateTime.now())
                .stream()
                .map(discount -> calculate(discount, basePrice))
                .max(Comparator.naturalOrder())
                .orElse(BigDecimal.ZERO)
                .min(basePrice);
    }

    private BigDecimal calculate(ProductDiscount discount, BigDecimal basePrice) {
        BigDecimal amount = discount.getType() == VoucherType.PERCENT
                ? basePrice.multiply(discount.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
                : discount.getValue();
        if (discount.getMaxDiscountAmount() != null) {
            amount = amount.min(discount.getMaxDiscountAmount());
        }
        return amount.max(BigDecimal.ZERO);
    }

    private void validate(ProductDiscount discount) {
        if (discount.getStartsAt() != null && discount.getEndsAt() != null && discount.getStartsAt().isAfter(discount.getEndsAt())) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
        if (discount.getType() == VoucherType.PERCENT
                && (discount.getValue().compareTo(BigDecimal.ZERO) <= 0 || discount.getValue().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
        if (discount.getType() == VoucherType.FIXED && discount.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
        boolean targetExists = switch (discount.getTargetType()) {
            case PRODUCT -> productRepository.existsById(discount.getTargetId());
            case CATEGORY -> categoryRepository.existsById(discount.getTargetId());
            case VARIANT_SIZE -> variantSizeRepository.existsById(discount.getTargetId());
        };
        if (!targetExists) {
            throw new AppException(ErrorCode.PRODUCT_DISCOUNT_TARGET_NOT_FOUND);
        }
    }

    private ProductDiscountResponse toResponse(ProductDiscount discount) {
        return ProductDiscountResponse.builder()
                .id(discount.getId())
                .name(discount.getName())
                .targetType(discount.getTargetType())
                .targetId(discount.getTargetId())
                .type(discount.getType())
                .value(discount.getValue())
                .maxDiscountAmount(discount.getMaxDiscountAmount())
                .active(discount.getActive())
                .startsAt(discount.getStartsAt())
                .endsAt(discount.getEndsAt())
                .build();
    }
}
