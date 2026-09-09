package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.dto.request.catalog.ProductReviewRequest;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import com.ttthinh.shoe_shop_basic.entity.order.Order;
import com.ttthinh.shoe_shop_basic.entity.order.OrderItem;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.enums.ShippingStatus;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderItemRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.OrderRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductReviewRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.VariantSizeRepository;
import com.ttthinh.shoe_shop_basic.service.ProductReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
class ProductReviewServiceIntegrationTest {
    @Autowired
    private ProductReviewService productReviewService;

    @Autowired
    private ProductReviewRepository productReviewRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private VariantSizeRepository variantSizeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void deliveredOrderItemCanBeReviewedOnceAndUpdatedWithImages() {
        Fixture fixture = createDeliveredFixture("review-ok");

        var first = productReviewService.createOrUpdate(fixture.user(), fixture.product().getId(), ProductReviewRequest.builder()
                .orderItemId(fixture.orderItem().getId())
                .rating(4)
                .comment("Comfortable")
                .imageUrls(List.of("https://example.com/one.jpg"))
                .build());
        var second = productReviewService.createOrUpdate(fixture.user(), fixture.product().getId(), ProductReviewRequest.builder()
                .orderItemId(fixture.orderItem().getId())
                .rating(5)
                .comment("Updated")
                .imageUrls(List.of("https://example.com/two.jpg", " "))
                .build());

        assertEquals(first.getId(), second.getId());
        assertEquals(5, second.getRating());
        assertEquals(List.of("https://example.com/two.jpg"), second.getImageUrls());
        assertEquals(1, productReviewRepository.findAll().stream()
                .filter(review -> fixture.orderItem().getId().equals(review.getOrderItem().getId()))
                .count());
    }

    @Test
    void userCannotReviewAnotherUsersOrderItem() {
        Fixture fixture = createDeliveredFixture("review-owner");
        UserAccount otherUser = saveUser("other-" + System.nanoTime() + "@example.com");

        AppException exception = assertThrows(AppException.class, () ->
                productReviewService.createOrUpdate(otherUser, fixture.product().getId(), ProductReviewRequest.builder()
                        .orderItemId(fixture.orderItem().getId())
                        .rating(5)
                        .build()));
        assertEquals(ErrorCode.PRODUCT_REVIEW_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void pendingOrderItemCannotBeReviewed() {
        Fixture fixture = createFixture("review-pending", OrderStatus.PENDING);

        AppException exception = assertThrows(AppException.class, () ->
                productReviewService.createOrUpdate(fixture.user(), fixture.product().getId(), ProductReviewRequest.builder()
                        .orderItemId(fixture.orderItem().getId())
                        .rating(5)
                        .build()));
        assertEquals(ErrorCode.PRODUCT_REVIEW_NOT_ALLOWED, exception.getErrorCode());
    }

    @Test
    void sameUserCanReviewSameProductForDifferentDeliveredOrderItems() {
        Fixture first = createDeliveredFixture("review-repeat-product");
        OrderItem secondOrderItem = createOrderItem(first.user(), first.product(), "review-repeat-product-second", OrderStatus.DELIVERED);

        var firstReview = productReviewService.createOrUpdate(first.user(), first.product().getId(), ProductReviewRequest.builder()
                .orderItemId(first.orderItem().getId())
                .rating(4)
                .comment("First purchase")
                .build());
        var secondReview = productReviewService.createOrUpdate(first.user(), first.product().getId(), ProductReviewRequest.builder()
                .orderItemId(secondOrderItem.getId())
                .rating(5)
                .comment("Second purchase")
                .build());

        assertEquals(2, productReviewRepository.findByProductOrderByCreatedAtDesc(first.product()).stream()
                .filter(review -> review.getUser().getId().equals(first.user().getId()))
                .count());
        assertEquals(first.orderItem().getId(), firstReview.getOrderItemId());
        assertEquals(secondOrderItem.getId(), secondReview.getOrderItemId());
    }

    private Fixture createDeliveredFixture(String prefix) {
        return createFixture(prefix, OrderStatus.DELIVERED);
    }

    private Fixture createFixture(String prefix, OrderStatus orderStatus) {
        UserAccount user = saveUser(prefix + "-" + System.nanoTime() + "@example.com");
        Product product = productRepository.save(Product.builder()
                .name(prefix)
                .slug(prefix + "-" + System.nanoTime())
                .basePrice(BigDecimal.valueOf(1000000))
                .status(ProductStatus.ACTIVE)
                .build());
        OrderItem item = createOrderItem(user, product, prefix, orderStatus);
        return new Fixture(user, product, item);
    }

    private OrderItem createOrderItem(UserAccount user, Product product, String prefix, OrderStatus orderStatus) {
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .color("Black")
                .active(true)
                .build());
        VariantSize size = variantSizeRepository.save(VariantSize.builder()
                .variant(variant)
                .size("42")
                .sku(prefix + "-" + System.nanoTime())
                .price(BigDecimal.valueOf(1000000))
                .build());
        Order order = orderRepository.save(Order.builder()
                .user(user)
                .status(orderStatus)
                .shippingStatus(orderStatus == OrderStatus.DELIVERED ? ShippingStatus.DELIVERED : ShippingStatus.NOT_CREATED)
                .totalPrice(BigDecimal.valueOf(1000000))
                .shippingFee(BigDecimal.ZERO)
                .discountPrice(BigDecimal.ZERO)
                .voucherUsageCounted(false)
                .build());
        OrderItem item = orderItemRepository.save(OrderItem.builder()
                .order(order)
                .variantSize(size)
                .unitPrice(BigDecimal.valueOf(1000000))
                .quantity(1)
                .lineTotal(BigDecimal.valueOf(1000000))
                .build());
        return item;
    }

    private UserAccount saveUser(String email) {
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPhone("09" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
        user.setPassword("encoded");
        user.addProvider(AuthProvider.LOCAL);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        return userAccountRepository.save(user);
    }

    private record Fixture(UserAccount user, Product product, OrderItem orderItem) {
    }
}
