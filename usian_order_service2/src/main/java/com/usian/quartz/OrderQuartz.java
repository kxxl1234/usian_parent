package com.usian.quartz;

import com.usian.pojo.TbOrder;
import com.usian.pojo.TbOrderItem;
import com.usian.redis.RedisClient;
import com.usian.service.OrderService;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.List;

/*
* 1.OrderQuartz放到spring容器中
* 2. @Autowired : 注入orderService
* */
public class OrderQuartz implements Job {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RedisClient redisClient;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String ip =null;

        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }


        if (redisClient.setnx("SETNX_LOCK_ORDER_KEY",ip,30)){
            System.out.println("--------执行关闭超时订单任务---------"+new Date());
            //1.查询超时订单
            List<TbOrder> tbOrderList = orderService.selectOverTimeTbOrder();

            //2.关闭超时订单
            for (int i = 0; i< tbOrderList.size(); i++){
                TbOrder tbOrder = tbOrderList.get(i);
                orderService.updateOverTimeTbOrder(tbOrder);

                //3.把超时订单中的商品库存数量加回去
                orderService.updateTbItemByOrderId(tbOrder.getOrderId());
            }
            //释放锁
            redisClient.del("SETNX_LOCK_ORDER_KEY");

        }else {
            System.out.println(
                    "============机器："+ip+" 占用分布式锁，任务正在执行=======================");
        }
    }

    
}
