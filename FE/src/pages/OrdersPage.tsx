import { Link } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { ArrowLeft } from "lucide-react";
import { orderApi } from "../api/orderApi";
import { getApiError } from "../api/client";
import { Button, DataTable, EmptyState, PageHeader, Price, StatusBadge } from "../components/ui";
import { getPaymentStatus, getShippingStatus } from "../components/orderStatus";
import { formatDate, orderFinalTotal, orderProductTotal, shortId } from "../lib/format";

export function OrdersPage() {
  const [page, setPage] = useState(0);
  const orders = useQuery({
    queryKey: ["orders", "me", page],
    queryFn: () => orderApi.minePage(page, 20),
    refetchInterval: (query) => (query.state.data?.items.some((order) =>
      order.paymentMethod === "VNPAY"
      && (order.paymentStatus === "UNPAID" || (order.status === "RETURNED" && order.paymentStatus === "PAID"))
    ) ? 3000 : false),
    refetchOnWindowFocus: true
  });

  return (
    <div className="orders-page">
      <PageHeader
        title="Don hang cua toi"
        subtitle="Track order, payment and shipping status from one place."
        actions={
          <Link to="/" className="btn btn-secondary">
            <ArrowLeft size={16} /> Ve trang chu
          </Link>
        }
      />
      {orders.isError ? <EmptyState title="Khong lay duoc don hang" detail={getApiError(orders.error)} /> : null}
      {orders.data?.items.length === 0 ? <EmptyState title="Chua co don hang" /> : null}
      {orders.data?.items.length ? (
        <DataTable>
          <thead>
            <tr>
              <th>ID</th>
              <th>Order</th>
              <th>Payment</th>
              <th>Shipping</th>
              <th>Product</th>
              <th>Ship</th>
              <th>Discount</th>
              <th>Total</th>
              <th>Created</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {orders.data.items.map((order) => (
              <tr key={order.id}>
                <td>{shortId(order.id)}</td>
                <td><StatusBadge value={order.status} /></td>
                <td><StatusBadge value={getPaymentStatus(order)} /></td>
                <td><StatusBadge value={getShippingStatus(order)} /></td>
                <td><Price value={orderProductTotal(order)} /></td>
                <td><Price value={order.shippingPrice} /></td>
                <td><Price value={order.discountPrice} /></td>
                <td><Price value={orderFinalTotal(order)} /></td>
                <td>{formatDate(order.createdAt)}</td>
                <td><Link to={`/orders/${order.id}`}>Detail</Link></td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      ) : null}
      <div className="pagination-row">
        <Button variant="secondary" disabled={orders.data?.first ?? true} onClick={() => setPage((current) => Math.max(0, current - 1))}>Previous</Button>
        <span className="toolbar-count">Page {(orders.data?.page ?? page) + 1} / {Math.max(orders.data?.totalPages ?? 1, 1)}</span>
        <Button variant="secondary" disabled={orders.data?.last ?? true} onClick={() => setPage((current) => current + 1)}>Next</Button>
      </div>
    </div>
  );
}
