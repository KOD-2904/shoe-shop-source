package com.ttthinh.shoe_shop_basic.order.service.impl;

import com.ttthinh.shoe_shop_basic.checkout.dto.request.ShippingFeeRequest;
import com.ttthinh.shoe_shop_basic.config.VNPayConfig;
import com.ttthinh.shoe_shop_basic.order.dto.request.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.order.dto.request.CreateOrderRequest;
import com.ttthinh.shoe_shop_basic.common.dto.PageResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderResponse;
import com.ttthinh.shoe_shop_basic.order.dto.response.OrderStatusHistoryResponse;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.cart.entity.CartItem;
import com.ttthinh.shoe_shop_basic.checkout.entity.ShippingFeeSnapshot;
import com.ttthinh.shoe_shop_basic.customer.entity.Address;
import com.ttthinh.shoe_shop_basic.inventory.entity.Inventory;
import com.ttthinh.shoe_shop_basic.order.entity.Order;
import com.ttthinh.shoe_shop_basic.order.entity.OrderItem;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.order.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;
import com.ttthinh.shoe_shop_basic.catalog.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.order.enums.ShippingStatus;
import com.ttthinh.shoe_shop_basic.common.exception.AppException;
import com.ttthinh.shoe_shop_basic.common.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.order.mapper.OrderMapper;
import com.ttthinh.shoe_shop_basic.cart.repository.CartItemRepository;
import com.ttthinh.shoe_shop_basic.inventory.repository.InventoryRepository;
import com.ttthinh.shoe_shop_basic.order.repository.OrderRepository;
import com.ttthinh.shoe_shop_basic.payment.repository.PaymentRepository;
import com.ttthinh.shoe_shop_basic.payment.service.impl.VNPayRefundService;
import com.ttthinh.shoe_shop_basic.catalog.repository.ProductReviewRepository;
import com.ttthinh.shoe_shop_basic.checkout.repository.ShippingFeeSnapshotRepository;
import com.ttthinh.shoe_shop_basic.checkout.service.CheckoutService;
import com.ttthinh.shoe_shop_basic.inventory.service.InventoryLockService;
import com.ttthinh.shoe_shop_basic.order.service.OrderService;
import com.ttthinh.shoe_shop_basic.order.service.OrderStatusHistoryService;
import com.ttthinh.shoe_shop_basic.promotion.service.ProductDiscountService;
import com.ttthinh.shoe_shop_basic.promotion.service.VoucherService;
import com.ttthinh.shoe_shop_basic.shipping.service.GHNShippingService;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.ttthinh.shoe_shop_basic.order.enums.OrderStatus.PENDING;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final CartItemRepository cartItemRepository;
    private final PaymentRepository paymentRepository;
    private final GHNShippingService ghnShippingService;
    private final OrderMapper orderMapper;
    private final InventoryLockService inventoryLockService;
    private final CheckoutService checkoutService;
    private final ShippingFeeSnapshotRepository shippingFeeSnapshotRepository;
    private final VNPayRefundService vnPayRefundService;
    private final VoucherService voucherService;
    private final OrderStatusHistoryService orderStatusHistoryService;
    private final ProductDiscountService productDiscountService;
    private final ProductReviewRepository productReviewRepository;
    private final VNPayConfig vnPayConfig;

    @Value("${cod.pending-timeout-minutes:1440}")
    private long codPendingTimeoutMinutes;

    @Override
    @Transactional
    public OrderResponse createOrderFromCart(UserAccount user, CreateOrderRequest request) {
        validateSupportedPaymentMethod(request.getPaymentMethod());
        List<CartItem> selectedItems = checkoutService.validateAndGetSelectedItems(user, request.getCartItemIds());
        Address address = checkoutService.resolveAddress(user, request.getAddressId());
        InventoryInfo inventoryInfo = validateInventoryAndCalculate(selectedItems);
        ShippingFeeSnapshot snapshot = consumeSnapshot(
                user,
                request.getShippingFeeSnapshotId(),
                address,
                selectedItems,
                inventoryInfo
        );

        Order order = buildOrderEntity(
                user,
                address,
                selectedItems,
                inventoryInfo,
                snapshot.getShippingFee(),
                snapshot.getDiscountAmount(),
                snapshot.getVoucherCode(),
                request.getNote()
        );

        inventoryLockService.lockCartItems(selectedItems);
        Order savedOrder = orderRepository.save(order);

        Payment payment = createPaymentForOrder(savedOrder, request.getPaymentMethod());
        paymentRepository.save(payment);
        orderStatusHistoryService.record(savedOrder, payment, user, null, null, null, "ORDER_CREATED", "Order created from cart");
        cartItemRepository.deleteAll(selectedItems);

        snapshot.setUsedAt(LocalDateTime.now());
        shippingFeeSnapshotRepository.save(snapshot);
        reserveVoucherUsage(savedOrder);

        return toOrderResponse(savedOrder, payment);
    }

    @Override
    @Transactional
    public OrderResponse buyNow(UserAccount user, BuyNowRequest request) {
        validateSupportedPaymentMethod(request.getPaymentMethod());
        Inventory inventory = inventoryRepository
                .findLockedByVariantSizeId(request.getVariantSizeId())
                .orElseThrow(() -> new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND));
        validateSellable(inventory);

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new AppException(ErrorCode.QUANTITY_NOT_VALID);
        }
        if (request.getQuantity() > inventory.getAvailableQuantity()) {
            throw new AppException(ErrorCode.OUT_OF_STOCK);
        }

        InventoryInfo info = InventoryInfo.builder()
                .totalWeight(200 * request.getQuantity())
                .totalPrice(productDiscountService.effectivePrice(inventory.getVariantSize()).multiply(BigDecimal.valueOf(request.getQuantity())))
                .maxLength(0)
                .maxWidth(0)
                .totalHeight(0)
                .build();

        Address address = checkoutService.resolveAddress(user, request.getAddressId());
        ShippingFeeSnapshot snapshot = consumeBuyNowSnapshot(user, request.getShippingFeeSnapshotId(), address, request, info);
        inventoryLockService.lockVariantSize(request.getVariantSizeId(), request.getQuantity());

        Order order = new Order();
        order.setUser(user);
        order.setStatus(PENDING);
        order.setShippingStatus(ShippingStatus.NOT_CREATED);
        order.setAddressId(address.getId());
        order.setShippingAddress(formatShippingAddress(address));
        order.setPhoneNumber(address.getPhoneNumber() != null ? address.getPhoneNumber() : user.getPhone());
        order.setReceiverName(address.getReceiverName() != null ? address.getReceiverName() : user.getEmail());
        order.setNote(request.getNote());
        order.setVoucherCode(normalizeVoucherCode(snapshot.getVoucherCode()));

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .variantSize(inventory.getVariantSize())
                .quantity(request.getQuantity())
                .unitPrice(productDiscountService.effectivePrice(inventory.getVariantSize()))
                .lineTotal(info.getTotalPrice())
                .build();

        order.getItems().add(orderItem);
        order.setDiscountPrice(snapshot.getDiscountAmount());
        order.setTotalPrice(info.getTotalPrice());
        order.setShippingFee(snapshot.getShippingFee());
        order.setFinalTotal(orderTotal(order));

        Order savedOrder = orderRepository.save(order);
        Payment payment = createPaymentForOrder(savedOrder, request.getPaymentMethod());
        paymentRepository.save(payment);
        orderStatusHistoryService.record(savedOrder, payment, user, null, null, null, "ORDER_CREATED", "Buy now order created");
        snapshot.setUsedAt(LocalDateTime.now());
        shippingFeeSnapshotRepository.save(snapshot);
        reserveVoucherUsage(savedOrder);

        return toOrderResponse(savedOrder, payment);
    }

    @Override
    @Transactional
    public OrderResponse adminCancelOrder(String orderId) {
        Order order = getOrder(orderId);
        if (!order.getStatus().adminCanCancel()) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }
        Payment payment = getDisplayPayment(order);
        StatusSnapshot before = snapshot(order, payment);
        if (order.getStatus() == PENDING) {
            if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
                vnPayRefundService.refundFull(payment, "Admin cancelled order " + order.getId());
                inventoryLockService.restoreDeducted(order);
            } else {
                inventoryLockService.releaseLocked(order);
            }
            if (payment != null && payment.getStatus() == PaymentStatus.UNPAID) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Order cancelled by admin");
            }
        } else if (order.getStatus() == OrderStatus.CONFIRMED) {
            vnPayRefundService.refundFull(payment, "Admin cancelled order " + order.getId());
            inventoryLockService.restoreDeducted(order);
            if (payment != null && payment.getStatus() == PaymentStatus.UNPAID) {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason("Order cancelled by admin");
            }
            if (payment != null && payment.getMethod() != PaymentMethod.VNPAY
                    && payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }
        }
        order.setStatus(OrderStatus.CANCELLED);
        releaseVoucherUsage(order);
        Order savedOrder = orderRepository.save(order);
        orderStatusHistoryService.record(savedOrder, payment, null, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "ADMIN_CANCEL", "Order cancelled by admin");
        return toOrderResponse(savedOrder, payment);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(String orderId, OrderStatus status) {
        if (status == OrderStatus.CANCELLED) {
            return adminCancelOrder(orderId);
        }

        Order order = getOrder(orderId);
        if (!order.getStatus().canTransitionTo(status)) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }

        Payment payment = getDisplayPayment(order);
        StatusSnapshot before = snapshot(order, payment);
        if (status == OrderStatus.CONFIRMED) {
            confirmOrder(order, payment);
        } else {
            order.setStatus(status);
        }

        Order savedOrder = orderRepository.save(order);
        orderStatusHistoryService.record(savedOrder, payment, null, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "ADMIN_STATUS_UPDATE", "Admin changed order status");
        return toOrderResponse(savedOrder, payment);
    }

    @Override
    public OrderResponse getOrderDetailForAdmin(String orderId) {
        Order order = getOrder(orderId);
        return toOrderResponse(order, getDisplayPayment(order));
    }

    @Override
    public List<OrderResponse> getAllOrders() {
        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        return toOrderResponses(orders);
    }

    @Override
    public PageResponse<OrderResponse> getAllOrders(int page, int size) {
        Page<Order> orderPage = orderRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return toOrderPage(orderPage);
    }

    @Override
    public List<OrderStatusHistoryResponse> getOrderHistory(UserAccount user, String orderId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        return orderStatusHistoryService.getHistory(order);
    }

    @Override
    public List<OrderStatusHistoryResponse> getOrderHistoryForAdmin(String orderId) {
        return orderStatusHistoryService.getHistory(getOrder(orderId));
    }

    @Override
    public List<OrderResponse> getMyOrders(UserAccount user) {
        List<Order> orders = orderRepository.findByUserOrderByCreatedAtDesc(user);
        return toOrderResponses(orders);
    }

    @Override
    public PageResponse<OrderResponse> getMyOrders(UserAccount user, int page, int size) {
        Page<Order> orderPage = orderRepository.findByUser(user, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return toOrderPage(orderPage);
    }

    @Override
    public OrderResponse getOrderDetail(UserAccount user, String orderId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        return toOrderResponse(order, getDisplayPayment(order));
    }

    @Override
    @Transactional
    public OrderResponse userCancelOrder(UserAccount user, String orderId) {
        Order order = getOrder(orderId);
        if (!order.getUser().getId().equals(user.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        if (!order.getStatus().userCanCancel()) {
            throw new AppException(ErrorCode.ORDER_CANNOT_CANCEL);
        }

        Payment payment = getDisplayPayment(order);
        StatusSnapshot before = snapshot(order, payment);
        if (payment != null && payment.getStatus() == PaymentStatus.PAID) {
            vnPayRefundService.refundFull(payment, "Customer cancelled order " + order.getId());
            if (payment.getMethod() != PaymentMethod.VNPAY) {
                payment.setStatus(PaymentStatus.REFUNDED);
            }
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            inventoryLockService.restoreDeducted(order);
        } else {
            inventoryLockService.releaseLocked(order);
        }
        if (payment != null && payment.getStatus() == PaymentStatus.UNPAID) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Order cancelled by user");
        }
        order.setStatus(OrderStatus.CANCELLED);
        releaseVoucherUsage(order);
        Order savedOrder = orderRepository.save(order);
        orderStatusHistoryService.record(savedOrder, payment, user, before.orderStatus(), before.paymentStatus(), before.shippingStatus(), "USER_CANCEL", "Order cancelled by customer");
        return toOrderResponse(savedOrder, payment);
    }

    private void confirmOrder(Order order, Payment payment) {
        if (payment == null) {
            throw new AppException(ErrorCode.PAYMENT_NOT_FOUND);
        }
        if (payment.getMethod() == PaymentMethod.COD) {
            inventoryLockService.deductLocked(order);
            payment.setExpiredAt(null);
            order.setStatus(OrderStatus.CONFIRMED);
            return;
        }
        if (payment.getStatus() != PaymentStatus.PAID) {
            throw new AppException(ErrorCode.INVALID_ORDER_STATUS);
        }
        order.setStatus(OrderStatus.CONFIRMED);
    }

    private Order getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
    }

    private Payment getDisplayPayment(Order order) {
        return paymentRepository.findDisplayPayments(order.getId()).stream()
                .findFirst()
                .orElse(null);
    }

    private StatusSnapshot snapshot(Order order, Payment payment) {
        return new StatusSnapshot(
                order.getStatus() != null ? order.getStatus().name() : null,
                payment != null && payment.getStatus() != null ? payment.getStatus().name() : null,
                order.getShippingStatus() != null ? order.getShippingStatus().name() : null
        );
    }

    private ShippingFeeSnapshot consumeSnapshot(
            UserAccount user,
            String snapshotId,
            Address address,
            List<CartItem> selectedItems,
            InventoryInfo inventoryInfo
    ) {
        if (snapshotId == null || snapshotId.isBlank()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_NOT_FOUND);
        }
        ShippingFeeSnapshot snapshot = shippingFeeSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_SNAPSHOT_NOT_FOUND));
        if (!snapshot.getUserId().equals(user.getId()) || snapshot.isUsed()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (snapshot.isExpired()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_EXPIRED);
        }
        if (!snapshot.getAddressId().equals(address.getId())) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (!snapshot.getCartSignature().equals(checkoutService.buildCartSignature(selectedItems))) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (snapshot.getProductTotal().compareTo(inventoryInfo.getTotalPrice()) != 0) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        return snapshot;
    }

    private ShippingFeeSnapshot consumeBuyNowSnapshot(
            UserAccount user,
            String snapshotId,
            Address address,
            BuyNowRequest request,
            InventoryInfo inventoryInfo
    ) {
        if (snapshotId == null || snapshotId.isBlank()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_NOT_FOUND);
        }
        ShippingFeeSnapshot snapshot = shippingFeeSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new AppException(ErrorCode.CHECKOUT_SNAPSHOT_NOT_FOUND));
        if (!snapshot.getUserId().equals(user.getId()) || snapshot.isUsed()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (snapshot.isExpired()) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_EXPIRED);
        }
        if (!snapshot.getAddressId().equals(address.getId())) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (!snapshot.getCartSignature().equals(checkoutService.buildBuyNowSignature(request.getVariantSizeId(), request.getQuantity()))) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        if (snapshot.getProductTotal().compareTo(inventoryInfo.getTotalPrice()) != 0) {
            throw new AppException(ErrorCode.CHECKOUT_SNAPSHOT_MISMATCH);
        }
        return snapshot;
    }

    private InventoryInfo validateInventoryAndCalculate(List<CartItem> items) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        int totalWeight = 0;

        for (CartItem item : items) {
            Inventory inventory = inventoryRepository
                    .findLockedByVariantSizeId(item.getVariantSize().getId())
                    .orElseThrow(() -> new AppException(ErrorCode.OUT_OF_STOCK));
            validateSellable(inventory);
            if (item.getQuantity() > inventory.getAvailableQuantity()) {
                throw new AppException(ErrorCode.OUT_OF_STOCK);
            }
            totalPrice = totalPrice.add(
                    productDiscountService.effectivePrice(item.getVariantSize()).multiply(BigDecimal.valueOf(item.getQuantity()))
            );
            totalWeight += 200 * item.getQuantity();
        }

        return InventoryInfo.builder()
                .maxLength(0)
                .maxWidth(0)
                .totalHeight(0)
                .totalWeight(totalWeight)
                .totalPrice(totalPrice)
                .build();
    }

    private BigDecimal calculateShippingFee(Address address, InventoryInfo info) {
        if (info.totalWeight == 0) {
            return BigDecimal.ZERO;
        }
        ShippingFeeRequest request = ShippingFeeRequest.builder()
                .toDistrictId(address.getDistrictId())
                .toWardCode(address.getWardCode())
                .weight(info.totalWeight)
                .length(info.maxLength)
                .width(info.maxWidth)
                .height(info.totalHeight)
                .insuranceValue(0)
                .build();
        try {
            return BigDecimal.valueOf(ghnShippingService.calculateShippingFee(request));
        } catch (Exception e) {
            log.error("Failed to calculate shipping fee", e);
            int baseFee = 30000;
            int extraWeight = Math.max(0, info.totalWeight - 1000);
            int extraFee = (extraWeight / 500) * 5000;
            return BigDecimal.valueOf(baseFee + extraFee);
        }
    }

    private Order buildOrderEntity(
            UserAccount user,
            Address address,
            List<CartItem> items,
            InventoryInfo info,
            BigDecimal shippingFee,
            BigDecimal discountAmount,
            String voucherCode,
            String note
    ) {
        Order order = new Order();
        order.setUser(user);
        order.setStatus(PENDING);
        order.setShippingStatus(ShippingStatus.NOT_CREATED);
        order.setAddressId(address.getId());
        order.setShippingAddress(formatShippingAddress(address));
        order.setPhoneNumber(address.getPhoneNumber() != null ? address.getPhoneNumber() : user.getPhone());
        order.setReceiverName(address.getReceiverName() != null ? address.getReceiverName() : user.getEmail());
        order.setNote(note);
        order.setVoucherCode(normalizeVoucherCode(voucherCode));
        order.setDiscountPrice(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        order.setTotalPrice(info.totalPrice);
        order.setShippingFee(shippingFee);
        order.setFinalTotal(orderTotal(order));

        for (CartItem item : items) {
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .variantSize(item.getVariantSize())
                    .quantity(item.getQuantity())
                    .unitPrice(productDiscountService.effectivePrice(item.getVariantSize()))
                    .lineTotal(productDiscountService.effectivePrice(item.getVariantSize()).multiply(BigDecimal.valueOf(item.getQuantity())))
                    .build();
            order.getItems().add(orderItem);
        }

        return order;
    }

    private Payment createPaymentForOrder(Order order, PaymentMethod paymentMethod) {
        Payment payment = Payment.builder()
                .order(order)
                .status(PaymentStatus.UNPAID)
                .method(paymentMethod)
                .amount(orderTotal(order))
                .build();

        if (paymentMethod == PaymentMethod.VNPAY) {
            payment.setExpiredAt(LocalDateTime.now().plusMinutes(vnpayPaymentTimeoutMinutes()));
        } else if (paymentMethod == PaymentMethod.COD) {
            payment.setExpiredAt(LocalDateTime.now().plusMinutes(codPendingTimeoutMinutes));
        }

        return payment;
    }

    private long vnpayPaymentTimeoutMinutes() {
        return vnPayConfig.getPaymentTimeoutMinutes() > 0 ? vnPayConfig.getPaymentTimeoutMinutes() : 15;
    }

    private void validateSupportedPaymentMethod(PaymentMethod paymentMethod) {
        if (paymentMethod != PaymentMethod.COD && paymentMethod != PaymentMethod.VNPAY) {
            throw new AppException(ErrorCode.UNSUPPORTED_PAYMENT_METHOD);
        }
    }

    private void validateSellable(Inventory inventory) {
        if (inventory == null
                || inventory.getVariantSize() == null
                || inventory.getVariantSize().getVariant() == null
                || !Boolean.TRUE.equals(inventory.getVariantSize().getVariant().getActive())
                || inventory.getVariantSize().getVariant().getProduct() == null
                || inventory.getVariantSize().getVariant().getProduct().getStatus() != ProductStatus.ACTIVE) {
            throw new AppException(ErrorCode.PRODUCT_VARIANT_NOT_FOUND);
        }
    }

    private BigDecimal orderTotal(Order order) {
        BigDecimal productTotal = order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        BigDecimal discount = order.getDiscountPrice() != null ? order.getDiscountPrice() : BigDecimal.ZERO;
        return productTotal.add(shipping).subtract(discount).max(BigDecimal.ZERO);
    }

    private void reserveVoucherUsage(Order order) {
        String voucherCode = normalizeVoucherCode(order.getVoucherCode());
        if (voucherCode == null || Boolean.TRUE.equals(order.getVoucherUsageCounted())) {
            return;
        }
        voucherService.markUsed(voucherCode, order.getId(), order.getUser() != null ? order.getUser().getId() : null);
        order.setVoucherCode(voucherCode);
        order.setVoucherUsageCounted(true);
        order.setVoucherUsageReleasedAt(null);
    }

    private void releaseVoucherUsage(Order order) {
        if (order == null
                || !Boolean.TRUE.equals(order.getVoucherUsageCounted())
                || order.getVoucherUsageReleasedAt() != null) {
            return;
        }
        voucherService.releaseUsed(order.getVoucherCode(), order.getId());
        order.setVoucherUsageCounted(false);
        order.setVoucherUsageReleasedAt(LocalDateTime.now());
    }

    private String normalizeVoucherCode(String voucherCode) {
        return voucherCode != null && !voucherCode.isBlank() ? voucherCode.trim().toUpperCase() : null;
    }

    private String formatShippingAddress(Address address) {
        if (address.getFullAddress() != null && !address.getFullAddress().isBlank()) {
            return address.getFullAddress();
        }
        return Stream.of(address.getDetailAddress(), address.getWardName(), address.getDistrictName(), address.getProvinceName())
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.joining(", "));
    }

    private OrderResponse toOrderResponse(Order order, Payment payment) {
        OrderResponse response = orderMapper.toOrderResponse(order);
        if (response.getItems() != null) {
            response.getItems().forEach(item ->
                    item.setReviewed(item.getId() != null && productReviewRepository.existsByOrderItemId(item.getId()))
            );
        }
        if (payment != null) {
            response.setPaymentId(payment.getId());
            response.setPaymentUrl(payment.getPaymentUrl());
            response.setPaymentMethod(payment.getMethod());
            response.setPaymentStatus(payment.getStatus());
        }
        return response;
    }

    private PageResponse<OrderResponse> toOrderPage(Page<Order> orderPage) {
        return PageResponse.<OrderResponse>builder()
                .items(toOrderResponses(orderPage.getContent()))
                .page(orderPage.getNumber())
                .size(orderPage.getSize())
                .totalItems(orderPage.getTotalElements())
                .totalPages(orderPage.getTotalPages())
                .first(orderPage.isFirst())
                .last(orderPage.isLast())
                .build();
    }

    private List<OrderResponse> toOrderResponses(List<Order> orders) {
        Map<String, Payment> displayPayments = displayPaymentsByOrderId(orders);
        return orders.stream()
                .map(order -> toOrderResponse(order, displayPayments.get(order.getId())))
                .toList();
    }

    private Map<String, Payment> displayPaymentsByOrderId(List<Order> orders) {
        List<String> orderIds = orders.stream().map(Order::getId).toList();
        if (orderIds.isEmpty()) {
            return Map.of();
        }
        Map<String, Payment> result = new HashMap<>();
        for (Payment payment : paymentRepository.findDisplayPaymentsForOrders(orderIds)) {
            result.putIfAbsent(payment.getOrder().getId(), payment);
        }
        return result;
    }

    @Scheduled(fixedDelayString = "${vnpay.refund-retry-delay-ms:120000}")
    @Transactional
    public void refundReturnedVNPayOrders() {
        List<Payment> payments = paymentRepository.findPaymentByStatus(PaymentStatus.PAID);
        for (Payment payment : payments) {
            Order order = payment.getOrder();
            if (payment.getMethod() != PaymentMethod.VNPAY || order.getStatus() != OrderStatus.RETURNED) {
                continue;
            }
            try {
                vnPayRefundService.refundFull(payment, "Returned order " + order.getId());
                if (payment.getStatus() == PaymentStatus.REFUNDED) {
                    inventoryLockService.restoreDeducted(order);
                    releaseVoucherUsage(order);
                    orderRepository.save(order);
                    paymentRepository.save(payment);
                }
            } catch (AppException exception) {
                log.error("Refund retry failed for returned orderId={}, paymentId={}",
                        order.getId(), payment.getId());
            }
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    private static class InventoryInfo {
        BigDecimal totalPrice;
        Integer totalWeight;
        Integer maxLength;
        Integer maxWidth;
        Integer totalHeight;
    }

    private record StatusSnapshot(String orderStatus, String paymentStatus, String shippingStatus) {
    }
}
