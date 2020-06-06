package com.usian.listener;

import com.usian.service.SearchItemService;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SearchMQListener {

    @Autowired
    private SearchItemService searchItemService;

   @RabbitListener(bindings = @QueueBinding(
           value = @Queue(value = "search_queue",durable = "true"),
           exchange = @Exchange(value = "item_exchange",type = ExchangeTypes.TOPIC),
           key ={"item.*"}
   ))

    public void listener(String msg) throws IOException {
       //调用searchService完成索引库同步
      int insertDocument =  searchItemService.insertDocument(msg);
      if (insertDocument>0){
          throw new RuntimeException("同步失败");
      }
   }

}
