import { FormEvent, useState } from "react";
import { Link, NavLink } from "react-router-dom";
import { Heart, KeyRound, MapPin, Package, UserRound } from "lucide-react";
import { authApi } from "../api/authApi";
import { getApiError } from "../api/client";
import { Button, Field, Input, PageHeader, Panel, StatusBadge } from "../components/ui";
import { useAuth } from "../state/AuthContext";
import { useToast } from "../state/ToastContext";

export function AccountPage() {
  const { user } = useAuth();
  const toast = useToast();
  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const canChangePassword = user?.providers?.includes("local");

  const submitPasswordChange = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      await authApi.changePassword({ currentPassword, newPassword });
      setCurrentPassword("");
      setNewPassword("");
      toast.success("Password da duoc cap nhat");
    } catch (error) {
      toast.error(getApiError(error));
    } finally {
      setLoading(false);
    }
  };

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
        <Panel className="account-action-panel">
          <h2>Password</h2>
          {canChangePassword ? (
            <form className="stack-form" onSubmit={submitPasswordChange}>
              <Field label="Current password">
                <Input
                  type="password"
                  value={currentPassword}
                  onChange={(event) => setCurrentPassword(event.target.value)}
                  required
                />
              </Field>
              <Field label="New password">
                <Input
                  type="password"
                  value={newPassword}
                  onChange={(event) => setNewPassword(event.target.value)}
                  required
                  minLength={8}
                />
              </Field>
              <Button loading={loading} type="submit">
                <KeyRound size={16} /> Change password
              </Button>
            </form>
          ) : (
            <p className="muted">Tai khoan nay dang dung Google login nen khong co password local de doi.</p>
          )}
        </Panel>
      </div>
    </div>
  );
}
