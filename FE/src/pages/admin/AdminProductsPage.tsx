import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ImageIcon, Plus, RefreshCw, Search } from "lucide-react";
import { catalogApi } from "../../api/catalogApi";
import { getApiError } from "../../api/client";
import { Button, DataTable, EmptyState, Field, Input, PageHeader, Panel, Price, Select, StatusBadge, Textarea } from "../../components/ui";
import { cacheTimes } from "../../lib/queryCache";
import { useToast } from "../../state/ToastContext";

function ProductThumb({ src, name }: { src?: string; name: string }) {
  const [failed, setFailed] = useState(false);
  return (
    <div className="product-thumb">
      {src && !failed ? <img src={src} alt={name} onError={() => setFailed(true)} /> : <ImageIcon size={18} />}
    </div>
  );
}

export function AdminProductsPage() {
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [basePrice, setBasePrice] = useState(0);
  const [brandId, setBrandId] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [description, setDescription] = useState("");
  const [images, setImages] = useState<File[]>([]);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("ALL");
  const [brandFilter, setBrandFilter] = useState("ALL");
  const [categoryFilter, setCategoryFilter] = useState("ALL");
  const queryClient = useQueryClient();
  const toast = useToast();
  const products = useQuery({ queryKey: ["products"], queryFn: catalogApi.products });
  const brands = useQuery({ queryKey: ["brands"], queryFn: catalogApi.brands, ...cacheTimes.catalogReference });
  const categories = useQuery({ queryKey: ["categories"], queryFn: catalogApi.categories, ...cacheTimes.catalogReference });
  const filteredProducts = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (products.data ?? []).filter((product) => {
      const matchesQuery = !keyword || [product.name, product.slug, product.description, product.brandName, product.categoryName].some((value) => value?.toLowerCase().includes(keyword));
      const matchesStatus = status === "ALL" || product.status === status;
      const matchesBrand = brandFilter === "ALL" || product.brandId === brandFilter;
      const matchesCategory = categoryFilter === "ALL" || product.categoryId === categoryFilter;
      return matchesQuery && matchesStatus && matchesBrand && matchesCategory;
    });
  }, [brandFilter, categoryFilter, products.data, query, status]);
  const activeCount = products.data?.filter((product) => product.status === "ACTIVE").length ?? 0;
  const create = useMutation({
    mutationFn: () => {
      const product = {
        name,
        slug: slug || undefined,
        basePrice,
        brandId: brandId || undefined,
        categoryId: categoryId || undefined,
        description
      };
      return images.length ? catalogApi.createProductWithImages(product, images, 0) : catalogApi.createProduct(product);
    },
    onSuccess: async () => {
      toast.success("Da tao product");
      setName("");
      setSlug("");
      setBrandId("");
      setCategoryId("");
      setDescription("");
      setBasePrice(0);
      setImages([]);
      await queryClient.invalidateQueries({ queryKey: ["products"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate();
  };

  return (
    <div className="admin-page">
      <PageHeader
        title="Products"
        actions={
          <Button variant="secondary" onClick={() => products.refetch()} loading={products.isFetching}>
            <RefreshCw size={16} /> Refresh
          </Button>
        }
      />
      <div className="metric-grid compact">
        <Panel><span>Total</span><strong>{products.data?.length ?? 0}</strong></Panel>
        <Panel><span>Active</span><strong>{activeCount}</strong></Panel>
        <Panel><span>Filtered</span><strong>{filteredProducts.length}</strong></Panel>
      </div>
      <Panel className="admin-main-panel">
        <div className="admin-toolbar admin-toolbar-wrap">
          <div className="admin-search">
            <Search size={17} />
            <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search product, brand, category" />
          </div>
          <Select value={status} onChange={(event) => setStatus(event.target.value)}>
            <option value="ALL">All statuses</option>
            <option value="ACTIVE">Active</option>
            <option value="DRAFT">Draft</option>
            <option value="INACTIVE">Inactive</option>
          </Select>
          <Select value={brandFilter} onChange={(event) => setBrandFilter(event.target.value)}>
            <option value="ALL">All brands</option>
            {brands.data?.map((brand) => <option value={brand.id} key={brand.id}>{brand.name}</option>)}
          </Select>
          <Select value={categoryFilter} onChange={(event) => setCategoryFilter(event.target.value)}>
            <option value="ALL">All categories</option>
            {categories.data?.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
          </Select>
        </div>
        {products.isError ? <EmptyState title="Khong lay duoc products" detail={getApiError(products.error)} /> : null}
        <DataTable>
          <thead><tr><th>Image</th><th>Product</th><th>Price</th><th>Status</th><th>Brand</th><th>Category</th></tr></thead>
          <tbody>
            {filteredProducts.map((product) => (
              <tr key={product.id}>
                <td><ProductThumb src={product.primaryImageUrl || product.imageUrls?.[0]} name={product.name} /></td>
                <td><strong>{product.name}</strong><div className="table-subtext">{product.slug || product.description || "-"}</div></td>
                <td><Price value={product.basePrice} /></td>
                <td><StatusBadge value={product.status} /></td>
                <td>{product.brandName || "-"}</td>
                <td>{product.categoryName || "-"}</td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      </Panel>
      <Panel>
        <h2><Plus size={17} /> Create product</h2>
        <form className="form-grid admin-create-form" onSubmit={submit}>
          <Field label="Name"><Input value={name} onChange={(event) => setName(event.target.value)} required /></Field>
          <Field label="Slug"><Input value={slug} onChange={(event) => setSlug(event.target.value)} placeholder="nike-air-max-90" /></Field>
          <Field label="Base price"><Input type="number" value={basePrice || ""} onChange={(event) => setBasePrice(Number(event.target.value))} required /></Field>
          <Field label="Brand">
            <Select value={brandId} onChange={(event) => setBrandId(event.target.value)} disabled={brands.isLoading || brands.isError}>
              <option value="">{brands.isLoading ? "Loading brands..." : "Select brand"}</option>
              {brands.data?.map((brand) => <option value={brand.id} key={brand.id}>{brand.name}</option>)}
            </Select>
          </Field>
          <Field label="Category">
            <Select value={categoryId} onChange={(event) => setCategoryId(event.target.value)} disabled={categories.isLoading || categories.isError}>
              <option value="">{categories.isLoading ? "Loading categories..." : "Select category"}</option>
              {categories.data?.map((category) => <option value={category.id} key={category.id}>{category.name}</option>)}
            </Select>
          </Field>
          <Field label="Images"><Input type="file" multiple onChange={(event) => setImages(Array.from(event.target.files || []))} /></Field>
          <Field label="Description"><Textarea value={description} onChange={(event) => setDescription(event.target.value)} /></Field>
          {brands.isError ? <div className="inline-error">{getApiError(brands.error)}</div> : null}
          {categories.isError ? <div className="inline-error">{getApiError(categories.error)}</div> : null}
          <Button loading={create.isPending} type="submit">Create</Button>
        </form>
      </Panel>
    </div>
  );
}
