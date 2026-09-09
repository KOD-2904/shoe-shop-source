import { Link, NavLink } from "react-router-dom";
import { Heart, MapPin, Package, UserRound } from "lucide-react";
import { PageHeader, Panel, StatusBadge } from "../components/ui";
import { useAuth } from "../state/AuthContext";

export function AccountPage() {
  const { user } = useAuth();

  return (
    <div className="account-page">
      <PageHeader title="Tai khoan" subtitle="Manage your profile, saved addresses and order history." />
      <div className="account-grid">
        <aside className="account-sidebar">
          <NavLink to="/account"><UserRound size={17} /> Profile</NavLink>
          <NavLink to="/wishlist"><Heart size={17} /> Wishlist</NavLink>
          <NavLink to="/addresses"><MapPin size={17} /> Addresses</NavLink>
          <NavLink to="/orders"><Package size={17} /> Orders</NavLink>
        </aside>
        <Panel>
          <h2>{user?.email}</h2>
          <div className="info-grid one">
            <div><span>Phone</span><strong>{user?.phone || "-"}</strong></div>
            <div><span>Providers</span><strong>{user?.providers?.join(", ") || "-"}</strong></div>
            <div><span>Email</span><StatusBadge value={user?.emailVerified ? "VERIFIED" : "UNVERIFIED"} /></div>
            <div><span>Roles</span><strong>{user?.roles?.join(", ")}</strong></div>
          </div>
        </Panel>
        <Panel className="account-action-panel">
          <h2>Dia chi nhan hang</h2>
          <p className="muted">Quan ly ten nguoi nhan, so dien thoai va dia chi giao hang.</p>
          <Link className="btn btn-primary" to="/addresses">Quan ly dia chi</Link>
        </Panel>
      </div>
    </div>
  );
}
