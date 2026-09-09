package com.ttthinh.shoe_shop_basic.repository.jpa;

import com.ttthinh.shoe_shop_basic.entity.customer.Address;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AddressRepository extends JpaRepository<Address, String> {
    Address findDefaultAddressByUserId(String userId);
    List<Address> findByUserIdOrderByIsDefaultDesc(String userId);
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId")
    void clearDefaultByUserId(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Address a where a.userId = :userId")
    List<Address> findByUserIdForUpdate(@Param("userId") String userId);
}
