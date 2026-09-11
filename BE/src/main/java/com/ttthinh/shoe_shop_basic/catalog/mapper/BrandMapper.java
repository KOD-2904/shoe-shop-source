package com.ttthinh.shoe_shop_basic.catalog.mapper;

import com.ttthinh.shoe_shop_basic.catalog.dto.response.BrandResponse;
import com.ttthinh.shoe_shop_basic.catalog.entity.Brand;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    public BrandResponse toBrandResponse(Brand brand);
    public List<BrandResponse> toBrandResponse(List<Brand> brand);
}
