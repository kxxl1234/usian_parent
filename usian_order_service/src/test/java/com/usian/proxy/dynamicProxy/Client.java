package com.usian.proxy.dynamicProxy;


import java.lang.reflect.Proxy;

public class Client {
    public static void main(String[] args) {

       // Star realStar = new RealStar();
        //ProxyStar proxyStar = new ProxyStar(realStar);

        //生成代理类对象
        //Object proxy = (Star) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),new Class[]{Star.class},proxyStar);

        //((Star) proxy).sing();

        RealCoder realCoder =new RealCoder();
        ProxyClass proxyClass = new ProxyClass(realCoder);

        //生成代理类对象
        Coder proxy = (Coder) Proxy.newProxyInstance(ClassLoader.getSystemClassLoader(),new Class[]{Star.class,Coder.class},proxyClass);

        proxy.code();
    }
}
