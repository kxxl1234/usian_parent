package com.usian.service;

import com.usian.mapper.TbUserMapper;
import com.usian.pojo.TbUser;
import com.usian.pojo.TbUserExample;
import com.usian.redis.RedisClient;
import com.usian.utils.MD5Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Transactional
public class SSOServiceImpl implements SSOService{

    @Autowired
    private TbUserMapper tbUserMapper;

    @Autowired
    private RedisClient redisClient;

    @Value("${USER_INFO}")
    private String USER_INFO;

    @Value("${SESSION_EXPIRE}")
    private Long SESSION_EXPIRE;

    @Override
    public Boolean checkUserInfo(String checkValue, Integer checkFlag) {
        TbUserExample tbUserExample = new TbUserExample();
        TbUserExample.Criteria criteria = tbUserExample.createCriteria();
        //1.checkflag设置查询条件：1=username 2=phone
        if (checkFlag==1){
            criteria.andUsernameEqualTo(checkValue);
        }else if (checkFlag==2){
            criteria.andPhoneEqualTo(checkValue);
        }

        //2.校验用户信息是否合格
        List<TbUser> tbUserList = tbUserMapper.selectByExample(tbUserExample);
        if (tbUserList==null || tbUserList.size()==0){
            return true;
        }
        return false;
    }

    @Override
    public Integer userRegister(TbUser tbUser) {
        String pwd = MD5Utils.digest(tbUser.getPassword());
        tbUser.setPassword(pwd);
        Date date = new Date();
        tbUser.setCreated(date);
        tbUser.setUpdated(date);
        return tbUserMapper.insertSelective(tbUser);
    }

    @Override
    public Map userLogin(String username, String password) {
        //1.把password加密
        String pwd = MD5Utils.digest(password);
        //2.判断用户名密码是否正确
        TbUserExample tbUserExample = new TbUserExample();
        TbUserExample.Criteria criteria = tbUserExample.createCriteria();
        criteria.andUsernameEqualTo(username);
        criteria.andPasswordEqualTo(pwd);
        List<TbUser> tbUserList = tbUserMapper.selectByExample(tbUserExample);
        if (tbUserList == null || tbUserList.size() == 0){
            return null;
        }
        //3.登陆成功把user装到redis，并设置失效时间
        TbUser tbUser = tbUserList.get(0);
        tbUser.setPassword(null);
        String token = UUID.randomUUID().toString();
        redisClient.set(USER_INFO+":"+token,tbUser);
        redisClient.expire(USER_INFO+":"+token,SESSION_EXPIRE);

        //4.返回结果 : map(token,userid,username)
        Map<String, Object> map = new HashMap<>();
        map.put("token",token);
        map.put("userid",tbUser.getId());
        map.put("username",tbUser.getUsername());
        return map;
    }

    @Override
    public TbUser getUserByToken(String token) {

        TbUser tbUser = (TbUser) redisClient.get(USER_INFO + ":" + token);
        if (tbUser!=null){
            redisClient.expire(USER_INFO+":"+token,SESSION_EXPIRE);
            return tbUser;
        }
        return null;
    }

    @Override
    public Boolean logOut(String token) {

        return redisClient.del(USER_INFO+":"+token);
    }


}
