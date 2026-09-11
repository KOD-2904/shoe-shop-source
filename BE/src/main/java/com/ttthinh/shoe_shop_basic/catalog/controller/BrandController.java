package com.ttthinh.shoe_shop_basic.catalog.controller;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.BrandRequest;
import com.ttthinh.shoe_shop_basic.common.dto.ApiResponse;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.BrandResponse;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.catalog.mapper.BrandMapper;
import com.ttthinh.shoe_shop_basic.catalog.service.BrandService;
import com.ttthinh.shoe_shop_basic.common.util.PageUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/brand")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;
    private final BrandMapper brandMapper;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<BrandResponse>> getBrandPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<BrandResponse>>builder()
                .code(200)
                .message("Success")
                .result(brandService.getBrandPage(
                        PageUtils.normalizePage(page),
                        PageUtils.normalizeSize(size)
                ))
                .build();
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BrandResponse> addBrand(@Valid @RequestBody BrandRequest brandRequest) {
        var result = brandMapper.toBrandResponse(brandService.create(brandRequest));
        return ApiResponse.<BrandResponse>builder()
                .result(result)
                .code(200)
                .message("Success")
                .build();
    }
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @PostMapping(value = "/addBrandImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<BrandResponse> addBrandImage(
            @RequestPart(value = "image") MultipartFile image,
            @RequestParam(value = "brandId") String brandId) {
        var result = brandService.addBrandImage(image, brandId);
        log.warn("---------------------");
        return ApiResponse.<BrandResponse>builder()
                .result(result)
                .code(200)
                .message("Success")
                .build();
    }
    @PostMapping(value = "/addBrandWithImage", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BrandResponse> addBrandWithImage(
            @RequestPart(value = "image", required = true) MultipartFile image,
            @Valid @RequestPart(value = "brand", required = true) BrandRequest brandRequest) {
        var result = brandService.addBrandWithImage(image, brandRequest);
        return ApiResponse.<BrandResponse>builder()
                .result(result)
                .code(200)
                .message("Success")
                .build();
    }
    @GetMapping("/getBrands")
    public ApiResponse<List<BrandResponse>> getBrands() {
        return ApiResponse.<List<BrandResponse>>builder()
                .code(200)
                .message("Success")
                .result(brandService.getAllBrands())
                .build();
    }
    @GetMapping("/getBrand")
    public ApiResponse<BrandResponse> getBrand(@RequestParam("brandId") String brandId) {
        return ApiResponse.<BrandResponse>builder()
                .code(200)
                .message("Success")
                .result(brandService.getBrandById(brandId))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<BrandResponse> updateBrand(
            @PathVariable String id,
            @Valid @RequestBody BrandRequest brandRequest) {
        return ApiResponse.<BrandResponse>builder()
                .result(brandService.update(id, brandRequest))
                .code(200)
                .message("Success")
                .build();
    }
}
