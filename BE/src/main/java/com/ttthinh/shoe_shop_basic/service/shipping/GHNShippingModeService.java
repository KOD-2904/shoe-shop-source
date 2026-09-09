package com.ttthinh.shoe_shop_basic.service.shipping;

import com.ttthinh.shoe_shop_basic.config.GHNConfig;
import com.ttthinh.shoe_shop_basic.dto.request.checkout.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.dto.request.shipping.GHNSettingsRequest;
import com.ttthinh.shoe_shop_basic.dto.response.shipping.GHNSettingsResponse;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.shipping.ShippingProviderSetting;
import com.ttthinh.shoe_shop_basic.enums.GHNMode;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.ShippingProviderSettingRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GHNShippingModeService {
    private static final String PROVIDER = "GHN";
    private static final int DEFAULT_MOCK_FEE = 30000;
    private static final DateTimeFormatter MOCK_CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final GHNConfig ghnConfig;
    private final ShippingProviderSettingRepository settingRepository;
    private final CacheManager cacheManager;

    private volatile GHNMode currentMode;
    private volatile Integer currentMockFixedFee;

    @PostConstruct
    public void initialize() {
        loadCurrentSetting();
    }

    public boolean isMockTest() {
        ensureLoaded();
        return currentMode == GHNMode.MOCK_TEST;
    }

    public int getMockFixedFee() {
        ensureLoaded();
        return currentMockFixedFee != null ? currentMockFixedFee : DEFAULT_MOCK_FEE;
    }

    public String buildMockOrderCode(Order order) {
        String orderId = order.getId() == null ? "ORDER" : order.getId().replace("-", "");
        String suffix = orderId.substring(0, Math.min(orderId.length(), 12)).toUpperCase();
        return "MOCK-GHN-" + LocalDateTime.now().format(MOCK_CODE_TIME) + "-" + suffix;
    }

    public String shippingFeeCacheKey(ShippingFeeRequest request) {
        ensureLoaded();
        return currentMode + "_" + getMockFixedFee()
                + "_" + request.getToDistrictId()
                + "_" + request.getToWardCode()
                + "_" + request.getWeight()
                + "_" + request.getLength()
                + "_" + request.getWidth()
                + "_" + request.getHeight()
                + "_" + request.getInsuranceValue();
    }

    @Transactional(readOnly = true)
    public GHNSettingsResponse getSettings() {
        ensureLoaded();
        return toResponse(currentMode, getMockFixedFee());
    }

    @Transactional
    public synchronized GHNSettingsResponse update(GHNSettingsRequest request) {
        ensureLoaded();
        GHNMode nextMode = request.getMode() != null ? request.getMode() : currentMode;
        int nextMockFee = request.getMockFixedFee() != null ? normalizeFee(request.getMockFixedFee()) : getMockFixedFee();

        ShippingProviderSetting setting = settingRepository.findByProvider(PROVIDER)
                .orElseGet(this::defaultSetting);
        setting.setMode(nextMode);
        setting.setMockFixedFee(nextMockFee);
        settingRepository.save(setting);

        currentMode = nextMode;
        currentMockFixedFee = nextMockFee;
        clearGhnCaches();
        return toResponse(currentMode, currentMockFixedFee);
    }

    private synchronized void loadCurrentSetting() {
        ShippingProviderSetting setting = settingRepository.findByProvider(PROVIDER)
                .orElseGet(() -> settingRepository.save(defaultSetting()));
        currentMode = setting.getMode() != null ? setting.getMode() : resolveConfiguredMode();
        currentMockFixedFee = normalizeFee(setting.getMockFixedFee());
    }

    private void ensureLoaded() {
        if (currentMode == null || currentMockFixedFee == null) {
            loadCurrentSetting();
        }
    }

    private ShippingProviderSetting defaultSetting() {
        return ShippingProviderSetting.builder()
                .provider(PROVIDER)
                .mode(resolveConfiguredMode())
                .mockFixedFee(normalizeFee(ghnConfig.getMockFixedFee()))
                .build();
    }

    private GHNMode resolveConfiguredMode() {
        if (StringUtils.hasText(ghnConfig.getMode())) {
            String normalized = ghnConfig.getMode().trim().replace("-", "_").toUpperCase();
            if ("MOCK".equals(normalized) || "TEST".equals(normalized)) {
                return GHNMode.MOCK_TEST;
            }
            if ("REAL".equals(normalized) || "MOCK_TEST".equals(normalized)) {
                return GHNMode.valueOf(normalized);
            }
        }
        return ghnConfig.isMockEnabled() || !isRealConfigured() ? GHNMode.MOCK_TEST : GHNMode.REAL;
    }

    private int normalizeFee(Integer fee) {
        int normalized = fee != null ? fee : DEFAULT_MOCK_FEE;
        if (normalized < 0) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
        return normalized;
    }

    private GHNSettingsResponse toResponse(GHNMode mode, int mockFixedFee) {
        return GHNSettingsResponse.builder()
                .mode(mode)
                .mockFixedFee(mockFixedFee)
                .mockActive(mode == GHNMode.MOCK_TEST)
                .realConfigured(isRealConfigured())
                .apiUrl(ghnConfig.getApiUrl())
                .build();
    }

    private boolean isRealConfigured() {
        return StringUtils.hasText(ghnConfig.getApiUrl())
                && StringUtils.hasText(ghnConfig.getToken())
                && ghnConfig.getShopId() != null
                && ghnConfig.getShopId() > 0
                && ghnConfig.getFromDistrictId() != null
                && ghnConfig.getFromDistrictId() > 0
                && StringUtils.hasText(ghnConfig.getFromWardCode());
    }

    private void clearGhnCaches() {
        for (String cacheName : List.of("shippingFee", "ghnProvinces", "ghnDistricts", "ghnWards")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }
}
