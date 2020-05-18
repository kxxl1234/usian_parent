package com.usian.service;

import com.usian.pojo.TbItem;
import com.usian.utils.PageResult;


public interface ItemService{
    TbItem selectItemInfo(Long itemId);

    PageResult selectTbItemAllByPage(Integer page, Integer rows);

    Integer insertTbItem(TbItem tbItem, String desc, String itemsParams);
}
