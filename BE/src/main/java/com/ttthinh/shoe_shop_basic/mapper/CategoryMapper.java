package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.catalog.CategoryResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CategoryMapper {
    @Mapping(source = "parent.id", target = "parentId")
    public CategoryResponse toCategoryResponse(Category category);
    List<CategoryResponse> toCategoryResponseList(List<Category> categories);
}

//Custom method (khi logic phức tạp)
//@Mapper(componentModel = "spring")
//public interface CategoryMapper {
//
//    @Mapping(target = "parentId", expression = "java(mapParentId(category))")
//    CategoryResponse toCategoryResponse(Category category);
//
//    default String mapParentId(Category category) {
//        return category.getParent() != null ? category.getParent().getId() : null;
//    }
//}
