package com.ttthinh.shoe_shop_basic.service.auth;

import com.ttthinh.shoe_shop_basic.dto.request.auth.RegisterRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse register(RegisterRequest registerRequest);
    List<UserResponse> getAllUsers();
    UserResponse getMyInformation();
    UserResponse getMyInformationById(String id);
    void deleteAllUsers();
}
