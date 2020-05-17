package com.usian.feign;

import com.usian.pojo.TbItem;
import com.usian.pojo.TbItemCat;
import com.usian.pojo.TbItemParam;
import com.usian.utils.PageResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient("usian-item-service")
public interface ItemServiceFeign {
    @RequestMapping("/service/item/selectItemInfo")
    public TbItem selectItemInfo(@RequestParam Long itemId);

    /*
    * 分页查询接口
    * */
    @RequestMapping("/service/item/selectTbItemAllByPage")
    public PageResult selectTbItemAllByPage(@RequestParam Integer page,@RequestParam Integer rows);

    /*
    * 根据id查询商品类目
    * */
    @RequestMapping("/service/itemCat/selectItemCategoryByParentId")
    List<TbItemCat> selectItemCategoryByParentId(@RequestParam Long id);

    @RequestMapping("/service/itemParam/selectItemParamByItemCatId/{itemCatId}")
    TbItemParam selectItemParamByItemCatId(@RequestParam Long itemCatId);
}
