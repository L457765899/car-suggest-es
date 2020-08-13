package com.sxb.lin.es.suggest.service.impl;

import com.sxb.lin.es.suggest.config.IndexAttributes;
import com.sxb.lin.es.suggest.db.dao.GeographicMapper;
import com.sxb.lin.es.suggest.db.entity.Geographic;
import com.sxb.lin.es.suggest.dto.Area;
import com.sxb.lin.es.suggest.dto.Key;
import com.sxb.lin.es.suggest.dto.SearchField;
import com.sxb.lin.es.suggest.service.SuggestService;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("aSuggestService")
public class ASuggestServiceImpl extends AbstractSuggestServiceImpl implements SuggestService {
    
    private static final String[] SPECIAL_ALL_AREA = {"北京市", "天津市", "郑州市", "吉林省", "南京市", "四川省", "苏州市", "江西省", "陕西省"};
    
    private static final String[] citys = {"重庆市","天津市","上海市","北京市"};
    
    @Resource(name = "bSSuggestService")
    private SuggestService suggestService;

    @Autowired
    private GeographicMapper geographicMapper;

    @Override
    public Map<String, Object> searchSuggest(Key key, Area area) throws Exception {
        area = this.doSearch(key);
        return suggestService.searchSuggest(key, area);
    }

    protected Area doSearch(Key key) throws Exception {
        List<String> terms = this.analyze(key.getKey());
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        for(String term : terms){
            if(term.length() >= 2){
                boolQuery.should(QueryBuilders.termQuery("name", term));
            }
        }
        key.setTerms(terms);
        
        if(boolQuery.should().size() > 0){
            SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(boolQuery);
            searchSourceBuilder.from(0); 
            searchSourceBuilder.size(1);
            searchSourceBuilder.sort("weight", SortOrder.DESC);
            searchSourceBuilder.sort("_score");
            
            SearchRequest searchRequest = new SearchRequest(IndexAttributes.SXB_AREA);
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            if(hits.getTotalHits().value > 0){
                SearchHit hit = hits.getAt(0);
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                Area area = new Area();
                area.setArea((String) sourceAsMap.get("name"));
                area.setAreaIdPath((String) sourceAsMap.get("pathunid"));
                return area;
            }
        }
        
        return null;
    }
    
    @Override
    protected SearchField getSearchField() {
        // do nothing
        return null;
    }

    @Override
    protected Map<String, Object> handleResult(SearchResponse searchResponse, Key key, Area area) {
        // do nothing
        return null;
    }
    
    @Override
    public SuggestService getSuggestService() {
        return suggestService;
    }

    @Override
    protected void delete() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(IndexAttributes.SXB_AREA);
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if(exists){
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(IndexAttributes.SXB_AREA);
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if(!deleteIndexResponse.isAcknowledged()){
                throw new Exception("删除" + IndexAttributes.SXB_AREA + "索引失败");
            }
        }
    }

    @Override
    protected XContentBuilder mapping() throws Exception {
        
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();

        //start properties
        builder.startObject("properties");
        
        builder.startObject("name");
        builder.field("type", "text");
        builder.field("analyzer", "index_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();
        
        builder.startObject("pathunid");
        builder.field("type", "keyword");
        builder.endObject();
        
        builder.startObject("weight");
        builder.field("type", "integer");
        builder.endObject();
        
        builder.startObject("type");
        builder.field("type", "integer");
        builder.endObject();
        
        builder.endObject();
        //end properties
        
        builder.endObject();
        
        return builder;
    }

    @Override
    protected void create() throws Exception {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(IndexAttributes.SXB_AREA);
        createIndexRequest.mapping(this.mapping());
        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        if(!createIndexResponse.isAcknowledged()){
            throw new Exception("创建" + IndexAttributes.SXB_AREA + "索引失败");
        }
    }

    @Override
    protected void bulk() throws Exception {
        List<Geographic> geographics = this.getGeographics();
        BulkRequest bulkRequest = new BulkRequest();
        for(Geographic geographic : geographics){
            Map<String, Object> source = new HashMap<>();
            source.put("name", geographic.getName());
            source.put("pathunid", geographic.getPathunid());
            source.put("type", geographic.getType());
            int indexOf = ArrayUtils.indexOf(SPECIAL_ALL_AREA, geographic.getName());
            if(indexOf > -1){
                source.put("weight", indexOf * 100);
            }else{
                source.put("weight", 1000);
            }
            
            IndexRequest indexRequest = new IndexRequest(IndexAttributes.SXB_AREA);
            indexRequest.id(geographic.getUnid().toString());
            indexRequest.source(source);
            bulkRequest.add(indexRequest);
        }
        
        BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
        if (bulkResponse.hasFailures()) {
            throw new Exception(IndexAttributes.SXB_AREA + "索引数据失败");
        }else{
            logger.info(IndexAttributes.SXB_AREA + "索引" + bulkRequest.numberOfActions() + "条数据");
        }
        logger.info(IndexAttributes.SXB_AREA + "总共索引" + geographics.size() + "条数据");
    }

    private List<Geographic> getGeographics(){
        List<Geographic> geographics = new ArrayList<>();
        Geographic d = new Geographic();
        d.setUnid(51331);
        d.setName("东区");
        d.setPathunid("-1,48,49,51331");
        d.setType(3);
        Geographic n = new Geographic();
        n.setUnid(51332);
        n.setName("南区");
        n.setPathunid("-1,48,49,51332");
        n.setType(3);
        Geographic x = new Geographic();
        x.setUnid(51333);
        x.setName("西区");
        x.setPathunid("-1,48,49,51333");
        x.setType(3);
        Geographic b = new Geographic();
        b.setUnid(51330);
        b.setName("北区");
        b.setPathunid("-1,48,49,51330");
        b.setType(3);
        geographics.add(d);
        geographics.add(n);
        geographics.add(x);
        geographics.add(b);
        
        geographics.addAll(geographicMapper.getListByType(4));//省级类型
        geographics.addAll(geographicMapper.getListByType(5));//城市级别

        for(Geographic g : geographics){
            if(ArrayUtils.contains(citys, g.getName())){
                String[] ids = g.getPathunid().split(",");
                if(ids.length >= 5){
                    g.setPathunid(ids[0]+","+ids[1]+","+ids[2]+","+ids[3]+","+ids[4]);
                    g.setUnid(Integer.parseInt(ids[4]));
                }
            }
        }
        
        return geographics;
    }

}
