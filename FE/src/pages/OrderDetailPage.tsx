import { Link, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft } from "lucide-react";
import { orderApi } from "../api/orderApi";
import { paymentApi } from "../api/paymentApi";
import { getApiError } from "../api/client";
import { Button, DataTable, EmptyState, PageHeader, Panel, Price, StatusBadge } from "../components/ui";
import { getPaymentStatus, getShippingStatus, OrderTimeline, StatusSummary } from "../components/orderStatus";
import { formatDate, orderFinalTotal, orderProductTotal } from "../lib/format";
import { useToast } from "../state/ToastContext";

export function OrderDetailPage() {
  const { id } = useParams();
  const toast = useToast();
  const queryClient = useQueryClient();
  const order = useQuery({
    queryKey: ["orders", id],
    queryFn: () => orderApi.byId(id!),
    enabled: Boolean(id),
    refetchInterval: (query) => {
      const current = query.state.data;
      if (current?.paymentMethod !== "VNPAY") return false;
      if (current.paymentStatus === "UNPAID") return 2000;
      return current.status === "RETURNED" && current.paymentStatus === "PAID" ? 5000 : false;
    },
    refetchOnWindowFocus: true
  });
  const history = useQuery({
    queryKey: ["orders", id, "history"],
    queryFn: () => orderApi.history(id!),
    enabled: Boolean(id)
  });
  const createPayment = useMutation({
    mutationFn: () => paymentApi.createVnpay(id!),
    onSuccess: (payment) => {
      window.location.href = payment.paymentUrl;
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const cancelOrder = useMutation({
    mutationFn: () => orderApi.cancel(id!),
    onSuccess: async (cancelledOrder) => {
      toast.success(cancelledOrder.paymentStatus === "REFUNDED" ? "Da huy don va hoan tien" : "Da huy don hang");
      await queryClient.invalidateQueries({ queryKey: ["orders"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const canPayVnpay = order.data?.paymentMethod === "VNPAY" && order.data?.paymentStatus === "UNPAID" && order.data?.status === "PENDING";
  const canCancel = Boolean(order.data && ["PENDING", "CONFIRMED"].includes(order.data.status));
  const productTotal = orderProductTotal(order.data);

  if (order.isError) return <EmptyState title="Khong lay duoc don hang" detail={getApiError(order.error)} />;

  return (
    <div className="order-detail-page">
      <PageHeader
        eyebrow="Order detail"
        title={`Order ${id?.slice(0, 8) || ""}`}
        actions={
          <>
            <Link to="/orders" className="btn btn-secondary">
              <ArrowLeft size={16} /> Quay lai
            </Link>
            {canPayVnpay ? (
              <Button loading={createPayment.isPending} onClick={() => createPayment.mutate()}>
                Thanh toan VNPAY
              </Button>
            ) : null}
            {canCancel ? (
              <Button
                variant="danger"
                loading={cancelOrder.isPending}
                onClick={() => window.confirm("Huy don hang nay? Don da thanh toan se duoc hoan tien.") && cancelOrder.mutate()}
              >
                Huy don
              </Button>
            ) : null}
          </>
        }
      />
      <StatusSummary order={order.data} />
      <Panel>
        <div className="info-grid">
          <div><span>Order</span><StatusBadge value={order.data?.status} /></div>
          <div><span>Payment</span><StatusBadge value={getPaymentStatus(order.data)} /></div>
          <div><span>Shipping status</span><StatusBadge value={getShippingStatus(order.data)} /></div>
          <div><span>Products</span><Price value={productTotal} /></div>
          <div><span>Shipping</span><Price value={order.data?.shippingPrice} /></div>
          <div><span>Discount{order.data?.voucherCode ? ` (${order.data.voucherCode})` : ""}</span><Price value={order.data?.discountPrice} /></div>
          <div><span>Total</span><Price value={orderFinalTotal(order.data)} /></div>
          <div><span>Created</span><strong>{formatDate(order.data?.createdAt)}</strong></div>
        </div>
      </Panel>
      <Panel>
        <OrderTimeline order={order.data} />
      </Panel>
      <Panel>
        <div className="section-heading compact">
          <div>
            <span className="eyebrow">Activity</span>
            <h2>Lich su don hang</h2>
          </div>
        </div>
        {history.isError ? <EmptyState title="Khong lay duoc lich su don hang" detail={getApiError(history.error)} /> : null}
        {!history.isLoading && history.data?.length === 0 ? <EmptyState title="Chua co lich su trang thai" /> : null}
        <div className="activity-list">
          {history.data?.map((item) => (
            <article className="activity-item" key={item.id}>
              <span className="activity-dot" aria-hidden="true" />
              <div>
                <div className="row-between">
                  <strong>{item.source.replace(/_/g, " ")}</strong>
                  <span className="muted">{formatDate(item.createdAt)}</span>
                </div>
                <p>{item.note || "Status updated"}</p>
                <div className="activity-badges">
                  {item.newOrderStatus ? <StatusBadge value={item.newOrderStatus} /> : null}
                  {item.newPaymentStatus ? <StatusBadge value={item.newPaymentStatus} /> : null}
                  {item.newShippingStatus ? <StatusBadge value={item.newShippingStatus} /> : null}
                </div>
              </div>
            </article>
          ))}
        </div>
      </Panel>
      <DataTable>
        <thead>
          <tr><th>Product</th><th>Variant</th><th>Qty</th><th>Unit</th><th>Total</th><th></th></tr>
        </thead>
        <tbody>
          {order.data?.items.map((item) => (
            <tr key={item.id}>
              <td>{item.productName}</td>
              <td>{[item.variantName, item.size ? `Size ${item.size}` : undefined].filter(Boolean).join(" / ") || item.variantSizeId || item.variantId}</td>
              <td>{item.quantity}</td>
              <td><Price value={item.unitPrice} /></td>
              <td><Price value={item.totalPrice} /></td>
              <td>
                {order.data?.status === "DELIVERED" ? (
                  item.reviewed ? <StatusBadge value="REVIEWED" /> : <Link to={`/products/${item.productId}?reviewItem=${item.id}`}>Review</Link>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </DataTable>
    </div>
  );
}
