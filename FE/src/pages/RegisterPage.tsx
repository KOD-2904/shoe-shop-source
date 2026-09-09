import { FormEvent, useState } from "react";
import { Link } from "react-router-dom";
import { Chrome } from "lucide-react";
import { Button, Field, Input } from "../components/ui";
import { authApi } from "../api/authApi";
import { getApiError } from "../api/client";
import { buildGoogleAuthUrl } from "../lib/googleAuth";
import { useToast } from "../state/ToastContext";

export function RegisterPage() {
  const [email, setEmail] = useState("");
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const toast = useToast();

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    setLoading(true);
    try {
      await authApi.register({ email, phone, password });
      setDone(true);
      toast.success("Dang ky thanh cong");
    } catch (error) {
      toast.error(getApiError(error));
    } finally {
      setLoading(false);
    }
  };

  const registerWithGoogle = () => {
    try {
      window.location.assign(buildGoogleAuthUrl("/"));
    } catch (error) {
      toast.error(getApiError(error));
    }
  };

  return (
    <main className="auth-page">
      <form className="auth-card" onSubmit={submit}>
        <h1>Register</h1>
        {done ? <div className="notice">Kiem tra email de verify tai khoan.</div> : null}
        <Field label="Email">
          <Input type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
        </Field>
        <Field label="Phone">
          <Input value={phone} onChange={(event) => setPhone(event.target.value)} required />
        </Field>
        <Field label="Password">
          <Input type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={6} />
        </Field>
        <Button loading={loading} type="submit">
          Create account
        </Button>
        <div className="auth-divider"><span>or</span></div>
        <Button type="button" variant="secondary" onClick={registerWithGoogle}>
          <Chrome size={16} /> Continue with Google
        </Button>
        <p className="muted center">
          Da co tai khoan? <Link to="/login">Login</Link>
        </p>
      </form>
    </main>
  );
}
