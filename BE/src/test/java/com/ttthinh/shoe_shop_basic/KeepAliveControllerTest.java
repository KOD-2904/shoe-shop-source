package com.ttthinh.shoe_shop_basic;

import com.ttthinh.shoe_shop_basic.common.controller.KeepAliveController;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KeepAliveControllerTest {

    @Test
    void keepAliveIsLightweight() {
        var response = new KeepAliveController().keepAlive();

        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("UP", response.getResult().get("status"));
        assertEquals("shoe-shop-api", response.getResult().get("service"));
        assertNotNull(response.getResult().get("timestamp"));
    }
}
