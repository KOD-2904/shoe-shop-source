package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.BrandRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.BrandResponse;
import com.ttthinh.shoe_shop_basic.dto.response.common.PageResponse;
import com.ttthinh.shoe_shop_basic.entity.catalog.Brand;
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
