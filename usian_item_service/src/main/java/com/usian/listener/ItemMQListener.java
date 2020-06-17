package com.usian.listener;

import com.usian.service.ItemService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ItemMQListener {

    @Autowired
    private ItemService itemService;

    /**
     * 监听者接收消息三要素：
     *  1、queue
     *  2、exchange
     *  3、routing key
     */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value="item_queue",durable = "true"),
            exchange = @Exchange(value="order_exchage",type= ExchangeTypes.TOPIC),
            key= {"*.*"}
    ))
    public void listen(String orderId)throws Exception {
        System.out.println("接收到消息：" + orderId);
        Integer result = itemService.updateTbItemByOrderId(orderId);
        if(!(result>0)){
            throw new RuntimeException("扣减失败");
        }
    }
}
