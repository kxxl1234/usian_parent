package com.usian.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.usian.mapper.TbItemCatMapper;
import com.usian.mapper.TbItemDescMapper;
import com.usian.mapper.TbItemMapper;
import com.usian.mapper.TbItemParamItemMapper;
import com.usian.pojo.*;
import com.usian.redis.RedisClient;
import com.usian.utils.IDUtils;
import com.usian.utils.PageResult;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ItemServiceImpl implements ItemService{

   @Autowired
   private TbItemMapper tbItemMapper;

   @Autowired
   private TbItemDescMapper tbItemDescMapper;

   @Autowired
   private TbItemParamItemMapper tbItemParamItemMapper;

   @Autowired
   private TbItemCatMapper tbItemCatMapper;

   @Autowired
   private AmqpTemplate amqpTemplate;

   @Autowired
   private RedisClient redisClient;

    @Value("${ITEM_INFO}")
    private String ITEM_INFO;

    @Value("${BASE}")
    private String BASE;

    @Value("${DESC}")
    private String DESC;

    @Value("${ITEM_INFO_EXPIRE}")
    private Long ITEM_INFO_EXPIRE;

    @Value("${SETNX_BASE_LOCK_KEY}")
    private String SETNX_BASE_LOCK_KEY;

    @Value("${SETNX_DESC_LOCK_KEY}")
    private String SETNX_DESC_LOCK_KEY;



    @Override
    public TbItem selectItemInfo(Long itemId){
        //1、先查询redis,如果有直接返回
        TbItem tbItem = (TbItem) redisClient.get(ITEM_INFO+":"+itemId+":"+BASE);
        if(tbItem!=null){
            return tbItem;
        }
        /*****************解决缓存击穿***************/
        if(redisClient.setnx(SETNX_BASE_LOCK_KEY+":"+itemId,itemId,30L)){
            //2、再查询mysql,并把查询结果缓存到redis,并设置失效时间
            tbItem = tbItemMapper.selectByPrimaryKey(itemId);

            /*****************解决缓存穿透*****************/
            if(tbItem!=null){
                redisClient.set(ITEM_INFO+":"+itemId+":"+BASE,tbItem);
                redisClient.expire(ITEM_INFO+":"+itemId+":"+BASE,ITEM_INFO_EXPIRE);
            }else{
                redisClient.set(ITEM_INFO+":"+itemId+":"+BASE,null);
                redisClient.expire(ITEM_INFO+":"+itemId+":"+BASE,30L);
            }
            redisClient.del(SETNX_BASE_LOCK_KEY+":"+itemId);
            return tbItem;
        }else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return selectItemInfo(itemId);
        }
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

        //设置倒叙排列
        example.setOrderByClause("updated DESC");

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

    @Override
    public Integer insertTbItem(TbItem tbItem, String desc, String itemParams) {
        //补齐Tbitem数据
        long itemId = IDUtils.genItemId(); //工具类获取id
        Date date = new Date();
        tbItem.setId(itemId);
        tbItem.setStatus((byte)1);
        tbItem.setUpdated(date); //添加时间
        tbItem.setCreated(date); //修改时间
        tbItem.setPrice(tbItem.getPrice()*100);
        int tbItemNum = tbItemMapper.insertSelective(tbItem);

        //补齐商品描述对象
        TbItemDesc tbItemDesc = new TbItemDesc();
        tbItemDesc.setItemId(itemId);
        tbItemDesc.setItemDesc(desc);
        tbItemDesc.setCreated(date);
        tbItemDesc.setUpdated(date);
        int tbItemDescNum = tbItemDescMapper.insertSelective(tbItemDesc);

        //补齐商品规格参数
        TbItemParamItem tbItemParamItem = new TbItemParamItem();
        tbItemParamItem.setItemId(itemId);
        tbItemParamItem.setParamData(itemParams);
        tbItemParamItem.setCreated(date);
        tbItemParamItem.setUpdated(date);
        int tbItemParamItemNum = tbItemParamItemMapper.insertSelective(tbItemParamItem);

        //发送mq，完成索引库同步
        amqpTemplate.convertAndSend("item_exchange","item.add",itemId);

        return tbItemNum + tbItemDescNum + tbItemParamItemNum;
    }

    @Override
    public Map<String, Object> preUpdateItem(Long itemId) {
        Map<String, Object> map = new HashMap<>();
        //根据商品 ID 查询商品
        TbItem item = tbItemMapper.selectByPrimaryKey(itemId);
        map.put("item", item);
        //根据商品 ID 查询商品描述
        TbItemDesc itemDesc = tbItemDescMapper.selectByPrimaryKey(itemId);
        map.put("itemDesc", itemDesc.getItemDesc());
        //根据商品 ID 查询商品类目
        TbItemCat itemCat = tbItemCatMapper.selectByPrimaryKey(item.getCid());
        map.put("itemCat", itemCat.getName());
        //根据商品 ID 查询商品规格参数
        TbItemParamItemExample example = new TbItemParamItemExample();
        TbItemParamItemExample.Criteria criteria = example.createCriteria();
        criteria.andItemIdEqualTo(itemId);
        List<TbItemParamItem> list = tbItemParamItemMapper.selectByExampleWithBLOBs(example);
        if (list != null && list.size() > 0) {
            map.put("itemParamItem", list.get(0).getParamData());
        }
        return map;
    }

    @Override
    public Integer updateTbItem(TbItem tbItem, String desc, String itemParams) {
        Date date = new Date();
        tbItem.setUpdated(date);
        tbItem.setPrice(tbItem.getPrice()*100);
        int tbItemNum =  tbItemMapper.updateByPrimaryKeySelective(tbItem);

        TbItemDesc tbItemDesc = new TbItemDesc();
        tbItemDesc.setItemId(tbItem.getId());
        tbItemDesc.setItemDesc(desc);
        tbItemDesc.setUpdated(date);
        int tbItemDescNum = tbItemDescMapper.updateByPrimaryKeySelective(tbItemDesc);


        TbItemParamItemExample tbItemParamItemExample = new TbItemParamItemExample();
        TbItemParamItemExample.Criteria criteria = tbItemParamItemExample.createCriteria();
        criteria.andItemIdEqualTo(tbItem.getId());
        System.out.println(tbItem.getId()+"-------------------------------------------");
        List<TbItemParamItem> tbItemParamItemList = tbItemParamItemMapper.selectByExampleWithBLOBs(tbItemParamItemExample);
        System.out.println("---------++++"+tbItemParamItemList.size()+"+++++++++++++++++++++++++");
        TbItemParamItem tbItemParamItem = tbItemParamItemList.get(0);
        tbItemParamItem.setParamData(itemParams);
        tbItemParamItem.setUpdated(date);
        int tbItemParamItemNum = tbItemParamItemMapper.updateByPrimaryKeySelective(tbItemParamItem);

        return tbItemNum + tbItemDescNum + tbItemParamItemNum;

    }

    @Override
    public Integer deleteItemById(Long itemId) {
        //商品表删除
       int tbItemNum = tbItemMapper.deleteByPrimaryKey(itemId);
        //商品描述删除
        TbItemDescExample tbItemDescExample = new TbItemDescExample();
        TbItemDescExample.Criteria tbItemDescExampleCriteria = tbItemDescExample.createCriteria();
        tbItemDescExampleCriteria.andItemIdEqualTo(itemId);
        int tbItemDescNum = tbItemDescMapper.deleteByExample(tbItemDescExample);
        //商品规格删除
        TbItemParamItemExample tbItemParamItemExample = new TbItemParamItemExample();
        TbItemParamItemExample.Criteria tbItemParamItemExampleCriteria = tbItemParamItemExample.createCriteria();
        tbItemParamItemExampleCriteria.andItemIdEqualTo(itemId);
        int tbItemParamItemNum = tbItemParamItemMapper.deleteByExample(tbItemParamItemExample);
        //返回
        return tbItemNum + tbItemDescNum + tbItemParamItemNum;
    }

/*    @Override
    public TbItemDesc selectItemDescByItemId(Long itemId) {
        //1.先查redis，如果有结果直接返回
        TbItemDesc tbItemDesc = (TbItemDesc) redisClient.get(ITEM_INFO + ":" + itemId + ":" + DESC);
        if (tbItemDesc!=null){
            return tbItemDesc;
        }
        //2.再查询mysql,并把查询的结果缓存到redis，并设置失效时间
        tbItemDesc = tbItemDescMapper.selectByPrimaryKey(itemId);

        if (tbItemDesc!=null){
            redisClient.set(ITEM_INFO+":"+itemId+":"+DESC,tbItemDesc);
            redisClient.expire(ITEM_INFO+":"+itemId+":"+DESC,ITEM_INFO_EXPIRE);
            return tbItemDesc;
        }

        redisClient.set(ITEM_INFO+":"+itemId+":"+DESC,null);
        redisClient.expire(ITEM_INFO+":"+itemId+":"+DESC,30L);
        return tbItemDesc;
    }*/
    @Override
    public TbItemDesc selectItemDescByItemId(Long itemId) {
        //1、先查询redis,如果有直接返回
        TbItemDesc tbItemDesc = (TbItemDesc) redisClient.get(ITEM_INFO+":"+itemId +":"+DESC);
        if(tbItemDesc!=null){
            return tbItemDesc;
        }
        if(redisClient.setnx(SETNX_DESC_LOCK_KEY+":"+itemId,itemId,30L)){
            //2、再查询mysql,并把查询结果缓存到redis,并设置失效时间
            tbItemDesc = tbItemDescMapper.selectByPrimaryKey(itemId);

            if(tbItemDesc!=null){
                redisClient.set(ITEM_INFO + ":" + itemId + ":" + DESC,tbItemDesc);
                redisClient.expire(ITEM_INFO +":"+itemId+":"+DESC,ITEM_INFO_EXPIRE);

            }else{
                redisClient.set(ITEM_INFO+":"+itemId+":"+DESC,null);
                redisClient.expire(ITEM_INFO+":"+itemId+":"+DESC,30L);
            }
            redisClient.del(SETNX_DESC_LOCK_KEY+":"+itemId);
            return tbItemDesc;
        }else{
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return selectItemDescByItemId(itemId);
        }
    }
}
