package com.usian.proxy.dynamicProxy;

public class RealCoder implements Coder {
    @Override
    public void signContract() {
        System.out.println("RealCoder.signContract");
    }

    @Override
    public void code() {
        System.out.println("在北京积云-大数据.coding...");
    }

    @Override
    public void collectMoney() {
        System.out.println("RealCoder.collectMoney");
    }
}
