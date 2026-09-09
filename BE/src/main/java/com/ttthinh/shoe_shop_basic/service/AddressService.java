package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.address.AddAddressRequest;
import com.ttthinh.shoe_shop_basic.dto.response.address.AddressResponse;
import com.ttthinh.shoe_shop_basic.entity.customer.Address;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.exception.AppException;
import com.ttthinh.shoe_shop_basic.exception.ErrorCode;
import com.ttthinh.shoe_shop_basic.repository.jpa.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AddressService {
    private final AddressRepository addressRepository;

    public Address getAddressById(String  id) {
        return addressRepository.findById(id).orElseThrow(
                () -> new AppException(ErrorCode.ADDRESS_NOT_FOUND)
        );
    }
    public Address getDefaultAddress(UserAccount userAccount) {
        return addressRepository.findDefaultAddressByUserId(userAccount.getId());
    }

    public List<AddressResponse> getMyAddresses(UserAccount userAccount) {
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userAccount.getId())
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public AddressResponse changeDefaultAddress(String addressId, UserAccount userAccount) {
        addressRepository.findByUserIdForUpdate(userAccount.getId());
        Address address = getAddressById(addressId);
        if (!address.getUserId().equals(userAccount.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        validateAddress(address);
        addressRepository.clearDefaultByUserId(userAccount.getId());
        address.setIsDefault(true);
        return toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public AddressResponse updateAddress(String addressId, AddAddressRequest request, UserAccount userAccount) {
        validateAddressRequest(request);
        addressRepository.findByUserIdForUpdate(userAccount.getId());
        Address address = getAddressById(addressId);
        if (!address.getUserId().equals(userAccount.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userAccount.getId());
            address.setIsDefault(true);
        }
        applyAddressRequest(address, request);
        return toAddressResponse(addressRepository.save(address));
    }

    @Transactional
    public Address setDefaultAddress(AddAddressRequest request, UserAccount userAccount) {
        validateAddressRequest(request);
        addressRepository.findByUserIdForUpdate(userAccount.getId());

        // tìm default hiện tại (nếu có)
        Address currentDefault = addressRepository.findDefaultAddressByUserId(userAccount.getId());

        boolean hasDefault = currentDefault != null;

        // CASE 1: chưa có default -> address mới luôn là default
        if (!hasDefault) {
            request.setIsDefault(true);
        }

        // CASE 2: đã có default
//        if (hasDefault && request.getIsDefault()) {
//            // unset default cũ
//            currentDefault.setIsDefault(false);
//            addressRepository.save(currentDefault);
//        }
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.clearDefaultByUserId(userAccount.getId());
        }

//        // tạo address mới
//        Address newAddress = new Address();
//        newAddress.setUserId(userAccount.getId());
//        newAddress.setProvinceId(request.getProvinceId());
//        newAddress.setWardCode(request.getWardCode());
//        newAddress.setDistrictId(request.getDistrictId());
//        newAddress.setIsDefault(request.getIsDefault());
        String fullAddress = buildFullAddress(request);
        Address newAddress = Address.builder()
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .receiverName(request.getReceiverName())
                .phoneNumber(request.getPhoneNumber())
                .detailAddress(request.getDetailAddress())
                .districtId(request.getDistrictId())
                .provinceId(request.getProvinceId())
                .provinceName(request.getProvinceName())
                .districtName(request.getDistrictName())
                .wardCode(request.getWardCode())
                .wardName(request.getWardName())
                .userId(userAccount.getId())
                .fullAddress(fullAddress)
                .build();

        return addressRepository.save(newAddress);
    }

    @Transactional
    public AddressResponse deleteAddress(String addressId, UserAccount userAccount) {
        addressRepository.findByUserIdForUpdate(userAccount.getId());
        Address address = getAddressById(addressId);
        if (!address.getUserId().equals(userAccount.getId())) {
            throw new AppException(ErrorCode.UNAUTHORIZED_ACCESS);
        }
        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());
        AddressResponse response = toAddressResponse(address);
        addressRepository.delete(address);
        if (wasDefault) {
            addressRepository.findByUserIdOrderByIsDefaultDesc(userAccount.getId())
                    .stream()
                    .findFirst()
                    .ifPresent(nextDefault -> {
                        nextDefault.setIsDefault(true);
                        addressRepository.save(nextDefault);
                    });
        }
        return response;
    }

    public String buildFullAddress(AddAddressRequest req) {
        return Stream.of(req.getDetailAddress(), req.getWardName(), req.getDistrictName(), req.getProvinceName())
                .filter(value -> value != null && !value.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private void applyAddressRequest(Address address, AddAddressRequest request) {
        address.setReceiverName(request.getReceiverName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setDetailAddress(request.getDetailAddress());
        address.setProvinceId(request.getProvinceId());
        address.setDistrictId(request.getDistrictId());
        address.setWardCode(request.getWardCode());
        address.setProvinceName(request.getProvinceName());
        address.setDistrictName(request.getDistrictName());
        address.setWardName(request.getWardName());
        address.setFullAddress(buildFullAddress(request));
    }

    private void validateAddressRequest(AddAddressRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getReceiverName())
                || !StringUtils.hasText(request.getPhoneNumber())
                || !StringUtils.hasText(request.getDetailAddress())
                || request.getProvinceId() == null
                || request.getDistrictId() == null
                || !StringUtils.hasText(request.getWardCode())
                || !StringUtils.hasText(request.getProvinceName())
                || !StringUtils.hasText(request.getDistrictName())
                || !StringUtils.hasText(request.getWardName())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateAddress(Address address) {
        if (address == null
                || !StringUtils.hasText(address.getReceiverName())
                || !StringUtils.hasText(address.getPhoneNumber())
                || !StringUtils.hasText(address.getDetailAddress())
                || address.getProvinceId() == null
                || address.getDistrictId() == null
                || !StringUtils.hasText(address.getWardCode())
                || !StringUtils.hasText(address.getProvinceName())
                || !StringUtils.hasText(address.getDistrictName())
                || !StringUtils.hasText(address.getWardName())) {
            throw new AppException(ErrorCode.VALIDATION_ERROR);
        }
    }

    public AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .isDefault(address.getIsDefault())
                .receiverName(address.getReceiverName())
                .phoneNumber(address.getPhoneNumber())
                .provinceId(address.getProvinceId())
                .districtId(address.getDistrictId())
                .wardCode(address.getWardCode())
                .provinceName(address.getProvinceName())
                .districtName(address.getDistrictName())
                .wardName(address.getWardName())
                .detailAddress(address.getDetailAddress())
                .fullAddress(address.getFullAddress())
                .build();
    }
}
