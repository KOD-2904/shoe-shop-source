import { api, unwrap } from "./client";
import type { ApiResponse, ProductResponse } from "../types";

export type ChatbotResponse = {
  answer: string;
  products: ProductResponse[];
  aiEnabled: boolean;
};

export const chatbotApi = {
  message: async (message: string) => {
    const { data } = await api.post<ApiResponse<ChatbotResponse> | ChatbotResponse>("/api/chatbot/message", { message });
    return unwrap<ChatbotResponse>(data);
  }
};
