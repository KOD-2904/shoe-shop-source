package com.ttthinh.shoe_shop_basic.service.impl;

import com.ttthinh.shoe_shop_basic.config.OpenAiConfig;
import com.ttthinh.shoe_shop_basic.dto.request.chat.ChatbotRequest;
import com.ttthinh.shoe_shop_basic.dto.response.catalog.ProductResponse;
import com.ttthinh.shoe_shop_basic.dto.response.chat.ChatbotResponse;
import com.ttthinh.shoe_shop_basic.service.ProductService;
import com.ttthinh.shoe_shop_basic.service.ShopChatbotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopChatbotServiceImpl implements ShopChatbotService {
    private final OpenAiConfig openAiConfig;
    private final ProductService productService;
    private final RestClient restClient;

    @Override
    public ChatbotResponse reply(ChatbotRequest request) {
        List<ProductResponse> suggestions = suggestProducts(request.getMessage());
        if (!openAiConfig.isEnabled() || !StringUtils.hasText(openAiConfig.getApiKey())) {
            return ChatbotResponse.builder()
                    .answer(fallbackAnswer(request.getMessage(), suggestions))
                    .products(suggestions)
                    .aiEnabled(false)
                    .build();
        }
        return ChatbotResponse.builder()
                .answer(callOpenAi(request.getMessage(), suggestions))
                .products(suggestions)
                .aiEnabled(true)
                .build();
    }

    private List<ProductResponse> suggestProducts(String message) {
        String query = normalize(message);
        return productService.getAllProducts().stream()
                .filter(product -> product.getStatus() == null || "ACTIVE".equalsIgnoreCase(product.getStatus()))
                .filter(product -> matches(product, query))
                .limit(4)
                .toList();
    }

    private boolean matches(ProductResponse product, String query) {
        if (!StringUtils.hasText(query)) {
            return true;
        }
        String haystack = normalize(String.join(" ",
                safe(product.getName()),
                safe(product.getSlug()),
                safe(product.getDescription()),
                safe(product.getBrandName()),
                safe(product.getCategoryName())
        ));
        return query.lines()
                .flatMap(line -> java.util.Arrays.stream(line.split("\\s+")))
                .filter(term -> term.length() > 2)
                .anyMatch(haystack::contains);
    }

    private String callOpenAi(String message, List<ProductResponse> suggestions) {
        try {
            String model = StringUtils.hasText(openAiConfig.getChatModel()) ? openAiConfig.getChatModel() : "gpt-4o-mini";
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", "You are a concise Vietnamese sneaker shop assistant. Recommend only products present in the provided catalog context. Do not invent stock, prices, payment state, or policies."),
                            Map.of("role", "user", "content", "Customer question: " + message + "\nCatalog context: " + catalogContext(suggestions))
                    ),
                    "temperature", 0.3
            );
            Map<?, ?> response = restClient.mutate()
                    .baseUrl(openAiConfig.getBaseUrl())
                    .defaultHeader("Authorization", "Bearer " + openAiConfig.getApiKey())
                    .build()
                    .post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            return extractAnswer(response);
        } catch (Exception exception) {
            log.warn("OpenAI chatbot failed, using fallback answer: {}", exception.getMessage());
            return fallbackAnswer(message, suggestions);
        }
    }

    private String catalogContext(List<ProductResponse> suggestions) {
        if (suggestions.isEmpty()) {
            return "No matching products.";
        }
        return suggestions.stream()
                .map(product -> product.getName() + " | " + safe(product.getBrandName()) + " | " + safe(product.getCategoryName()) + " | " + product.getSalePrice())
                .toList()
                .toString();
    }

    private String extractAnswer(Map<?, ?> response) {
        if (response == null) {
            return "Minh chua the tao cau tra loi luc nay.";
        }
        Object choices = response.get("choices");
        if (choices instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
            Object message = first.get("message");
            if (message instanceof Map<?, ?> messageMap) {
                Object content = messageMap.get("content");
                if (content != null && StringUtils.hasText(content.toString())) {
                    return content.toString();
                }
            }
        }
        return "Minh chua the tao cau tra loi luc nay.";
    }

    private String fallbackAnswer(String message, List<ProductResponse> suggestions) {
        if (suggestions.isEmpty()) {
            return "Minh chua tim thay san pham phu hop. Ban co the hoi theo brand, danh muc, nhu cau su dung hoac khoang gia.";
        }
        return "Minh goi y mot vai mau phu hop voi cau hoi cua ban.";
    }

    private String normalize(String value) {
        return safe(value).toLowerCase()
                .replace('đ', 'd')
                .replace('Đ', 'd');
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
