package com.ttthinh.shoe_shop_basic.catalog.service;

import com.ttthinh.shoe_shop_basic.catalog.dto.request.BrandRequest;
import com.ttthinh.shoe_shop_basic.catalog.dto.response.BrandResponse;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.catalog.entity.Brand;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BrandService {
    public Brand create(BrandRequest brand);
    public BrandResponse update(String id, BrandRequest brand);
    PageResponse<BrandResponse> getBrandPage(int page, int size);
    public List<BrandResponse> getAllBrands();

    public BrandResponse addBrandImage(MultipartFile image, String brandId);

    public BrandResponse addBrandWithImage(MultipartFile image, BrandRequest brandRequest);

     public BrandResponse getBrandById(String brandId);
}
