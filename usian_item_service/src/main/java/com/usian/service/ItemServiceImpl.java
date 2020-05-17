package com.usian.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.usian.mapper.TbItemMapper;
import com.usian.pojo.TbItem;
import com.usian.pojo.TbItemExample;
import com.usian.utils.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ItemServiceImpl implements ItemService{

   @Autowired
   private TbItemMapper tbItemMapper;

    @Override
    public TbItem selectItemInfo(Long itemId) {
        return tbItemMapper.selectByPrimaryKey(itemId);
    }

    /**
     * 分页查询所有商品。
     * @param page
     * @param rows
     * @return
     */
    @Override
    public PageResult selectTbItemAllByPage(Integer page, Integer rows) {
        PageHelper.startPage(page,rows);    //设置分页查询数据
        TbItemExample example = new TbItemExample(); //创建逆向工程生成的实体类的sql工具类
        TbItemExample.Criteria criteria = example.createCriteria(); //创建类的条件，类似于创建一个where，添加条件的
        criteria.andStatusEqualTo((byte)1); //创建条件是 and status(字段)=1,等于1表示成功
        List<TbItem> tbItemList = tbItemMapper.selectByExample(example); //逆向工程生成的方法
        PageInfo<TbItem> pageInfo = new PageInfo<>(tbItemList); //分页 pageInfo<类名>
        PageResult result = new PageResult(); //创建接口文档返回的类
        result.setPageIndex(pageInfo.getNextPage()); //使用pageInfo的参数注入到返回类中
        result.setTotalPage(pageInfo.getTotal()); //使用pageInfo的参数注入到返回类中
        result.setResult(pageInfo.getList()); //使用pageInfo的参数注入到返回类中

        return result; //返回参数
    }
}
