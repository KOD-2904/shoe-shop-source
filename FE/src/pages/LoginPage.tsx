import { FormEvent, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { Chrome, PackageSearch } from "lucide-react";
import { Button, Field, Input } from "../components/ui";
import { getApiError } from "../api/client";
import { buildGoogleAuthUrl } from "../lib/googleAuth";
import { useAuth } from "../state/AuthContext";
import { useToast } from "../state/ToastContext";

export function LoginPage() {
  const [identifier, setIdentifier] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const auth = useAuth();
  const toast = useToast();
  const returnTo = (location.state as { from?: string } | null)?.from || "/";

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      await auth.login(identifier, password);
      toast.success("Dang nhap thanh cong");
      navigate(returnTo);
    } catch (error) {
      toast.error(getApiError(error));
    } finally {
      setLoading(false);
    }
  };

  const loginWithGoogle = () => {
    try {
      window.location.assign(buildGoogleAuthUrl(returnTo));
    } catch (error) {
      toast.error(getApiError(error));
    }
  };

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <div className="auth-brand">
          <PackageSearch size={28} />
          <h1>Shoe Shop</h1>
        </div>
        <Field label="Email hoac phone">
          <Input value={identifier} onChange={(event) => setIdentifier(event.target.value)} required autoFocus />
        </Field>
        <Field label="Password">
          <Input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required />
        </Field>
        <Button loading={loading} type="submit">
          Login
        </Button>
        <div className="auth-divider"><span>or</span></div>
        <Button type="button" variant="secondary" onClick={loginWithGoogle}>
          <Chrome size={16} /> Login with Google
        </Button>
        <p className="muted center">
          Chua co tai khoan? <Link to="/register">Register</Link>
        </p>
      </form>
    </main>
  );
}
