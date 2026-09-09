package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.dto.request.order.BuyNowRequest;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.entity.catalog.Product;
import com.ttthinh.shoe_shop_basic.entity.catalog.ProductVariant;
import com.ttthinh.shoe_shop_basic.entity.catalog.VariantSize;
import com.ttthinh.shoe_shop_basic.entity.checkout.ShippingFeeSnapshot;
import com.ttthinh.shoe_shop_basic.entity.customer.Address;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import com.ttthinh.shoe_shop_basic.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.enums.PaymentMethod;
import com.ttthinh.shoe_shop_basic.enums.ProductStatus;
import com.ttthinh.shoe_shop_basic.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.repository.jpa.AddressRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.InventoryRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ProductVariantRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.ShippingFeeSnapshotRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.UserAccountRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.VariantSizeRepository;
import com.ttthinh.shoe_shop_basic.service.CheckoutService;
import com.ttthinh.shoe_shop_basic.service.OrderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
class CheckoutConcurrencyIntegrationTest {
    @Autowired
    private OrderService orderService;

    @Autowired
    private CheckoutService checkoutService;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private VariantSizeRepository variantSizeRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ShippingFeeSnapshotRepository shippingFeeSnapshotRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void concurrentBuyNowOnlyAllowsOneOrderWhenSingleUnitIsAvailable() throws Exception {
        VariantSize variantSize = createVariantSizeWithInventory(1);
        UserFixture firstUser = createUserFixture("first", variantSize);
        UserFixture secondUser = createUserFixture("second", variantSize);

        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Boolean>> results = List.of(
                    executor.submit(buyNowTask(start, firstUser, variantSize)),
                    executor.submit(buyNowTask(start, secondUser, variantSize))
            );

            start.countDown();

            long successes = results.stream()
                    .map(this::completedSuccessfully)
                    .filter(Boolean::booleanValue)
                    .count();

            Inventory inventory = inventoryRepository.findByVariantSizeId(variantSize.getId()).orElseThrow();
            Long createdOrders = entityManager.createQuery("""
                            select count(item)
                            from OrderItem item
                            where item.variantSize.id = :variantSizeId
                            """, Long.class)
                    .setParameter("variantSizeId", variantSize.getId())
                    .getSingleResult();

            assertEquals(1, successes);
            assertEquals(1, createdOrders);
            assertEquals(1, inventory.getQuantity());
            assertEquals(1, inventory.getQuantityLocked());
        } finally {
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> buyNowTask(CountDownLatch start, UserFixture fixture, VariantSize variantSize) {
        return () -> {
            start.await();
            orderService.buyNow(fixture.user(), BuyNowRequest.builder()
                    .variantSizeId(variantSize.getId())
                    .quantity(1)
                    .paymentMethod(PaymentMethod.COD)
                    .addressId(fixture.address().getId())
                    .shippingFeeSnapshotId(fixture.snapshot().getId())
                    .build());
            return true;
        };
    }

    private boolean completedSuccessfully(Future<Boolean> result) {
        try {
            return Boolean.TRUE.equals(result.get());
        } catch (Exception ignored) {
            return false;
        }
    }

    private VariantSize createVariantSizeWithInventory(int quantity) {
        Product product = productRepository.save(Product.builder()
                .name("Concurrency Test Shoe " + System.nanoTime())
                .slug("concurrency-test-shoe-" + System.nanoTime())
                .basePrice(BigDecimal.valueOf(100000))
                .status(ProductStatus.ACTIVE)
                .build());
        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .color("White")
                .active(true)
                .build());
        VariantSize variantSize = variantSizeRepository.save(VariantSize.builder()
                .variant(variant)
                .size("42")
                .sku("CONCURRENCY-" + System.nanoTime())
                .price(BigDecimal.valueOf(100000))
                .build());
        inventoryRepository.save(Inventory.builder()
                .variantSize(variantSize)
                .quantity(quantity)
                .quantityLocked(0)
                .build());
        return variantSize;
    }

    private UserFixture createUserFixture(String prefix, VariantSize variantSize) {
        UserAccount user = new UserAccount();
        user.setEmail(prefix + "-checkout-" + System.nanoTime() + "@example.test");
        user.setPassword("encoded-password");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setProviders(Set.of(AuthProvider.LOCAL));
        user = userAccountRepository.save(user);

        Address address = addressRepository.save(Address.builder()
                .userId(user.getId())
                .isDefault(true)
                .receiverName("Checkout User")
                .phoneNumber("0900000000")
                .provinceId(1)
                .districtId(1)
                .wardCode("1")
                .provinceName("Province")
                .districtName("District")
                .wardName("Ward")
                .detailAddress("Test address")
                .fullAddress("Test address, Ward, District, Province")
                .build());

        BigDecimal productTotal = BigDecimal.valueOf(100000);
        BigDecimal shippingFee = BigDecimal.valueOf(30000);
        ShippingFeeSnapshot snapshot = shippingFeeSnapshotRepository.save(ShippingFeeSnapshot.builder()
                .userId(user.getId())
                .addressId(address.getId())
                .cartSignature(checkoutService.buildBuyNowSignature(variantSize.getId(), 1))
                .productTotal(productTotal)
                .shippingFee(shippingFee)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(productTotal.add(shippingFee))
                .weight(200)
                .length(0)
                .width(0)
                .height(0)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build());

        return new UserFixture(user, address, snapshot);
    }

    private record UserFixture(UserAccount user, Address address, ShippingFeeSnapshot snapshot) {
    }
}
