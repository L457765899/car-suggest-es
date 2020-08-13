package com.sxb.lin.es.suggest.service.impl;

import com.sxb.lin.es.suggest.config.IndexAttributes;
import com.sxb.lin.es.suggest.db.dao.CarcategoryMapper;
import com.sxb.lin.es.suggest.dto.Area;
import com.sxb.lin.es.suggest.dto.Car;
import com.sxb.lin.es.suggest.dto.Key;
import com.sxb.lin.es.suggest.dto.SearchField;
import com.sxb.lin.es.suggest.service.SuggestService;
import com.sxb.lin.es.suggest.util.RetUtil;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service("bFSCSuggestService")
public class BFSCSuggestServiceImpl extends AbstractSuggestServiceImpl implements SuggestService {

    @Resource(name = "bFSCCSuggestService")
    private SuggestService suggestService;

    @Autowired
    private CarcategoryMapper carcategoryMapper;

    @Override
    protected SearchField getSearchField() {
        SearchField searchField = new SearchField();
        searchField.setIndex(IndexAttributes.SXB_CATEGORY);
        searchField.setField("search_bfsc");
        searchField.setIncludeFields(new String[]{"brand", "series", "factoryName", "modeType", "category", "officialQuote"});
        return searchField;
    }

    @Override
    protected Map<String, Object> handleResult(SearchResponse searchResponse, Key key, Area area) {
        List<Map<String, Object>> resultData = new ArrayList<>();
        SearchHits searchHits = searchResponse.getHits();
        SearchHit[] searchHitsArray = searchHits.getHits();

        if (searchHits.getTotalHits().value == 1) {
            Map<String, Object> mapData = searchHitsArray[0].getSourceAsMap();
            Map<String, Object> firstData = new HashMap<>();
            int modeType = Integer.parseInt(mapData.get("modeType").toString());
            String category = (String) mapData.get("category");
            String reduceCategory = this.reduceCategory(0, category);
            firstData.put("brand", mapData.get("brand"));
            firstData.put("series", mapData.get("series"));
            firstData.put("factoryName", mapData.get("factoryName"));
            firstData.put("category", category);
            firstData.put("modeType", modeType);
            firstData.put("match", 1);
            if (mapData.get("officialQuote") != null) {
                int officialQuote = Integer.parseInt(mapData.get("officialQuote").toString());
                firstData.put("officialQuote", officialQuote);
                firstData.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                        + " " + reduceCategory + "/" + (officialQuote / 100));
            } else {
                firstData.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                        + " " + reduceCategory);
            }
            resultData.add(firstData);

            //如果品牌工厂车系车型只有一个结果，需要将颜色集合追加到后面
            String colors = carcategoryMapper.selectColorById(Integer.parseInt(searchHitsArray[0].getId()));
            if (StringUtils.hasLength(colors)) {
                String[] colorArray = colors.split(",");
                for (String color : colorArray) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("brand", mapData.get("brand"));
                    data.put("series", mapData.get("series"));
                    data.put("factoryName", mapData.get("factoryName"));
                    data.put("category", category);
                    data.put("color", color);
                    data.put("modeType", modeType);
                    data.put("match", 1);
                    if (mapData.get("officialQuote") != null) {
                        int officialQuote = Integer.parseInt(mapData.get("officialQuote").toString());
                        data.put("officialQuote", officialQuote);
                        data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                                + " " + reduceCategory + "/" + (officialQuote / 100) + "/" + color);
                    } else {
                        data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                                + " " + reduceCategory + "/" + color);
                    }
                    resultData.add(data);
                }
            }
        } else {
            for (int i = 0, length = searchHitsArray.length; i < length; i++) {
                Map<String, Object> mapData = searchHitsArray[i].getSourceAsMap();
                int modeType = Integer.parseInt(mapData.get("modeType").toString());
                String category = (String) mapData.get("category");
                String reduceCategory = this.reduceCategory(i, category);
                Map<String, Object> data = new HashMap<>();
                data.put("brand", mapData.get("brand"));
                data.put("series", mapData.get("series"));
                data.put("factoryName", mapData.get("factoryName"));
                data.put("category", category);
                data.put("modeType", modeType);
                data.put("match", 1);
                if (mapData.get("officialQuote") != null) {
                    int officialQuote = Integer.parseInt(mapData.get("officialQuote").toString());
                    data.put("officialQuote", officialQuote);
                    data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                            + " " + reduceCategory + "/" + (officialQuote / 100));
                } else {
                    data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                            + " " + reduceCategory);
                }
                resultData.add(data);
            }
        }

        return RetUtil.getRetValue(this.subDatas(resultData,key.getKey(),area));
    }

    @Override
    public SuggestService getSuggestService() {
        return suggestService;
    }

    @Override
    protected void delete() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(IndexAttributes.SXB_CATEGORY);
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if (exists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(IndexAttributes.SXB_CATEGORY);
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new Exception("删除" + IndexAttributes.SXB_CATEGORY + "索引失败");
            }
        }
    }

    @Override
    protected XContentBuilder mapping() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();

        //start properties
        builder.startObject("properties");

        builder.startObject("brand");
        builder.field("type", "keyword");
        builder.endObject();

        builder.startObject("factoryName");
        builder.field("type", "keyword");
        builder.endObject();

        builder.startObject("series");
        builder.field("type", "keyword");
        builder.startObject("fields");
        builder.startObject("seriesText");
        builder.field("type", "text");
        builder.field("analyzer", "index_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();
        builder.endObject();
        builder.endObject();

        builder.startObject("category");
        builder.field("type", "keyword");
        builder.startObject("fields");
        builder.startObject("categoryText");
        builder.field("type", "text");
        builder.field("analyzer", "index_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();
        builder.endObject();
        builder.endObject();
        
        builder.startObject("year");
        builder.field("type", "integer");
        builder.endObject();

        builder.startObject("modeType");
        builder.field("type", "integer");
        builder.endObject();

        builder.startObject("officialQuote");
        builder.field("type", "integer");
        builder.endObject();

        builder.startObject("search_bfsc");
        builder.field("type", "text");
        builder.field("analyzer", "index_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.field("store", false);
        builder.endObject();

        builder.startObject("weight");
        builder.field("type", "integer");
        builder.endObject();

        builder.endObject();
        //end properties

        builder.endObject();
        return builder;
    }

    @Override
    protected void create() throws Exception {
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(IndexAttributes.SXB_CATEGORY);
        createIndexRequest.mapping(this.mapping());
        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        if (!createIndexResponse.isAcknowledged()) {
            throw new Exception("创建" + IndexAttributes.SXB_CATEGORY + "索引失败");
        }
    }

    @Override
    protected void bulk() throws Exception {
        List<Map<String, Object>> categoryList = carcategoryMapper.getAllForIndex();
        int len = categoryList.size();
        int pageCount = (len - 1) / 5000 + 1;
        Map<Integer, BulkRequest> pageMap = new HashMap<>();
        for (int i = 1; i <= pageCount; i++) {
            BulkRequest request = new BulkRequest();
            pageMap.put(i, request);
        }
        for (int i = 1; i <= len; i++) {
            Map<String, Object> categoryMap = categoryList.get(i - 1);
            String categoryId = categoryMap.get("id").toString();
            IndexRequest indexRequest = new IndexRequest(IndexAttributes.SXB_CATEGORY);
            indexRequest.id(categoryId);
            indexRequest.source(this.getSource(categoryMap));

            int page = (i - 1) / 5000 + 1;
            BulkRequest bulkRequest = pageMap.get(page);
            bulkRequest.add(indexRequest);
        }
        for (BulkRequest bulkRequest : pageMap.values()) {
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                throw new Exception(IndexAttributes.SXB_CATEGORY + "索引数据失败");
            } else {
                logger.info(IndexAttributes.SXB_CATEGORY + "索引" + bulkRequest.numberOfActions() + "条数据");
            }
        }
        logger.info(IndexAttributes.SXB_CATEGORY + "总共索引" + len + "条数据");
    }

    private Map<String, Object> getSource(Map<String, Object> categoryMap) {

        String brand = (String) categoryMap.get("brand");
        String factoryName = (String) categoryMap.get("factoryName");
        String series = (String) categoryMap.get("series");
        String category = (String) categoryMap.get("category");
        Integer officialQuote = (Integer) categoryMap.get("officialQuote");
        int modeType = (int) categoryMap.get("modeType");
        Map<String, Object> source = new HashMap<>();
        source.put("brand", brand);
        source.put("factoryName", factoryName);
        source.put("series", series);
        source.put("category", category);
        source.put("modeType", modeType);
        source.put("weight", this.getWeight(modeType));
        source.put("officialQuote", officialQuote);

        StringBuilder bfscBuilder = new StringBuilder();
        bfscBuilder.append(brand);
        this.appendFilterString(bfscBuilder, factoryName);
        this.appendFilterString(bfscBuilder, series);
        this.handleImportCar(bfscBuilder, modeType, factoryName);
        this.appendString(bfscBuilder, category);
        
        String year = this.getYear(category);
        if(StringUtils.hasText(year)) {
        	source.put("year", Integer.parseInt(year));
        }
        this.appendString(bfscBuilder, year);
        
        if (officialQuote != null && officialQuote > 0) {
            Integer OfficialQuote = officialQuote / 100;
            this.appendString(bfscBuilder, OfficialQuote.toString());
            String number = this.getNumber(series);
            String displacement = this.getDisplacement(category);

            if (StringUtils.hasLength(number)) {
                this.appendString(bfscBuilder, number + OfficialQuote);
                this.appendString(bfscBuilder, number + year);
                this.appendString(bfscBuilder, number + OfficialQuote + year);
                this.appendString(bfscBuilder, number + year + OfficialQuote);

                //额外加的
                if (StringUtils.hasLength(displacement)) {
                    this.appendString(bfscBuilder, number + displacement);

                    this.appendString(bfscBuilder, number + OfficialQuote + displacement);
                    this.appendString(bfscBuilder, number + displacement + OfficialQuote);

                    this.appendString(bfscBuilder, number + year + displacement);
                    this.appendString(bfscBuilder, number + displacement + year);
                }
            }

            this.appendString(bfscBuilder, OfficialQuote + year);
            this.appendString(bfscBuilder, year + OfficialQuote);

            //额外加的
            if (StringUtils.hasLength(displacement)) {
                this.appendString(bfscBuilder, displacement + OfficialQuote);
                this.appendString(bfscBuilder, OfficialQuote + displacement);

                this.appendString(bfscBuilder, displacement + year);
                this.appendString(bfscBuilder, year + displacement);

                this.appendString(bfscBuilder, OfficialQuote + displacement + year);
                this.appendString(bfscBuilder, OfficialQuote + year + displacement);

                this.appendString(bfscBuilder, year + displacement + OfficialQuote);
                this.appendString(bfscBuilder, year + OfficialQuote + displacement);

                this.appendString(bfscBuilder, displacement + OfficialQuote + year);
                this.appendString(bfscBuilder, displacement + year + OfficialQuote);
            }
        }
        source.put("search_bfsc", bfscBuilder.toString());

        return source;
    }
    
    @Override
	protected QueryBuilder getQueryBuilder(Key key, Area area, String field) {
    	QueryBuilder queryBuilder = super.getQueryBuilder(key, area, field);
        return QueryBuilders.constantScoreQuery(queryBuilder);
	}

	@Override
	protected SearchSourceBuilder getSearchSourceBuilder(Key key, Area area, SearchField field) {
		SearchSourceBuilder searchSourceBuilder = super.getSearchSourceBuilder(key, area, field);
		searchSourceBuilder.sort("year", SortOrder.DESC);
		return searchSourceBuilder;
	}

	public Car getCarByCondition(String brand, String series, int year, Integer officialQuote, String category) throws Exception {
		BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
		boolQuery.must(QueryBuilders.termQuery("brand", brand));
		boolQuery.must(QueryBuilders.matchQuery("series.seriesText", series).operator(Operator.AND));
		boolQuery.must(QueryBuilders.termQuery("year", year % 100));
		if(officialQuote != null) {
			boolQuery.must(QueryBuilders.termQuery("officialQuote", officialQuote));
			boolQuery.should(QueryBuilders.matchQuery("category.categoryText", category));
		}else {
			boolQuery.must(QueryBuilders.matchQuery("category.categoryText", category).operator(Operator.AND));
		}
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQuery);
        searchSourceBuilder.fetchSource(this.getSearchField().getIncludeFields(), null);
        searchSourceBuilder.size(1);
        
        SearchResponse searchResponse = this.search(searchSourceBuilder);
        if (searchResponse.getHits().getTotalHits().value > 0) {
        	Map<String, Object> mapData = searchResponse.getHits().getHits()[0].getSourceAsMap();
        	Car car = new Car();
        	car.setBrand(mapData.get("brand").toString());
        	car.setSeries(mapData.get("series").toString());
        	car.setFactoryName(mapData.get("factoryName").toString());
        	car.setCategory(mapData.get("category").toString());
        	car.setModeType((int) mapData.get("modeType"));
            if (mapData.get("officialQuote") != null) {
            	car.setOfficialQuote(Integer.parseInt(mapData.get("officialQuote").toString()));
            }
            return car;
        }
		return null;
	}
}
