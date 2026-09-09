package com.ttthinh.shoe_shop_basic.service.shipping;

import com.ttthinh.shoe_shop_basic.config.GHNConfig;

import com.ttthinh.shoe_shop_basic.entity.customer.Address;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GHNCacheService {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final GHNConfig ghnConfig;

    private static final String KEY = "ghn:shop:info";

    public GHNShopInfo get() {
        try {
            String json = redisTemplate.opsForValue().get(KEY);

            if (json != null) {
                return objectMapper.readValue(json, GHNShopInfo.class);
            }

            GHNShopInfo shopInfo = loadFromDBOrConfig();
            set(shopInfo);

            return shopInfo;

        } catch (Exception e) {
            return loadFromDBOrConfig();
        }
    }

    public void set(GHNShopInfo data) {
        try {
            String json = objectMapper.writeValueAsString(data);
            redisTemplate.opsForValue().set(KEY, json, 1, TimeUnit.DAYS);
        } catch (Exception ignored) {}
    }

    public void evictShopInfo() {
        redisTemplate.delete(KEY);
    }

    private GHNShopInfo loadFromDBOrConfig() {
        return GHNShopInfo.builder()
                .shopId(ghnConfig.getShopId())
                .token(ghnConfig.getToken())
                .fromAddress(Address.builder()
                        .provinceId(ghnConfig.getFromProvinceCode())
                        .provinceName(ghnConfig.getFromProvinceName())
                        .districtId(ghnConfig.getFromDistrictId())
                        .districtName(ghnConfig.getFromDistrictName())
                        .wardCode(ghnConfig.getFromWardCode())
                        .wardName(ghnConfig.getFromWardName())
                        .detailAddress(ghnConfig.getFromDetailAddress())
                        .fullAddress(ghnConfig.getFullAddress())
                        .build())
                .build();
    }
}
