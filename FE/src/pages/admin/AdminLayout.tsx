import { NavLink, Outlet } from "react-router-dom";
import { BadgePercent, Boxes, Gauge, Layers3, PackageCheck, PackagePlus, ShoppingBag, Tags, TicketPercent, Users } from "lucide-react";

const links = [
  { to: "/admin", label: "Dashboard", icon: Gauge, end: true },
  { to: "/admin/products", label: "Products", icon: ShoppingBag },
  { to: "/admin/brands", label: "Brands", icon: Tags },
  { to: "/admin/categories", label: "Categories", icon: Layers3 },
  { to: "/admin/variants", label: "Variants", icon: PackagePlus },
  { to: "/admin/inventory", label: "Inventory", icon: PackageCheck },
  { to: "/admin/vouchers", label: "Vouchers", icon: TicketPercent },
  { to: "/admin/product-discounts", label: "Discounts", icon: BadgePercent },
  { to: "/admin/orders", label: "Orders", icon: Boxes },
  { to: "/admin/users", label: "Users", icon: Users }
];

export function AdminLayout() {
  return (
    <div className="admin-shell">
      <aside className="admin-sidebar">
        <div className="admin-sidebar-title">Operations</div>
        {links.map((link) => {
          const Icon = link.icon;
          return (
            <NavLink to={link.to} end={link.end} key={link.to}>
              <Icon size={18} />
              <span>{link.label}</span>
            </NavLink>
          );
        })}
      </aside>
      <section className="admin-content">
        <Outlet />
      </section>
    </div>
  );
}
