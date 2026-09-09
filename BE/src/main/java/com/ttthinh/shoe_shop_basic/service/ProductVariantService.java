package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductVariantService {
    public ProductVariantResponse addVariantImages(List<MultipartFile> images, String variantId, Integer primaryIndex) ;

    public ProductVariantResponse getProductVariant(String id);
    public List<ProductVariantResponse> getAllProductVariant();
    public List<ProductVariantResponse> getProductVariantByProduct(String product);
    public ProductVariantResponse addProductVariant(ProductVariantRequest productVariantRequest);
    public List<ProductVariantResponse> addProductVariants(List<ProductVariantRequest> productVariantRequest);
    public ProductVariantResponse updateProductVariant(String id, ProductVariantRequest productVariantRequest);

    public void setPrimaryVariantImage(String variantId, String imageId);

}
