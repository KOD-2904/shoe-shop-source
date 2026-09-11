package com.ttthinh.shoe_shop_basic.order.entity;

import com.ttthinh.shoe_shop_basic.common.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.order.enums.OrderStatus;
import com.ttthinh.shoe_shop_basic.order.enums.ShippingStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    UserAccount user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    OrderStatus status;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    ShippingStatus shippingStatus;

    BigDecimal subtotal;

    BigDecimal discountPrice;

    BigDecimal totalPrice;  //tong gia san pham

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<OrderItem> items = new HashSet<>();

    private String addressId;           // ID địa chỉ giao hàng
    private String shippingAddress;   // full address text (backup)
    private BigDecimal shippingFee;   // phí ship
    private BigDecimal finalTotal;    // tổng = totalPrice + shippingFee
    @Column(length = 64)
    private String voucherCode;
    @Column(nullable = false)
    private Boolean voucherUsageCounted = false;
    private LocalDateTime voucherUsageReleasedAt;
    private String note;              // ghi chú đơn hàng
    private String phoneNumber;       // số điện thoại nhận hàng
    private String receiverName;
    private String shippingProvider;
    private String shippingOrderCode;
    private String trackingUrl;
    private LocalDateTime shippingCreatedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
}
