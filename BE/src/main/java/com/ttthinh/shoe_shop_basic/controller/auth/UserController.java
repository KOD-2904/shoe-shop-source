package com.ttthinh.shoe_shop_basic.controller.auth;

import com.ttthinh.shoe_shop_basic.dto.request.auth.RegisterRequest;
import com.ttthinh.shoe_shop_basic.dto.request.address.AddAddressRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.auth.UserResponse;
import com.ttthinh.shoe_shop_basic.entity.auth.UserAccount;
import com.ttthinh.shoe_shop_basic.service.auth.UserService;

import com.ttthinh.shoe_shop_basic.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AddressService addressService;

    @PostMapping("/address/add")
    public ApiResponse addAddress(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid AddAddressRequest addAddressRequest) {
        var result = addressService.setDefaultAddress(addAddressRequest, user);
        return ApiResponse.builder()
                .result(result)
                .message("Add address successful")
                .code(200)
                .build();
    }

    @GetMapping("/users/addresses")
    public ApiResponse getMyAddresses(
            @AuthenticationPrincipal(expression = "user") UserAccount user) {
        var result = addressService.getMyAddresses(user);
        return ApiResponse.builder()
                .result(result)
                .message("success")
                .code(200)
                .build();
    }

    @GetMapping("/api/addresses")
    public ApiResponse getAddresses(
            @AuthenticationPrincipal(expression = "user") UserAccount user) {
        return getMyAddresses(user);
    }

    @PostMapping("/api/addresses")
    public ApiResponse createAddress(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @RequestBody @Valid AddAddressRequest addAddressRequest) {
        var address = addressService.setDefaultAddress(addAddressRequest, user);
        return ApiResponse.builder()
                .result(addressService.toAddressResponse(address))
                .message("Add address successful")
                .code(200)
                .build();
    }

    @PutMapping("/api/addresses/{addressId}/default")
    public ApiResponse changeDefaultAddress(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String addressId) {
        return ApiResponse.builder()
                .result(addressService.changeDefaultAddress(addressId, user))
                .message("Default address updated")
                .code(200)
                .build();
    }

    @PutMapping("/api/addresses/{addressId}")
    public ApiResponse updateAddress(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String addressId,
            @RequestBody @Valid AddAddressRequest addAddressRequest) {
        return ApiResponse.builder()
                .result(addressService.updateAddress(addressId, addAddressRequest, user))
                .message("Address updated")
                .code(200)
                .build();
    }

    @DeleteMapping("/api/addresses/{addressId}")
    public ApiResponse deleteAddress(
            @AuthenticationPrincipal(expression = "user") UserAccount user,
            @PathVariable String addressId) {
        return ApiResponse.builder()
                .result(addressService.deleteAddress(addressId, user))
                .message("Address deleted")
                .code(200)
                .build();
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest
    ) {
        UserResponse userResponse = userService.register(registerRequest);
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Registered Successfully")
                .result(userResponse)
                .build();
        //return ApiResponse.success("Registered Successfully",userResponse);
    }

    @GetMapping("/users")
    public ApiResponse<List<UserResponse>> getUsers() {
        var list = userService.getAllUsers();
        return ApiResponse.<List<UserResponse>>builder()
                .code(200)
                .message("Found " + list.size() + " users")
                .result(list)
                .build();
    }
    @GetMapping("/myinfor")
    public ApiResponse<UserResponse> getMyInformation() {
        var userResponse = userService.getMyInformation();
        return ApiResponse.<UserResponse>builder()
                .message("Get Information Successfully")
                .result(userResponse)
                .build();
    }

    @GetMapping("/inforbyid/{id}")
    public ApiResponse<UserResponse> getMyInformationById(@PathVariable String id) {
        return ApiResponse.<UserResponse>builder()
                .code(200)
                .message("Get Information Successfully")
                .result(userService.getMyInformationById(id))
                .build();
    }
    @DeleteMapping("/users/delete")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ApiResponse deleteAllUser() {
        userService.deleteAllUsers();
        return ApiResponse.builder()
                .code(200)
                .message("Deleted Successfully")
                .build();
    }
}
