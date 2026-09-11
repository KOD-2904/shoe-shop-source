package com.ttthinh.shoe_shop_basic.order.controller;

import com.ttthinh.shoe_shop_basic.order.dto.request.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.order.dto.request.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.common.dto.ApiResponse;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderStatusHistoryResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.order.service.OrderService;
import com.ttthinh.shoe_shop_basic.payment.service.PaymentApplicationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@Slf4j
@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentApplicationService paymentApplicationService;

    @PostMapping
    public ApiResponse<?> createOrder(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid CreateOrderRequest req,
            HttpServletRequest httpServletRequest
    ) {

        var orderResponse = orderService.createOrderFromCart(user, req);
        if (req.getPaymentMethod() == PaymentMethod.VNPAY){
            attachVNPayUrl(user, orderResponse, httpServletRequest);
        }
        log.info("Shipping Fee: {}", orderResponse.getShippingPrice());
        return ApiResponse.builder()
                .result(orderResponse)
                .code(200)
                .message("Order created")
                .build();
    }

    @GetMapping("/me")
    public List<OrderResponse> myOrders(
            @AuthenticationPrincipal(expression = "user") UserAccount user
    ) {
        return orderService.getMyOrders(user);
    }

    @GetMapping(value = "/me", params = {"page", "size"})
    public PageResponse<OrderResponse> myOrdersPage(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.getMyOrders(user, page, Math.min(size, 100));
    }

    @GetMapping("/{id}")
    public OrderResponse orderDetail(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String id
    ) {
        return orderService.getOrderDetail(user, id);
    }

    @GetMapping("/{id}/history")
    public List<OrderStatusHistoryResponse> orderHistory(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String id
    ) {
        return orderService.getOrderHistory(user, id);
    }

    @PostMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String orderId
    ) {
        return orderService.userCancelOrder(user, orderId);
    }

    @PostMapping("/buy-now")
    public OrderResponse buyNow(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid BuyNowRequest request,
            HttpServletRequest httpServletRequest
    ) {
        var orderResponse = orderService.buyNow(user, request);
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            attachVNPayUrl(user, orderResponse, httpServletRequest);
        }
        return orderResponse;
    }

    private void attachVNPayUrl(UserAccount user, OrderResponse orderResponse, HttpServletRequest httpServletRequest) {
        var payment = paymentApplicationService.createVNPayPayment(user, orderResponse.getId(), httpServletRequest);
        orderResponse.setPaymentUrl(payment.getPaymentUrl());
        orderResponse.setPaymentId(payment.getPaymentId());
        log.info("Payment URL created for orderId={}, paymentId={}", orderResponse.getId(), orderResponse.getPaymentId());
    }
}

