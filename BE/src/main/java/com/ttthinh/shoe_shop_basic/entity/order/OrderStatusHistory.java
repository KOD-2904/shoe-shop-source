package com.ttthinh.shoe_shop_basic.entity.order;

import com.ttthinh.shoe_shop_basic.entity.BaseEntity;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(
        name = "order_status_history",
        indexes = {
                @Index(name = "idx_order_status_history_order_id", columnList = "order_id"),
                @Index(name = "idx_order_status_history_actor_id", columnList = "actor_id")
        }
)
public class OrderStatusHistory extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    UserAccount actor;

    @Column(length = 40)
    String oldOrderStatus;

    @Column(length = 40)
    String newOrderStatus;

    @Column(length = 40)
    String oldPaymentStatus;

    @Column(length = 40)
    String newPaymentStatus;

    @Column(length = 40)
    String oldShippingStatus;

    @Column(length = 40)
    String newShippingStatus;

    @Column(nullable = false, length = 64)
    String source;

    @Column(length = 1000)
    String note;
}
