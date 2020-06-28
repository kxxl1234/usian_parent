package com.usian.proxy.staticProxy;

public class Client {
    public static void main(String[] args) {
        Star star = new RealStar();
        Star proxy = new ProxyStar(star);
        //对接代理人
        proxy.confer();         //面谈    ---经纪人做的
        proxy.signContract();   //签合同   ---经纪人做的
        proxy.bookTicket();     //订票     ---经纪人做的
        proxy.sing();           //唱歌     ---周杰伦唱歌
        proxy.collectMoney();   //收钱     ---经纪人做的
    }
}
