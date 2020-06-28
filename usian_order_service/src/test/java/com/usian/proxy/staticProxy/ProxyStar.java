package com.usian.proxy.staticProxy;

public class ProxyStar implements Star {

    private Star star;

    public ProxyStar(Star star){
        this.star=star;
    }


    @Override
    public void confer() {
        System.out.println("ProxyStar.confer");
    }

    @Override
    public void signContract() {
        System.out.println("ProxyStar.signContract");
    }

    @Override
    public void bookTicket() {
        System.out.println("ProxyStar.bookTicket");
    }

    @Override
    public void sing() {
        System.out.println("方法执行前");//通过调用真实角色的方法来完成业务逻辑，并可以附加自己的操作。
        star.sing();
        System.out.println("方法执行后");//通过调用真实角色的方法来完成业务逻辑，并可以附加自己的操作。
    }

    @Override
    public void collectMoney() {
        System.out.println("ProxyStar.collectMoney");
    }
}
