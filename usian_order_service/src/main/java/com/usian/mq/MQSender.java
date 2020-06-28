package com.usian.mq;

import com.usian.mapper.LocalMessageMapper;
import com.usian.pojo.LocalMessage;
import com.usian.utils.JsonUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ConfirmCallback;
import org.springframework.amqp.rabbit.core.RabbitTemplate.ReturnCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/*
* 任务：
*   1.发送消息
*   2.消息确认成功返回后 修改local_message(status:1)
* */
@Component
public class MQSender implements ReturnCallback , ConfirmCallback{


    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private LocalMessageMapper localMessageMapper;

    /*
    * 消息发送失败时调用
    * */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText,
                                String exchange, String routingKey) {

        System.out.println("return message:"+message.getBody().toString()
                + ",exchange:" + exchange + ",routingKey:" + routingKey);
    }

    /*
    * 下游服务：消息确认成功返回后调用
    * */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {

        String id = correlationData.getId();
        if (ack){
            //修改本地消息表的状态
            LocalMessage localMessage = new LocalMessage();
            localMessage.setTxNo(id);
            localMessage.setState(1);
            localMessageMapper.updateByPrimaryKeySelective(localMessage);
        }
    }


    /*
    * 发送消息
    * */
    public void sendMsg(LocalMessage localMessage) {

        RabbitTemplate rabbitTemplate = (RabbitTemplate)this.amqpTemplate;
        rabbitTemplate.setMandatory(true);//开启消息发送失败回退
        rabbitTemplate.setReturnCallback(this);
        rabbitTemplate.setConfirmCallback(this);

        //消息ID：用户消息确认成功返回后 修改本地消息表的状态
        CorrelationData correlationData = new CorrelationData(localMessage.getTxNo());

        rabbitTemplate.convertAndSend("order_exchage","order.add", JsonUtils.objectToJson(localMessage),correlationData);


    }
}
