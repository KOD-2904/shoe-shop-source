package com.ttthinh.shoe_shop_basic.order.service;

import com.ttthinh.shoe_shop_basic.order.dto.request.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.order.dto.request.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderStatusHistoryResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.order.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import org.springframework.stereotype.Service;

import java.util.List;

public interface OrderService {

    OrderResponse createOrderFromCart(UserAccount user, CreateOrderRequest createOrderRequest);

    List<OrderResponse> getMyOrders(UserAccount user);

    PageResponse<OrderResponse> getMyOrders(UserAccount user, int page, int size);

    OrderResponse getOrderDetail(UserAccount user, String orderId);

    OrderResponse userCancelOrder(UserAccount user, String orderId);
    OrderResponse buyNow(UserAccount user, BuyNowRequest request);

    OrderResponse adminCancelOrder(String orderId);

    OrderResponse updateOrderStatus(String orderId, OrderStatus status);

    OrderResponse getOrderDetailForAdmin(String orderId);

    List<OrderResponse> getAllOrders();

    PageResponse<OrderResponse> getAllOrders(int page, int size);

    List<OrderStatusHistoryResponse> getOrderHistory(UserAccount user, String orderId);

    List<OrderStatusHistoryResponse> getOrderHistoryForAdmin(String orderId);
    //OrderResponse updateOrderStatus(String orderId, OrderStatus status);
}

