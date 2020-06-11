package com.usian.service;

import com.usian.pojo.TbItem;
import com.usian.redis.RedisClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
@Service
@Transactional
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisClient redisClient;

    @Value("${CART_REDIS_KEY}")
    private String CART_REDIS_KEY;

    @Override
    public Map<String, TbItem> selectCartByUserId(String userId) {
        return (Map<String, TbItem>) redisClient.hget(CART_REDIS_KEY, userId);
    }

    @Override
    public Boolean insertCart(Map<String, TbItem> cart, String userId) {

        return redisClient.hset(CART_REDIS_KEY,userId,cart);
    }
}
