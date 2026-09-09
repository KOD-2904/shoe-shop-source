package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductVariantResponse;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.VariantSizeResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantImage;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper {
    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(target = "primaryImageUrl", source = "images", qualifiedByName = "mapPrimaryImageUrl")
    @Mapping(target = "imageUrls", source = "images", qualifiedByName = "mapImageUrls")
    public ProductVariantResponse toProductVariantResponse(ProductVariant productVariant);

    @Mapping(source = "product.id", target = "productId")
    @Mapping(source = "product.name", target = "productName")
    @Mapping(target = "primaryImageUrl", source = "images", qualifiedByName = "mapPrimaryImageUrl")
    @Mapping(target = "imageUrls", source = "images", qualifiedByName = "mapImageUrls")
    public List<ProductVariantResponse> toProductVariantsResponse(List<ProductVariant> variantList);

    @Mapping(source = "variant.id", target = "variantId")
    @Mapping(source = "variant.color", target = "variantColor")
    @Mapping(source = "variant.product.id", target = "productId")
    @Mapping(source = "variant.product.name", target = "productName")
    @Mapping(source = "inventory.quantity", target = "quantity")
    VariantSizeResponse toVariantSizeResponse(VariantSize variantSize);

    //@Named("mapProductStatusToString")
    //default String mapProductStatusToString(ProductStatus status) {
        //return status != null ? status.name() : null;
    //}

    @Named("mapPrimaryImageUrl")
    default String mapPrimaryImageUrl(Set<VariantImage> images) {
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .sorted(Comparator.comparing((VariantImage image) -> !Boolean.TRUE.equals(image.getPrimaryImage()))
                        .thenComparing(image -> image.getSortOrder() == null ? Integer.MAX_VALUE : image.getSortOrder()))
                .map(VariantImage::getUrl)
                .findFirst()
                .orElse(null);
    }

    @Named("mapImageUrls")
    default List<String> mapImageUrls(Set<VariantImage> images) {
        if (images == null || images.isEmpty()) return List.of();
        return images.stream()
                .sorted(Comparator.comparing((VariantImage image) -> image.getSortOrder() == null ? Integer.MAX_VALUE : image.getSortOrder()))
                .map(VariantImage::getUrl)
                .toList();
    }
}
