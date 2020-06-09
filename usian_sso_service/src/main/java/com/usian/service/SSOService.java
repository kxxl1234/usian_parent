package com.usian.service;

import com.usian.pojo.TbUser;

import java.util.Map;

public interface SSOService {
    Boolean checkUserInfo(String checkValue, Integer checkFlag);

    Integer userRegister(TbUser tbUser);

    Map userLogin(String username, String password);

    TbUser getUserByToken(String token);

    Boolean logOut(String token);
}
