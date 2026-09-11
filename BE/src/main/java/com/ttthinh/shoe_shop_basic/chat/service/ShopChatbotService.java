package com.ttthinh.shoe_shop_basic.chat.service;

import com.ttthinh.shoe_shop_basic.chat.dto.request.ChatbotRequest;
import com.ttthinh.shoe_shop_basic.chat.dto.response.ChatbotResponse;

public interface ShopChatbotService {
    ChatbotResponse reply(ChatbotRequest request);
}
