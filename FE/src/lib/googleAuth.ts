const GOOGLE_RETURN_TO_KEY = "shoe_shop_google_return_to";
const GOOGLE_CALLBACK_STARTED_KEY = "shoe_shop_google_callback_started";

export function getGoogleReturnTo() {
  return sessionStorage.getItem(GOOGLE_RETURN_TO_KEY) || "/";
}

export function clearGoogleAuthSession() {
  sessionStorage.removeItem(GOOGLE_RETURN_TO_KEY);
  sessionStorage.removeItem(GOOGLE_CALLBACK_STARTED_KEY);
}

export function beginGoogleCallbackOnce() {
  if (sessionStorage.getItem(GOOGLE_CALLBACK_STARTED_KEY)) {
    return false;
  }
  sessionStorage.setItem(GOOGLE_CALLBACK_STARTED_KEY, "true");
  return true;
}

export function buildGoogleAuthUrl(returnTo = "/") {
  sessionStorage.setItem(GOOGLE_RETURN_TO_KEY, returnTo);
  sessionStorage.removeItem(GOOGLE_CALLBACK_STARTED_KEY);
  const apiBaseUrl = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
  return `${apiBaseUrl}/oauth2/authorization/google`;
}
