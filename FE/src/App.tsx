import { Navigate, Route, Routes, useLocation } from "react-router-dom";
import { AppLayout } from "./layout/AppLayout";
import { useAuth } from "./state/AuthContext";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ForgotPasswordPage } from "./pages/ForgotPasswordPage";
import { ResetPasswordPage } from "./pages/ResetPasswordPage";
import { OAuth2SuccessPage } from "./pages/OAuth2SuccessPage";
import { VerifyEmailPage } from "./pages/VerifyEmailPage";
import { StorefrontPage } from "./pages/StorefrontPage";
import { ProductDetailPage } from "./pages/ProductDetailPage";
import { CartPage } from "./pages/CartPage";
import { CheckoutPage } from "./pages/CheckoutPage";
import { AddressesPage } from "./pages/AddressesPage";
import { OrdersPage } from "./pages/OrdersPage";
import { OrderDetailPage } from "./pages/OrderDetailPage";
import { AccountPage } from "./pages/AccountPage";
import { WishlistPage } from "./pages/WishlistPage";
import { PaymentResultPage } from "./pages/PaymentResultPage";
import { AdminLayout } from "./pages/admin/AdminLayout";
import { AdminDashboardPage } from "./pages/admin/AdminDashboardPage";
import { AdminProductsPage } from "./pages/admin/AdminProductsPage";
import { AdminBrandsPage } from "./pages/admin/AdminBrandsPage";
import { AdminCategoriesPage } from "./pages/admin/AdminCategoriesPage";
import { AdminVariantsPage } from "./pages/admin/AdminVariantsPage";
import { AdminOrdersPage } from "./pages/admin/AdminOrdersPage";
import { AdminUsersPage } from "./pages/admin/AdminUsersPage";
import { AdminVouchersPage } from "./pages/admin/AdminVouchersPage";
import { AdminProductDiscountsPage } from "./pages/admin/AdminProductDiscountsPage";
import { AdminInventoryPage } from "./pages/admin/AdminInventoryPage";
import { EmptyState } from "./components/ui";

function RequireAuth({ children }: { children: JSX.Element }) {
  const { isAuthenticated, initializing } = useAuth();
  const location = useLocation();
  if (initializing) return <div className="loading-screen">Loading...</div>;
  return isAuthenticated ? children : <Navigate to="/login" replace state={{ from: `${location.pathname}${location.search}` }} />;
}

function RequireAdmin({ children }: { children: JSX.Element }) {
  const { isAdmin, initializing } = useAuth();
  if (initializing) return <div className="loading-screen">Loading...</div>;
  return isAdmin ? children : <EmptyState title="Khong co quyen admin" detail="Tai khoan hien tai khong co ROLE_ADMIN." />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/oauth2/success" element={<OAuth2SuccessPage />} />
      <Route path="/verify-email" element={<VerifyEmailPage />} />
      <Route element={<AppLayout />}>
        <Route index element={<StorefrontPage />} />
        <Route path="/products/:id" element={<ProductDetailPage />} />
        <Route path="/payment-result" element={<PaymentResultPage />} />
        <Route
          path="/cart"
          element={
            <RequireAuth>
              <CartPage />
            </RequireAuth>
          }
        />
        <Route
          path="/checkout"
          element={
            <RequireAuth>
              <CheckoutPage />
            </RequireAuth>
          }
        />
        <Route
          path="/checkout/address"
          element={
            <RequireAuth>
              <AddressesPage />
            </RequireAuth>
          }
        />
        <Route
          path="/addresses"
          element={
            <RequireAuth>
              <AddressesPage />
            </RequireAuth>
          }
        />
        <Route
          path="/wishlist"
          element={
            <RequireAuth>
              <WishlistPage />
            </RequireAuth>
          }
        />
        <Route
          path="/orders"
          element={
            <RequireAuth>
              <OrdersPage />
            </RequireAuth>
          }
        />
        <Route
          path="/orders/:id"
          element={
            <RequireAuth>
              <OrderDetailPage />
            </RequireAuth>
          }
        />
        <Route
          path="/account"
          element={
            <RequireAuth>
              <AccountPage />
            </RequireAuth>
          }
        />
        <Route
          path="/admin"
          element={
            <RequireAdmin>
              <AdminLayout />
            </RequireAdmin>
          }
        >
          <Route index element={<AdminDashboardPage />} />
          <Route path="products" element={<AdminProductsPage />} />
          <Route path="brands" element={<AdminBrandsPage />} />
          <Route path="categories" element={<AdminCategoriesPage />} />
          <Route path="variants" element={<AdminVariantsPage />} />
          <Route path="inventory" element={<AdminInventoryPage />} />
          <Route path="vouchers" element={<AdminVouchersPage />} />
          <Route path="product-discounts" element={<AdminProductDiscountsPage />} />
          <Route path="orders" element={<AdminOrdersPage />} />
          <Route path="users" element={<AdminUsersPage />} />
        </Route>
      </Route>
    </Routes>
  );
}
