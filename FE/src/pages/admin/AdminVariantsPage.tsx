import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ImagePlus, PackagePlus, Plus, RefreshCw, Trash2, Warehouse } from "lucide-react";
import { catalogApi } from "../../api/catalogApi";
import { inventoryApi } from "../../api/inventoryApi";
import { getApiError } from "../../api/client";
import { Button, DataTable, EmptyState, Field, Input, PageHeader, Panel, Select, StatusBadge } from "../../components/ui";
import { useToast } from "../../state/ToastContext";
import type { ProductVariantResponse, VariantSizeRequest } from "../../types";

const emptySizeRow = (): VariantSizeRequest => ({
  size: "",
  sku: "",
  price: 0,
  initQuantity: 0
});

export function AdminVariantsPage() {
  const [productId, setProductId] = useState("");
  const [color, setColor] = useState("");
  const [sizeRows, setSizeRows] = useState<VariantSizeRequest[]>([emptySizeRow()]);
  const [inventoryVariantSizeId, setInventoryVariantSizeId] = useState("");
  const [inventoryQuantity, setInventoryQuantity] = useState(0);
  const [checkedQuantity, setCheckedQuantity] = useState<number | null>(null);
  const [imageVariantId, setImageVariantId] = useState("");
  const [variantImages, setVariantImages] = useState<File[]>([]);
  const [primaryIndex, setPrimaryIndex] = useState(0);
  const [createdVariants, setCreatedVariants] = useState<ProductVariantResponse[]>([]);
  const toast = useToast();
  const queryClient = useQueryClient();
  const options = useQuery({ queryKey: ["variants", "form-options"], queryFn: catalogApi.variantFormOptions });
  const selectedProduct = useMemo(
    () => options.data?.products.find((product) => product.id === productId),
    [options.data?.products, productId]
  );
  const variants = useMemo(() => {
    const byId = new Map<string, ProductVariantResponse>();
    [...(options.data?.variants ?? []), ...createdVariants].forEach((variant) => byId.set(variant.id, variant));
    return Array.from(byId.values());
  }, [createdVariants, options.data?.variants]);
  const inventoryOptions = useMemo(() => {
    return variants.flatMap((variant) =>
      (variant.sizes ?? []).map((size) => ({
        id: size.id,
        productName: variant.productName || options.data?.products.find((product) => product.id === variant.productId)?.name || size.productName || "Product",
        color: variant.color || size.variantColor || "Color",
        size: size.size,
        sku: size.sku,
        quantity: size.quantity,
        price: size.price
      }))
    );
  }, [options.data?.products, variants]);
  const selectedInventoryOption = useMemo(
    () => inventoryOptions.find((option) => option.id === inventoryVariantSizeId),
    [inventoryOptions, inventoryVariantSizeId]
  );

  const create = useMutation({
    mutationFn: () =>
      catalogApi.createVariant({
        productId,
        color,
        active: true,
        sizes: sizeRows.map((row) => ({
          ...row,
          price: Number(row.price),
          initQuantity: Number(row.initQuantity || 0)
        }))
      }),
    onSuccess: async (variant) => {
      toast.success("Da tao variant");
      setCreatedVariants((current) => [variant, ...current.filter((item) => item.id !== variant.id)]);
      const firstSizeId = variant.sizes?.[0]?.id;
      if (firstSizeId) setInventoryVariantSizeId(firstSizeId);
      setImageVariantId(variant.id);
      setColor("");
      setSizeRows([emptySizeRow()]);
      await queryClient.invalidateQueries({ queryKey: ["variants", "form-options"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const uploadImages = useMutation({
    mutationFn: () => catalogApi.addVariantImages(imageVariantId, variantImages, primaryIndex),
    onSuccess: async (variant) => {
      toast.success("Da them anh variant");
      setCreatedVariants((current) => [variant, ...current.filter((item) => item.id !== variant.id)]);
      setVariantImages([]);
      setPrimaryIndex(0);
      await queryClient.invalidateQueries({ queryKey: ["variants", "form-options"] });
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const updateSizeRow = (index: number, patch: Partial<VariantSizeRequest>) => {
    setSizeRows((current) => current.map((row, rowIndex) => (rowIndex === index ? { ...row, ...patch } : row)));
  };

  const addSizeRow = () => setSizeRows((current) => [...current, emptySizeRow()]);
  const removeSizeRow = (index: number) => setSizeRows((current) => current.filter((_, rowIndex) => rowIndex !== index));

  const setInventory = useMutation({
    mutationFn: () => inventoryApi.set(inventoryVariantSizeId, inventoryQuantity),
    onSuccess: (inventory) => {
      toast.success("Da cap nhat ton kho");
      setCheckedQuantity(inventory.quantity);
    },
    onError: (error) => toast.error(getApiError(error))
  });

  const checkInventory = useMutation({
    mutationFn: () => inventoryApi.get(inventoryVariantSizeId),
    onSuccess: (inventory) => setCheckedQuantity(inventory.quantity),
    onError: (error) => toast.error(getApiError(error))
  });

  const submit = (event: FormEvent) => {
    event.preventDefault();
    create.mutate();
  };

  const submitImages = (event: FormEvent) => {
    event.preventDefault();
    uploadImages.mutate();
  };

  return (
    <div className="admin-page">
      <PageHeader
        title="Variants & inventory"
        actions={
          <Button variant="secondary" onClick={() => options.refetch()} loading={options.isFetching}>
            <RefreshCw size={16} /> Refresh options
          </Button>
        }
      />
      <Panel>
        <h2><PackagePlus size={17} /> Create variant</h2>
        {options.isError ? <EmptyState title="Khong lay duoc product options" detail={getApiError(options.error)} /> : null}
        <form className="form-grid" onSubmit={submit}>
          <div className="form-two-cols">
            <Field label="Product">
              <Select value={productId} onChange={(event) => setProductId(event.target.value)} required disabled={options.isLoading || options.isError}>
                <option value="">{options.isLoading ? "Loading products..." : "Select product"}</option>
                {options.data?.products.map((product) => (
                  <option value={product.id} key={product.id}>{product.name}{product.status ? ` - ${product.status}` : ""}</option>
                ))}
              </Select>
            </Field>
            <Field label="Color"><Input value={color} onChange={(event) => setColor(event.target.value)} required /></Field>
          </div>
          {selectedProduct ? (
            <div className="selection-summary">
              <strong>{selectedProduct.name}</strong>
              <span>{color || "Choose a color"}{selectedProduct.status ? ` - ${selectedProduct.status}` : ""}</span>
            </div>
          ) : null}
          <div className="variant-size-header">
            <h3>Sizes</h3>
            <Button type="button" variant="secondary" onClick={addSizeRow}><Plus size={16} /> Add size</Button>
          </div>
          <DataTable>
            <thead><tr><th>Size</th><th>SKU</th><th>Price</th><th>Qty</th><th></th></tr></thead>
            <tbody>
              {sizeRows.map((row, index) => (
                <tr key={index}>
                  <td><Input value={row.size} onChange={(event) => updateSizeRow(index, { size: event.target.value })} required /></td>
                  <td><Input value={row.sku} onChange={(event) => updateSizeRow(index, { sku: event.target.value })} required /></td>
                  <td><Input type="number" value={row.price || ""} onChange={(event) => updateSizeRow(index, { price: Number(event.target.value) })} required /></td>
                  <td><Input type="number" value={row.initQuantity || ""} onChange={(event) => updateSizeRow(index, { initQuantity: Number(event.target.value) })} /></td>
                  <td>
                    <Button type="button" variant="ghost" onClick={() => removeSizeRow(index)} disabled={sizeRows.length === 1}>
                      <Trash2 size={16} />
                    </Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </DataTable>
          <Button loading={create.isPending} type="submit">Create variant</Button>
        </form>
      </Panel>
      <Panel>
        <h2><Warehouse size={17} /> Inventory</h2>
        <div className="form-grid admin-create-form">
          <Field label="Variant size">
            <Select
              value={inventoryVariantSizeId}
              onChange={(event) => {
                setInventoryVariantSizeId(event.target.value);
                setCheckedQuantity(null);
              }}
              disabled={options.isLoading || options.isError || inventoryOptions.length === 0}
              required
            >
              <option value="">
                {options.isLoading ? "Loading variant sizes..." : inventoryOptions.length ? "Select product / color / size" : "Create a variant first"}
              </option>
              {inventoryOptions.map((option) => (
                <option value={option.id} key={option.id}>
                  {option.productName} / {option.color} / Size {option.size} / {option.sku}
                </option>
              ))}
            </Select>
          </Field>
          {selectedInventoryOption ? (
            <div className="selection-summary">
              <strong>{selectedInventoryOption.productName}</strong>
              <span>{selectedInventoryOption.color} / Size {selectedInventoryOption.size} / SKU {selectedInventoryOption.sku}</span>
            </div>
          ) : null}
          <Field label="Quantity"><Input type="number" value={inventoryQuantity || ""} onChange={(event) => setInventoryQuantity(Number(event.target.value))} /></Field>
          <div className="button-row">
            <Button variant="secondary" loading={checkInventory.isPending} onClick={() => checkInventory.mutate()} disabled={!inventoryVariantSizeId}>Check</Button>
            <Button loading={setInventory.isPending} onClick={() => setInventory.mutate()} disabled={!inventoryVariantSizeId}>Set stock</Button>
          </div>
          {checkedQuantity !== null ? <div className="notice">Ton kho hien tai: <StatusBadge value={String(checkedQuantity)} /></div> : null}
        </div>
      </Panel>
      <Panel>
        <h2><ImagePlus size={17} /> Variant images</h2>
        <form className="form-grid admin-create-form" onSubmit={submitImages}>
          <Field label="Variant">
            <Select value={imageVariantId} onChange={(event) => setImageVariantId(event.target.value)} disabled={variants.length === 0}>
              <option value="">{variants.length ? "Select product / color" : "Create a variant first"}</option>
              {variants.map((variant) => (
                <option value={variant.id} key={variant.id}>{variant.productName} / {variant.color}</option>
              ))}
            </Select>
          </Field>
          <Field label="Images"><Input type="file" accept="image/*" multiple onChange={(event) => setVariantImages(Array.from(event.target.files || []))} /></Field>
          <Field label="Primary image position"><Input type="number" min={0} value={primaryIndex} onChange={(event) => setPrimaryIndex(Number(event.target.value))} /></Field>
          <Button loading={uploadImages.isPending} disabled={!imageVariantId || !variantImages.length} type="submit">Upload images</Button>
        </form>
      </Panel>
    </div>
  );
}
