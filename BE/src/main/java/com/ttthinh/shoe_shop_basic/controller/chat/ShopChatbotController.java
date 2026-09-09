package com.ttthinh.shoe_shop_basic.controller.chat;

import com.ttthinh.shoe_shop_basic.dto.request.chat.ChatbotRequest;
import com.ttthinh.shoe_shop_basic.dto.response.auth.ApiResponse;
import com.ttthinh.shoe_shop_basic.dto.response.chat.ChatbotResponse;
import com.ttthinh.shoe_shop_basic.service.ShopChatbotService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chatbot")
public class ShopChatbotController {
    private final ShopChatbotService chatbotService;

    @PostMapping("/message")
    public ApiResponse<ChatbotResponse> message(@RequestBody @Valid ChatbotRequest request) {
        return ApiResponse.<ChatbotResponse>builder()
                .code(200)
                .message("success")
                .result(chatbotService.reply(request))
                .build();
    }
}
