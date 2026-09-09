package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.dto.request.promotion.VoucherRequest;
import com.ttthinh.shoe_shop_basic.dto.response.promotion.VoucherPreviewResponse;
import com.ttthinh.shoe_shop_basic.dto.response.promotion.VoucherResponse;
import com.ttthinh.shoe_shop_basic.entity.promotion.Voucher;
import com.ttthinh.shoe_shop_basic.entity.promotion.VoucherUsage;
import com.ttthinh.shoe_shop_basic.enums.VoucherType;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.VoucherRepository;
import com.ttthinh.shoe_shop_basic.repository.jpa.VoucherUsageRepository;
import com.ttthinh.shoe_shop_basic.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository voucherUsageRepository;

    @Override
    @Transactional(readOnly = true)
    public List<VoucherResponse> getAll() {
        return voucherRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VoucherResponse create(VoucherRequest request) {
        Voucher voucher = Voucher.builder()
                .code(normalizeCode(request.getCode()))
                .name(request.getName())
                .type(request.getType())
                .value(request.getValue())
                .minOrderAmount(defaultZero(request.getMinOrderAmount()))
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimit(request.getUsageLimit())
                .usedCount(0)
                .active(request.getActive() == null || request.getActive())
                .startsAt(request.getStartsAt())
                .endsAt(request.getEndsAt())
                .build();
        validateVoucherConfig(voucher);
        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public VoucherResponse setActive(String id, boolean active) {
        Voucher voucher = voucherRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        voucher.setActive(active);
        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional(readOnly = true)
    public VoucherPreviewResponse preview(String code, BigDecimal productTotal) {
        BigDecimal discount = calculateDiscount(code, productTotal);
        return VoucherPreviewResponse.builder()
                .code(normalizeCode(code))
                .discountAmount(discount)
                .productTotal(productTotal)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal calculateDiscount(String code, BigDecimal productTotal) {
        if (!StringUtils.hasText(code)) return BigDecimal.ZERO;
        Voucher voucher = voucherRepository.findByCodeIgnoreCaseForUpdate(normalizeCode(code))
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        validateVoucher(voucher, productTotal);
        BigDecimal discount = voucher.getType() == VoucherType.PERCENT
                ? productTotal.multiply(voucher.getValue()).divide(BigDecimal.valueOf(100), 2, RoundingMode.DOWN)
                : voucher.getValue();
        if (voucher.getMaxDiscountAmount() != null) {
            discount = discount.min(voucher.getMaxDiscountAmount());
        }
        return discount.min(productTotal).max(BigDecimal.ZERO);
    }

    @Override
    @Transactional
    public void markUsed(String code) {
        markUsed(code, null, null);
    }

    @Override
    @Transactional
    public void markUsed(String code, String orderId, String userId) {
        if (!StringUtils.hasText(code)) return;
        String normalizedCode = normalizeCode(code);
        if (StringUtils.hasText(orderId)
                && voucherUsageRepository.existsByOrderIdAndVoucherCodeAndReleasedAtIsNull(orderId, normalizedCode)) {
            return;
        }
        Voucher voucher = voucherRepository.findByCodeIgnoreCaseForUpdate(normalizedCode)
                .orElseThrow(() -> new AppException(ErrorCode.VOUCHER_NOT_FOUND));
        validateVoucher(voucher, null);
        voucher.setUsedCount((voucher.getUsedCount() == null ? 0 : voucher.getUsedCount()) + 1);
        voucherRepository.save(voucher);
        if (StringUtils.hasText(orderId)) {
            voucherUsageRepository.save(VoucherUsage.builder()
                    .voucher(voucher)
                    .voucherCode(normalizedCode)
                    .orderId(orderId)
                    .userId(userId)
                    .build());
        }
    }

    @Override
    @Transactional
    public void releaseUsed(String code) {
        releaseUsed(code, null);
    }

    @Override
    @Transactional
    public void releaseUsed(String code, String orderId) {
        if (!StringUtils.hasText(code)) return;
        String normalizedCode = normalizeCode(code);
        if (StringUtils.hasText(orderId)) {
            var usage = voucherUsageRepository.findByOrderIdAndVoucherCodeAndReleasedAtIsNull(orderId, normalizedCode);
            if (usage.isEmpty()) {
                return;
            }
            usage.ifPresent(activeUsage -> {
                activeUsage.setReleasedAt(LocalDateTime.now());
                voucherUsageRepository.save(activeUsage);
            });
        }
        voucherRepository.findByCodeIgnoreCaseForUpdate(normalizedCode)
                .ifPresent(voucher -> {
                    int usedCount = voucher.getUsedCount() == null ? 0 : voucher.getUsedCount();
                    voucher.setUsedCount(Math.max(0, usedCount - 1));
                    voucherRepository.save(voucher);
                });
    }

    private void validateVoucherConfig(Voucher voucher) {
        if (voucher.getType() == VoucherType.PERCENT
                && (voucher.getValue().compareTo(BigDecimal.ZERO) <= 0 || voucher.getValue().compareTo(BigDecimal.valueOf(100)) > 0)) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
        if (voucher.getType() == VoucherType.FIXED && voucher.getValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
    }

    private void validateVoucher(Voucher voucher, BigDecimal productTotal) {
        LocalDateTime now = LocalDateTime.now();
        if (!Boolean.TRUE.equals(voucher.getActive())) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
        if (voucher.getStartsAt() != null && now.isBefore(voucher.getStartsAt())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }
        if (voucher.getEndsAt() != null && now.isAfter(voucher.getEndsAt())) {
            throw new AppException(ErrorCode.VOUCHER_EXPIRED);
        }
        if (voucher.getUsageLimit() != null && voucher.getUsedCount() != null && voucher.getUsedCount() >= voucher.getUsageLimit()) {
            throw new AppException(ErrorCode.VOUCHER_USAGE_LIMIT_REACHED);
        }
        if (productTotal != null && productTotal.compareTo(defaultZero(voucher.getMinOrderAmount())) < 0) {
            throw new AppException(ErrorCode.VOUCHER_INVALID);
        }
    }

    private VoucherResponse toResponse(Voucher voucher) {
        return VoucherResponse.builder()
                .id(voucher.getId())
                .code(voucher.getCode())
                .name(voucher.getName())
                .type(voucher.getType())
                .value(voucher.getValue())
                .minOrderAmount(voucher.getMinOrderAmount())
                .maxDiscountAmount(voucher.getMaxDiscountAmount())
                .usageLimit(voucher.getUsageLimit())
                .usedCount(voucher.getUsedCount())
                .active(voucher.getActive())
                .startsAt(voucher.getStartsAt())
                .endsAt(voucher.getEndsAt())
                .build();
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
