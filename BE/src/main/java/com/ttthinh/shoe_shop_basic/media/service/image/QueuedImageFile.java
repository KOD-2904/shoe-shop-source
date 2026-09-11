package com.ttthinh.shoe_shop_basic.media.service.image;

public record QueuedImageFile(
        byte[] bytes,
        String originalFilename,
        String contentType
) {
}
