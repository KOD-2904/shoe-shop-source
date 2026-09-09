package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.request.catalog.VariantSizeRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductVariantMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.service.ProductDiscountService;
import com.ttthinh.shoe_shop_basic.service.ProductVariantService;
import com.ttthinh.shoe_shop_basic.service.image.ImageUploadQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper productVariantMapper;
    private final ImageUploadQueueService imageUploadQueueService;
    private final ProductDiscountService productDiscountService;

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "variants", allEntries = true),
            @CacheEvict(value = "variantsByProduct", key = "#result.productId")
    })
    public ProductVariantResponse addVariantImages(List<MultipartFile> images, String variantId, Integer primaryIndex) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(()-> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
  //      String imagePath = "products/"+variant.getProduct().getName()+"/images/";
        if (images == null || images.isEmpty()) {
            return enrichPricing(productVariantMapper.toProductVariantResponse(variant), variant);
        }
        imageUploadQueueService.enqueueVariantImages(
                images,
                variant.getProduct().getId(),
                variant.getId(),
                primaryIndex
        );

        return enrichPricing(productVariantMapper.toProductVariantResponse(variant), variant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "variants", key = "#id")
    public ProductVariantResponse getProductVariant(String id) {
        ProductVariant productVariant = variantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        return enrichPricing(productVariantMapper.toProductVariantResponse(productVariant), productVariant);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "variants", key = "'all'")
    public List<ProductVariantResponse> getAllProductVariant() {
        List<ProductVariant> variants = variantRepository.findAll();
        return enrichPricing(productVariantMapper.toProductVariantsResponse(variants), variants);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "variantsByProduct", key = "#productId")
    public List<ProductVariantResponse> getProductVariantByProduct(String productId) {
        List<ProductVariant> variants = variantRepository.findByProductId(productId);
        return enrichPricing(productVariantMapper.toProductVariantsResponse(variants), variants);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "variants", allEntries = true),
            @CacheEvict(value = "variantsByProduct", key = "#result.productId")
    })
    public ProductVariantResponse addProductVariant(ProductVariantRequest productVariantRequest) {
        Product product = productRepository.findById(productVariantRequest.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductVariant productVariant = ProductVariant.builder()
                .product(product)
                .color(productVariantRequest.getColor())
                .active(productVariantRequest.getActive() != null ? productVariantRequest.getActive() : true)
                .build();

        ProductVariant savedVariant = variantRepository.save(productVariant);
        addSizesToVariant(savedVariant, productVariantRequest.getSizes());

        ProductVariant saved = variantRepository.save(savedVariant);
        return enrichPricing(productVariantMapper.toProductVariantResponse(saved), saved);
    }
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "variants", allEntries = true),
            @CacheEvict(value = "variantsByProduct", allEntries = true)
    })
    public List<ProductVariantResponse> addProductVariants(
            List<ProductVariantRequest> requests
    ) {
        if (requests == null || requests.isEmpty()) {
            return List.of();
        }

        // Lấy product từ request đầu tiên
        String productId = requests.get(0).getProductId();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        List<ProductVariant> variants = new ArrayList<>();

        for (ProductVariantRequest req : requests) {

            // Optional: validate cùng productId
            if (!productId.equals(req.getProductId())) {
                throw new AppException(ErrorCode.INVALID_PRODUCT_VARIANT_REQUEST);
            }

            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .color(req.getColor())
                    .active(req.getActive() != null ? req.getActive() : true)
                    .build();

            addSizesToVariant(variant, req.getSizes());

            variants.add(variant);
        }

      // return productVariantMapper.toProductVariantsResponse(variantRepository.saveAll(variants));
        return variantRepository.saveAll(variants)
                .stream()
                .map(variant -> enrichPricing(productVariantMapper.toProductVariantResponse(variant), variant))
                .toList();
    }


    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "variants", allEntries = true),
            @CacheEvict(value = "variantsByProduct", allEntries = true)
    })
    public ProductVariantResponse updateProductVariant(String id, ProductVariantRequest productVariantRequest) {
        ProductVariant variant = variantRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        Product product = productRepository.findById(productVariantRequest.getProductId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        variant.setProduct(product);
        variant.setColor(productVariantRequest.getColor());
        variant.setActive(productVariantRequest.getActive() == null || productVariantRequest.getActive());
        upsertSizes(variant, productVariantRequest.getSizes());

        ProductVariant saved = variantRepository.save(variant);
        return enrichPricing(productVariantMapper.toProductVariantResponse(saved), saved);
    }

    @Transactional
    public void setPrimaryVariantImage(String variantId, String imageId) {
        ProductVariant variant = variantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));

        variant.getImages().forEach(img ->
                img.setPrimaryImage(img.getId().equals(imageId))
        );
    }

    private void addSizesToVariant(
            ProductVariant variant,
            List<VariantSizeRequest> sizeRequests
    ) {
        if (sizeRequests == null || sizeRequests.isEmpty()) {
            return;
        }

        for (VariantSizeRequest request : sizeRequests) {
            VariantSize variantSize = VariantSize.builder()
                    .variant(variant)
                    .size(request.getSize())
                    .sku(request.getSku())
                    .price(request.getPrice())
                    .build();

            Integer quantity = request.getInitQuantity();
            if (quantity != null && quantity >= 0) {
                Inventory inventory = Inventory.builder()
                        .variantSize(variantSize)
                        .quantity(quantity)
                        .build();
                variantSize.setInventory(inventory);
            }

            variant.getSizes().add(variantSize);
        }
    }

    private void upsertSizes(ProductVariant variant, List<VariantSizeRequest> sizeRequests) {
        if (sizeRequests == null || sizeRequests.isEmpty()) {
            return;
        }

        for (VariantSizeRequest request : sizeRequests) {
            Optional<VariantSize> existingSize = variant.getSizes().stream()
                    .filter(size -> size.getSize().equalsIgnoreCase(request.getSize()))
                    .findFirst();

            if (existingSize.isPresent()) {
                VariantSize size = existingSize.get();
                size.setSku(request.getSku());
                size.setPrice(request.getPrice());
                if (request.getInitQuantity() != null) {
                    Inventory inventory = size.getInventory();
                    if (inventory == null) {
                        inventory = Inventory.builder()
                                .variantSize(size)
                                .quantity(request.getInitQuantity())
                                .build();
                        size.setInventory(inventory);
                    } else {
                        inventory.setQuantity(request.getInitQuantity());
                    }
                }
            } else {
                addSizesToVariant(variant, List.of(request));
            }
        }
    }

    private List<ProductVariantResponse> enrichPricing(List<ProductVariantResponse> responses, List<ProductVariant> variants) {
        for (int i = 0; i < responses.size(); i++) {
            enrichPricing(responses.get(i), variants.get(i));
        }
        return responses;
    }

    private ProductVariantResponse enrichPricing(ProductVariantResponse response, ProductVariant variant) {
        if (response == null || response.getSizes() == null || variant == null || variant.getSizes() == null) {
            return response;
        }
        response.getSizes().forEach(sizeResponse -> variant.getSizes().stream()
                .filter(size -> size.getId().equals(sizeResponse.getId()))
                .findFirst()
                .ifPresent(size -> applyPricing(sizeResponse, size)));
        return response;
    }

    private void applyPricing(com.ttthinh.shoe_shop_basic.dto.response.catalog.VariantSizeResponse response, VariantSize size) {
        BigDecimal original = size.getPrice();
        BigDecimal sale = productDiscountService.effectivePrice(size);
        response.setOriginalPrice(original);
        response.setSalePrice(sale);
        response.setOnSale(sale.compareTo(original) < 0);
        if (Boolean.TRUE.equals(response.getOnSale())) {
            response.setDiscountPercent(original.subtract(sale)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(original, 0, RoundingMode.HALF_UP)
                    .intValue());
            response.setPrice(sale);
        }
    }

}
