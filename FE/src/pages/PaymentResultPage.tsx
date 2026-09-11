import { Link, useSearchParams } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { orderApi } from "../api/orderApi";
import { getApiError } from "../api/client";
import { EmptyState, Panel, Price, StatusBadge } from "../components/ui";
import { getPaymentStatus } from "../components/orderStatus";
import { orderFinalTotal } from "../lib/format";

export function PaymentResultPage() {
  const [params] = useSearchParams();
  const code = params.get("code") || params.get("vnp_ResponseCode");
  const transactionStatus = params.get("transactionStatus") || params.get("vnp_TransactionStatus");
  const orderId = params.get("orderId") || params.get("vnp_TxnRef");
  const paymentId = params.get("paymentId") || params.get("vnp_OrderInfo");
  const redirectSuccess = code === "00" && transactionStatus === "00";
  const order = useQuery({
    queryKey: ["orders", orderId],
    queryFn: () => orderApi.byId(orderId!),
    enabled: Boolean(orderId),
    refetchInterval: (query) => (query.state.data?.paymentStatus === "UNPAID" ? 2000 : false),
    refetchOnWindowFocus: true
  });
  const paymentSuccess = redirectSuccess || getPaymentStatus(order.data) === "PAID";

  return (
    <div className="narrow">
      <EmptyState
        title={paymentSuccess ? "Đã thanh toán thành công đơn hàng" : "Ket qua thanh toan VNPAY"}
        detail={paymentSuccess ? undefined : "Trang nay chi nhan du lieu redirect. Trang thai thanh toan cuoi cung duoc lay tu chi tiet don hang sau khi VNPAY IPN cap nhat."}
      />
      <Panel>
        <div className="info-grid one">
          <div><span>Redirect code</span><strong>{code || "-"}</strong></div>
          <div><span>Transaction status</span><strong>{transactionStatus || "-"}</strong></div>
          <div><span>Payment ID</span><strong className="mono">{paymentId || "-"}</strong></div>
          <div><span>Order ID</span><strong className="mono">{orderId || "-"}</strong></div>
        </div>
      </Panel>
      {order.isError ? <EmptyState title="Khong lay duoc don hang" detail={getApiError(order.error)} /> : null}
      {order.data ? (
        <Panel>
          <div className="info-grid one">
            <div><span>Order</span><StatusBadge value={order.data.status} /></div>
            <div><span>Payment</span><StatusBadge value={getPaymentStatus(order.data)} /></div>
            <div><span>Total</span><Price value={orderFinalTotal(order.data)} /></div>
          </div>
        </Panel>
      ) : null}
      <div className="center">
        {orderId ? <Link to={`/orders/${orderId}`} className="btn btn-primary">Xem don hang</Link> : <Link to="/orders" className="btn btn-primary">Xem don hang</Link>}
      </div>
    </div>
  );
}
