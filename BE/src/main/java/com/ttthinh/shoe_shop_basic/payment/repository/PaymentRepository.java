package com.ttthinh.shoe_shop_basic.payment.repository;

import com.ttthinh.shoe_shop_basic.order.entity.Order;
import com.ttthinh.shoe_shop_basic.payment.entity.Payment;
import com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findPaymentById(String id);

    Optional<Payment> findByVnpTxnRef(String vnpTxnRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order where p.order.id = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.id = :id")
    Optional<Payment> findPaymentByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.order where p.vnpTxnRef = :vnpTxnRef")
    Optional<Payment> findByVnpTxnRefForUpdate(@Param("vnpTxnRef") String vnpTxnRef);

    List<Payment> findByOrderId(String orderId);
    Optional<Payment> findByOrderIdAndStatus(String orderId, PaymentStatus status);
    Optional<Payment> findTopByOrderIdAndStatusOrderByCreatedAtDesc(String orderId, PaymentStatus status);
    Optional<Payment> findTopByOrderIdOrderByCreatedAtDesc(String orderId);
    boolean existsByOrderIdAndStatus(String orderId, PaymentStatus status);
    boolean existsByOrderIdAndStatusIn(String orderId, Collection<PaymentStatus> statuses);

    Payment findByOrder(Order order);
    List<Payment> findPaymentByStatus(PaymentStatus status);
    List<Payment> findByStatusAndExpiredAtBefore(PaymentStatus status, LocalDateTime now);

    @Query("select p.id from Payment p where p.status = :status and p.expiredAt < :now")
    List<String> findExpiredPaymentIds(@Param("status") PaymentStatus status, @Param("now") LocalDateTime now);

    @Query("""
            select p
            from Payment p
            where p.order.id = :orderId
            order by
                case when p.status = com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus.PAID then 0 else 1 end,
                p.createdAt desc
            """)
    List<Payment> findDisplayPayments(@Param("orderId") String orderId);

    @Query("""
            select p
            from Payment p
            where p.order.id in :orderIds
            order by
                case when p.status = com.ttthinh.shoe_shop_basic.payment.enums.PaymentStatus.PAID then 0 else 1 end,
                p.createdAt desc
            """)
    List<Payment> findDisplayPaymentsForOrders(@Param("orderIds") Collection<String> orderIds);
}
