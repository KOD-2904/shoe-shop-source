package com.ttthinh.shoe_shop_basic.utils;

public final class PageUtils {
    private static final int MAX_PAGE_SIZE = 100;

    private PageUtils() {
    }

    public static int normalizePage(int page) {
        return Math.max(page, 0);
    }

    public static int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
