package com.usian.service;

import com.usian.mapper.TbItemMapper;
import com.usian.mapper.TbOrderItemMapper;
import com.usian.mapper.TbOrderMapper;
import com.usian.mapper.TbOrderShippingMapper;
import com.usian.pojo.*;
import com.usian.redis.RedisClient;
import com.usian.utils.JsonUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class OrderServiceImpl implements OrderService {

    @Value("${ORDER_ID_KEY}")
    private String ORDER_ID_KEY;

    @Value("${ORDER_ITEM_ID_KEY}")
    private String ORDER_ITEM_ID_KEY;

    @Value("${ORDER_ID_BEGIN}")
    private Long ORDER_ID_BEGIN;

    @Autowired
    private RedisClient redisClient;

    @Autowired
    private TbOrderMapper tbOrderMapper;

    @Autowired
    private TbOrderItemMapper tbOrderItemMapper;

    @Autowired
    private TbOrderShippingMapper tbOrderShippingMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private TbItemMapper tbItemMapper;



    @Override
    public String insertOrder(OrderInfo orderInfo) {
        //1.解析orderinfo
        TbOrder tbOrder = orderInfo.getTbOrder();
        TbOrderShipping tbOrderShipping = orderInfo.getTbOrderShipping();
        List<TbOrderItem> tbOrderItemList = JsonUtils.jsonToList(orderInfo.getOrderItem(), TbOrderItem.class);

        //2.保存订单信息
        if (!redisClient.exists(ORDER_ID_KEY)){
            redisClient.set(ORDER_ID_KEY,ORDER_ID_BEGIN);
        }
        Long orderId = redisClient.incr(ORDER_ID_KEY, 1);
        tbOrder.setOrderId(orderId.toString());
        Date date = new Date();
        tbOrder.setCreateTime(date);
        tbOrder.setUpdateTime(date);
        //1、未付款，2、已付款，3、未发货，4、已发货，5、交易成功，6、交易关闭
        tbOrder.setStatus(1);
        tbOrderMapper.insertSelective(tbOrder);

        //3.保存明细信息
        if (!redisClient.exists(ORDER_ITEM_ID_KEY)){
            redisClient.set(ORDER_ITEM_ID_KEY,0);
        }
        for (int i = 0; i < tbOrderItemList.size(); i++){
            Long orderItemId = redisClient.incr(ORDER_ITEM_ID_KEY, 1L);
            TbOrderItem tbOrderItem = tbOrderItemList.get(i);
            tbOrderItem.setId(orderItemId.toString());
            tbOrderItem.setOrderId(orderId.toString());
            tbOrderItemMapper.insertSelective(tbOrderItem);
        }

        //4.保存物流信息
        tbOrderShipping.setOrderId(orderId.toString());
        tbOrderShipping.setCreated(date);
        tbOrderShipping.setUpdated(date);
        tbOrderShippingMapper.insertSelective(tbOrderShipping);

        //发布消息到mq，完成扣减库存
        amqpTemplate.convertAndSend("order_exchage","order.add", orderId);

        //5.返回订单id
        return orderId.toString();
    }

    @Override
    public List<TbOrder> selectOverTimeTbOrder() {

        return tbOrderMapper.selectOverTimeTbOrder();
    }

    @Override
    public void updateOverTimeTbOrder(TbOrder tbOrder) {
        tbOrder.setStatus(6);
        Date date = new Date();
        tbOrder.setCloseTime(date);
        tbOrder.setEndTime(date);
        tbOrder.setUpdateTime(date);
        tbOrderMapper.updateByPrimaryKeySelective(tbOrder);
    }

    @Override
    public void updateTbItemByOrderId(String orderId) {
        //1.通过orderId查询List<TbOrderItem>
        TbOrderItemExample tbOrderItemExample = new TbOrderItemExample();
        TbOrderItemExample.Criteria criteria = tbOrderItemExample.createCriteria();
        criteria.andOrderIdEqualTo(orderId);

        List<TbOrderItem> tbOrderItemList  = tbOrderItemMapper.selectByExample(tbOrderItemExample);
        for (int i = 0; i < tbOrderItemList.size(); i++) {
            TbOrderItem tbOrderItem =  tbOrderItemList.get(i);
            //2.修改商品库存
            TbItem tbItem = tbItemMapper.selectByPrimaryKey(Long.valueOf(tbOrderItem.getItemId()));
            tbItem.setNum(tbItem.getNum()+tbOrderItem.getNum());
            tbItem.setUpdated(new Date());
            tbItemMapper.updateByPrimaryKey(tbItem);

        }

    }
}
