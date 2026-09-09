import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Heart } from "lucide-react";
import { catalogApi } from "../api/catalogApi";
import { getApiError } from "../api/client";
import { wishlistApi } from "../api/wishlistApi";
import { ProductPurchaseCard } from "../components/ProductPurchaseCard";
import { ProductReviews } from "../components/ProductReviews";
import { EmptyState, PageHeader } from "../components/ui";
import { cacheTimes } from "../lib/queryCache";
import { useAuth } from "../state/AuthContext";
import { useToast } from "../state/ToastContext";

export function ProductDetailPage() {
  const { id } = useParams();
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const reviewOrderItemId = new URLSearchParams(location.search).get("reviewItem") || undefined;
  const toast = useToast();
  const queryClient = useQueryClient();
  const product = useQuery({ queryKey: ["product", id], queryFn: () => catalogApi.product(id!), enabled: Boolean(id), ...cacheTimes.productDetail });
  const variants = useQuery({ queryKey: ["variants", "product", id], queryFn: () => catalogApi.variantsByProduct(id!), enabled: Boolean(id), ...cacheTimes.productDetail });
  const wishlist = useQuery({ queryKey: ["wishlist"], queryFn: wishlistApi.mine, enabled: isAuthenticated });
  const isWishlisted = Boolean(id && wishlist.data?.some((item) => item.productId === id));
  const addWishlist = useMutation({
    mutationFn: wishlistApi.add,
    onSuccess: async () => {
      toast.success("Da luu vao wishlist");
      await queryClient.invalidateQueries({ queryKey: ["wishlist"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const removeWishlist = useMutation({
    mutationFn: wishlistApi.remove,
    onSuccess: async () => {
      toast.success("Da xoa khoi wishlist");
      await queryClient.invalidateQueries({ queryKey: ["wishlist"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const toggleWishlist = () => {
    if (!id) return;
    if (!isAuthenticated) {
      navigate("/login", { state: { from: `${location.pathname}${location.search}` } });
      return;
    }
    if (isWishlisted) removeWishlist.mutate(id);
    else addWishlist.mutate(id);
  };

  if (product.isError) return <EmptyState title="Khong lay duoc san pham" detail={getApiError(product.error)} />;
  if (variants.isError) return <EmptyState title="Khong lay duoc variants" detail={getApiError(variants.error)} />;

  return (
    <div className="product-detail-page">
      <PageHeader
        eyebrow={product.data?.brandName || "Product detail"}
        title={product.data?.name || "Product detail"}
        subtitle={product.data?.categoryName || "Choose your color, size and quantity before checkout."}
      />
      {product.isLoading || variants.isLoading ? <div className="loading-screen">Loading product...</div> : null}
      {product.data ? (
        <>
          <ProductPurchaseCard
            product={product.data}
            variants={variants.data ?? []}
            mode="detail"
            isWishlisted={isWishlisted}
            wishlistLoading={addWishlist.isPending || removeWishlist.isPending}
            onWishlistToggle={toggleWishlist}
            wishlistIcon={<Heart size={16} fill={isWishlisted ? "currentColor" : "none"} />}
          />
          <ProductReviews productId={product.data.id} orderItemId={reviewOrderItemId} />
        </>
      ) : null}
    </div>
  );
}
