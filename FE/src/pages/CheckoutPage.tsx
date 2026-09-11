import { FormEvent, useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useMutation, useQuery } from "@tanstack/react-query";
import { addressApi } from "../api/addressApi";
import { cartApi } from "../api/cartApi";
import { checkoutApi } from "../api/checkoutApi";
import { orderApi } from "../api/orderApi";
import { getApiError } from "../api/client";
import { Button, EmptyState, Field, Input, PageHeader, Panel, Price, Select, Textarea } from "../components/ui";
import { clearBuyNowIntent, getBuyNowIntent } from "../lib/checkoutIntent";
import { formatDate } from "../lib/format";
import { useToast } from "../state/ToastContext";
import type { CheckoutPreviewResponse, PaymentMethod } from "../types";

export function CheckoutPage() {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const mode = params.get("mode") === "buy-now" ? "buy-now" : "cart";
  const isBuyNow = mode === "buy-now";
  const buyNowIntent = useMemo(() => (isBuyNow ? getBuyNowIntent() : null), [isBuyNow]);
  const [selected, setSelected] = useState<string[]>([]);
  const [addressId, setAddressId] = useState("");
  const [note, setNote] = useState("");
  const [voucherCode, setVoucherCode] = useState("");
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>("COD");
  const [preview, setPreview] = useState<CheckoutPreviewResponse | null>(null);
  const toast = useToast();
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get, enabled: !isBuyNow });
  const addresses = useQuery({ queryKey: ["addresses"], queryFn: addressApi.mine });
  const selectedAddress = useMemo(
    () => addresses.data?.find((address) => address.id === addressId) || addresses.data?.find((address) => address.isDefault),
    [addressId, addresses.data]
  );
  const effectiveAddressId = addressId || selectedAddress?.id || "";
  const selectedTotal = useMemo(() => {
    if (buyNowIntent) return buyNowIntent.unitPrice * buyNowIntent.quantity;
    return cart.data?.items.filter((item) => selected.includes(item.cartItemId)).reduce((sum, item) => sum + item.lineTotal, 0) ?? 0;
  }, [buyNowIntent, cart.data?.items, selected]);
  const backendProductTotal = preview?.productTotal ?? selectedTotal;
  const hasAddresses = Boolean(addresses.data?.length);
  const previewExpired = preview?.expiresAt ? new Date(preview.expiresAt).getTime() <= Date.now() : false;
  const hasCheckoutItems = isBuyNow ? Boolean(buyNowIntent) : selected.length > 0;
  const normalizedVoucherCode = voucherCode.trim();
  const addressComplete = Boolean(
    selectedAddress?.receiverName &&
      selectedAddress?.phoneNumber &&
      selectedAddress?.districtId &&
      selectedAddress?.wardCode &&
      (selectedAddress?.fullAddress || selectedAddress?.detailAddress)
  );
  const canCreateOrder = Boolean(hasCheckoutItems && preview && !previewExpired && preview.addressId === effectiveAddressId && addressComplete);

  useEffect(() => {
    if (isBuyNow && !buyNowIntent) navigate("/", { replace: true });
  }, [buyNowIntent, isBuyNow, navigate]);

  useEffect(() => {
    if (!addresses.isLoading && addresses.data?.length === 0) {
      navigate(`/addresses?return=${encodeURIComponent(`/checkout${isBuyNow ? "?mode=buy-now" : ""}`)}`, { replace: true });
    }
  }, [addresses.data?.length, addresses.isLoading, isBuyNow, navigate]);

  useEffect(() => {
    setPreview(null);
  }, [effectiveAddressId, paymentMethod, selected.join("|"), buyNowIntent?.variantSizeId, buyNowIntent?.quantity, voucherCode]);

  const createPreview = useMutation({
    mutationFn: () => {
      if (buyNowIntent) {
        return checkoutApi.buyNowPreview({
          variantSizeId: buyNowIntent.variantSizeId,
          quantity: buyNowIntent.quantity,
          addressId: effectiveAddressId,
          paymentMethod,
          voucherCode: normalizedVoucherCode || undefined
        });
      }
      return checkoutApi.preview({
        addressId: effectiveAddressId,
        cartItemIds: selected,
        totalProductPrice: selectedTotal,
        shippingFeeRequest: {
          toDistrictId: selectedAddress?.districtId || 0,
          toWardCode: selectedAddress?.wardCode || "",
          weight: 1000,
          length: 30,
          width: 20,
          height: 12,
          insuranceValue: selectedTotal
        },
        voucherCode: normalizedVoucherCode || undefined
      });
    },
    onSuccess: (nextPreview) => {
      setPreview(nextPreview);
      if (!isBuyNow) toast.success("Da tinh phi van chuyen");
    },
    onError: (error) => toast.error(getApiError(error))
  });

  useEffect(() => {
    if (isBuyNow && buyNowIntent && effectiveAddressId && addressComplete && !normalizedVoucherCode && !preview && !createPreview.isPending) {
      createPreview.mutate();
    }
  }, [addressComplete, buyNowIntent, effectiveAddressId, isBuyNow, normalizedVoucherCode, preview, createPreview.isPending]);

  const createOrder = useMutation({
    mutationFn: () => {
      if (!preview || previewExpired) throw new Error("Vui long tinh shipping truoc khi tao don");
      if (buyNowIntent) {
        return orderApi.buyNow({
          variantSizeId: buyNowIntent.variantSizeId,
          quantity: buyNowIntent.quantity,
          addressId: preview.addressId,
          shippingFeeSnapshotId: preview.shippingFeeSnapshotId,
          paymentMethod,
          voucherCode: normalizedVoucherCode || undefined,
          note
        });
      }
      return orderApi.create({
        cartItemIds: selected,
        addressId: preview.addressId,
        shippingFeeSnapshotId: preview.shippingFeeSnapshotId,
        paymentMethod,
        voucherCode: normalizedVoucherCode || undefined,
        note
      });
    },
    onSuccess: (order) => {
      clearBuyNowIntent();
      toast.success("Da tao don hang");
      if (order.paymentUrl) window.location.href = order.paymentUrl;
      else window.location.href = `/orders/${order.id}`;
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    createOrder.mutate();
  };

  return (
    <div className="checkout-page">
      <PageHeader
        eyebrow={isBuyNow ? "Express purchase" : "Secure checkout"}
        title={isBuyNow ? "Buy now checkout" : "Checkout"}
        subtitle="Confirm items, delivery details and payment method before placing your order."
      />
      {!isBuyNow && cart.data?.items.length === 0 ? <EmptyState title="Gio hang trong" /> : null}
      {addresses.isError ? <EmptyState title="Khong lay duoc dia chi" detail={getApiError(addresses.error)} /> : null}
      <form className="checkout-grid" onSubmit={submit}>
        <Panel className="checkout-items-panel">
          <h2>Items</h2>
          <div className="stack">
            {buyNowIntent ? (
              <div className="check-row">
                <span>{buyNowIntent.productName}{buyNowIntent.variantLabel ? ` - ${buyNowIntent.variantLabel}` : ""} x {buyNowIntent.quantity}</span>
                <Price value={backendProductTotal} />
              </div>
            ) : null}
            {!isBuyNow ? cart.data?.items.map((item) => (
              <label className="check-row" key={item.cartItemId}>
                <input
                  type="checkbox"
                  checked={selected.includes(item.cartItemId)}
                  onChange={(event) => setSelected((current) => (event.target.checked ? [...current, item.cartItemId] : current.filter((id) => id !== item.cartItemId)))}
                />
                <span>{item.productName}</span>
                <Price value={item.lineTotal} />
              </label>
            )) : null}
          </div>
        </Panel>
        <Panel className="checkout-summary-panel">
          <h2>Shipping & payment</h2>
          <div className="form-grid">
            <Field label="Shipping address">
              <Select value={effectiveAddressId} onChange={(event) => setAddressId(event.target.value)} required disabled={addresses.isLoading || addresses.isError}>
                <option value="">{addresses.isLoading ? "Loading addresses..." : "Select address"}</option>
                {addresses.data?.map((address) => (
                  <option value={address.id} key={address.id}>
                    {address.isDefault ? "Default - " : ""}{address.fullAddress || [address.detailAddress, address.wardName, address.districtName, address.provinceName].filter(Boolean).join(", ")}
                  </option>
                ))}
              </Select>
            </Field>
            {!hasAddresses && !addresses.isLoading ? <Link className="btn btn-secondary" to="/addresses">Them dia chi</Link> : null}
            {selectedAddress ? (
              <div className={addressComplete ? "selection-summary" : "inline-error"}>
                <strong>{selectedAddress.receiverName || "Chua co ten nguoi nhan"} - {selectedAddress.phoneNumber || "Chua co SDT"}</strong>
                <span>{selectedAddress.fullAddress || [selectedAddress.detailAddress, selectedAddress.wardName, selectedAddress.districtName, selectedAddress.provinceName].filter(Boolean).join(", ")}</span>
                {!addressComplete ? <Link to={`/addresses?return=${encodeURIComponent(`/checkout${isBuyNow ? "?mode=buy-now" : ""}`)}`}>Cap nhat day du dia chi GHN</Link> : null}
              </div>
            ) : null}
            <Field label="Payment">
              <Select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value as PaymentMethod)}>
                <option value="COD">COD</option>
                <option value="VNPAY">VNPAY</option>
              </Select>
            </Field>
            <Field label="Voucher code" hint="Discount is locked after checkout preview.">
              <Input value={voucherCode} onChange={(event) => setVoucherCode(event.target.value.toUpperCase())} placeholder="WELCOME10" />
            </Field>
            <Field label="Note">
              <Textarea value={note} onChange={(event) => setNote(event.target.value)} />
            </Field>
          </div>
          <div className="summary">
            <div><span>Products</span><Price value={backendProductTotal} /></div>
            <div><span>Shipping</span><Price value={preview?.shippingFee} /></div>
            <div><span>Discount{preview?.voucherCode ? ` (${preview.voucherCode})` : ""}</span><Price value={preview?.discountAmount} /></div>
            <div><strong>Total</strong><Price value={preview?.totalAmount ?? backendProductTotal} /></div>
          </div>
          {preview ? (
            <div className={previewExpired ? "inline-error" : "selection-summary"}>
              <strong>{previewExpired ? "Shipping preview da het han" : "Shipping preview da san sang"}</strong>
              <span>Snapshot: {preview.shippingFeeSnapshotId.slice(0, 8)} - Hieu luc den {formatDate(preview.expiresAt)}</span>
            </div>
          ) : null}
          <div className="button-row">
            <Button type="button" variant="secondary" loading={createPreview.isPending} onClick={() => createPreview.mutate()} disabled={!hasCheckoutItems || !effectiveAddressId || !addressComplete}>
              Preview total
            </Button>
            <Button type="submit" loading={createOrder.isPending} disabled={!canCreateOrder}>
              Place order
            </Button>
          </div>
        </Panel>
      </form>
    </div>
  );
}
