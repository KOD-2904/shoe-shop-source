package com.ttthinh.shoe_shop_basic.dto.request.payment;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VNPayCallbackRequest {
    private String vnp_TmnCode;           // Mã website
    private String vnp_Amount;            // Số tiền thanh toán (đã *100)
    private String vnp_BankCode;          // Mã ngân hàng
    private String vnp_BankTranNo;        // Mã giao dịch tại ngân hàng
    private String vnp_CardType;          // Loại thẻ
    private String vnp_OrderInfo;         // Thông tin đơn hàng
    private String vnp_PayDate;           // Thời gian thanh toán
    private String vnp_ResponseCode;      // Mã phản hồi (00 = thành công)
    private String vnp_TxnRef;            // Mã tham chiếu (orderId của merchant)
    private String vnp_TransactionNo;     // Mã giao dịch VNPAY
    private String vnp_TransactionStatus; // Trạng thái giao dịch
    private String vnp_SecureHash;        // Chữ ký bảo mật

}
