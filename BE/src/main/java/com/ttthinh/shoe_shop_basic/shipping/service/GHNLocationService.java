package com.ttthinh.shoe_shop_basic.shipping.service;

import com.ttthinh.shoe_shop_basic.config.GHNConfig;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class GHNLocationService {
    private static final String PROVINCE_PATH = "/master-data/province";
    private static final String DISTRICT_PATH = "/master-data/district";
    private static final String WARD_PATH = "/master-data/ward";

    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate;

    @Cacheable(value = "ghnProvinces", key = "#root.target.masterDataCacheMode() + ':all'")
    public Object getProvinces() {
        if (!shouldCallGhnMasterData()) {
            return List.of(Map.of("ProvinceID", 202, "ProvinceName", "HCM"));
        }
        return getData(masterDataBaseUrl() + PROVINCE_PATH);
    }

    @Cacheable(value = "ghnDistricts", key = "#root.target.masterDataCacheMode() + ':' + #provinceId")
    public Object getDistricts(Integer provinceId) {
        if (!shouldCallGhnMasterData()) {
            return List.of(Map.of("DistrictID", 1524, "DistrictName", "Hai Chau", "ProvinceID", provinceId));
        }
        String url = UriComponentsBuilder.fromUriString(masterDataBaseUrl() + DISTRICT_PATH)
                .queryParam("province_id", provinceId)
                .toUriString();
        return getData(url);
    }

    @Cacheable(value = "ghnWards", key = "#root.target.masterDataCacheMode() + ':' + #districtId")
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
                log.warn("GHN master-data returned invalid response for url={}, body={}", url, body);
                throw new AppException(ErrorCode.CAN_NOT_CONNECT_GHN);
            }
            return body.get("data");
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.warn("GHN master-data request failed for url={}: {}", url, exception.getMessage());
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
        return !ghnConfig.isMasterDataMockEnabled();
    }

    public String masterDataCacheMode() {
        return shouldCallGhnMasterData() ? "real" : "mock";
    }
}
