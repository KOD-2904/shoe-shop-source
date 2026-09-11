import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { Mail, PackageSearch } from "lucide-react";
import { authApi } from "../api/authApi";
import { getApiError } from "../api/client";
import { Button, Field, Input } from "../components/ui";
import { useToast } from "../state/ToastContext";

export function ForgotPasswordPage() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const toast = useToast();

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      await authApi.forgotPassword({ email });
      setDone(true);
      toast.success("Neu email hop le, link reset da duoc gui");
    } catch (error) {
      toast.error(getApiError(error));
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <div className="auth-brand">
          <PackageSearch size={28} />
          <h1>Forgot password</h1>
        </div>
        {done ? <div className="notice">Kiem tra email de lay link reset password.</div> : null}
        <Field label="Email">
          <Input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required autoFocus />
        </Field>
        <Button loading={loading} type="submit">
          <Mail size={16} /> Send reset link
        </Button>
        <p className="muted center">
          Nho mat khau? <Link to="/login">Login</Link>
        </p>
      </form>
    </main>
  );
}
