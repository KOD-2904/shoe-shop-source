import type { OrderResponse } from "../types";

export const formatMoney = (value?: number) =>
  new Intl.NumberFormat("vi-VN", { style: "currency", currency: "VND" }).format(value ?? 0);

export const formatDate = (value?: string) => {
  if (!value) return "-";
  return new Intl.DateTimeFormat("vi-VN", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
};

export const shortId = (id?: string) => (id ? id.slice(0, 8) : "-");

export const orderFinalTotal = (
  order?: Pick<OrderResponse, "discountPrice" | "finalTotalPrice" | "items" | "shippingPrice" | "totalPrice">
) => {
  if (!order) return undefined;
  if (order.items?.length) {
    return (orderProductTotal(order) || 0) + (order.shippingPrice || 0) - (order.discountPrice || 0);
  }
  return order.finalTotalPrice ?? order.totalPrice;
};

export const orderProductTotal = (order?: Pick<OrderResponse, "items" | "totalPrice">) => {
  if (!order) return undefined;
  if (!order.items?.length) return order.totalPrice;
  return order.items.reduce((sum, item) => sum + (item.totalPrice || 0), 0);
};
