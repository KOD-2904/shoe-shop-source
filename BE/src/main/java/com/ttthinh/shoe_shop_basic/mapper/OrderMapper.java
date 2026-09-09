package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.order.OrderItemResponse;
import com.ttthinh.shoe_shop_basic.dto.response.order.OrderResponse;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "items", source = "items")
    @Mapping(target = "totalPrice", source = "totalPrice")
    @Mapping(target = "finalTotalPrice", expression = "java(orderTotal(order))")
    @Mapping(target = "discountPrice", source = "discountPrice")
    @Mapping(target = "shippingPrice", source = "shippingFee")

    OrderResponse toOrderResponse(Order order);
//    default LocalDateTime map(Instant instant) {
//        if (instant == null) {
//            return null;
//        }
//        // Lựa chọn tốt nhất cho Việt Nam: múi giờ Asia/Ho_Chi_Minh (+07:00)
//        return LocalDateTime.ofInstant(instant, ZoneId.of("Asia/Ho_Chi_Minh"));
//
//        // Hoặc dùng UTC nếu bạn muốn đồng bộ toàn cầu:
//        // return LocalDateTime.ofInstant(instant, ZoneId.of("UTC"));
//
//        // Hoặc múi giờ mặc định của server (không khuyến khích):
//        // return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
//    }

    @Mapping(target = "productId", source = "variantSize.variant.product.id")
    @Mapping(target = "productName", source = "variantSize.variant.product.name")
    @Mapping(target = "variantSizeId", source = "variantSize.id")
    @Mapping(target = "variantName", source = "variantSize.variant.color")
    @Mapping(target = "size", source = "variantSize.size")
    @Mapping(target = "totalPrice", expression =
            "java(calcTotal(item.getUnitPrice(), item.getQuantity()))")
    OrderItemResponse toOrderItemResponse(OrderItem item);

   // List<OrderItemResponse> toOrderItemResponseList(Set<OrderItem> items);

    default BigDecimal calcTotal(BigDecimal price, Integer quantity) {
        return price.multiply(BigDecimal.valueOf(quantity));
    }

    default BigDecimal orderTotal(Order order) {
        BigDecimal productTotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountPrice() != null ? order.getDiscountPrice() : BigDecimal.ZERO;
        return productTotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
    }
}
