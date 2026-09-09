package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.CategoryRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.CategoryResponse;
import com.ttthinh.shoe_shop_basic.dto.response.common.PageResponse;

import java.util.List;

public interface CategoryService {
    public CategoryResponse createCategory(CategoryRequest request);
//    public CategoryResponse getCategoryById(int id);
    PageResponse<CategoryResponse> getCategoryPage(int page, int size);
    public List<CategoryResponse> getAllCategories();
    public CategoryResponse update(String categoryId ,CategoryRequest request);

     CategoryResponse getCategory(String id);
}
