package com.ttthinh.shoe_shop_basic.dto.response.shipping;

import com.ttthinh.shoe_shop_basic.enums.GHNMode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GHNSettingsResponse {
    GHNMode mode;
    Integer mockFixedFee;
    boolean mockActive;
    boolean realConfigured;
    String apiUrl;
}
