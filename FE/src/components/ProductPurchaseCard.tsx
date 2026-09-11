import { MouseEvent, ReactNode, useMemo, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { Heart, Minus, PackageCheck, Plus, RotateCcw, ShieldCheck, ShoppingCart, Truck, Zap } from "lucide-react";
import { cartApi } from "../api/cartApi";
import { getApiError } from "../api/client";
import { saveBuyNowIntent } from "../lib/checkoutIntent";
import { formatMoney } from "../lib/format";
import { useToast } from "../state/ToastContext";
import type { ProductResponse, ProductVariantResponse, VariantSizeResponse } from "../types";
import { Button, Price, StatusBadge } from "./ui";

type Props = {
  product: ProductResponse;
  variants: ProductVariantResponse[];
  mode?: "card" | "detail";
  isWishlisted?: boolean;
  wishlistLoading?: boolean;
  wishlistIcon?: ReactNode;
  onWishlistToggle?: () => void;
};

const inStock = (size: VariantSizeResponse) => (size.quantity ?? 0) > 0;

export function ProductPurchaseCard({
  product,
  variants,
  mode = "card",
  isWishlisted,
  wishlistLoading,
  wishlistIcon,
  onWishlistToggle
}: Props) {
  const activeVariants = useMemo(() => variants.filter((variant) => variant.active !== false), [variants]);
  const [selectedVariantId, setSelectedVariantId] = useState(activeVariants[0]?.id || "");
  const selectedVariant = activeVariants.find((variant) => variant.id === selectedVariantId) || activeVariants[0];
  const availableSizes = selectedVariant?.sizes?.filter(inStock) ?? [];
  const [selectedSizeId, setSelectedSizeId] = useState("");
  const selectedSize = availableSizes.find((size) => size.id === selectedSizeId) || availableSizes[0];
  const [quantity, setQuantity] = useState(1);
  const toast = useToast();
  const queryClient = useQueryClient();
  const navigate = useNavigate();
  const imageUrl = selectedVariant?.primaryImageUrl || selectedVariant?.imageUrls?.[0] || product.primaryImageUrl || product.imageUrls?.[0];
  const maxQuantity = selectedSize?.quantity ?? 0;
  const canBuy = Boolean(selectedSize) && quantity > 0;
  const displayPrice = selectedSize?.salePrice || selectedSize?.price || product.salePrice || product.basePrice;
  const originalPrice = selectedSize?.originalPrice || product.originalPrice || product.basePrice;
  const onSale = Boolean(selectedSize?.onSale || product.onSale) && originalPrice > displayPrice;
  const isLowStock = Boolean(selectedSize && (selectedSize.quantity ?? 0) <= 3);
  const variantLabel = selectedVariant ? [product.brandName, selectedVariant.color].filter(Boolean).join(" / ") : product.categoryName || product.slug || "Sneaker";

  const addCart = useMutation({
    mutationFn: () => cartApi.add({ variantSizeId: selectedSize!.id, quantity }),
    onSuccess: async () => {
      toast.success("Da them vao gio hang");
      await queryClient.invalidateQueries({ queryKey: ["cart"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const buyNow = () => {
    if (!selectedSize) return;
    saveBuyNowIntent({
      variantSizeId: selectedSize.id,
      quantity,
      productName: product.name,
      variantLabel: [selectedVariant?.color, selectedSize.size].filter(Boolean).join(" / "),
      imageUrl,
      unitPrice: displayPrice
    });
    navigate("/checkout?mode=buy-now");
  };

  const stopCardNavigation = (event: MouseEvent) => event.preventDefault();
  const toggleWishlist = (event: MouseEvent) => {
    event.preventDefault();
    event.stopPropagation();
    onWishlistToggle?.();
  };
  const setVariant = (variantId: string) => {
    setSelectedVariantId(variantId);
    setSelectedSizeId("");
    setQuantity(1);
  };

  return (
    <article className={`product-card purchase-card ${mode === "detail" ? "purchase-card-detail" : ""}`}>
      <Link to={`/products/${product.id}`} className="product-image product-photo-link" aria-label={product.name}>
        {mode === "card" ? (
          <div className="product-card-badges">
            <StatusBadge value={product.status === "ACTIVE" ? "NEW" : product.status || "NEW"} />
            {isLowStock ? <StatusBadge value="LOW_STOCK" /> : null}
          </div>
        ) : null}
        {imageUrl ? <img src={imageUrl} alt={product.name} /> : <ShoppingCart size={42} />}
        {mode === "card" ? (
          onWishlistToggle ? (
            <button
              className={`wishlist-button ${isWishlisted ? "wishlist-button-active" : ""}`}
              type="button"
              aria-label={isWishlisted ? `Remove ${product.name} from wishlist` : `Add ${product.name} to wishlist`}
              disabled={wishlistLoading}
              onClick={toggleWishlist}
            >
              {wishlistIcon || <Heart size={17} />}
            </button>
          ) : (
            <span className="wishlist-button" aria-hidden="true"><Heart size={17} /></span>
          )
        ) : null}
      </Link>
      <div className="product-info">
        <div>
          <div className="row-between product-title-row">
            <h2>{product.name}</h2>
            {mode === "detail" ? <StatusBadge value={product.status} /> : null}
          </div>
          <p className="product-variant-line">{variantLabel}</p>
          <div className="row-between">
            <Price value={displayPrice} />
            {onSale ? <span className="old-price">{formatMoney(originalPrice)}</span> : null}
            {onSale && (selectedSize?.discountPercent || product.discountPercent) ? <StatusBadge value={`SALE_${selectedSize?.discountPercent || product.discountPercent}%`} /> : null}
            {selectedSize ? <span className={`stock-text ${isLowStock ? "stock-low" : ""}`}>{isLowStock ? "Low stock" : `${selectedSize.quantity} in stock`}</span> : <span className="stock-text stock-out">Out of stock</span>}
          </div>
          {mode === "detail" && product.description ? <p className="product-description">{product.description}</p> : null}
        </div>

        <div className="purchase-controls" onClick={stopCardNavigation}>
          {activeVariants.length ? (
            <div className="option-block">
              {mode === "detail" ? <span className="option-label">Color</span> : null}
              <div className="choice-group" aria-label="Variants">
                {activeVariants.map((variant) => (
                  <button
                    type="button"
                    className={`choice-pill ${selectedVariant?.id === variant.id ? "active" : ""}`}
                    key={variant.id}
                    onClick={() => setVariant(variant.id)}
                  >
                    {variant.color}
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          {selectedVariant ? (
            <div className="option-block">
              {mode === "detail" ? <span className="option-label">Size</span> : null}
              <div className="choice-group" aria-label="Sizes">
                {(selectedVariant.sizes ?? []).map((size) => (
                  <button
                    type="button"
                    className={`choice-pill ${selectedSize?.id === size.id ? "active" : ""}`}
                    key={size.id}
                    disabled={!inStock(size)}
                    onClick={() => {
                      setSelectedSizeId(size.id);
                      setQuantity(1);
                    }}
                  >
                    {size.size}
                  </button>
                ))}
              </div>
            </div>
          ) : null}

          <div className="buy-row">
            <div className="stepper">
              <button type="button" onClick={() => setQuantity((value) => Math.max(1, value - 1))} disabled={!canBuy || quantity <= 1} aria-label="Decrease quantity">
                <Minus size={14} />
              </button>
              <span>{quantity}</span>
              <button type="button" onClick={() => setQuantity((value) => Math.min(maxQuantity, value + 1))} disabled={!canBuy || quantity >= maxQuantity} aria-label="Increase quantity">
                <Plus size={14} />
              </button>
            </div>
            <Button type="button" variant="secondary" loading={addCart.isPending} disabled={!canBuy} onClick={() => addCart.mutate()}>
              <ShoppingCart size={16} /> Add
            </Button>
            <Button type="button" disabled={!canBuy} onClick={buyNow}>
              <Zap size={16} /> Buy now
            </Button>
          </div>
          {mode === "detail" ? (
            <>
            {onWishlistToggle ? (
              <Button type="button" variant="secondary" loading={wishlistLoading} onClick={onWishlistToggle}>
                {wishlistIcon || <Heart size={16} />} {isWishlisted ? "Saved" : "Save to wishlist"}
              </Button>
            ) : null}
            <div className="commerce-promises">
              <div><Truck size={17} /><span>Fast GHN shipping preview at checkout</span></div>
              <div><RotateCcw size={17} /><span>Return support through order status</span></div>
              <div><ShieldCheck size={17} /><span>Secure COD and VNPAY payment</span></div>
              <div><PackageCheck size={17} /><span>Live inventory protected before purchase</span></div>
            </div>
            </>
          ) : null}
        </div>
      </div>
    </article>
  );
}
