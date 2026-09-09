package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductResponse;
import com.ttthinh.shoe_shop_basic.dto.response.common.PageResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
import com.ttthinh.shoe_shop_basic.entity.catalog.Category;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductMapper;
import com.ttthinh.shoe_shop_basic.repository.jpa.BrandRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.CategoryRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.service.ProductService;
import com.ttthinh.shoe_shop_basic.service.ProductDiscountService;
import com.ttthinh.shoe_shop_basic.service.image.ImageUploadQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final ProductMapper productMapper;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ImageUploadQueueService imageUploadQueueService;
    private final ProductDiscountService productDiscountService;
    @Override
    @CacheEvict(value = "products", allEntries = true)
    public Product addProduct(ProductRequest productRequest) {
        validateProductRequest(productRequest);
        productRepository.findBySlug(productRequest.getSlug().trim())
                .ifPresent(product -> {
                    throw new AppException(ErrorCode.DATA_INTEGRITY_VIOLATION);
                });
        Brand brand = null;
        if (productRequest.getBrandId() != null && !productRequest.getBrandId().isBlank()) {
            brand = brandRepository.findById(productRequest.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        }

        Category category = null;
        if (productRequest.getCategoryId() != null && !productRequest.getCategoryId().isBlank()) {
            category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        Product product = Product.builder()
                .brand(brand)
                .category(category)
                .name(productRequest.getName().trim())
                .slug(productRequest.getSlug().trim())
                .description(productRequest.getDescription())
                .basePrice(productRequest.getBasePrice())
                .status(ProductStatus.ACTIVE)
                .build();
        return productRepository.save(product);
        //return productMapper.toProductResponse(productRepository.save(product));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productDetail", key = "#id")
    })
    public ProductResponse updateProduct(String id, ProductRequest productRequest) {
        validateProductRequest(productRequest);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        productRepository.findBySlug(productRequest.getSlug().trim())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new AppException(ErrorCode.DATA_INTEGRITY_VIOLATION);
                });

        Brand brand = null;
        if (StringUtils.hasText(productRequest.getBrandId())) {
            brand = brandRepository.findById(productRequest.getBrandId())
                    .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        }

        Category category = null;
        if (StringUtils.hasText(productRequest.getCategoryId())) {
            category = categoryRepository.findById(productRequest.getCategoryId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
        }

        product.setName(productRequest.getName().trim());
        product.setSlug(productRequest.getSlug().trim());
        product.setDescription(productRequest.getDescription());
        product.setBasePrice(productRequest.getBasePrice());
        product.setBrand(brand);
        product.setCategory(category);
        Product savedProduct = productRepository.save(product);
        return enrichPricing(productMapper.toProductResponse(savedProduct), savedProduct);
    }

    @Override
    public PageResponse<ProductResponse> getProductPage(int page, int size) {
        Page<Product> productPage = productRepository.findAll(PageRequest.of(page, size));
        return PageResponse.<ProductResponse>builder()
                .items(enrichPricing(productMapper.toProductResponse(productPage.getContent()), productPage.getContent()))
                .page(productPage.getNumber())
                .size(productPage.getSize())
                .totalItems(productPage.getTotalElements())
                .totalPages(productPage.getTotalPages())
                .first(productPage.isFirst())
                .last(productPage.isLast())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        var result = productRepository.findAll();
        log.warn("Queried all products");
        return enrichPricing(productMapper.toProductResponse(result), result);
    }

    @Override
    public List<ProductResponse> getProductsByCategory(String category) {
        if (!StringUtils.hasText(category)) {
            return getAllProducts();
        }
        List<Product> result = productRepository.findByCategoryIdAndStatus(category, ProductStatus.ACTIVE);
        return enrichPricing(productMapper.toProductResponse(result), result);
    }

    @Override
    public List<ProductResponse> getProductsByBrand(String brand) {
        if (!StringUtils.hasText(brand)) {
            return getAllProducts();
        }
        List<Product> result = productRepository.findByBrandIdAndStatus(brand, ProductStatus.ACTIVE);
        return enrichPricing(productMapper.toProductResponse(result), result);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", allEntries = true)
    public ProductResponse createProductWithImage(ProductRequest productRequest, List<MultipartFile> images, Integer primaryIndex) {
        Product product = addProduct(productRequest);
        if (images == null || images.isEmpty()) {
            return enrichPricing(productMapper.toProductResponse(product), product);
        }
        imageUploadQueueService.enqueueProductImages(images, product.getId(), primaryIndex);
        return enrichPricing(productMapper.toProductResponse(product), product);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productDetail", key = "#productId")
    })
    public ProductResponse addProductImages(
            List<MultipartFile> images,
            String productId,
            Integer primaryIndex
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));

        if (images == null || images.isEmpty()) {
            return enrichPricing(productMapper.toProductResponse(product), product);
        }

        imageUploadQueueService.enqueueProductImages(images, product.getId(), primaryIndex);

        return enrichPricing(productMapper.toProductResponse(product), product);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "productDetail", key = "#productId")
    public ProductResponse getProduct(String productId) {
        log.warn("Queried product");
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        return enrichPricing(productMapper.toProductResponse(product), product);
    }

    private void validateProductRequest(ProductRequest productRequest) {
        if (productRequest == null
                || !StringUtils.hasText(productRequest.getName())
                || !StringUtils.hasText(productRequest.getSlug())
                || productRequest.getBasePrice() == null
                || productRequest.getBasePrice().signum() <= 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private List<ProductResponse> enrichPricing(List<ProductResponse> responses, List<Product> products) {
        for (int i = 0; i < responses.size(); i++) {
            enrichPricing(responses.get(i), products.get(i));
        }
        return responses;
    }

    private ProductResponse enrichPricing(ProductResponse response, Product product) {
        if (response == null || product == null || product.getBasePrice() == null) {
            return response;
        }
        var effectivePrice = productDiscountService.effectiveProductPrice(product);
        response.setOriginalPrice(product.getBasePrice());
        response.setSalePrice(effectivePrice);
        response.setOnSale(effectivePrice.compareTo(product.getBasePrice()) < 0);
        if (Boolean.TRUE.equals(response.getOnSale())) {
            response.setDiscountPercent(product.getBasePrice().subtract(effectivePrice)
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .divide(product.getBasePrice(), 0, java.math.RoundingMode.HALF_UP)
                    .intValue());
        }
        return response;
    }

}
