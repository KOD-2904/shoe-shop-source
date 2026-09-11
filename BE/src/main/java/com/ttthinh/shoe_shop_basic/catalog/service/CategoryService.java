package com.ttthinh.shoe_shop_basic.catalog.service;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.CategoryRequest;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.CategoryResponse;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;

import java.util.List;

public interface CategoryService {
    public CategoryResponse createCategory(CategoryRequest request);
//    public CategoryResponse getCategoryById(int id);
    PageResponse<CategoryResponse> getCategoryPage(int page, int size);
    public List<CategoryResponse> getAllCategories();
    public CategoryResponse update(String categoryId ,CategoryRequest request);

     CategoryResponse getCategory(String id);
}
