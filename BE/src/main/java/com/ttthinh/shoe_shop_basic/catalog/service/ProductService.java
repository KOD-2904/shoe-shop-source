package com.ttthinh.shoe_shop_basic.catalog.service;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.ProductRequest;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.ProductResponse;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.catalog.entity.Product;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProductService {
    public Product addProduct(ProductRequest productRequest);
    public ProductResponse updateProduct(String id, ProductRequest productRequest);
    //public ProductResponse updateProductStatus(ProductRequest productRequest);
    PageResponse<ProductResponse> getProductPage(int page, int size);
    public List<ProductResponse> getAllProducts();
    public List<ProductResponse> getProductsByCategory(String category);
    public List<ProductResponse> getProductsByBrand(String brand);
    public ProductResponse createProductWithImage(ProductRequest productRequest,
                                                  List<MultipartFile> images,
                                                  Integer primaryIndex);

    public ProductResponse addProductImages(List<MultipartFile> images, String productId, Integer primaryIndex);

    ProductResponse getProduct(String productId);
}
