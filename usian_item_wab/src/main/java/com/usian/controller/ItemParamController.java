package com.usian.controller;

import com.usian.feign.ItemServiceFeign;
import com.usian.pojo.TbItemParam;
import com.usian.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/backend/itemParam")
public class ItemParamController {

    @Autowired
    private ItemServiceFeign itemServiceFeign;

    @RequestMapping("/selectItemParamByItemCatId/{itemCatId}")
    public Result selectItemParamByItemCatId(@PathVariable Long itemCatId){

        TbItemParam tbItemParam =itemServiceFeign.selectItemParamByItemCatId(itemCatId);
        if (tbItemParam != null){
           return Result.ok(tbItemParam);
        }
        return Result.error("查无结果");
    }
}
