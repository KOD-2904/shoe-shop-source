package com.ttthinh.shoe_shop_basic.order.controller.admin;

import com.ttthinh.shoe_shop_basic.order.dto.request.UpdateOrderStatusRequest;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderStatusHistoryResponse;
import com.ttthinh.shoe_shop_basic.order.service.OrderService;
import com.ttthinh.shoe_shop_basic.shipping.service.ShippingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderController {
    private final OrderService orderService;
    private final ShippingOrderService shippingOrderService;
    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping(params = {"page", "size"})
    public PageResponse<OrderResponse> getAllOrdersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return orderService.getAllOrders(page, Math.min(size, 100));
    }

    /**
     * Admin xem chi tiết order
     */
    @GetMapping("/{orderId}")
    public OrderResponse getOrderDetail(
            @PathVariable String orderId
    ) {
        return orderService.getOrderDetailForAdmin(orderId);
    }

    @GetMapping("/{orderId}/history")
    public List<OrderStatusHistoryResponse> getOrderHistory(@PathVariable String orderId) {
        return orderService.getOrderHistoryForAdmin(orderId);
    }

    /**
     * Admin cập nhật trạng thái order
     */
    @PutMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable String orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderStatus(orderId, request.getStatus());
    }

    /**
     * Admin hủy order
     */
    @PutMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable String orderId
    ) {
        return orderService.adminCancelOrder(orderId);
    }

    @PostMapping("/{orderId}/shipping/ghn")
    public OrderResponse createGHNShippingOrder(@PathVariable String orderId) {
        return shippingOrderService.createGHNOrder(orderId);
    }
}
