package com.ttthinh.shoe_shop_basic.inventory.mapper;

import com.ttthinh.shoe_shop_basic.inventory.dto.response.InventoryResponse;
import com.ttthinh.shoe_shop_basic.inventory.entity.Inventory;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface InventoryMapper {
    @Mapping(source = "variantSize.id", target = "variantSizeId")
    public InventoryResponse inventoryToInventory(Inventory inventory);
}
