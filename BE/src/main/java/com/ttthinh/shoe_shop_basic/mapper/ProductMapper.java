package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
import com.ttthinh.shoe_shop_basic.entity.catalog.Category;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductImage;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ProductMapper {
    @Mapping(target = "brandId", source = "brand", qualifiedByName = "mapBrandId")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "categoryId", source = "category", qualifiedByName = "mapCategoryId")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "primaryImageUrl", source = "images", qualifiedByName = "mapPrimaryImageUrl")
    @Mapping(target = "imageUrls", source = "images", qualifiedByName = "mapImageUrls")
    ProductResponse toProductResponse(Product product);

    @Mapping(target = "brandId", source = "brand", qualifiedByName = "mapBrandId")
    @Mapping(target = "brandName", source = "brand.name")
    @Mapping(target = "categoryId", source = "category", qualifiedByName = "mapCategoryId")
    @Mapping(target = "categoryName", source = "category.name")
    @Mapping(target = "primaryImageUrl", source = "images", qualifiedByName = "mapPrimaryImageUrl")
    @Mapping(target = "imageUrls", source = "images", qualifiedByName = "mapImageUrls")
    List<ProductResponse> toProductResponse(List<Product> productList);

    @Named("mapBrandId")
    default String mapBrandId(Brand brand) {
        return brand != null ? brand.getId() : null;
    }
    @Named("mapCategoryId")
    default String mapCategoryId(Category category) {
        return category != null ? category.getId() : null;
    }
    @Named("mapProductStatusToString")
    default String mapProductStatusToString(ProductStatus status) {
        return status != null ? status.name() : null;
    }

    @Named("mapPrimaryImageUrl")
    default String mapPrimaryImageUrl(Set<ProductImage> images) {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .sorted(Comparator.comparing((ProductImage image) -> !Boolean.TRUE.equals(image.getPrimaryImage()))
                        .thenComparing(image -> image.getSortOrder() == null ? Integer.MAX_VALUE : image.getSortOrder()))
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null);
    }

    @Named("mapImageUrls")
    default List<String> mapImageUrls(Set<ProductImage> images) {
        if (images == null || images.isEmpty()) return List.of();
        return images.stream()
                .sorted(Comparator.comparing((ProductImage image) -> image.getSortOrder() == null ? Integer.MAX_VALUE : image.getSortOrder()))
                .map(ProductImage::getUrl)
                .toList();
    }
}
