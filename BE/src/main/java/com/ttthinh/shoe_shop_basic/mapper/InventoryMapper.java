package com.ttthinh.shoe_shop_basic.mapper;

import com.ttthinh.shoe_shop_basic.dto.response.inventory.InventoryResponse;
import com.ttthinh.shoe_shop_basic.entity.inventory.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    @Mapping(source = "variantSize.id", target = "variantSizeId")
    public InventoryResponse inventoryToInventory(Inventory inventory);
}
