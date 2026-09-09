package com.ttthinh.shoe_shop_basic.service.shipping;

import com.ttthinh.shoe_shop_basic.config.GHNConfig;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GHNLocationService {
    private static final String PROVINCE_PATH = "/master-data/province";
    private static final String DISTRICT_PATH = "/master-data/district";
    private static final String WARD_PATH = "/master-data/ward";

    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate;
    private final GHNShippingModeService shippingModeService;

    @Cacheable(value = "ghnProvinces", key = "'all'")
    public Object getProvinces() {
        if (!shouldCallGhnMasterData()) {
            return List.of(Map.of("ProvinceID", 202, "ProvinceName", "HCM"));
        }
        return getData(masterDataBaseUrl() + PROVINCE_PATH);
    }

    @Cacheable(value = "ghnDistricts", key = "#provinceId")
    public Object getDistricts(Integer provinceId) {
        if (!shouldCallGhnMasterData()) {
            return List.of(Map.of("DistrictID", 1524, "DistrictName", "Hai Chau", "ProvinceID", provinceId));
        }
        String url = UriComponentsBuilder.fromUriString(masterDataBaseUrl() + DISTRICT_PATH)
                .queryParam("province_id", provinceId)
                .toUriString();
        return getData(url);
    }

    @Cacheable(value = "ghnWards", key = "#districtId")
    public Object getWards(Integer districtId) {
        if (!shouldCallGhnMasterData()) {
            return List.of(Map.of("WardCode", "40101", "WardName", "Thach Thang", "DistrictID", districtId));
        }
        String url = UriComponentsBuilder.fromUriString(masterDataBaseUrl() + WARD_PATH)
                .queryParam("district_id", districtId)
                .toUriString();
        return getData(url);
    }

    private Object getData(String url) {
        validateConfig();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Token", ghnConfig.getToken());
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null || !Integer.valueOf(200).equals(body.get("code"))) {
                throw new AppException(ErrorCode.CAN_NOT_CONNECT_GHN);
            }
            return body.get("data");
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new AppException(ErrorCode.CAN_NOT_CONNECT_GHN);
        }
    }

    private void validateConfig() {
        if (!StringUtils.hasText(ghnConfig.getApiUrl()) || !StringUtils.hasText(ghnConfig.getToken())) {
            throw new AppException(ErrorCode.CAN_NOT_CONNECT_GHN);
        }
    }

    private String masterDataBaseUrl() {
        String apiUrl = ghnConfig.getApiUrl();
        if (apiUrl.endsWith("/v2")) {
            return apiUrl.substring(0, apiUrl.length() - 3);
        }
        return apiUrl;
    }

    private boolean shouldCallGhnMasterData() {
        return !shippingModeService.isMockTest() && !ghnConfig.isMasterDataMockEnabled();
    }
}
