import { useEffect, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { refreshTokens, getApiError } from "../api/client";
import { EmptyState } from "../components/ui";
import { beginGoogleCallbackOnce, clearGoogleAuthSession, getGoogleReturnTo } from "../lib/googleAuth";
import { useAuth } from "../state/AuthContext";
import { useToast } from "../state/ToastContext";

export function OAuth2SuccessPage() {
  const [error, setError] = useState("");
  const auth = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const toast = useToast();

  useEffect(() => {
    const completeLogin = async () => {
      const params = new URLSearchParams(location.search);
      const oauth2Error = params.get("oauth2Error");
      if (oauth2Error) {
        setError(params.get("message") || oauth2Error);
        clearGoogleAuthSession();
        return;
      }

      if (!beginGoogleCallbackOnce()) {
        return;
      }

      await refreshTokens();
      await auth.refreshUser({ force: true });
      const returnTo = getGoogleReturnTo();
      clearGoogleAuthSession();
      toast.success("Dang nhap Google thanh cong");
      navigate(returnTo, { replace: true });
    };

    completeLogin().catch((nextError) => {
      clearGoogleAuthSession();
      setError(getApiError(nextError));
    });
  }, [auth, location.search, navigate, toast]);

  if (error) {
    return (
      <main className="auth-page">
        <div className="auth-card">
          <EmptyState title="Khong hoan tat dang nhap Google" detail={error} />
          <Link className="btn btn-secondary" to="/login">Back to login</Link>
        </div>
      </main>
    );
  }

  return <div className="loading-screen">Dang hoan tat dang nhap Google...</div>;
}
