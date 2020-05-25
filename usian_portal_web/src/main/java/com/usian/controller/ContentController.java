package com.usian.controller;

import com.usian.feign.ContentServiceFeign;
import com.usian.utils.AdNode;
import com.usian.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/frontend/content")
public class ContentController {

    @Autowired
    private ContentServiceFeign contentServiceFeign;

    /*
    * 查询首页打广告
    * */
    @RequestMapping("/selectFrontendContentByAD")
    public Result selectFrontendContentByAD(){
        List<AdNode> adNodeList = contentServiceFeign.selectFrontendContentByAD();
        if (adNodeList != null && adNodeList.size() > 0) {
            return Result.ok(adNodeList);
        }
        return Result.error("查无结果");
    }
}
