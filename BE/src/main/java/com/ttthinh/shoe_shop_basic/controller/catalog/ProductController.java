package com.ttthinh.shoe_shop_basic.controller.catalog;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductFormOptionsResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductResponse;
import com.ttthinh.shoe_shop_basic.dto.response.common.PageResponse;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.mapper.ProductMapper;
import com.ttthinh.shoe_shop_basic.service.BrandService;
import com.ttthinh.shoe_shop_basic.service.CategoryService;
import com.ttthinh.shoe_shop_basic.service.ProductService;
import com.ttthinh.shoe_shop_basic.utils.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.ObjectMapper;

import java.util.List;


@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final BrandService brandService;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> getProductPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<ProductResponse>>builder()
                .message("success")
                .code(200)
                .result(productService.getProductPage(
                        PageUtils.normalizePage(page),
                        PageUtils.normalizeSize(size)
                ))
                .build();
    }

//    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
//    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
//    public ApiResponse<ProductResponse> crateProducts(
//            @RequestParam("product")String productRequest,
//            @RequestPart(value = "images", required = false) List<MultipartFile> images,
//            @RequestParam(value = "primaryIndex", required = false) Integer primaryIndex) {
//        ObjectMapper mapper = new ObjectMapper();
//        ProductRequest request;
//        try {
//            request = mapper.readValue(productRequest, ProductRequest.class);
//        } catch (Exception e) {
//            throw new AppException(ErrorCode.VALIDATION_ERROR);
//        }
//        return ApiResponse.<ProductResponse>builder()
//                .result(productService.createProductWithImage(
//                        request, images, primaryIndex
//                ))
//                .code(200)
//                .message("success")
//                .build();
//    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping(value = "/form-options")
    public ApiResponse<ProductFormOptionsResponse> getProductFormOptions() {
        return ApiResponse.<ProductFormOptionsResponse>builder()
                .message("success")
                .code(200)
                .result(ProductFormOptionsResponse.builder()
                        .brands(brandService.getAllBrands())
                        .categories(categoryService.getAllCategories())
                        .build())
                .build();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/with-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProductResponse> creatProduct(
            @Valid @RequestPart("product")ProductRequest productRequest,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "primaryIndex", required = false) Integer primaryIndex) {

        return ApiResponse.<ProductResponse>builder()
                .result(productService.createProductWithImage(
                        productRequest, images, primaryIndex
                ))
                .code(200)
                .message("success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/")
    public ApiResponse<ProductResponse> createProduct(
            @Valid @RequestBody ProductRequest productRequest) {
        return ApiResponse.<ProductResponse>builder()
                .result(productMapper.toProductResponse(productService.addProduct(
                        productRequest)
                ))
                .code(200)
                .message("success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/addProductImage")
    public ApiResponse<ProductResponse> addProductImage(
            @RequestPart(value = "images") List<MultipartFile> images,
            @RequestParam(value = "productId")String productId,
            @RequestParam(value = "primaryIndex")Integer primaryIndex
    ){
        var result = ApiResponse.<ProductResponse>builder()
                .message("success")
                .code(200)
                .result(productService.addProductImages(images, productId, primaryIndex))
                .build();
        return result;
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PutMapping(value = "/{id}")
    public ApiResponse<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductRequest productRequest) {
        return ApiResponse.<ProductResponse>builder()
                .result(productService.updateProduct(id, productRequest))
                .code(200)
                .message("success")
                .build();
    }

    @GetMapping(value = "/getProducts")
    public ApiResponse<List<ProductResponse>> getProducts(
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String brandId
    ){
        List<ProductResponse> result;
        if (categoryId != null && !categoryId.isBlank()) {
            result = productService.getProductsByCategory(categoryId);
        } else if (brandId != null && !brandId.isBlank()) {
            result = productService.getProductsByBrand(brandId);
        } else {
            result = productService.getAllProducts();
        }
        return ApiResponse.<List<ProductResponse>>builder()
                .message("success")
                .code(200)
                .result(result)
                .build();
    }
    @GetMapping(value = "/getProduct")
    public ApiResponse<ProductResponse> getProducts(@RequestParam String productId){
        return ApiResponse.<ProductResponse>builder()
                .message("success")
                .code(200)
                .result(productService.getProduct(productId))
                .build();
    }
}
