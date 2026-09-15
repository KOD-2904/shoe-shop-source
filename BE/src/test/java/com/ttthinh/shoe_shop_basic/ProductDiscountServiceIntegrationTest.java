package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import com.ttthinh.shoe_shop_basic.catalog.entity.ProductVariant;
import com.ttthinh.shoe_shop_basic.catalog.entity.VariantSize;
import com.ttthinh.shoe_shop_basic.catalog.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.catalog.repository.ProductRepository;
import com.ttthinh.shoe_shop_basic.catalog.repository.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.catalog.repository.VariantSizeRepository;
import com.ttthinh.shoe_shop_basic.promotion.dto.request.ProductDiscountRequest;
import com.ttthinh.shoe_shop_basic.promotion.enums.PromotionTargetType;
import com.ttthinh.shoe_shop_basic.promotion.enums.VoucherType;
import com.ttthinh.shoe_shop_basic.promotion.service.ProductDiscountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
class ProductDiscountServiceIntegrationTest {
    @Autowired
    private ProductDiscountService productDiscountService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private VariantSizeRepository variantSizeRepository;

    @Test
    void productTargetDiscountAppliesToProductAndVariantPrices() {
        String suffix = String.valueOf(System.nanoTime());
        Product product = productRepository.save(Product.builder()
                .name("Discount Shoe " + suffix)
                .slug("discount-shoe-" + suffix)
                .basePrice(BigDecimal.valueOf(100000))
                .status(ProductStatus.ACTIVE)
                .build());
        ProductVariant variant = variantRepository.save(ProductVariant.builder()
                .product(product)
                .color("Black")
                .active(true)
                .build());
        VariantSize size = variantSizeRepository.save(VariantSize.builder()
                .variant(variant)
                .size("42")
                .sku("DISC-" + suffix)
                .price(BigDecimal.valueOf(200000))
                .build());

        productDiscountService.create(ProductDiscountRequest.builder()
                .name("Product sale")
                .targetType(PromotionTargetType.PRODUCT)
                .targetId(product.getId())
                .type(VoucherType.PERCENT)
                .value(BigDecimal.TEN)
                .active(true)
                .startsAt(Instant.now().minusSeconds(60))
                .endsAt(Instant.now().plusSeconds(3600))
                .build());

        assertEquals(0, BigDecimal.valueOf(90000).compareTo(productDiscountService.effectiveProductPrice(product)));
        assertEquals(0, BigDecimal.valueOf(180000).compareTo(productDiscountService.effectivePrice(size)));
    }
}
