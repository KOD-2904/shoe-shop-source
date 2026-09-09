package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.catalog.BrandResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BrandMapper {
    public BrandResponse toBrandResponse(Brand brand);
    public List<BrandResponse> toBrandResponse(List<Brand> brand);
}
