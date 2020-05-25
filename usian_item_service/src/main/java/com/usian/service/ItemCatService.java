package com.usian.service;

import com.usian.pojo.TbItem;
import com.usian.pojo.TbItemCat;
import com.usian.utils.CatResult;
import com.usian.utils.PageResult;

import java.util.List;


public interface ItemCatService {
    List<TbItemCat> selectItemCategoryByParentId(Long id);

    CatResult selectItemCategoryAll();
}
