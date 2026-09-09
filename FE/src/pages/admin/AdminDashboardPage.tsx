import { useQuery } from "@tanstack/react-query";
import { adminApi } from "../../api/adminApi";
import { catalogApi } from "../../api/catalogApi";
import { inventoryApi } from "../../api/inventoryApi";
import { DataTable, PageHeader, Panel, Price, StatusBadge } from "../../components/ui";
import { formatDate, orderFinalTotal, shortId } from "../../lib/format";

export function AdminDashboardPage() {
  const products = useQuery({ queryKey: ["products"], queryFn: catalogApi.products });
  const orders = useQuery({ queryKey: ["admin", "orders"], queryFn: adminApi.orders });
  const inventory = useQuery({ queryKey: ["admin", "inventory"], queryFn: inventoryApi.all });
  const revenue = orders.data?.reduce((sum, order) => sum + (orderFinalTotal(order) || 0), 0) ?? 0;
  const pending = orders.data?.filter((order) => order.status === "PENDING").length ?? 0;
  const activeProducts = products.data?.filter((product) => product.status === "ACTIVE").length ?? 0;
  const lowStock = inventory.data?.filter((item) => item.lowStock).length ?? 0;
  const outOfStock = inventory.data?.filter((item) => item.outOfStock).length ?? 0;

  return (
    <div className="admin-page">
      <PageHeader title="Overview" />
      <div className="metric-grid">
        <Panel><span>Total products</span><strong>{products.data?.length ?? 0}</strong><small>{activeProducts} active</small></Panel>
        <Panel><span>Total orders</span><strong>{orders.data?.length ?? 0}</strong><small>{pending} pending</small></Panel>
        <Panel><span>Gross sales</span><Price value={revenue} /><small>All loaded orders</small></Panel>
        <Panel><span>Inventory alerts</span><strong>{lowStock}</strong><small>{outOfStock} out of stock</small></Panel>
      </div>
      <Panel>
        <h2>Recent orders</h2>
        <DataTable>
          <thead><tr><th>Order</th><th>Status</th><th>Total</th><th>Created</th></tr></thead>
          <tbody>
            {orders.data?.slice(0, 8).map((order) => (
              <tr key={order.id}>
                <td className="mono">{shortId(order.id)}</td>
                <td><StatusBadge value={order.status} /></td>
                <td><Price value={orderFinalTotal(order)} /></td>
                <td>{formatDate(order.createdAt)}</td>
              </tr>
            ))}
          </tbody>
        </DataTable>
      </Panel>
    </div>
  );
}
