import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowRight, Filter, Heart, Search, SlidersHorizontal } from "lucide-react";
import { useLocation, useNavigate } from "react-router-dom";
import { catalogApi } from "../api/catalogApi";
import { getApiError } from "../api/client";
import { wishlistApi } from "../api/wishlistApi";
import { ProductPurchaseCard } from "../components/ProductPurchaseCard";
import { Button, EmptyState, SectionHeader, Select, SkeletonGrid, StatusBadge } from "../components/ui";
import { useAuth } from "../state/AuthContext";
import { useToast } from "../state/ToastContext";

export function StorefrontPage() {
  const [query, setQuery] = useState("");
  const [brand, setBrand] = useState("ALL");
  const [category, setCategory] = useState("ALL");
  const [availability, setAvailability] = useState("ALL");
  const [sort, setSort] = useState("NEWEST");
  const { isAuthenticated } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const toast = useToast();
  const queryClient = useQueryClient();
  const products = useQuery({ queryKey: ["products"], queryFn: catalogApi.products });
  const variants = useQuery({ queryKey: ["variants"], queryFn: catalogApi.variants });
  const brands = useQuery({ queryKey: ["brands"], queryFn: catalogApi.brands });
  const categories = useQuery({ queryKey: ["categories"], queryFn: catalogApi.categories });
  const wishlist = useQuery({ queryKey: ["wishlist"], queryFn: wishlistApi.mine, enabled: isAuthenticated });
  const variantsByProduct = useMemo(() => {
    const grouped = new Map<string, typeof variants.data>();
    (variants.data ?? []).forEach((variant) => {
      grouped.set(variant.productId, [...(grouped.get(variant.productId) ?? []), variant]);
    });
    return grouped;
  }, [variants.data]);
  const filteredProducts = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    const withStock = (productId: string) => (variantsByProduct.get(productId) ?? []).some((variant) => variant.sizes?.some((size) => (size.quantity ?? 0) > 0));
    const result = [...(products.data ?? [])].filter((product) => {
      const matchesQuery = !keyword || [product.name, product.slug, product.description, product.brandName, product.categoryName].some((value) => value?.toLowerCase().includes(keyword));
      const matchesBrand = brand === "ALL" || product.brandId === brand;
      const matchesCategory = category === "ALL" || product.categoryId === category;
      const hasStock = withStock(product.id);
      const matchesAvailability = availability === "ALL" || (availability === "IN_STOCK" ? hasStock : !hasStock);
      return matchesQuery && matchesBrand && matchesCategory && matchesAvailability;
    });
    return result.sort((a, b) => {
      if (sort === "PRICE_ASC") return (a.basePrice ?? 0) - (b.basePrice ?? 0);
      if (sort === "PRICE_DESC") return (b.basePrice ?? 0) - (a.basePrice ?? 0);
      if (sort === "NAME") return a.name.localeCompare(b.name);
      return b.id.localeCompare(a.id);
    });
  }, [availability, brand, category, products.data, query, sort, variantsByProduct]);
  const heroProduct = products.data?.find((product) => product.primaryImageUrl || product.imageUrls?.[0]) || products.data?.[0];
  const heroImage = heroProduct?.primaryImageUrl || heroProduct?.imageUrls?.[0];
  const featuredProducts = filteredProducts.slice(0, 4);
  const wishlistIds = useMemo(() => new Set((wishlist.data ?? []).map((item) => item.productId)), [wishlist.data]);
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
  const toggleWishlist = (productId: string) => {
    if (!isAuthenticated) {
      navigate("/login", { state: { from: `${location.pathname}${location.search}` } });
      return;
    }
    if (wishlistIds.has(productId)) removeWishlist.mutate(productId);
    else addWishlist.mutate(productId);
  };
  const wishlistPending = addWishlist.isPending || removeWishlist.isPending;

  return (
    <div className="storefront-page">
      <section className="hero-campaign">
        <div className="hero-copy">
          <p className="eyebrow">New season edit</p>
          <h1>Performance sneakers with street-level polish.</h1>
          <p>Shop curated running, lifestyle, training and court-ready pairs with live stock and fast checkout.</p>
          <div className="button-row">
            <a className="btn btn-primary" href="#shop">Shop collection <ArrowRight size={16} /></a>
            <a className="btn btn-outline" href="#featured">View featured</a>
          </div>
        </div>
        <div className="hero-product">
          {heroImage ? <img src={heroImage} alt={heroProduct?.name || "Featured sneaker"} /> : <SlidersHorizontal size={56} />}
          <div>
            <StatusBadge value="FEATURED" />
            <strong>{heroProduct?.name || "Curated sneaker drops"}</strong>
          </div>
        </div>
      </section>

      <section className="merch-grid" aria-label="Shop by category">
        {["Running", "Lifestyle", "Basketball", "Training"].map((item) => (
          <a href="#shop" className="merch-tile" key={item}>
            <span>{item}</span>
            <ArrowRight size={17} />
          </a>
        ))}
      </section>

      <section id="featured">
        <SectionHeader eyebrow="Editor picks" title="Featured this week" detail="A tighter edit of products that are ready to move." />
        {products.isLoading || variants.isLoading ? <SkeletonGrid count={4} /> : null}
        <div className="product-grid featured-grid">
          {featuredProducts.map((product) => (
            <ProductPurchaseCard
              product={product}
              variants={variantsByProduct.get(product.id) ?? []}
              key={product.id}
              isWishlisted={wishlistIds.has(product.id)}
              wishlistLoading={wishlistPending}
              onWishlistToggle={() => toggleWishlist(product.id)}
              wishlistIcon={<Heart size={17} fill={wishlistIds.has(product.id) ? "currentColor" : "none"} />}
            />
          ))}
        </div>
      </section>

      <section id="shop" className="shop-section">
        <SectionHeader eyebrow="Catalog" title="Shop all sneakers" detail={`${filteredProducts.length} products available`} />
        <div className="shop-toolbar">
          <div className="search-shell catalog-search">
            <Search size={18} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search products, brands, categories" aria-label="Search catalog" />
          </div>
          <div className="filter-row">
            <Filter size={18} />
            <Select value={brand} onChange={(event) => setBrand(event.target.value)} aria-label="Filter by brand">
              <option value="ALL">All brands</option>
              {brands.data?.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
            </Select>
            <Select value={category} onChange={(event) => setCategory(event.target.value)} aria-label="Filter by category">
              <option value="ALL">All categories</option>
              {categories.data?.map((item) => <option value={item.id} key={item.id}>{item.name}</option>)}
            </Select>
            <Select value={availability} onChange={(event) => setAvailability(event.target.value)} aria-label="Filter by availability">
              <option value="ALL">All availability</option>
              <option value="IN_STOCK">In stock</option>
              <option value="OUT_OF_STOCK">Out of stock</option>
            </Select>
            <Select value={sort} onChange={(event) => setSort(event.target.value)} aria-label="Sort products">
              <option value="NEWEST">Newest</option>
              <option value="PRICE_ASC">Price low-high</option>
              <option value="PRICE_DESC">Price high-low</option>
              <option value="NAME">Name A-Z</option>
            </Select>
          </div>
        </div>
      </section>
      {products.isLoading || variants.isLoading ? <SkeletonGrid /> : null}
      {products.isError ? (
        <EmptyState title="Chua lay duoc catalog" detail={getApiError(products.error)} />
      ) : null}
      {variants.isError ? (
        <EmptyState title="Chua lay duoc variants" detail={getApiError(variants.error)} />
      ) : null}
      {products.data && products.data.length === 0 ? <EmptyState title="Chua co san pham" /> : null}
      {products.data && products.data.length > 0 && filteredProducts.length === 0 ? (
        <EmptyState
          title="No products match your filters"
          detail="Try another brand, category or availability state."
          action={<Button type="button" variant="secondary" onClick={() => { setQuery(""); setBrand("ALL"); setCategory("ALL"); setAvailability("ALL"); }}>Reset filters</Button>}
        />
      ) : null}
      <div className="product-grid">
        {filteredProducts.map((product) => (
          <ProductPurchaseCard
            product={product}
            variants={variantsByProduct.get(product.id) ?? []}
            key={product.id}
            isWishlisted={wishlistIds.has(product.id)}
            wishlistLoading={wishlistPending}
            onWishlistToggle={() => toggleWishlist(product.id)}
            wishlistIcon={<Heart size={17} fill={wishlistIds.has(product.id) ? "currentColor" : "none"} />}
          />
        ))}
      </div>

      <section className="service-band">
        <div><strong>Authentic catalog</strong><span>Managed brand, category and variant data.</span></div>
        <div><strong>Live checkout</strong><span>Cart, buy-now and shipping preview stay connected.</span></div>
        <div><strong>Order visibility</strong><span>Payment and delivery states are easy to follow.</span></div>
      </section>
    </div>
  );
}
