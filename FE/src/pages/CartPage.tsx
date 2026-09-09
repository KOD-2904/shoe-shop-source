import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, ImageIcon, Trash2 } from "lucide-react";
import { cartApi } from "../api/cartApi";
import { getApiError } from "../api/client";
import { Button, DataTable, EmptyState, PageHeader, Price } from "../components/ui";
import { useToast } from "../state/ToastContext";
import type { CartItemResponse, CartResponse } from "../types";

export function CartPage() {
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get, refetchOnWindowFocus: false });
  const [items, setItems] = useState<CartItemResponse[]>([]);

  useEffect(() => {
    setItems(cart.data?.items ?? []);
  }, [cart.data?.items]);

  const subtotal = useMemo(() => items.reduce((sum, item) => sum + item.price * item.quantity, 0), [items]);

  const setCachedItems = (nextItems: CartItemResponse[]) => {
    queryClient.setQueryData<CartResponse>(["cart"], (current) =>
      current ? { ...current, items: nextItems, subtotal: nextItems.reduce((sum, item) => sum + item.lineTotal, 0) } : current
    );
  };

  const updateLocalQuantity = (cartItemId: string, quantity: number) => {
    const safeQuantity = Math.max(1, quantity || 1);
    setItems((current) =>
      current.map((item) =>
        item.cartItemId === cartItemId ? { ...item, quantity: safeQuantity, lineTotal: item.price * safeQuantity } : item
      )
    );
  };

  const remove = useMutation({
    mutationFn: cartApi.remove,
    onSuccess: (_removed, cartItemId) => {
      setItems((current) => {
        const nextItems = current.filter((item) => item.cartItemId !== cartItemId);
        setCachedItems(nextItems);
        return nextItems;
      });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const clear = useMutation({
    mutationFn: cartApi.clear,
    onSuccess: () => {
      setItems([]);
      setCachedItems([]);
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const checkout = useMutation({
    mutationFn: async () => {
      const originalItems = cart.data?.items ?? [];
      const originalById = new Map(originalItems.map((item) => [item.cartItemId, item.quantity]));
      const changedItems = items.filter((item) => originalById.get(item.cartItemId) !== item.quantity);
      const savedItems = await Promise.all(changedItems.map((item) => cartApi.update(item.cartItemId, item.quantity)));
      const savedById = new Map(savedItems.map((item) => [item.cartItemId, item]));
      return items.map((item) => savedById.get(item.cartItemId) ?? item);
    },
    onSuccess: (nextItems) => {
      setItems(nextItems);
      setCachedItems(nextItems);
      navigate("/checkout");
    },
    onError: (error) => toast.error(getApiError(error))
  });

  return (
    <div className="cart-page">
      <PageHeader
        title="Gio hang"
        subtitle="Review quantities and totals before moving to checkout."
        actions={
          <>
            <Button variant="secondary" onClick={() => clear.mutate()} loading={clear.isPending} disabled={!items.length}>
              Clear
            </Button>
            <Button onClick={() => checkout.mutate()} loading={checkout.isPending} disabled={!items.length}>
              Checkout <ArrowRight size={16} />
            </Button>
          </>
        }
      />
      {cart.isError ? <EmptyState title="Khong lay duoc gio hang" detail={getApiError(cart.error)} /> : null}
      {cart.data && items.length === 0 ? <EmptyState title="Gio hang trong" /> : null}
      {items.length ? (
        <div className="cart-layout">
          <div className="cart-items-panel">
            <DataTable>
              <thead>
                <tr>
                  <th className="cart-image-cell">Image</th>
                  <th>Product</th>
                  <th>Variant</th>
                  <th>Price</th>
                  <th>Qty</th>
                  <th>Total</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {items.map((item) => (
                  <tr key={item.cartItemId}>
                    <td className="cart-image-cell">
                      <div className="cart-item-thumb">
                        {item.imageUrl ? <img src={item.imageUrl} alt={item.productName} /> : <ImageIcon size={18} />}
                      </div>
                    </td>
                    <td><strong>{item.productName}</strong><div className="table-subtext">{item.category || item.brand || "Sneaker"}</div></td>
                    <td>{[item.size, item.color].filter(Boolean).join(" / ") || item.variantSizeId || item.variantId}</td>
                    <td><Price value={item.price} /></td>
                    <td>
                      <input className="qty-input" type="number" min={1} value={item.quantity} aria-label={`Quantity for ${item.productName}`} onChange={(event) => updateLocalQuantity(item.cartItemId, Number(event.target.value))} />
                    </td>
                    <td><Price value={item.lineTotal} /></td>
                    <td>
                      <Button variant="icon" aria-label={`Remove ${item.productName}`} onClick={() => remove.mutate(item.cartItemId)}>
                        <Trash2 size={16} />
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </DataTable>
          </div>
          <aside className="order-summary-card">
            <h2>Order summary</h2>
            <div className="summary">
              <div><span>Subtotal</span><Price value={subtotal} /></div>
              <div><span>Shipping</span><span className="muted">Calculated at checkout</span></div>
              <div><strong>Estimated total</strong><Price value={subtotal} /></div>
            </div>
            <Button className="full-width" onClick={() => checkout.mutate()} loading={checkout.isPending}>
              Checkout <ArrowRight size={16} />
            </Button>
          </aside>
        </div>
      ) : null}
    </div>
  );
}
