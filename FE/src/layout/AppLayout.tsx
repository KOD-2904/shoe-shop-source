import { useState } from "react";
import { Link, NavLink, Outlet, useNavigate } from "react-router-dom";
import { Heart, Home, LogOut, Menu, PackageSearch, Search, ShoppingBag, Shield, UserRound, X } from "lucide-react";
import { useQuery } from "@tanstack/react-query";
import { cartApi } from "../api/cartApi";
import { useAuth } from "../state/AuthContext";
import { Button } from "../components/ui";
import { ShopChatbot } from "../components/ShopChatbot";

export function AppLayout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get, enabled: Boolean(user), refetchOnWindowFocus: false });
  const cartCount = cart.data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0;

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div className="app-shell">
      <header className="topbar">
        <Link to="/" className="brand">
          <PackageSearch size={24} />
          <span>Stride</span>
        </Link>
        <button className="mobile-menu-button" type="button" aria-label="Open navigation" onClick={() => setMenuOpen(true)}>
          <Menu size={22} />
        </button>
        <div className="search-shell">
          <Search size={18} />
          <input placeholder="Search sneakers, brands, SKU" aria-label="Search products" />
        </div>
        <nav className={`topnav ${menuOpen ? "topnav-open" : ""}`} aria-label="Main navigation">
          <button className="mobile-menu-close" type="button" aria-label="Close navigation" onClick={() => setMenuOpen(false)}>
            <X size={22} />
          </button>
          <NavLink to="/" onClick={() => setMenuOpen(false)}>
            <Home size={18} />
            <span>Shop</span>
          </NavLink>
          <NavLink to="/cart" title="Gio hang" onClick={() => setMenuOpen(false)} className="cart-link">
            <ShoppingBag size={19} />
            <span>Cart</span>
            {cartCount ? <span className="cart-count" aria-label={`${cartCount} cart items`}>{cartCount}</span> : null}
          </NavLink>
          <NavLink to="/orders" onClick={() => setMenuOpen(false)}>Orders</NavLink>
          <NavLink to="/wishlist" onClick={() => setMenuOpen(false)}>
            <Heart size={18} />
            <span>Wishlist</span>
          </NavLink>
          <NavLink to="/addresses" onClick={() => setMenuOpen(false)}>Addresses</NavLink>
          {isAdmin ? (
            <NavLink to="/admin" onClick={() => setMenuOpen(false)}>
              <Shield size={18} />
              <span>Admin</span>
            </NavLink>
          ) : null}
          {user ? (
            <>
              <NavLink to="/account" onClick={() => setMenuOpen(false)} className="account-link">
                <UserRound size={18} />
                <span>{user.email}</span>
              </NavLink>
              <Button variant="ghost" onClick={handleLogout} title="Dang xuat">
                <LogOut size={18} />
              </Button>
            </>
          ) : (
            <NavLink to="/login" onClick={() => setMenuOpen(false)}>Login</NavLink>
          )}
        </nav>
        {menuOpen ? <button className="drawer-backdrop" type="button" aria-label="Close navigation" onClick={() => setMenuOpen(false)} /> : null}
      </header>
      <main className="content">
        <Outlet />
      </main>
      <footer className="site-footer">
        <div>
          <strong>Stride</strong>
          <span>Curated sneakers, reliable checkout, clear order tracking.</span>
        </div>
        <div className="footer-links">
          <Link to="/">Shop</Link>
          <Link to="/wishlist">Wishlist</Link>
          <Link to="/orders">Orders</Link>
          <Link to="/addresses">Addresses</Link>
        </div>
      </footer>
      <ShopChatbot />
    </div>
  );
}
