import { Link } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, HeartOff } from "lucide-react";
import { getApiError } from "../api/client";
import { catalogApi } from "../api/catalogApi";
import { wishlistApi } from "../api/wishlistApi";
import { EmptyState, PageHeader, SkeletonGrid } from "../components/ui";
import { ProductPurchaseCard } from "../components/ProductPurchaseCard";
import { useToast } from "../state/ToastContext";

export function WishlistPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const wishlist = useQuery({ queryKey: ["wishlist"], queryFn: wishlistApi.mine });
  const variants = useQuery({ queryKey: ["variants"], queryFn: catalogApi.variants });
  const remove = useMutation({
    mutationFn: wishlistApi.remove,
    onSuccess: async () => {
      toast.success("Da xoa khoi wishlist");
      await queryClient.invalidateQueries({ queryKey: ["wishlist"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const variantsByProduct = new Map<string, typeof variants.data>();
  (variants.data ?? []).forEach((variant) => {
    variantsByProduct.set(variant.productId, [...(variantsByProduct.get(variant.productId) ?? []), variant]);
  });

  return (
    <div className="wishlist-page">
      <PageHeader
        eyebrow="Saved products"
        title="Wishlist"
        subtitle="Keep pairs you are considering and move them to cart when the right size is available."
        actions={<Link to="/" className="btn btn-secondary"><ArrowLeft size={16} /> Continue shopping</Link>}
      />
      {wishlist.isLoading || variants.isLoading ? <SkeletonGrid count={4} /> : null}
      {wishlist.isError ? <EmptyState title="Khong lay duoc wishlist" detail={getApiError(wishlist.error)} /> : null}
      {wishlist.data?.length === 0 ? (
        <EmptyState
          title="Wishlist dang trong"
          detail="Save sneakers from product cards to compare later."
          action={<Link to="/" className="btn btn-primary">Shop products</Link>}
        />
      ) : null}
      {wishlist.data?.length ? (
        <div className="product-grid">
          {wishlist.data.map((item) => (
            <ProductPurchaseCard
              key={item.id}
              product={item.product}
              variants={variantsByProduct.get(item.productId) ?? []}
              isWishlisted
              wishlistLoading={remove.isPending}
              onWishlistToggle={() => remove.mutate(item.productId)}
              wishlistIcon={<HeartOff size={17} />}
            />
          ))}
        </div>
      ) : null}
    </div>
  );
}
