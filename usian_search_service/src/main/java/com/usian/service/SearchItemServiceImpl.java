package com.usian.service;

import com.github.pagehelper.PageHelper;
import com.usian.mapper.SearchItemMapper;
import com.usian.pojo.SearchItem;
import com.usian.utils.JsonUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

@Service
@Transactional
public class SearchItemServiceImpl implements SearchItemService {

    @Autowired
    private SearchItemMapper searchItemMapper;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Value("${ES_INDEX_NAME}")
    private String ES_INDEX_NAME;

    @Value("${ES_TYPE_NAME}")
    private String ES_TYPE_NAME;

    @Override
    public boolean importAll() {
        try {
            if(!isExistsIndex()){
                createIndex();
            }
            int page=1;
            while (true){
                /**分页每次导入一千条*/
                PageHelper.startPage(page,1000);
                //1、查询mysql中的商品信息
                List<SearchItem> esDocumentList = searchItemMapper.getItemList();
                if(esDocumentList==null || esDocumentList.size()==0){
                    break;
                }
                BulkRequest bulkRequest = new BulkRequest();
                for (int i = 0; i < esDocumentList.size(); i++) {
                    SearchItem searchItem =  esDocumentList.get(i);
                    //2、把商品信息添加到es中
                    bulkRequest.add(new IndexRequest(ES_INDEX_NAME, ES_TYPE_NAME).
                            source(JsonUtils.objectToJson(searchItem), XContentType.JSON));
                }
                restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
                page++;
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }
  /*  @Override
    public boolean importAll() {
        try {
            if (!isExistsIndex()){
                createIndex();
            }
            int page = 1;
            while (true){
                PageHelper.startPage(page,1000);
                //1.从mysql查询商品列表信息
                List<SearchItem> searchItemList = searchItemMapper.getItemList();
                if (searchItemList==null || searchItemList.size()==0){
                    break;
                }
                BulkRequest bulkRequest = new BulkRequest();
                for (SearchItem searchItem : searchItemList){
                    //2.把查询的结果导入到es
                    bulkRequest.add(new IndexRequest(ES_INDEX_NAME,ES_TYPE_NAME).
                            source(JsonUtils.objectToJson(searchItem), XContentType.JSON));
                }
                restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);
                page++;
            }

        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }*/
    /*
    * 判断索引库是否存在
    * */
    private boolean isExistsIndex() throws IOException {
        GetIndexRequest getIndexRequest = new GetIndexRequest();
        getIndexRequest.indices(ES_INDEX_NAME);
        return restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
    }

    /*
    * 创建索引库
    * */
    private void createIndex() throws IOException {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest();
        createIndexRequest.settings(Settings.builder().put("number_of_shards",2).
                put("number_of_replicas",1));

        restHighLevelClient.indices().create(createIndexRequest,RequestOptions.DEFAULT);
    }


}
