package com.ttthinh.shoe_shop_basic.shipping.service;

import com.ttthinh.shoe_shop_basic.config.GHNConfig;
import com.ttthinh.shoe_shop_basic.checkout.dto.request.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.customer.entity.Address;
import com.ttthinh.shoe_shop_basic.order.entity.Order;
import com.ttthinh.shoe_shop_basic.order.entity.OrderItem;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.customer.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GHNShippingService {
    private final GHNConfig ghnConfig;
    private final RestTemplate restTemplate;
    private final GHNCacheService ghnCacheService;
    private final AddressRepository addressRepository;
    private final GHNShippingModeService shippingModeService;

    @Cacheable(value = "shippingFee", key = "#root.target.shippingFeeCacheKey(#request)")
    public Integer calculateShippingFee(ShippingFeeRequest request) {
        if (shippingModeService.isMockTest()) {
            int fixedFee = shippingModeService.getMockFixedFee();
            log.info("GHN MOCK_TEST shipping fee returned: {}", fixedFee);
            return fixedFee;
        }

        String url = ghnConfig.getApiUrl() + "/shipping-order/fee";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("service_type_id", 2);  // 2: hàng nhẹ, 5: hàng nặng
        payload.put("from_district_id", ghnConfig.getFromDistrictId());
        payload.put("from_ward_code", ghnConfig.getFromWardCode());
        payload.put("to_district_id", request.getToDistrictId());
        payload.put("to_ward_code", request.getToWardCode());
        payload.put("weight", request.getWeight());

        // Thêm kích thước nếu có
        if (request.getLength() != null && request.getLength() > 0) {
            payload.put("length", request.getLength());
            payload.put("width", request.getWidth());
            payload.put("height", request.getHeight());
        }

        // Bảo hiểm (nếu có)
        if (request.getInsuranceValue() != null && request.getInsuranceValue() > 0) {
            payload.put("insurance_value", request.getInsuranceValue());
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getBody() != null && (Integer) response.getBody().get("code") == 200) {
                Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
                Integer total = (Integer) data.get("total");
                log.info("Shipping fee calculated: {}", total);
                return total;
            } else {
                log.error("GHN API error: {}", response.getBody());
                throw new AppException(ErrorCode.CAN_NOT_SOLVE_SHIPPING_FEE);
                //throw new RuntimeException("Không thể tính phí vận chuyển");
            }
        } catch (Exception e) {
            log.error("Error calling GHN API", e);
            throw new AppException(ErrorCode.CAN_NOT_SOLVE_SHIPPING_FEE);
            //throw new RuntimeException("Lỗi kết nối tới GHN: " + e.getMessage());
        }
    }

    public String shippingFeeCacheKey(ShippingFeeRequest request) {
        return shippingModeService.shippingFeeCacheKey(request);
    }

    public String createOrder(Order order, Payment payment) {
        if (shippingModeService.isMockTest()) {
            String mockOrderCode = shippingModeService.buildMockOrderCode(order);
            log.info("GHN MOCK_TEST order created orderId={}, mockOrderCode={}", order.getId(), mockOrderCode);
            return mockOrderCode;
        }

        Address address = addressRepository.findById(order.getAddressId())
                .orElseThrow(() -> new AppException(ErrorCode.ADDRESS_NOT_FOUND));
        String url = ghnConfig.getApiUrl() + "/shipping-order/create";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());
        headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));

        Map<String, Object> payload = new HashMap<>();
        payload.put("payment_type_id", 1);
        payload.put("required_note", "KHONGCHOXEMHANG");
        payload.put("note", order.getNote());
        payload.put("from_name", "Shoe Shop");
        payload.put("from_phone", "0900000000");
        payload.put("from_address", ghnConfig.getFullAddress());
        payload.put("from_ward_name", ghnConfig.getFromWardName());
        payload.put("from_district_name", ghnConfig.getFromDistrictName());
        payload.put("from_province_name", ghnConfig.getFromProvinceName());
        payload.put("to_name", order.getReceiverName());
        payload.put("to_phone", order.getPhoneNumber() == null ? "0900000000" : order.getPhoneNumber());
        payload.put("to_address", order.getShippingAddress());
        payload.put("to_ward_code", address.getWardCode());
        payload.put("to_district_id", address.getDistrictId());
        payload.put("cod_amount", payment != null && payment.getMethod() == PaymentMethod.COD
                ? orderTotal(order).intValue()
                : 0);
        payload.put("weight", Math.max(200, order.getItems().stream().mapToInt(item -> item.getQuantity() * 200).sum()));
        payload.put("length", 20);
        payload.put("width", 20);
        payload.put("height", 10);
        payload.put("service_type_id", 2);
        payload.put("items", buildItems(order));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Map.class
            );
            Map<?, ?> body = response.getBody();
            if (body == null || !Integer.valueOf(200).equals(body.get("code"))) {
                log.error("GHN create order error: {}", body);
                throw new AppException(ErrorCode.SHIPPING_ORDER_FAILED);
            }
            Map<?, ?> data = (Map<?, ?>) body.get("data");
            Object orderCode = data.get("order_code");
            if (orderCode == null) {
                throw new AppException(ErrorCode.SHIPPING_ORDER_FAILED);
            }
            return orderCode.toString();
        } catch (AppException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("Error creating GHN order", exception);
            throw new AppException(ErrorCode.SHIPPING_ORDER_FAILED);
        }
    }

    public String getOrderStatus(String orderCode) {
        if (shippingModeService.isMockTest()) {
            return null;
        }
        String url = ghnConfig.getApiUrl() + "/shipping-order/detail";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token", ghnConfig.getToken());

        Map<String, Object> payload = new HashMap<>();
        payload.put("order_code", orderCode);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    Map.class
            );
            Map<?, ?> body = response.getBody();
            if (body == null || !Integer.valueOf(200).equals(body.get("code"))) {
                log.warn("GHN detail error for orderCode={}: {}", orderCode, body);
                return null;
            }
            Map<?, ?> data = (Map<?, ?>) body.get("data");
            Object status = data == null ? null : data.get("status");
            return status == null ? null : status.toString();
        } catch (Exception exception) {
            log.warn("Error polling GHN status for orderCode={}: {}", orderCode, exception.getMessage());
            return null;
        }
    }

    private BigDecimal orderTotal(Order order) {
        BigDecimal productTotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountPrice() != null ? order.getDiscountPrice() : BigDecimal.ZERO;
        return productTotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
    }

    private List<Map<String, Object>> buildItems(Order order) {
        return order.getItems().stream()
                .map(this::buildItem)
                .toList();
    }

    private Map<String, Object> buildItem(OrderItem item) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", item.getVariantSize().getVariant().getProduct().getName());
        payload.put("quantity", item.getQuantity());
        payload.put("price", item.getUnitPrice().intValue());
        payload.put("weight", 200);
        return payload;
    }
}
