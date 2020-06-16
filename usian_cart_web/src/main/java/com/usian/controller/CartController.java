package com.usian.controller;


import com.usian.feign.CartServiceFeign;
import com.usian.feign.ItemServiceFeign;
import com.usian.pojo.TbItem;
import com.usian.utils.CookieUtils;
import com.usian.utils.JsonUtils;
import com.usian.utils.Result;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/frontend/cart")
public class CartController {

    @Value("${CART_COOKIE_KEY}")
    private String CART_COOKIE_KEY;

    @Value("${CART_COOKIE_EXPIRE}")
    private Integer CART_COOKIE_EXPIRE;

    @Autowired
    private ItemServiceFeign itemServiceFeign;

    @Autowired
    private CartServiceFeign cartServiceFeign;

    //添加购物车
    @RequestMapping("/addItem")
    public Result addItem(Long itemId, String userId, @RequestParam(defaultValue = "1") Integer num,
                          HttpServletRequest request, HttpServletResponse response) {
       try {
           if (StringUtils.isBlank(userId)){
               //未登录
               // 1、从cookie中查询商品列表。
              Map<String, TbItem> cart = getCartFormCookie(request);
               //2、添加商品到购物车
               addItemToCart(cart,itemId,num);

               //3、把购车商品列表写入cookie
                addClientCookie(cart,request,response);
           }else {
               //*已登陆*//
               // 1、从cookie中查询商品列表。
                Map<String,TbItem> cart= getCartFormRedis(userId);
               //2、添加商品到购物车
               addItemToCart(cart,itemId,num);
               //3、把购车商品列表写入redis
               Boolean addCartToRedis = addCartToRedis(cart,userId);
               if (!addCartToRedis){
                   return Result.error("添加失败");
               }

           }
           return Result.ok();
       }catch (Exception e){
           e.printStackTrace();
           return Result.error("error");
       }

    }

    //添加购物车到Redis
    private Boolean addCartToRedis(Map<String, TbItem> cart, String userId) {
        return cartServiceFeign.insertCart(cart,userId);
    }

    //从redis中查询购物车
    private Map<String,TbItem> getCartFormRedis(String userId) {
        Map<String,TbItem> cart = cartServiceFeign.selectCartByUserId(userId);
        if (cart!=null && cart.size()>0){
            return cart;
        }
        return new HashMap<String,TbItem>();
    }

    //查看购物车
    @RequestMapping("/showCart")
    public Result showCart(String userId,HttpServletRequest request,HttpServletResponse response){

        try {
            List<TbItem> tbItemList = new ArrayList<TbItem>();
            if (StringUtils.isBlank(userId)){
                //未登录
                String cartJson = CookieUtils.getCookieValue(request, CART_COOKIE_KEY, true);
                Map<String,TbItem> cart = JsonUtils.jsonToMap(cartJson, TbItem.class);
                Set<String> keySet = cart.keySet();
                for(String itemId :keySet){
                    tbItemList.add(cart.get(itemId));
                }

            }else {
                //登陆
                Map<String, TbItem> cart = getCartFormRedis(userId);
                Set<String> keySet = cart.keySet();
                for (String itemId : keySet){
                    TbItem tbItem = cart.get(itemId);
                    tbItemList.add(tbItem);
                }

            }
            return Result.ok(tbItemList);
        }catch (Exception e){
            e.printStackTrace();
        }
        return Result.error("error");
    }

    //修改购物车
    @RequestMapping("/updateItemNum")
    public Result updateItemNum(String userId,Long itemId,Integer num,
                                HttpServletRequest request,HttpServletResponse response){
        try {
            if (StringUtils.isBlank(userId)){
               //未登录
                //1、获得cookie中的购物车
                Map<String, TbItem> cart =  getCartFormCookie(request);
                //2、修改购物车中的商品
                TbItem tbItem = cart.get(itemId.toString());
                if (tbItem!=null){
                    tbItem.setNum(num);
                }
                cart.put(itemId.toString(),tbItem);
                //3、把购物车写到cookie
                addClientCookie(cart,request,response);
            }else {
                //登录
                //1、获得cookie中的购物车
                Map<String, TbItem> cart = getCartFormRedis(userId);

                //2、修改购物车中的商品
                TbItem tbItem = cart.get(itemId.toString());
                tbItem.setNum(num);
                cart.put(itemId.toString(),tbItem);

                //3、把购物车写到redis
                addCartToRedis(cart,userId);
            }
            return Result.ok();
        }catch (Exception e){
            e.printStackTrace();
            return Result.error("修改失败");
        }
    }

    //删除购物车
    @RequestMapping("/deleteItemFromCart")
    public Result deleteItemFromCart(Long itemId, String userId, HttpServletRequest
            request, HttpServletResponse response) {
        try {
            if (StringUtils.isBlank(userId)) {
                //在用户未登录的状态下
                //1.获得cookie的购物车
                Map<String,TbItem> cart = getCartFormCookie(request);
                //2.删除购物车中的商品
                cart.remove(itemId.toString());
                //3.把购物车写到cookie
                addClientCookie(cart,request,response);
            } else {
                // 在用户已登录的状态
                //1.获得redis的购物车
                Map<String, TbItem> cart = getCartFormRedis(userId);

                //2.删除购物车中的商品
                cart.remove(itemId.toString());

                //3.把购物车写到redis中
                addCartToRedis(cart,userId);

            }
            return Result.ok();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.error("删除失败");
    }


    private void addClientCookie(Map<String, TbItem> cart, HttpServletRequest request, HttpServletResponse response) {
        String cartJson = JsonUtils.objectToJson(cart);
        CookieUtils.setCookie(request,response,CART_COOKIE_KEY,cartJson,CART_COOKIE_EXPIRE,true);
    }

    private void addItemToCart(Map<String, TbItem> cart, Long itemId, Integer num) {
        TbItem tbItem = cart.get(itemId.toString());
        if (tbItem!=null){
            //购物已存在该商品：数量num
            tbItem.setNum(tbItem.getNum()+num);
        }else {
            tbItem = itemServiceFeign.selectItemInfo(itemId);
            tbItem.setNum(num);
        }
        cart.put(itemId.toString(),tbItem);
    }

    private Map<String, TbItem> getCartFormCookie(HttpServletRequest request) {
        String cartJson = CookieUtils.getCookieValue(request, CART_COOKIE_KEY, true);
        //购物车已存在
        if (StringUtils.isNotBlank(cartJson)){
            Map<String,TbItem> map =JsonUtils.jsonToMap(cartJson,TbItem.class);
            return map;
        }
        //购物车不存在
        return new HashMap<String,TbItem>();
    }


}