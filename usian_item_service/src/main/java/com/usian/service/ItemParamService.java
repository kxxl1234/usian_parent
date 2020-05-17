package com.usian.service;


import com.usian.pojo.TbItemParam;

public interface ItemParamService {

    TbItemParam selectItemParamByItemCatId(Long itemCatId);
}
