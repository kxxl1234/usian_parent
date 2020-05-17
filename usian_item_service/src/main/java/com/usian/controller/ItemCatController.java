package com.usian.controller;

import com.usian.pojo.TbItemCat;
import com.usian.service.ItemCatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/service/itemCat")
public class ItemCatController {

    @Autowired
    private ItemCatService itemCatService;

    @RequestMapping("/selectItemCategoryByParentId")
    public List<TbItemCat> selectItemCategoryByParentId(Long id){
        return itemCatService.selectItemCategoryByParentId(id);
    }

}
