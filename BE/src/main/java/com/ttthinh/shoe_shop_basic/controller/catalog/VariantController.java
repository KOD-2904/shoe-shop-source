package com.ttthinh.shoe_shop_basic.controller.catalog;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductVariantRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.VariantFormOptionsResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.service.ProductService;
import com.ttthinh.shoe_shop_basic.service.ProductVariantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/variants")
public class VariantController {
    private final ProductVariantService productVariantService;
    private final ProductService productService;

    @GetMapping("/form-options")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<VariantFormOptionsResponse> getVariantFormOptions() {
        return ApiResponse.<VariantFormOptionsResponse>builder()
                .result(VariantFormOptionsResponse.builder()
                        .products(productService.getAllProducts())
                        .variants(productVariantService.getAllProductVariant())
                        .build())
                .code(200)
                .message("success")
                .build();
    }

    @GetMapping
    public ApiResponse<List<ProductVariantResponse>> getVariants() {
        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(productVariantService.getAllProductVariant())
                .code(200)
                .message("success")
                .build();
    }

    @GetMapping("/product/{productId}")
    public ApiResponse<List<ProductVariantResponse>> getVariantsByProduct(@PathVariable String productId) {
        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(productVariantService.getProductVariantByProduct(productId))
                .code(200)
                .message("success")
                .build();
    }

    @PostMapping("/addVariant")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<ProductVariantResponse> addVariant(@Valid @RequestBody ProductVariantRequest request) {
        var result = productVariantService.addProductVariant(request);
        return ApiResponse.<ProductVariantResponse>builder()
                .result(result)
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping("/addVariants")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<List<ProductVariantResponse>> addVariants(
            @Valid @RequestBody List<ProductVariantRequest> request) {
        var result = productVariantService.addProductVariants(request);
        return ApiResponse.<List<ProductVariantResponse>>builder()
                .result(result)
                .code(200)
                .message("success")
                .build();
    }
    @PostMapping(
            value = "/{variantId}/images",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<ProductVariantResponse> addVariantImages(
            @PathVariable String variantId,
            @RequestPart("images") List<MultipartFile> images,
            @RequestParam(required = false) Integer primaryIndex
    ) {
        return ApiResponse.<ProductVariantResponse>builder()
                .result(
                        productVariantService.addVariantImages(
                                images,
                                variantId,
                                primaryIndex
                        )
                )
                .message("success")
                .code(200)
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<ProductVariantResponse> updateVariant(
            @PathVariable String id,
            @Valid @RequestBody ProductVariantRequest request) {
        return ApiResponse.<ProductVariantResponse>builder()
                .result(productVariantService.updateProductVariant(id, request))
                .code(200)
                .message("success")
                .build();
    }

}
