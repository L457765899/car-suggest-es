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
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("bFSCCSuggestService")
public class BFSCCSuggestServiceImpl extends AbstractSuggestServiceImpl implements SuggestService {

    @Autowired
    private CarcategoryMapper carcategoryMapper;

    @Override
    protected SearchField getSearchField() {
        SearchField searchField = new SearchField();
        searchField.setIndex(IndexAttributes.SXB_CATEGORY_COLOR);
        searchField.setField("search_bfscc");
        searchField.setIncludeFields(new String[]{"brand", "series", "factoryName", "modeType", "category", "officialQuote", "outColor"});
        return searchField;
    }

    @Override
    protected Map<String, Object> handleResult(SearchResponse searchResponse, Key key, Area area) {
        List<Map<String, Object>> resultData = new ArrayList<>();

        SearchHits searchHits = searchResponse.getHits();
        SearchHit[] searchHitsArray = searchHits.getHits();

        for (int i = 0, length = searchHitsArray.length; i < length; i++) {
            Map<String, Object> mapData = searchHitsArray[i].getSourceAsMap();
            int modeType = Integer.parseInt(mapData.get("modeType").toString());
            String category = (String) mapData.get("category");
            String reduceCategory = this.reduceCategory(0, category);
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
                if (!StringUtils.isEmpty(mapData.get("outColor"))) {
                    data.put("color", mapData.get("outColor"));
                    data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                            + " " + reduceCategory + "/" + (officialQuote / 100) + "/" + mapData.get("outColor"));
                } else {
                    data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                            + " " + reduceCategory + "/" + (officialQuote / 100));
                }
            } else {
            	if (!StringUtils.isEmpty(mapData.get("outColor"))) {
                    data.put("color", mapData.get("outColor"));
                    data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                            + " " + reduceCategory + "/" + mapData.get("outColor"));
                } else {
                    data.put("show", mapData.get("series") + this.convertModeType(modeType, mapData.get("series"))
                            + " " + reduceCategory);
                }
            }
            resultData.add(data);
        }
        return RetUtil.getRetValue(this.subDatas(resultData,key.getKey(),area));
    }

    @Override
    public SuggestService getSuggestService() {
        return null;
    }

    @Override
    protected void delete() throws Exception {
        GetIndexRequest getIndexRequest = new GetIndexRequest(IndexAttributes.SXB_CATEGORY_COLOR);
        boolean exists = client.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
        if (exists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(IndexAttributes.SXB_CATEGORY_COLOR);
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new Exception("删除" + IndexAttributes.SXB_CATEGORY_COLOR + "索引失败");
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
        builder.endObject();

        builder.startObject("category");
        builder.field("type", "keyword");
        builder.endObject();

        builder.startObject("outColor");
        builder.field("type", "keyword");
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

        builder.startObject("search_bfscc");
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
        CreateIndexRequest createIndexRequest = new CreateIndexRequest(IndexAttributes.SXB_CATEGORY_COLOR);
        createIndexRequest.mapping(this.mapping());
        CreateIndexResponse createIndexResponse = client.indices().create(createIndexRequest, RequestOptions.DEFAULT);
        if (!createIndexResponse.isAcknowledged()) {
            throw new Exception("创建" + IndexAttributes.SXB_CATEGORY_COLOR + "索引失败");
        }
    }

    @Override
    protected void bulk() throws Exception {
        List<Map<String, Object>> categoryList = carcategoryMapper.getAllForIndex();
        List<Car> carList = new ArrayList<>();
        for (Map<String, Object> categoryMap : categoryList) {
            String brand = (String) categoryMap.get("brand");
            String factoryName = (String) categoryMap.get("factoryName");
            String series = (String) categoryMap.get("series");
            String category = (String) categoryMap.get("category");
            Integer officialQuote = (Integer) categoryMap.get("officialQuote");
            int modeType = (int) categoryMap.get("modeType");
            String outColor = (String) categoryMap.get("outColor");
            if (StringUtils.hasLength(outColor)) {
                String[] colors = outColor.split(",");
                for (String color : colors) {
                    Car car = new Car();
                    car.setBrand(brand);
                    car.setFactoryName(factoryName);
                    car.setSeries(series);
                    car.setCategory(category);
                    car.setOfficialQuote(officialQuote);
                    car.setOutColor(color);
                    car.setModeType(modeType);
                    carList.add(car);
                }
            } else {
                Car car = new Car();
                car.setBrand(brand);
                car.setFactoryName(factoryName);
                car.setSeries(series);
                car.setCategory(category);
                car.setOfficialQuote(officialQuote);
                car.setModeType(modeType);
                carList.add(car);
            }
        }

        int len = carList.size();
        int pageCount = (len - 1) / 5000 + 1;
        Map<Integer, BulkRequest> pageMap = new HashMap<>();
        for (int i = 1; i <= pageCount; i++) {
            BulkRequest request = new BulkRequest();
            pageMap.put(i, request);
        }
        for (int i = 1; i <= len; i++) {
            Car car = carList.get(i - 1);
            IndexRequest indexRequest = new IndexRequest(IndexAttributes.SXB_CATEGORY_COLOR);
            indexRequest.source(this.getSource(car));

            int page = (i - 1) / 5000 + 1;
            BulkRequest bulkRequest = pageMap.get(page);
            bulkRequest.add(indexRequest);
        }
        for (BulkRequest bulkRequest : pageMap.values()) {
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                throw new Exception(IndexAttributes.SXB_CATEGORY_COLOR + "索引数据失败");
            } else {
                logger.info(IndexAttributes.SXB_CATEGORY_COLOR + "索引" + bulkRequest.numberOfActions() + "条数据");
            }
        }
        logger.info(IndexAttributes.SXB_CATEGORY_COLOR + "总共索引" + len + "条数据");
    }

    private Map<String, Object> getSource(Car car) {
        Map<String, Object> source = new HashMap<>();
        source.put("brand", car.getBrand());
        source.put("factoryName", car.getFactoryName());
        source.put("series", car.getSeries());
        source.put("category", car.getCategory());
        source.put("modeType", car.getModeType());
        source.put("weight", this.getWeight(car.getModeType()));
        source.put("officialQuote", car.getOfficialQuote());
        source.put("outColor", car.getOutColor());

        StringBuilder bfsccBuilder = new StringBuilder();
        bfsccBuilder.append(car.getBrand());
        this.appendFilterString(bfsccBuilder, car.getFactoryName());
        this.appendFilterString(bfsccBuilder, car.getSeries());
        this.handleImportCar(bfsccBuilder, car.getModeType(), car.getFactoryName());
        this.appendString(bfsccBuilder, car.getCategory());
        
        String year = this.getYear(car.getCategory());
        if(StringUtils.hasText(year)) {
        	source.put("year", Integer.parseInt(year));
        }
        this.appendString(bfsccBuilder, year);
        
        if (car.getOfficialQuote() != null && car.getOfficialQuote() > 0) {
            Integer OfficialQuote = car.getOfficialQuote() / 100;
            this.appendString(bfsccBuilder, OfficialQuote.toString());

            String number = this.getNumber(car.getSeries());
            String displacement = this.getDisplacement(car.getCategory());

            if (StringUtils.hasLength(number)) {
                this.appendString(bfsccBuilder, number + OfficialQuote);
                this.appendString(bfsccBuilder, number + year);
                this.appendString(bfsccBuilder, number + OfficialQuote + year);
                this.appendString(bfsccBuilder, number + year + OfficialQuote);

                //额外加的
                if (StringUtils.hasLength(displacement)) {
                    this.appendString(bfsccBuilder, number + displacement);

                    this.appendString(bfsccBuilder, number + OfficialQuote + displacement);
                    this.appendString(bfsccBuilder, number + displacement + OfficialQuote);

                    this.appendString(bfsccBuilder, number + year + displacement);
                    this.appendString(bfsccBuilder, number + displacement + year);
                }
            }

            this.appendString(bfsccBuilder, OfficialQuote + year);
            this.appendString(bfsccBuilder, year + OfficialQuote);

            //额外加的
            if (StringUtils.hasLength(displacement)) {
                this.appendString(bfsccBuilder, displacement + OfficialQuote);
                this.appendString(bfsccBuilder, OfficialQuote + displacement);

                this.appendString(bfsccBuilder, displacement + year);
                this.appendString(bfsccBuilder, year + displacement);

                this.appendString(bfsccBuilder, OfficialQuote + displacement + year);
                this.appendString(bfsccBuilder, OfficialQuote + year + displacement);

                this.appendString(bfsccBuilder, year + displacement + OfficialQuote);
                this.appendString(bfsccBuilder, year + OfficialQuote + displacement);

                this.appendString(bfsccBuilder, displacement + OfficialQuote + year);
                this.appendString(bfsccBuilder, displacement + year + OfficialQuote);
            }

        }

        if (StringUtils.hasLength(car.getOutColor())) {
            this.appendString(bfsccBuilder, car.getOutColor());
        }
        source.put("search_bfscc", bfsccBuilder.toString());

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
}
