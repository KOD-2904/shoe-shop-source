package com.ttthinh.shoe_shop_basic.catalog.controller;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.CategoryRequest;
import com.ttthinh.shoe_shop_basic.common.dto.ApiResponse;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.CategoryFormOptionsResponse;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.CategoryResponse;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.catalog.service.CategoryService;
import com.ttthinh.shoe_shop_basic.common.util.PageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping
    public ApiResponse<PageResponse<CategoryResponse>> getCategoryPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.<PageResponse<CategoryResponse>>builder()
                .result(categoryService.getCategoryPage(
                        PageUtils.normalizePage(page),
                        PageUtils.normalizeSize(size)
                ))
                .code(200)
                .message("Success")
                .build();
    }

    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @GetMapping("/form-options")
    public ApiResponse<CategoryFormOptionsResponse> getCategoryFormOptions() {
        return ApiResponse.<CategoryFormOptionsResponse>builder()
                .result(CategoryFormOptionsResponse.builder()
                        .parentCategories(categoryService.getAllCategories())
                        .build())
                .code(200)
                .message("Success")
                .build();
    }

    @PostMapping("/add")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<CategoryResponse> add(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>
                builder()
                .result(categoryService.createCategory(request))
                .code(200)
                .message("Success")
                .build();
    }
    @GetMapping("/getCategorys")
    public ApiResponse<List<CategoryResponse>> getCategorys() {
        return ApiResponse.<List<CategoryResponse>>
                        builder()
                .result(categoryService.getAllCategories())
                .code(200)
                .message("Success")
                .build();
    }
    @GetMapping("/getCategory")
    public ApiResponse<CategoryResponse> getCategory(@RequestParam("id") String id) {
        return ApiResponse.<CategoryResponse>builder()
                .code(200)
                .message("Success")
                .result(categoryService.getCategory(id))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse<CategoryResponse> update(
            @PathVariable String id,
            @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.<CategoryResponse>builder()
                .code(200)
                .message("Success")
                .result(categoryService.update(id, request))
                .build();
    }
}
