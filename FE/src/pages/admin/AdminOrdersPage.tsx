import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RefreshCw, Search, Truck } from "lucide-react";
import { adminApi } from "../../api/adminApi";
import { getApiError } from "../../api/client";
import { Button, DataTable, EmptyState, Field, InlineAlert, Input, PageHeader, Panel, Price, Select, StatusBadge } from "../../components/ui";
import { getNextOrderStatuses, getPaymentStatus, getShippingStatus, isFinalOrderStatus, orderStatuses } from "../../components/orderStatus";
import { formatDate, orderFinalTotal, shortId } from "../../lib/format";
import { useToast } from "../../state/ToastContext";
import type { GHNMode, OrderStatus } from "../../types";

export function AdminOrdersPage() {
  const [statusByOrder, setStatusByOrder] = useState<Record<string, OrderStatus>>({});
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [query, setQuery] = useState("");
  const [page, setPage] = useState(0);
  const [selectedHistoryOrderId, setSelectedHistoryOrderId] = useState<string | null>(null);
  const [mockFixedFee, setMockFixedFee] = useState("");
  const toast = useToast();
  const queryClient = useQueryClient();
  const orders = useQuery({ queryKey: ["admin", "orders", page], queryFn: () => adminApi.ordersPage(page, 20) });
  const ghnSettings = useQuery({ queryKey: ["admin", "shipping", "ghn", "settings"], queryFn: adminApi.ghnSettings });
  const orderHistory = useQuery({
    queryKey: ["admin", "orders", selectedHistoryOrderId, "history"],
    queryFn: () => adminApi.orderHistory(selectedHistoryOrderId!),
    enabled: Boolean(selectedHistoryOrderId)
  });
  const filteredOrders = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (orders.data?.items ?? []).filter((order) => {
      const paymentStatus = getPaymentStatus(order);
      const shippingStatus = getShippingStatus(order);
      const matchesStatus = statusFilter === "ALL" || [order.status, paymentStatus, shippingStatus].includes(statusFilter);
      const matchesQuery = !keyword || [order.id, order.userId, order.paymentId, order.shippingOrderCode].some((value) => value?.toLowerCase().includes(keyword));
      return matchesStatus && matchesQuery;
    });
  }, [orders.data?.items, query, statusFilter]);
  const totalRevenue = filteredOrders.reduce((sum, order) => sum + (orderFinalTotal(order) || 0), 0);
  const pendingCount = orders.data?.items.filter((order) => ["PENDING", "CONFIRMED", "PACKING"].includes(order.status)).length ?? 0;
  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["admin", "orders"] });
  useEffect(() => {
    if (ghnSettings.data?.mockFixedFee !== undefined) {
      setMockFixedFee(String(ghnSettings.data.mockFixedFee));
    }
  }, [ghnSettings.data?.mockFixedFee]);

  const parsedMockFee = Number(mockFixedFee);
  const mockFeeValid = mockFixedFee.trim() !== "" && Number.isFinite(parsedMockFee) && parsedMockFee >= 0;

  const updateGhnSettings = useMutation({
    mutationFn: adminApi.updateGhnSettings,
    onSuccess: async (settings) => {
      toast.success(settings.mode === "MOCK_TEST" ? "Da bat GHN MOCK_TEST" : "Da bat GHN REAL");
      queryClient.setQueryData(["admin", "shipping", "ghn", "settings"], settings);
      await queryClient.invalidateQueries({ queryKey: ["admin", "shipping", "ghn", "settings"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const updateGhnMode = (mode: GHNMode) => {
    updateGhnSettings.mutate({
      mode,
      mockFixedFee: mockFeeValid ? Math.round(parsedMockFee) : undefined
    });
  };

  const saveMockFee = () => {
    if (!mockFeeValid) {
      toast.error("Phi ship mock khong hop le");
      return;
    }
    updateGhnSettings.mutate({
      mode: ghnSettings.data?.mode ?? "MOCK_TEST",
      mockFixedFee: Math.round(parsedMockFee)
    });
  };

  const update = useMutation({
    mutationFn: ({ id, status }: { id: string; status: OrderStatus }) => adminApi.updateStatus(id, status),
    onSuccess: async () => {
      toast.success("Da cap nhat order");
      await invalidate();
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const cancel = useMutation({
    mutationFn: adminApi.cancel,
    onSuccess: async (cancelledOrder) => {
      toast.success(cancelledOrder.paymentStatus === "REFUNDED" ? "Da huy order va hoan tien" : "Da cancel order");
      await invalidate();
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const createShipping = useMutation({
    mutationFn: adminApi.createShipping,
    onSuccess: async (createdOrder) => {
      toast.success(createdOrder.shippingProvider === "GHN_MOCK_TEST" ? "Da tao van don GHN mock" : "Da tao van don GHN");
      await invalidate();
    },
    onError: (error) => toast.error(getApiError(error))
  });

  return (
    <div className="admin-page">
      <PageHeader
        title="Orders"
        actions={
          <Button variant="secondary" onClick={() => orders.refetch()} loading={orders.isFetching}>
            <RefreshCw size={16} /> Refresh
          </Button>
        }
      />
      <div className="metric-grid compact">
        <Panel><span>Filtered revenue</span><Price value={totalRevenue} /></Panel>
        <Panel><span>Need action</span><strong>{pendingCount}</strong></Panel>
        <Panel><span>Visible orders</span><strong>{filteredOrders.length} / {orders.data?.totalItems ?? 0}</strong></Panel>
      </div>
      <Panel className="shipping-mode-panel">
        <div className="shipping-mode-main">
          <div className="shipping-mode-copy">
            <Truck size={18} aria-hidden="true" />
            <div>
              <span className="eyebrow">GHN shipping</span>
              <h2>Shipping mode <StatusBadge value={ghnSettings.data?.mode || "LOADING"} /></h2>
            </div>
          </div>
          <div className="segmented-control" role="group" aria-label="GHN shipping mode">
            {(["MOCK_TEST", "REAL"] as GHNMode[]).map((mode) => (
              <button
                type="button"
                key={mode}
                className={ghnSettings.data?.mode === mode ? "active" : ""}
                disabled={updateGhnSettings.isPending}
                onClick={() => updateGhnMode(mode)}
              >
                {mode.replace("_", " ")}
              </button>
            ))}
          </div>
        </div>
        <div className="shipping-mode-controls">
          <Field label="Mock fixed fee">
            <Input
              type="number"
              min={0}
              step={1000}
              value={mockFixedFee}
              onChange={(event) => setMockFixedFee(event.target.value)}
            />
          </Field>
          <Button variant="secondary" loading={updateGhnSettings.isPending} onClick={saveMockFee}>Save fee</Button>
          <span className="toolbar-count">REAL config: {ghnSettings.data?.realConfigured ? "Ready" : "Missing"}</span>
        </div>
        {ghnSettings.isError ? <InlineAlert tone="warning">{getApiError(ghnSettings.error)}</InlineAlert> : null}
        {ghnSettings.data?.mode === "REAL" && !ghnSettings.data.realConfigured ? (
          <InlineAlert tone="warning">GHN REAL chua du token, shop id hoac dia chi gui hang.</InlineAlert>
        ) : null}
      </Panel>
      <div className="admin-toolbar">
        <div className="admin-search">
          <Search size={17} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search order, user, payment" />
        </div>
        <Select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
          <option value="ALL">All statuses</option>
          <option value="UNPAID">UNPAID payments</option>
          <option value="PAID">PAID payments</option>
          <option value="REFUNDED">REFUNDED payments</option>
          <option value="DELIVERING">DELIVERING shipments</option>
          {orderStatuses.map((nextStatus) => <option value={nextStatus} key={nextStatus}>{nextStatus.replace(/_/g, " ")}</option>)}
        </Select>
      </div>
      {selectedHistoryOrderId ? (
        <Panel>
          <div className="section-heading compact">
            <div>
              <span className="eyebrow">Activity</span>
              <h2>Order {shortId(selectedHistoryOrderId)}</h2>
            </div>
            <Button variant="ghost" onClick={() => setSelectedHistoryOrderId(null)}>Close</Button>
          </div>
          {orderHistory.isError ? <EmptyState title="Khong lay duoc lich su don hang" detail={getApiError(orderHistory.error)} /> : null}
          {!orderHistory.isLoading && orderHistory.data?.length === 0 ? <EmptyState title="Chua co lich su trang thai" /> : null}
          <div className="activity-list">
            {orderHistory.data?.map((item) => (
              <article className="activity-item" key={item.id}>
                <span className="activity-dot" aria-hidden="true" />
                <div>
                  <div className="row-between">
                    <strong>{item.source.replace(/_/g, " ")}</strong>
                    <span className="muted">{formatDate(item.createdAt)}</span>
                  </div>
                  <p>{[item.note, item.actorEmail ? `Actor: ${item.actorEmail}` : undefined].filter(Boolean).join(" - ") || "Status updated"}</p>
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
      ) : null}
      {orders.isError ? <EmptyState title="Khong lay duoc orders" detail={getApiError(orders.error)} /> : null}
      <DataTable>
        <thead><tr><th>Order</th><th>Order status</th><th>Payment</th><th>Shipping</th><th>Total</th><th>Created</th><th>Next order status</th><th>Activity</th><th></th></tr></thead>
        <tbody>
          {filteredOrders.map((order) => {
            const nextStatuses = getNextOrderStatuses(order.status);
            const selectedStatus = statusByOrder[order.id] || nextStatuses[0] || order.status;
            const canCancel = ["PENDING", "CONFIRMED"].includes(order.status);
            const canCreateShipping = order.status === "READY_TO_SHIP" && getShippingStatus(order) === "NOT_CREATED";
            const shippingInfo = [order.shippingProvider, order.shippingOrderCode].filter(Boolean).join(" ");
            return (
              <tr key={order.id}>
                <td>
                  <strong>{shortId(order.id)}</strong>
                  <div className="table-subtext">{shippingInfo || order.paymentId || order.userId}</div>
                </td>
                <td><StatusBadge value={order.status} /></td>
                <td><StatusBadge value={getPaymentStatus(order)} /></td>
                <td><StatusBadge value={getShippingStatus(order)} /></td>
                <td>
                  <Price value={orderFinalTotal(order)} />
                  <div className="table-subtext">Ship <Price value={order.shippingPrice} /></div>
                </td>
                <td>{formatDate(order.createdAt)}</td>
                <td>
                  <Select
                    value={selectedStatus}
                    disabled={!nextStatuses.length}
                    onChange={(event) => setStatusByOrder((current) => ({ ...current, [order.id]: event.target.value }))}
                  >
                    {nextStatuses.length ? nextStatuses.map((status) => <option value={status} key={status}>{status.replace(/_/g, " ")}</option>) : <option value={order.status}>{isFinalOrderStatus(order.status) ? "Final status" : "No action"}</option>}
                  </Select>
                </td>
                <td>
                  <Button variant="ghost" onClick={() => setSelectedHistoryOrderId(order.id)}>
                    History
                  </Button>
                </td>
                <td className="table-actions">
                  {canCreateShipping ? (
                    <Button variant="secondary" loading={createShipping.isPending} onClick={() => createShipping.mutate(order.id)}>
                      {ghnSettings.data?.mode === "MOCK_TEST" ? "Tao GHN mock" : "Tao GHN"}
                    </Button>
                  ) : (
                    <Button variant="secondary" disabled={!nextStatuses.length} loading={update.isPending} onClick={() => update.mutate({ id: order.id, status: selectedStatus })}>Save</Button>
                  )}
                  <Button variant="danger" disabled={!canCancel} loading={cancel.isPending} onClick={() => cancel.mutate(order.id)}>Cancel</Button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </DataTable>
      <div className="pagination-row">
        <Button variant="secondary" disabled={orders.data?.first ?? true} onClick={() => setPage((current) => Math.max(0, current - 1))}>Previous</Button>
        <span className="toolbar-count">Page {(orders.data?.page ?? page) + 1} / {Math.max(orders.data?.totalPages ?? 1, 1)}</span>
        <Button variant="secondary" disabled={orders.data?.last ?? true} onClick={() => setPage((current) => current + 1)}>Next</Button>
      </div>
    </div>
  );
}
