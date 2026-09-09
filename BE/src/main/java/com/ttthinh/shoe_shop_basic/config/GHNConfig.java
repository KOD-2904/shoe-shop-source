package com.ttthinh.shoe_shop_basic.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class GHNConfig {
    @Value("${ghn.token}")
    private String token;

    @Value("${ghn.shop-id}")
    private Integer shopId;

    @Value("${ghn.from-district-id}")
    private Integer fromDistrictId;

    @Value("${ghn.from-district-name}")
    private String fromDistrictName;

    @Value("${ghn.from-ward-code}")
    private String fromWardCode;

    @Value("${ghn.from-ward-name}")
    private String fromWardName;

    @Value("${ghn.from-province-code}")
    private Integer fromProvinceCode;

    @Value("${ghn.from-province-name}")
    private String fromProvinceName;

    @Value("${ghn.from-detail-address}")
    private String fromDetailAddress;

    @Value("${ghn.api-url}")
    private String apiUrl;

    @Value("${ghn.mode:${GHN_MODE:}}")
    private String mode;

    @Value("${ghn.mock-enabled:${GHN_MOCK_ENABLE:false}}")
    private boolean mockEnabled;

    @Value("${ghn.mock-fixed-fee:${GHN_MOCK_FIXED_FEE:30000}}")
    private Integer mockFixedFee;

    @Value("${ghn.master-data-mock-enabled:false}")
    private boolean masterDataMockEnabled;

    public String getFullAddress() {
        return fromDetailAddress + ", " + fromWardName + ", " + fromDistrictName + ", " + fromProvinceName;
    }
}
