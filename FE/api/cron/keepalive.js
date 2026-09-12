export default async function handler(request, response) {
  response.setHeader("Cache-Control", "no-store");

  if (request.method !== "GET") {
    response.setHeader("Allow", "GET");
    return response.status(405).json({ success: false, message: "Method not allowed" });
  }

  const expectedSecret = process.env.CRON_SECRET;
  if (expectedSecret && request.headers.authorization !== `Bearer ${expectedSecret}`) {
    return response.status(401).json({ success: false, message: "Unauthorized" });
  }

  const apiBaseUrl = (process.env.API_BASE_URL || process.env.VITE_API_BASE_URL || "").replace(/\/+$/, "");
  if (!apiBaseUrl) {
    return response.status(500).json({ success: false, message: "Missing API_BASE_URL" });
  }

  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 8000);

  try {
    const backendResponse = await fetch(`${apiBaseUrl}/api/keepalive`, {
      method: "GET",
      headers: { "User-Agent": "shoe-shop-vercel-cron" },
      signal: controller.signal
    });
    const body = await backendResponse.text();
    let parsedBody = null;
    try {
      parsedBody = body ? JSON.parse(body) : null;
    } catch {
      parsedBody = body.slice(0, 300);
    }

    return response.status(backendResponse.ok ? 200 : 502).json({
      success: backendResponse.ok,
      backendStatus: backendResponse.status,
      body: parsedBody
    });
  } catch (error) {
    return response.status(502).json({
      success: false,
      message: error instanceof Error ? error.message : "Keepalive failed"
    });
  } finally {
    clearTimeout(timeout);
  }
}
