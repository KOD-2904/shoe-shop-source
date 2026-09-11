import { FormEvent, useMemo, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { KeyRound, PackageSearch } from "lucide-react";
import { authApi } from "../api/authApi";
import { getApiError } from "../api/client";
import { Button, Field, Input } from "../components/ui";
import { useToast } from "../state/ToastContext";

export function ResetPasswordPage() {
  const [params] = useSearchParams();
  const token = useMemo(() => params.get("token") || "", [params]);
  const [newPassword, setNewPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [done, setDone] = useState(false);
  const toast = useToast();

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (!token) {
      toast.error("Reset token khong hop le");
      return;
    }

    setLoading(true);
    try {
      await authApi.resetPassword({ token, newPassword });
      setDone(true);
      toast.success("Reset password thanh cong");
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
          <h1>Reset password</h1>
        </div>
        {!token ? <div className="notice danger">Reset token khong hop le.</div> : null}
        {done ? <div className="notice">Password da duoc cap nhat. Ban co the login lai.</div> : null}
        <Field label="New password">
          <Input
            type="password"
            value={newPassword}
            onChange={(event) => setNewPassword(event.target.value)}
            required
            minLength={8}
            disabled={!token || done}
            autoFocus
          />
        </Field>
        <Button loading={loading} type="submit" disabled={!token || done}>
          <KeyRound size={16} /> Reset password
        </Button>
        <p className="muted center">
          <Link to="/login">Back to login</Link>
        </p>
      </form>
    </main>
  );
}
