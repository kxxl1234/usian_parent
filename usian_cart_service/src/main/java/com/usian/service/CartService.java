package com.usian.service;

import com.usian.pojo.TbItem;

import java.util.Map;

public interface CartService {
    Map<String, TbItem> selectCartByUserId(String userId);

    Boolean insertCart(Map<String, TbItem> cart, String userId);
}
