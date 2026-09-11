package com.ttthinh.shoe_shop_basic.auth.service;

import com.ttthinh.shoe_shop_basic.auth.dto.request.RegisterRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ChangePasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ForgotPasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.request.ResetPasswordRequest;
import com.ttthinh.shoe_shop_basic.auth.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    UserResponse register(RegisterRequest registerRequest);
    List<UserResponse> getAllUsers();
    UserResponse getMyInformation();
    UserResponse getMyInformationById(String id);
    void deleteAllUsers();
    void changePassword(ChangePasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}
