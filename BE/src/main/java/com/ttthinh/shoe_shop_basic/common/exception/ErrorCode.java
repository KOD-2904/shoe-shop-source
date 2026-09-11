package com.ttthinh.shoe_shop_basic.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNAUTHENTICATED_EXCEPTION(9999, "Unauthenticated Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXIST(1001, "User Exist", HttpStatus.CONFLICT),
    USER_NOT_EXIST(1002, "User NOT Exist", HttpStatus.BAD_REQUEST),  // Đã sửa code từ 1001 → 1002
    EMAIL_EXIST(1003, "Email Exist", HttpStatus.BAD_REQUEST),
    PHONE_EXIST(1004, "Phone Exist", HttpStatus.BAD_REQUEST),
    USERNAME_EXIST(10011, "Username Exist", HttpStatus.CONFLICT),
    VALIDATION_ERROR(1005, "Validation Error", HttpStatus.BAD_REQUEST),
    ROLE_NOT_EXIST(1006, "Role NOT exist", HttpStatus.BAD_REQUEST),
    PASSWORD_NOT_MATCH(1007, "Password Not Match", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED_ACCESS(1008, "You do not have permission", HttpStatus.FORBIDDEN),  // FORBIDDEN = 403
    UNAUTHENTICATED(1009, "Unauthenticated", HttpStatus.UNAUTHORIZED),  // UNAUTHORIZED = 401
    USERNAME_NOT_VALID(10010, "Username Not Valid", HttpStatus.BAD_REQUEST),
    NOT_VALID_TOKEN(10011, "Not Valid Token", HttpStatus.BAD_REQUEST),
    USERNAME_PASSWORD_NOT_MATCH(10011, "Username or Pw not match", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_ACTIVE(10011, "Account not active", HttpStatus.BAD_REQUEST),
    END_POINT_NOT_FOUND(10012, "End Point Not Found", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(10013, "Category Not Found", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND(10014, "Product Not Found", HttpStatus.BAD_REQUEST),
    BRAND_NOT_FOUND(10015, "Brand Not Found", HttpStatus.BAD_REQUEST),
    PRODUCT_VARIANT_NOT_FOUND(10016, "Product Variant Not Found", HttpStatus.BAD_REQUEST),
    UPLOAD_IMAGE_TO_CLOUD_FAILED(10017, "Upload Image to Cloud Failed", HttpStatus.BAD_REQUEST),
    SIZE_UPLOAD_EXCEEDED(10018, "Size Upload Exceeded", HttpStatus.BAD_REQUEST),
    DATA_INTEGRITY_VIOLATION(10019, "Data Integrity Violation", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCT_VARIANT_REQUEST(10020,"INVALID_PRODUCT_VARIANT_REQUEST" ,HttpStatus.BAD_REQUEST ),
    QUANTITY_NOT_VALID(10021, "Quantity Not Valid", HttpStatus.BAD_REQUEST),
    OUT_OF_STOCK(10022, "Out of Stock", HttpStatus.BAD_REQUEST),
    CART_ITEM_NOT_FOUND(10023, "Cart Item Not Found", HttpStatus.BAD_REQUEST),
    TOKEN_EXPIRED(10024, "Token Expired", HttpStatus.UNAUTHORIZED),
    CART_EMPTY(10025, "Cart Empty", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND(10026, "Order Not Found" , HttpStatus.BAD_REQUEST ),
    ORDER_CANNOT_CANCEL(10027, "Order Can Cancel", HttpStatus.BAD_REQUEST ),
    INVALID_ORDER_STATUS(10028, "Invalid Order Status", HttpStatus.BAD_REQUEST ),
    PAYMENT_NOT_FOUND(10029, "Payment Not Found", HttpStatus.BAD_REQUEST ),
    UNAUTHORIZED_DEVICE(10030, "Unauthorized Device", HttpStatus.UNAUTHORIZED),
    INVALID_LOGIN_PROVIDER(10033, "Invalid login provider", HttpStatus.BAD_REQUEST),
    GOOGLE_AUTH_FAILED(10034, "Google authentication failed", HttpStatus.UNAUTHORIZED),
    CAN_NOT_SOLVE_SHIPPING_FEE(10029, "Can Not Solve Shipping Fee", HttpStatus.BAD_REQUEST ),
    CAN_NOT_CONNECT_GHN(10030, "Can not connect to GHN", HttpStatus.BAD_REQUEST),
    CAN_NOT_HASH(10032, "Error while hashing", HttpStatus.BAD_REQUEST) ,
    ADDRESS_NOT_FOUND(10031, "Address Not Found", HttpStatus.BAD_REQUEST ),
    CHECKOUT_SNAPSHOT_NOT_FOUND(10035, "Checkout snapshot not found", HttpStatus.BAD_REQUEST),
    CHECKOUT_SNAPSHOT_EXPIRED(10036, "Checkout snapshot expired", HttpStatus.BAD_REQUEST),
    CHECKOUT_SNAPSHOT_MISMATCH(10037, "Checkout snapshot does not match current checkout data", HttpStatus.BAD_REQUEST),
    PAYMENT_ALREADY_PROCESSED(10038, "Payment already processed", HttpStatus.BAD_REQUEST),
    SHIPPING_ORDER_FAILED(10039, "Shipping order failed", HttpStatus.BAD_REQUEST),
    TOO_MANY_REFRESH_REQUESTS(10040, "Too many refresh token requests", HttpStatus.TOO_MANY_REQUESTS),
    VNPAY_REFUND_FAILED(10041, "VNPAY refund failed", HttpStatus.BAD_GATEWAY),
    PRODUCT_REVIEW_NOT_FOUND(10042, "Product Review Not Found", HttpStatus.BAD_REQUEST),
    PRODUCT_REVIEW_NOT_ALLOWED(10043, "Only delivered purchased products can be reviewed", HttpStatus.BAD_REQUEST),
    VOUCHER_NOT_FOUND(10044, "Voucher Not Found", HttpStatus.BAD_REQUEST),
    VOUCHER_INVALID(10045, "Voucher Invalid", HttpStatus.BAD_REQUEST),
    VOUCHER_EXPIRED(10046, "Voucher Expired", HttpStatus.BAD_REQUEST),
    VOUCHER_USAGE_LIMIT_REACHED(10047, "Voucher Usage Limit Reached", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_PAYMENT_METHOD(10048, "Unsupported payment method", HttpStatus.BAD_REQUEST),
    PRODUCT_DISCOUNT_NOT_FOUND(10049, "Product Discount Not Found", HttpStatus.BAD_REQUEST),
    PRODUCT_DISCOUNT_TARGET_NOT_FOUND(10050, "Product Discount Target Not Found", HttpStatus.BAD_REQUEST),
    PASSWORD_RESET_NOT_ALLOWED(10051, "Password reset is only available for local password accounts", HttpStatus.BAD_REQUEST),
    VNPAY_PAYMENT_EXPIRED(10052, "VNPay payment expired", HttpStatus.BAD_REQUEST),;

    private final int code;  // Mã lỗi tự định nghĩa
    private final String message;
    private final HttpStatus httpStatus;  // HTTP status code

    ErrorCode(int code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}
