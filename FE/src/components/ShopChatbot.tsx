import { FormEvent, useMemo, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useQuery } from "@tanstack/react-query";
import { Bot, MessageCircle, Minus, Send, ShoppingBag, Sparkles, X } from "lucide-react";
import { catalogApi } from "../api/catalogApi";
import { cartApi } from "../api/cartApi";
import { chatbotApi } from "../api/chatbotApi";
import { orderApi } from "../api/orderApi";
import { Button } from "./ui";
import { useAuth } from "../state/AuthContext";
import { formatMoney, shortId } from "../lib/format";
import type { ProductResponse, ProductVariantResponse } from "../types";

type ChatProduct = ProductResponse & {
  colors: string[];
  sizes: string[];
  availableQuantity: number;
};

type ChatMessage = {
  id: number;
  role: "bot" | "user";
  text: string;
  products?: ChatProduct[];
  actions?: { label: string; value: string }[];
};

const quickActions = [
  "Goi y giay running",
  "Giay duoi 2 trieu",
  "Con size 42 khong?",
  "Kiem tra gio hang",
  "Trang thai don hang"
];

const intentTerms = {
  running: ["running", "run", "chay", "runner"],
  basketball: ["basketball", "bong ro", "court"],
  lifestyle: ["lifestyle", "di choi", "casual", "street", "hang ngay"],
  training: ["training", "gym", "tap", "train"]
};

const normalize = (value: string) =>
  value
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/đ/g, "d");

function buildCatalog(products: ProductResponse[] = [], variants: ProductVariantResponse[] = []) {
  const variantsByProduct = new Map<string, ProductVariantResponse[]>();
  variants.forEach((variant) => {
    variantsByProduct.set(variant.productId, [...(variantsByProduct.get(variant.productId) ?? []), variant]);
  });
  return products.map((product) => {
    const productVariants = variantsByProduct.get(product.id) ?? [];
    const sizes = Array.from(new Set(productVariants.flatMap((variant) => variant.sizes?.map((size) => size.size) ?? [])));
    const colors = Array.from(new Set(productVariants.map((variant) => variant.color).filter(Boolean)));
    const availableQuantity = productVariants.reduce(
      (sum, variant) => sum + (variant.sizes ?? []).reduce((sizeSum, size) => sizeSum + (size.quantity ?? 0), 0),
      0
    );
    return { ...product, sizes, colors, availableQuantity };
  });
}

function extractBudget(text: string) {
  const normalized = normalize(text);
  const millionMatch = normalized.match(/(?:duoi|toi da|max|under)\s*(\d+(?:[.,]\d+)?)\s*(?:trieu|m|millon|million)/);
  if (millionMatch) return Number(millionMatch[1].replace(",", ".")) * 1_000_000;
  const plainMatch = normalized.match(/(?:duoi|toi da|max|under)\s*([\d.]{6,})/);
  if (plainMatch) return Number(plainMatch[1].replace(/\./g, ""));
  return null;
}

function extractSize(text: string) {
  const match = normalize(text).match(/(?:size|sz)\s*(\d{2}(?:\.5)?)/);
  return match?.[1] ?? null;
}

function rankProducts(catalog: ChatProduct[], text: string) {
  const normalized = normalize(text);
  const budget = extractBudget(text);
  const size = extractSize(text);
  const matchedUseCase = Object.entries(intentTerms).find(([, terms]) => terms.some((term) => normalized.includes(term)))?.[0];

  return catalog
    .filter((product) => product.status !== "INACTIVE")
    .filter((product) => !budget || (product.basePrice ?? 0) <= budget)
    .filter((product) => !size || product.sizes.includes(size))
    .map((product) => {
      const searchable = normalize([product.name, product.brandName, product.categoryName, product.description, product.colors.join(" "), product.sizes.join(" ")].join(" "));
      let score = 0;
      if (matchedUseCase && searchable.includes(matchedUseCase)) score += 8;
      if (product.availableQuantity > 0) score += 4;
      normalized.split(/\s+/).forEach((term) => {
        if (term.length > 2 && searchable.includes(term)) score += 1;
      });
      return { product, score };
    })
    .sort((a, b) => b.score - a.score || (a.product.basePrice ?? 0) - (b.product.basePrice ?? 0))
    .map((item) => item.product)
    .slice(0, 4);
}

export function ShopChatbot() {
  const [open, setOpen] = useState(false);
  const [minimized, setMinimized] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      id: 1,
      role: "bot",
      text: "Xin chao, minh la tro ly cua Stride. Minh co the goi y giay theo nhu cau, loc theo size/gia/brand, hoac giup ban tim cart va order.",
      actions: quickActions.map((value) => ({ label: value, value }))
    }
  ]);
  const nextId = useRef(2);
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const products = useQuery({ queryKey: ["products"], queryFn: catalogApi.products, enabled: open });
  const variants = useQuery({ queryKey: ["variants"], queryFn: catalogApi.variants, enabled: open });
  const cart = useQuery({ queryKey: ["cart"], queryFn: cartApi.get, enabled: open && isAuthenticated, refetchOnWindowFocus: false });
  const orders = useQuery({ queryKey: ["orders"], queryFn: orderApi.mine, enabled: open && isAuthenticated, refetchOnWindowFocus: false });
  const catalog = useMemo(() => buildCatalog(products.data, variants.data), [products.data, variants.data]);

  const pushMessage = (message: Omit<ChatMessage, "id">) => {
    setMessages((current) => [...current, { ...message, id: nextId.current++ }]);
  };

  const answer = async (raw: string) => {
    const text = raw.trim();
    if (!text) return;
    const normalized = normalize(text);
    pushMessage({ role: "user", text });

    if (normalized.includes("gio hang") || normalized.includes("cart")) {
      if (!isAuthenticated) {
        pushMessage({ role: "bot", text: "Ban can dang nhap de minh kiem tra gio hang.", actions: [{ label: "Dang nhap", value: "__login" }] });
        return;
      }
      const itemCount = cart.data?.items.reduce((sum, item) => sum + item.quantity, 0) ?? 0;
      pushMessage({
        role: "bot",
        text: itemCount ? `Gio hang hien co ${itemCount} san pham, tam tinh ${formatMoney(cart.data?.subtotal)}.` : "Gio hang cua ban dang trong.",
        actions: [{ label: "Mo gio hang", value: "__cart" }]
      });
      return;
    }

    if (normalized.includes("don hang") || normalized.includes("order")) {
      if (!isAuthenticated) {
        pushMessage({ role: "bot", text: "Ban can dang nhap de xem don hang.", actions: [{ label: "Dang nhap", value: "__login" }] });
        return;
      }
      const latestOrder = orders.data?.[0];
      pushMessage({
        role: "bot",
        text: latestOrder ? `Don gan nhat ${shortId(latestOrder.id)} dang o trang thai ${latestOrder.status}.` : "Ban chua co don hang nao.",
        actions: [{ label: "Xem don hang", value: "__orders" }]
      });
      return;
    }

    if (normalized.includes("ship") || normalized.includes("giao hang")) {
      pushMessage({
        role: "bot",
        text: "Phi ship se duoc tinh chinh xac trong checkout theo dia chi GHN. Vao gio hang, chon dia chi, roi bam preview total de xem tong thanh toan."
      });
      return;
    }

    if (normalized.includes("doi tra") || normalized.includes("tra hang") || normalized.includes("bao hanh")) {
      pushMessage({
        role: "bot",
        text: "Shop hien theo doi trang thai don va thanh toan ro rang. Neu can doi tra, hay kiem tra don hang va lien he shop voi ma don de duoc xu ly."
      });
      return;
    }

    const suggestions = rankProducts(catalog, text);
    try {
      const serverReply = await chatbotApi.message(text);
      if (serverReply.answer) {
        pushMessage({
          role: "bot",
          text: serverReply.answer,
          products: buildCatalog(serverReply.products ?? [], variants.data ?? []),
          actions: [{ label: "Xem tat ca san pham", value: "__shop" }]
        });
        return;
      }
    } catch {
      // Keep the local assistant available when the backend AI endpoint is offline.
    }

    if (suggestions.length) {
      const size = extractSize(text);
      const budget = extractBudget(text);
      pushMessage({
        role: "bot",
        text: [
          "Minh tim duoc vai mau phu hop.",
          size ? `Co loc size ${size}.` : "",
          budget ? `Gia trong khoang duoi ${formatMoney(budget)}.` : ""
        ].filter(Boolean).join(" "),
        products: suggestions,
        actions: [{ label: "Xem tat ca san pham", value: "__shop" }]
      });
      return;
    }

    pushMessage({
      role: "bot",
      text: "Minh chua tim thay mau that phu hop. Ban co the hoi theo kieu: giay running, giay duoi 2 trieu, con size 42, Nike lifestyle, hoac trang thai don hang.",
      actions: quickActions.slice(0, 3).map((value) => ({ label: value, value }))
    });
  };

  const submit = (event: FormEvent) => {
    event.preventDefault();
    void answer(input);
    setInput("");
  };

  const handleAction = (value: string) => {
    if (value === "__login") navigate("/login");
    else if (value === "__cart") navigate("/cart");
    else if (value === "__orders") navigate("/orders");
    else if (value === "__shop") navigate("/");
    else void answer(value);
  };

  if (!open) {
    return (
      <button className="chat-launcher" type="button" onClick={() => setOpen(true)} aria-label="Open shop assistant">
        <MessageCircle size={22} />
      </button>
    );
  }

  return (
    <section className={`chatbot ${minimized ? "chatbot-minimized" : ""}`} aria-label="Shop assistant">
      <header className="chatbot-header">
        <div>
          <span><Bot size={18} /> Stride assistant</span>
          <small>{products.isFetching || variants.isFetching ? "Dang cap nhat catalog" : "Online"}</small>
        </div>
        <div className="chatbot-actions">
          <button type="button" onClick={() => setMinimized((current) => !current)} aria-label={minimized ? "Expand chat" : "Minimize chat"}>
            <Minus size={17} />
          </button>
          <button type="button" onClick={() => setOpen(false)} aria-label="Close chat">
            <X size={17} />
          </button>
        </div>
      </header>
      {!minimized ? (
        <>
          <div className="chatbot-messages">
            {messages.map((message) => (
              <article className={`chat-message chat-message-${message.role}`} key={message.id}>
                <p>{message.text}</p>
                {message.products?.length ? (
                  <div className="chat-products">
                    {message.products.map((product) => (
                      <Link to={`/products/${product.id}`} className="chat-product" key={product.id}>
                        <div>
                          <strong>{product.name}</strong>
                          <span>{[product.brandName, product.categoryName].filter(Boolean).join(" / ") || "Sneaker"}</span>
                        </div>
                        <div>
                          <b>{formatMoney(product.basePrice)}</b>
                          <small>{product.availableQuantity > 0 ? `${product.availableQuantity} in stock` : "Out of stock"}</small>
                        </div>
                      </Link>
                    ))}
                  </div>
                ) : null}
                {message.actions?.length ? (
                  <div className="chat-suggestions">
                    {message.actions.map((action) => (
                      <button type="button" key={action.label} onClick={() => handleAction(action.value)}>
                        {action.label}
                      </button>
                    ))}
                  </div>
                ) : null}
              </article>
            ))}
          </div>
          <form className="chatbot-input" onSubmit={submit}>
            <Sparkles size={17} />
            <input value={input} onChange={(event) => setInput(event.target.value)} placeholder="Hoi ve san pham, size, gia, don hang..." aria-label="Message shop assistant" />
            <Button type="submit" variant="icon" disabled={!input.trim()} aria-label="Send message">
              <Send size={16} />
            </Button>
          </form>
          <div className="chatbot-footnote">
            <ShoppingBag size={14} /> Tu van dua tren catalog hien tai cua shop.
          </div>
        </>
      ) : null}
    </section>
  );
}
