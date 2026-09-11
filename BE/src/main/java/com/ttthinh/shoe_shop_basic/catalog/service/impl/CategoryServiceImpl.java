package com.ttthinh.shoe_shop_basic.catalog.service.impl;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.CategoryRequest;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.CategoryResponse;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.catalog.entity.Category;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.catalog.mapper.CategoryMapper;
import com.ttthinh.shoe_shop_basic.catalog.repository.CategoryRepository;
import com.ttthinh.shoe_shop_basic.catalog.service.CategoryService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Override
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryResponse createCategory(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        if(request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            category.setParent(parent);
        }
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    public PageResponse<CategoryResponse> getCategoryPage(int page, int size) {
        Page<Category> categoryPage = categoryRepository.findAll(PageRequest.of(page, size));
        return PageResponse.<CategoryResponse>builder()
                .items(categoryMapper.toCategoryResponseList(categoryPage.getContent()))
                .page(categoryPage.getNumber())
                .size(categoryPage.getSize())
                .totalItems(categoryPage.getTotalElements())
                .totalPages(categoryPage.getTotalPages())
                .first(categoryPage.isFirst())
                .last(categoryPage.isLast())
                .build();
    }

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryResponse> getAllCategories() {
        return categoryMapper.toCategoryResponseList(categoryRepository.findAll());
    }

    @Override
    @Caching(evict = {
            @CacheEvict(value = "categories", allEntries = true),
            @CacheEvict(value = "categoryDetail", key = "#categoryId"),
            @CacheEvict(value = "products", allEntries = true),
            @CacheEvict(value = "productDetail", allEntries = true)
    })
    public CategoryResponse update(String categoryId, CategoryRequest request) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));

        category.setName(request.getName());
        if (request.getParentId() == null || request.getParentId().isBlank()) {
            category.setParent(null);
        } else {
            if (request.getParentId().equals(categoryId)) {
                throw new AppException(ErrorCode.VALIDATION_ERROR);
            }
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND));
            ensureNotDescendant(category, parent);
            category.setParent(parent);
        }

        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Cacheable(value = "categoryDetail", key = "#id")
    public CategoryResponse getCategory(String id) {
        return categoryMapper.toCategoryResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CATEGORY_NOT_FOUND)));
    }

    private void ensureNotDescendant(Category category, Category proposedParent) {
        Category cursor = proposedParent;
        while (cursor != null) {
            if (cursor.getId().equals(category.getId())) {
                throw new AppException(ErrorCode.VALIDATION_ERROR);
            }
            cursor = cursor.getParent();
        }
    }
}
