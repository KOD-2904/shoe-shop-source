import { useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { AlertTriangle, RefreshCw, Search, Warehouse } from "lucide-react";
import { inventoryApi } from "../../api/inventoryApi";
import { getApiError } from "../../api/client";
import { Button, DataTable, EmptyState, Field, Input, PageHeader, Panel, Price, Select, StatusBadge } from "../../components/ui";
import { useToast } from "../../state/ToastContext";

export function AdminInventoryPage() {
  const [query, setQuery] = useState("");
  const [stockFilter, setStockFilter] = useState("ALL");
  const [threshold, setThreshold] = useState(5);
  const [quantityBySize, setQuantityBySize] = useState<Record<string, number>>({});
  const toast = useToast();
  const queryClient = useQueryClient();
  const inventory = useQuery({ queryKey: ["admin", "inventory"], queryFn: inventoryApi.all });
  const lowStock = useQuery({
    queryKey: ["admin", "inventory", "low-stock", threshold],
    queryFn: () => inventoryApi.lowStock(threshold)
  });
  const setStock = useMutation({
    mutationFn: ({ variantSizeId, quantity }: { variantSizeId: string; quantity: number }) => inventoryApi.set(variantSizeId, quantity),
    onSuccess: async () => {
      toast.success("Da cap nhat ton kho");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ["admin", "inventory"] }),
        queryClient.invalidateQueries({ queryKey: ["variants", "form-options"] })
      ]);
    },
    onError: (error) => toast.error(getApiError(error))
  });
  const filteredInventory = useMemo(() => {
    const keyword = query.trim().toLowerCase();
    return (inventory.data ?? []).filter((item) => {
      const haystack = [item.productName, item.color, item.size, item.sku, item.variantSizeId].filter(Boolean).join(" ").toLowerCase();
      const matchesQuery = !keyword || haystack.includes(keyword);
      const matchesStock =
        stockFilter === "ALL" ||
        (stockFilter === "OUT" && item.outOfStock) ||
        (stockFilter === "LOW" && item.lowStock && !item.outOfStock) ||
        (stockFilter === "AVAILABLE" && !item.lowStock);
      return matchesQuery && matchesStock;
    });
  }, [inventory.data, query, stockFilter]);
  const totalUnits = inventory.data?.reduce((sum, item) => sum + (item.quantity || 0), 0) ?? 0;
  const lockedUnits = inventory.data?.reduce((sum, item) => sum + (item.quantityLocked || 0), 0) ?? 0;
  const lowStockCount = lowStock.data?.length ?? 0;

  return (
    <div className="admin-page">
      <PageHeader
        title="Inventory"
        actions={
          <Button variant="secondary" onClick={() => { inventory.refetch(); lowStock.refetch(); }} loading={inventory.isFetching || lowStock.isFetching}>
            <RefreshCw size={16} /> Refresh
          </Button>
        }
      />
      <div className="metric-grid compact">
        <Panel><span>Total units</span><strong>{totalUnits}</strong><small>Across all sizes</small></Panel>
        <Panel><span>Locked units</span><strong>{lockedUnits}</strong><small>Reserved by active orders</small></Panel>
        <Panel><span>Low stock</span><strong>{lowStockCount}</strong><small>Threshold {threshold}</small></Panel>
      </div>
      <Panel>
        <div className="section-heading compact">
          <div>
            <span className="eyebrow">Stock watch</span>
            <h2><AlertTriangle size={17} /> Low stock sizes</h2>
          </div>
          <Field label="Threshold">
            <Input type="number" min={0} value={threshold} onChange={(event) => setThreshold(Number(event.target.value))} />
          </Field>
        </div>
        {lowStock.isError ? <EmptyState title="Khong lay duoc low-stock" detail={getApiError(lowStock.error)} /> : null}
        {!lowStock.isLoading && lowStock.data?.length === 0 ? <EmptyState title="Khong co size sap het hang" detail="Tat ca size dang cao hon nguong canh bao." /> : null}
        {lowStock.data?.length ? (
          <div className="activity-list">
            {lowStock.data.slice(0, 6).map((item) => (
              <article className="activity-item" key={item.variantSizeId}>
                <span className="activity-dot" aria-hidden="true" />
                <div>
                  <div className="row-between">
                    <strong>{item.productName}</strong>
                    <StatusBadge value={item.outOfStock ? "OUT_OF_STOCK" : "LOW_STOCK"} />
                  </div>
                  <p>{item.color || "Color"} / Size {item.size} / SKU {item.sku}</p>
                  <div className="activity-badges">
                    <StatusBadge value={`Available ${item.availableQuantity}`} />
                    <StatusBadge value={`Locked ${item.quantityLocked}`} />
                  </div>
                </div>
              </article>
            ))}
          </div>
        ) : null}
      </Panel>
      <div className="admin-toolbar">
        <div className="admin-search">
          <Search size={17} />
          <input value={query} onChange={(event) => setQuery(event.target.value)} placeholder="Search product, color, size, SKU" />
        </div>
        <Select value={stockFilter} onChange={(event) => setStockFilter(event.target.value)}>
          <option value="ALL">All stock</option>
          <option value="AVAILABLE">Healthy stock</option>
          <option value="LOW">Low stock</option>
          <option value="OUT">Out of stock</option>
        </Select>
        <span className="toolbar-count">{filteredInventory.length} rows</span>
      </div>
      {inventory.isError ? <EmptyState title="Khong lay duoc ton kho" detail={getApiError(inventory.error)} /> : null}
      <DataTable>
        <thead>
          <tr><th>Product</th><th>Variant</th><th>SKU</th><th>Price</th><th>On hand</th><th>Locked</th><th>Available</th><th>Status</th><th>Set stock</th></tr>
        </thead>
        <tbody>
          {filteredInventory.map((item) => {
            const nextQuantity = quantityBySize[item.variantSizeId] ?? item.quantity;
            return (
              <tr key={item.variantSizeId}>
                <td>
                  <strong>{item.productName}</strong>
                  <div className="table-subtext">{item.productId}</div>
                </td>
                <td>{item.color || "Color"} / Size {item.size}</td>
                <td className="mono">{item.sku}</td>
                <td><Price value={item.price} /></td>
                <td><strong>{item.quantity}</strong></td>
                <td>{item.quantityLocked}</td>
                <td><strong>{item.availableQuantity}</strong></td>
                <td><StatusBadge value={item.outOfStock ? "OUT_OF_STOCK" : item.lowStock ? "LOW_STOCK" : "IN_STOCK"} /></td>
                <td className="table-actions">
                  <Input
                    type="number"
                    min={0}
                    value={nextQuantity}
                    onChange={(event) => setQuantityBySize((current) => ({ ...current, [item.variantSizeId]: Number(event.target.value) }))}
                    aria-label={`Set stock for ${item.productName} size ${item.size}`}
                  />
                  <Button
                    variant="secondary"
                    loading={setStock.isPending}
                    onClick={() => setStock.mutate({ variantSizeId: item.variantSizeId, quantity: Number(nextQuantity) })}
                  >
                    Save
                  </Button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </DataTable>
    </div>
  );
}
