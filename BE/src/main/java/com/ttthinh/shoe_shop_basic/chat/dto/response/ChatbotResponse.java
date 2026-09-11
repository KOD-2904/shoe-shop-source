package com.ttthinh.shoe_shop_basic.chat.dto.response;

import com.ttthinh.shoe_shop_basic.catalog.dto.response.ProductResponse;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatbotResponse {
    String answer;
    List<ProductResponse> products;
    Boolean aiEnabled;
}
