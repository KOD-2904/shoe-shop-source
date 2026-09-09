package com.ttthinh.shoe_shop_basic.service;

import com.ttthinh.shoe_shop_basic.dto.request.chat.ChatbotRequest;
import com.ttthinh.shoe_shop_basic.dto.response.chat.ChatbotResponse;

public interface ShopChatbotService {
    ChatbotResponse reply(ChatbotRequest request);
}
