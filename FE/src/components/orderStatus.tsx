import { Clock3, PackageCheck, PackageOpen, RefreshCw, Truck, XCircle } from "lucide-react";
import type { OrderResponse, OrderStatus, PaymentStatus, ShippingStatus } from "../types";
import { StatusBadge } from "./ui";

export const orderStatuses: OrderStatus[] = [
  "PENDING",
  "CONFIRMED",
  "PACKING",
  "READY_TO_SHIP",
  "SHIPPING",
  "DELIVERED",
  "CANCELLED",
  "FAILED",
  "RETURNED"
];

const mainOrderFlow: OrderStatus[] = ["PENDING", "CONFIRMED", "PACKING", "READY_TO_SHIP", "SHIPPING"];
const paymentFlow: PaymentStatus[] = ["UNPAID", "PAID"];
const shippingFlow: ShippingStatus[] = ["NOT_CREATED", "CREATED", "PICKING", "PICKED", "DELIVERING", "DELIVERED"];

const nextOrderStatus: Record<string, OrderStatus[]> = {
  PENDING: ["CONFIRMED", "CANCELLED"],
  CONFIRMED: ["PACKING", "CANCELLED"],
  PACKING: ["READY_TO_SHIP"]
};

const finalOrderStatuses = new Set(["DELIVERED", "CANCELLED", "FAILED", "RETURNED"]);
const negativeStatuses = new Set(["CANCELLED", "FAILED", "RETURNED", "REFUNDED", "DELIVERY_FAILED"]);

export function labelStatus(value?: string) {
  return value ? value.replace(/_/g, " ") : "-";
}

export function getNextOrderStatuses(status?: string) {
  return status ? nextOrderStatus[status] ?? [] : [];
}

export function isFinalOrderStatus(status?: string) {
  return Boolean(status && finalOrderStatuses.has(status));
}

export function getPaymentStatus(order?: Pick<OrderResponse, "paymentStatus">): PaymentStatus | undefined {
  return order?.paymentStatus;
}

export function getShippingStatus(order?: Pick<OrderResponse, "shippingStatus" | "status">): ShippingStatus {
  if (order?.shippingStatus) return order.shippingStatus;
  if (order?.status === "SHIPPING") return "DELIVERING";
  if (order?.status === "DELIVERED") return "DELIVERED";
  if (order?.status === "FAILED") return "DELIVERY_FAILED";
  if (order?.status === "RETURNED") return "RETURNED";
  if (order?.status === "CANCELLED") return "CANCELLED";
  return "NOT_CREATED";
}

export function getOrderStatusTone(status?: string) {
  if (!status) return "idle";
  if (negativeStatuses.has(status)) return "bad";
  if (status === "DELIVERED" || status === "PAID") return "done";
  if (status === "SHIPPING" || status === "DELIVERING") return "moving";
  return "active";
}

export function StatusSummary({ order }: { order?: OrderResponse }) {
  const paymentStatus = getPaymentStatus(order);
  const shippingStatus = getShippingStatus(order);

  return (
    <div className="status-summary">
      <StatusCard title="Order" value={order?.status} detail={order?.status === "READY_TO_SHIP" ? "Ready for GHN handoff" : "Shop workflow"} />
      <StatusCard title="Payment" value={paymentStatus} detail={order?.paymentMethod ? `Method: ${order.paymentMethod}` : "Payment tracking"} />
      <StatusCard title="Shipping" value={shippingStatus} detail={order?.shippingOrderCode ? `GHN: ${order.shippingOrderCode}` : "Delivery tracking"} />
    </div>
  );
}

function StatusCard({ title, value, detail }: { title: string; value?: string; detail: string }) {
  const Icon = getIcon(value);
  return (
    <div className={`status-card status-card-${getOrderStatusTone(value)}`}>
      <div className="status-card-icon"><Icon size={18} /></div>
      <div>
        <span>{title}</span>
        <strong>{labelStatus(value)}</strong>
        <small>{detail}</small>
      </div>
    </div>
  );
}

export function OrderTimeline({ order }: { order?: OrderResponse }) {
  const orderProgressStatus = order?.status === "DELIVERED" ? "SHIPPING" : order?.status;
  return (
    <div className="order-timeline-block">
      <h2>Order progress</h2>
      <Timeline steps={mainOrderFlow} current={orderProgressStatus} />
      <div className="split-flow">
        <div>
          <h2>Payment</h2>
          <Timeline steps={paymentFlow} current={getPaymentStatus(order)} compact />
        </div>
        <div>
          <h2>Shipping</h2>
          <Timeline steps={shippingFlow} current={getShippingStatus(order)} compact />
        </div>
      </div>
    </div>
  );
}

function Timeline({ steps, current, compact = false }: { steps: string[]; current?: string; compact?: boolean }) {
  const currentIndex = steps.indexOf(current || "");
  const isException = Boolean(current && currentIndex === -1);

  return (
    <div className={`status-flow ${compact ? "status-flow-compact" : ""}`}>
      {steps.map((step, index) => {
        const state = currentIndex === index ? "current" : "muted";
        return (
          <div className={`status-step status-step-${state}`} key={step}>
            <strong>{labelStatus(step)}</strong>
          </div>
        );
      })}
      {isException ? (
        <div className={`status-step status-step-exception status-step-${getOrderStatusTone(current)}`}>
          <strong><StatusBadge value={current} /></strong>
        </div>
      ) : null}
    </div>
  );
}

function getIcon(value?: string) {
  switch (value) {
    case "PENDING":
    case "UNPAID":
      return Clock3;
    case "CONFIRMED":
    case "PAID":
    case "DELIVERED":
      return PackageCheck;
    case "PACKING":
      return PackageOpen;
    case "READY_TO_SHIP":
    case "CREATED":
    case "PICKING":
    case "PICKED":
      return PackageCheck;
    case "SHIPPING":
    case "DELIVERING":
      return Truck;
    case "FAILED":
    case "CANCELLED":
    case "RETURNED":
    case "REFUNDED":
    case "DELIVERY_FAILED":
      return XCircle;
    default:
      return RefreshCw;
  }
}
