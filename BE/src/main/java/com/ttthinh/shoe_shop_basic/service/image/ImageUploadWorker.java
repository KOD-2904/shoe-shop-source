package com.ttthinh.shoe_shop_basic.service.image;

import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductImage;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantImage;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.BrandRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductImageRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.VariantImageRepository;
import com.ttthinh.shoe_shop_basic.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadWorker {
    private static final String ROOT_FOLDER = "shoe-shop";

    private final CloudinaryService cloudinaryService;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final VariantImageRepository variantImageRepository;
    private final BrandRepository brandRepository;
    private final CacheManager cacheManager;

    @Async("imageUploadTaskExecutor")
    @Transactional
    public void uploadProductImages(List<QueuedImageFile> files, String productId, Integer primaryIndex) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_NOT_FOUND));
        if (primaryIndex != null) {
            productImageRepository.clearPrimaryByProductId(productId);
        }

        int startSortOrder = productImageRepository.findMaxSortOrder(productId) + 1;
        for (int i = 0; i < files.size(); i++) {
            QueuedImageFile file = files.get(i);
            String url = cloudinaryService.upload(file.bytes(), file.originalFilename(), productFolder(productId));
            productImageRepository.save(ProductImage.builder()
                    .product(product)
                    .url(url)
                    .primaryImage(primaryIndex != null && primaryIndex == i)
                    .sortOrder(startSortOrder + i)
                    .build());
        }
        afterCommit(() -> {
            clearCache("products");
            evictCache("productDetail", productId);
        });
        log.info("Queued product image upload completed for product {}", productId);
    }

    @Async("imageUploadTaskExecutor")
    @Transactional
    public void uploadVariantImages(List<QueuedImageFile> files, String productId, String variantId, Integer primaryIndex) {
        ProductVariant variant = productVariantRepository.findById(variantId)
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        if (primaryIndex != null) {
            variantImageRepository.clearPrimaryByVariantId(variantId);
        }

        int startSortOrder = variantImageRepository.findMaxSortOrder(variantId) + 1;
        for (int i = 0; i < files.size(); i++) {
            QueuedImageFile file = files.get(i);
            String url = cloudinaryService.upload(file.bytes(), file.originalFilename(), variantFolder(productId, variantId));
            variantImageRepository.save(VariantImage.builder()
                    .variant(variant)
                    .url(url)
                    .primaryImage(primaryIndex != null && primaryIndex == i)
                    .sortOrder(startSortOrder + i)
                    .build());
        }
        afterCommit(() -> {
            clearCache("variants");
            evictCache("variantsByProduct", productId);
            evictCache("variants", variantId);
        });
        log.info("Queued variant image upload completed for variant {}", variantId);
    }

    @Async("imageUploadTaskExecutor")
    @Transactional
    public void uploadBrandLogo(QueuedImageFile file, String brandId) {
        Brand brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new AppException(ErrorCode.BRAND_NOT_FOUND));
        String url = cloudinaryService.upload(file.bytes(), file.originalFilename(), brandFolder(brandId));
        brand.setLogoUrl(url);
        brandRepository.save(brand);
        afterCommit(() -> {
            clearCache("brands");
            evictCache("brandDetail", brandId);
        });
        log.info("Queued brand logo upload completed for brand {}", brandId);
    }

    private String productFolder(String productId) {
        return ROOT_FOLDER + "/product/" + productId;
    }

    private String variantFolder(String productId, String variantId) {
        return ROOT_FOLDER + "/product/" + productId + "/" + variantId;
    }

    private String brandFolder(String brandId) {
        return ROOT_FOLDER + "/brand/" + brandId;
    }

    private void afterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private void clearCache(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }

    private void evictCache(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}
