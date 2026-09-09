import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { authApi } from "../api/authApi";
import { getApiError } from "../api/client";
import { EmptyState } from "../components/ui";

export function VerifyEmailPage() {
  const [params] = useSearchParams();
  const [message, setMessage] = useState("Dang verify...");
  const token = params.get("token");

  useEffect(() => {
    if (!token) {
      setMessage("Thieu token verify.");
      return;
    }
    authApi
      .verifyEmail(token)
      .then(() => setMessage("Email da duoc verify."))
      .catch((error) => setMessage(getApiError(error)));
  }, [token]);

  return (
    <main className="auth-page">
      <div className="auth-card">
        <EmptyState title={message} />
        <Link to="/login" className="btn btn-primary">
          Login
        </Link>
      </div>
    </main>
  );
}
