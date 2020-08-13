package com.sxb.lin.es.suggest.service.impl;

import com.sxb.lin.es.suggest.config.CarColorCollection;
import com.sxb.lin.es.suggest.config.IndexAttributes;
import com.sxb.lin.es.suggest.db.dao.BrandMapper;
import com.sxb.lin.es.suggest.db.dao.CarcategoryMapper;
import com.sxb.lin.es.suggest.db.dao.CarsriesMapper;
import com.sxb.lin.es.suggest.db.entity.Brand;
import com.sxb.lin.es.suggest.dto.Area;
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
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service("bSSuggestService")
public class BSSuggestServiceImpl extends AbstractSuggestServiceImpl implements SuggestService {

    private final static Logger logger = LoggerFactory.getLogger(BSSuggestServiceImpl.class);

    @Resource(name = "bFSSuggestService")
    private SuggestService suggestService;

    @Autowired
    private CarcategoryMapper carcategoryMapper;

    @Autowired
    private CarsriesMapper carsriesMapper;

    @Autowired
    private BrandMapper BrandMapper;

    @Override
    protected SearchField getSearchField() {
        SearchField searchField = new SearchField();
        searchField.setIndex(IndexAttributes.SXB_SERIES);
        searchField.setField("search_bs");
        searchField.setIncludeFields(new String[]{"brand", "series", "factoryName", "modeType"});
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
            firstData.put("brand", mapData.get("brand"));
            firstData.put("series", mapData.get("series"));
            firstData.put("factoryName", mapData.get("factoryName"));
            firstData.put("modeType", modeType);
            firstData.put("match", 1);
            firstData.put("show", mapData.get("brand") + " " + mapData.get("series") 
            	+ this.convertModeType(modeType, mapData.get("series")));
            resultData.add(firstData);

            //如果品牌车系只有一个结果，需要将官方报价集合追加到后面
            List<Integer> officialQuotes = carcategoryMapper.getOfficialQuoteBySeriesId(
            		Integer.parseInt(searchHitsArray[0].getId()));
            List<Integer> exist = new ArrayList<>();
            for (Integer officialQuote : officialQuotes) {
                if (officialQuote != null && officialQuote > 0
                		&& !exist.contains(officialQuote)) {
                    exist.add(officialQuote);
                    Map<String, Object> data = new HashMap<>();
                    data.put("brand", mapData.get("brand"));
                    data.put("series", mapData.get("series"));
                    data.put("factoryName", mapData.get("factoryName"));
                    data.put("officialQuote", officialQuote);
                    data.put("modeType", modeType);
                    data.put("match", 1);
                    
                    data.put("show", mapData.get("brand") + " " + mapData.get("series")
                            + this.convertModeType(modeType, mapData.get("series")) 
                            + " " + (officialQuote / 100));
                    resultData.add(data);
                }
            }
        } else {
            Set<String> brands = new LinkedHashSet<>();
            for (SearchHit searchHit : searchHitsArray) {
                Map<String, Object> mapData = searchHit.getSourceAsMap();
                int modeType = Integer.parseInt(mapData.get("modeType").toString());
                String brand = mapData.get("brand").toString();
                if (brand.contains(key.getKey())) {
                    brands.add(brand);
                } else if (key.getKey().contains(brand)) {
                    brands.add(brand);
                }
                Map<String, Object> data = new HashMap<>();
                data.put("brand", mapData.get("brand"));
                data.put("series", mapData.get("series"));
                data.put("factoryName", mapData.get("factoryName"));
                data.put("modeType", modeType);
                data.put("match", 1);
                data.put("show", mapData.get("brand") + " " + mapData.get("series") 
                    + this.convertModeType(modeType, mapData.get("series")));
                resultData.add(data);
            }
            //如果品牌车系小于3个结果，需要将官方报价集合追加到后面
            if(searchHits.getTotalHits().value < 4) {
            	for (SearchHit searchHit : searchHitsArray) {
            		Map<String, Object> mapData = searchHit.getSourceAsMap();
            		int modeType = Integer.parseInt(mapData.get("modeType").toString());
	            	List<Integer> officialQuotes = carcategoryMapper.getOfficialQuoteBySeriesId(
	                		Integer.parseInt(searchHit.getId()));
	            	List<Integer> exist = new ArrayList<>();
	                for (Integer officialQuote : officialQuotes) {
	                    if (officialQuote != null && officialQuote > 0
	                    		&& !exist.contains(officialQuote)) {
	                        exist.add(officialQuote);
	                        
	                        Map<String, Object> officialQuoteData = new HashMap<>();
	                        officialQuoteData.put("brand", mapData.get("brand"));
	                        officialQuoteData.put("series", mapData.get("series"));
	                        officialQuoteData.put("factoryName", mapData.get("factoryName"));
	                        officialQuoteData.put("officialQuote", officialQuote);
	                        officialQuoteData.put("modeType", modeType);
	                        officialQuoteData.put("match", 1);
	                        
	                        officialQuoteData.put("show", mapData.get("brand") + " " + mapData.get("series")
	                                + this.convertModeType(modeType, mapData.get("series")) 
	                                + " " + (officialQuote / 100));
	                        resultData.add(officialQuoteData);
	                    }
	                }
            	}
            }
            if (brands.size() > 0) {
                List<Map<String, Object>> brandDataList = new ArrayList<>();
                for (String brand : brands) {
                    Map<String, Object> brandData = new HashMap<>();
                    brandData.put("brand", brand);
                    brandData.put("show", brand);
                    brandDataList.add(brandData);
                }
                brandDataList.addAll(resultData);
                resultData = brandDataList;
            }
        }
        return RetUtil.getRetValue(this.subDatas(resultData, key.getKey(), area));
    }

    @Override
    public SuggestService getSuggestService() {
        return suggestService;
    }

    @Override
    protected void delete() throws Exception {
        GetIndexRequest brandRequest = new GetIndexRequest(IndexAttributes.SXB_BRAND);
        boolean bexists = client.indices().exists(brandRequest, RequestOptions.DEFAULT);
        if (bexists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(IndexAttributes.SXB_BRAND);
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new Exception("删除" + IndexAttributes.SXB_BRAND + "索引失败");
            }
        }

        GetIndexRequest seriesRequest = new GetIndexRequest(IndexAttributes.SXB_SERIES);
        boolean sexists = client.indices().exists(seriesRequest, RequestOptions.DEFAULT);
        if (sexists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(IndexAttributes.SXB_SERIES);
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new Exception("删除" + IndexAttributes.SXB_SERIES + "索引失败");
            }
        }

        GetIndexRequest aliasRequest = new GetIndexRequest(IndexAttributes.SXB_SERIES_ALIAS);
        boolean aexists = client.indices().exists(aliasRequest, RequestOptions.DEFAULT);
        if (aexists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(IndexAttributes.SXB_SERIES_ALIAS);
            AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new Exception("删除" + IndexAttributes.SXB_SERIES_ALIAS + "索引失败");
            }
        }
    }
    
    protected XContentBuilder brandMapping() throws Exception {
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();

        //start properties
        builder.startObject("properties");

        builder.startObject("name");
        builder.field("type", "keyword");
        builder.endObject();

        builder.startObject("nameText");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();

        builder.startObject("query");
        builder.field("type", "percolator");
        builder.endObject();

        builder.startObject("image");
        builder.field("type", "keyword");
        builder.endObject();

        builder.endObject();
        //end properties

        builder.endObject();
        return builder;
    }

    protected XContentBuilder aliasMapping() throws Exception {
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

        builder.startObject("modeType");
        builder.field("type", "integer");
        builder.endObject();

        builder.startObject("weight");
        builder.field("type", "integer");
        builder.endObject();

        builder.startObject("alias");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();

        builder.startObject("query");
        builder.field("type", "percolator");
        builder.endObject();

        builder.endObject();
        //end properties

        builder.endObject();
        return builder;
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

        builder.startObject("modeType");
        builder.field("type", "integer");
        builder.endObject();

        builder.startObject("seriesText");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();

        builder.startObject("query");
        builder.field("type", "percolator");
        builder.endObject();

        builder.startObject("accurateQuery");
        builder.field("type", "percolator");
        builder.endObject();

        builder.startObject("search_bs");
        builder.field("type", "text");
        builder.field("analyzer", "index_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.field("store", false);
        builder.endObject();

        builder.startObject("search_bfs");
        builder.field("type", "text");
        builder.field("analyzer", "index_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.field("store", false);
        builder.endObject();

        builder.startObject("weight");
        builder.field("type", "integer");
        builder.endObject();

        builder.startObject("sxbCount");
        builder.field("type", "integer");
        builder.endObject();

        builder.endObject();
        //end properties

        builder.endObject();
        return builder;
    }

    @Override
    protected void create() throws Exception {
    	CreateIndexRequest brandRequest = new CreateIndexRequest(IndexAttributes.SXB_BRAND);
    	brandRequest.mapping(this.brandMapping());
        CreateIndexResponse brandResponse = client.indices().create(brandRequest, RequestOptions.DEFAULT);
        if (!brandResponse.isAcknowledged()) {
            throw new Exception("创建" + IndexAttributes.SXB_BRAND + "索引失败");
        }
    	
        CreateIndexRequest seriesRequest = new CreateIndexRequest(IndexAttributes.SXB_SERIES);
        seriesRequest.mapping(this.mapping());
        CreateIndexResponse seriesResponse = client.indices().create(seriesRequest, RequestOptions.DEFAULT);
        if (!seriesResponse.isAcknowledged()) {
            throw new Exception("创建" + IndexAttributes.SXB_SERIES + "索引失败");
        }

        CreateIndexRequest aliasRequest = new CreateIndexRequest(IndexAttributes.SXB_SERIES_ALIAS);
        aliasRequest.mapping(this.aliasMapping());
        CreateIndexResponse aliasResponse = client.indices().create(aliasRequest, RequestOptions.DEFAULT);
        if (!aliasResponse.isAcknowledged()) {
            throw new Exception("创建" + IndexAttributes.SXB_SERIES_ALIAS + "索引失败");
        }
    }

    @Override
    protected void bulk() throws Exception {
        //品牌
    	List<Brand> brands = BrandMapper.getBrands();
    	BulkRequest brandRequest = new BulkRequest();
    	for(Brand brand : brands) {
    		Map<String, Object> brandMap = new HashMap<>();
    		brandMap.put("name", brand.getName());
    		brandMap.put("image", brand.getImage());

    		//反向查询，需要
    		String nameText = brand.getName().replace("汽车", "");
    		if(nameText.equals("SWM斯威")) {
                nameText = "斯威";
            }else if(nameText.equals("广汽传祺")) {
                nameText = "传祺";
            }
            brandMap.put("nameText", nameText);

            Map<String, Object> query = new HashMap<>();
            Map<String, Object> matchPhrase = new HashMap<>();
            Map<String, Object> nameTextQuery = new HashMap<>();
            nameTextQuery.put("query", nameText);
            nameTextQuery.put("slop", 1);
            matchPhrase.put("nameText", nameTextQuery);
            query.put("match_phrase", matchPhrase);
            brandMap.put("query", query);

    		IndexRequest indexRequest = new IndexRequest(IndexAttributes.SXB_BRAND);
    		indexRequest.id(brand.getId().toString());
            indexRequest.source(brandMap);
            brandRequest.add(indexRequest);
    	}
    	BulkResponse brandResponse = client.bulk(brandRequest, RequestOptions.DEFAULT);
    	if (brandResponse.hasFailures()) {
            throw new Exception(IndexAttributes.SXB_BRAND + "索引数据失败");
        } else {
            logger.info(IndexAttributes.SXB_BRAND + "索引" + brandRequest.numberOfActions() + "条数据");
        }

        //车系
        List<Map<String, Object>> list = carsriesMapper.getAllForIndex();
        int len = list.size();
        int pageCount = (len - 1) / 1000 + 1;
        Map<Integer, BulkRequest> pageMap = new HashMap<>();
        for (int i = 1; i <= pageCount; i++) {
            BulkRequest request = new BulkRequest();
            pageMap.put(i, request);
        }
        List<Map<String, Object>> seriesAlias = new ArrayList<>();
        for (int i = 1; i <= len; i++) {
            Map<String, Object> seriesMap = list.get(i - 1);
            Integer seriesId = (Integer) seriesMap.get("ID");
            IndexRequest indexRequest = new IndexRequest(IndexAttributes.SXB_SERIES);
            indexRequest.id(seriesId.toString());
            indexRequest.source(this.getSource(seriesMap, seriesAlias));

            int page = (i - 1) / 1000 + 1;
            BulkRequest bulkRequest = pageMap.get(page);
            bulkRequest.add(indexRequest);
        }
        for (BulkRequest bulkRequest : pageMap.values()) {
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                throw new Exception(IndexAttributes.SXB_SERIES + "索引数据失败");
            } else {
                logger.info(IndexAttributes.SXB_SERIES + "索引" + bulkRequest.numberOfActions() + "条数据");
            }
        }
        logger.info(IndexAttributes.SXB_SERIES + "总共索引" + len + "条数据");

        //车系别名
        if(seriesAlias.size() > 0) {
            BulkRequest aliasRequest = new BulkRequest();
            for (Map<String, Object> alias : seriesAlias) {
                IndexRequest indexRequest = new IndexRequest(IndexAttributes.SXB_SERIES_ALIAS);
                indexRequest.source(alias);
                aliasRequest.add(indexRequest);
            }
            BulkResponse aliasResponse = client.bulk(aliasRequest, RequestOptions.DEFAULT);
            if (aliasResponse.hasFailures()) {
                throw new Exception(IndexAttributes.SXB_SERIES_ALIAS + "索引数据失败");
            } else {
                logger.info(IndexAttributes.SXB_SERIES_ALIAS + "索引" + aliasRequest.numberOfActions() + "条数据");
            }
        }
    }

    private Map<String, Object> getSource(Map<String, Object> seriesMap, List<Map<String, Object>> seriesAlias) throws Exception {
        String brand = (String) seriesMap.get("BRAND");
        String factoryName = (String) seriesMap.get("FACTORYNAME");
        String series = (String) seriesMap.get("SERIES");
        Integer modeType = (Integer) seriesMap.get("MODETYPE");

        Map<String, Object> source = new HashMap<>();
        source.put("brand", brand);
        source.put("factoryName", factoryName);
        source.put("series", series);
        source.put("modeType", modeType);

        int weight = this.getWeight(modeType);
        source.put("weight", weight);

        //按车系的热度设置权重，这里没有热度数据，先用weight代替
        source.put("sxbCount", weight);

        List<Map<String, Object>> seriesNames = new ArrayList<>();
        if(series.contains("-")) {
            Pattern pattern = Pattern.compile("([a-zA-Z]+-[a-zA-Z]+)");
            Matcher matcher = pattern.matcher(series);
            while(matcher.find()){
                String aliase = matcher.group(1).replace("-", "");
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", aliase);
                seriesNames.add(seriesName);
            }
        }

        if(brand.equals("宝马")) {
            if(series.equals("宝马3系") && modeType == 1) {
                String[] aliases = {"320i", "320Li", "325i", "325Li", "330i", "330Li"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("宝马5系") && modeType == 1) {
                String[] aliases = {"525Li", "530Li"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("宝马5系(进口)") && modeType == 2) {
                String[] aliases = {"525i", "530i", "540i"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("宝马5系新能源") && modeType == 1) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "530Le");
                seriesNames.add(seriesName);
            }else if(series.equals("宝马7系") && modeType == 2) {
                String[] aliases = {"730Li", "740Li", "750Li", "760Li"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("宝马4系") && modeType == 2) {
                String[] aliases = {"425i", "430i"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }
        }else if(brand.equals("奔驰")) {
            if(series.equals("奔驰A级") && modeType == 1) {
                String[] aliases = {"A180L", "A200L", "A220L"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("奔驰C级") && modeType == 1) {
                String[] aliases = {"C200L", "C260L", "C300L", "C260"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("奔驰E级") && modeType == 1) {
                String[] aliases = {"E260L", "E300L", "E350L"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("奔驰E级(进口)") && modeType == 2) {
                String[] aliases = {"E260", "E300", "E350"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("奔驰E级新能源") && modeType == 1) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "E300eL");
                seriesNames.add(seriesName);
            }else if(series.equals("奔驰S级") && modeType == 2) {
                String[] aliases = {"S320L", "S350L", "S450L", "S500L"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.contains("GLA")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "GLA");
                seriesNames.add(seriesName);
            }else if(series.contains("GLB")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "GLB");
                seriesNames.add(seriesName);
            }else if(series.contains("GLC")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "GLC");
                seriesNames.add(seriesName);
            }else if(series.contains("GLE")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "GLE");
                seriesNames.add(seriesName);
            }else if(series.contains("GLS")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "GLS");
                seriesNames.add(seriesName);
            }
        }else if(brand.equals("雷克萨斯")) {
            if(series.equals("雷克萨斯ES")) {
                String[] aliases = {"ES200", "ES260", "ES300h"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("雷克萨斯RX")) {
                String[] aliases = {"RX300", "RX450h"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("雷克萨斯NX")) {
                String[] aliases = {"NX200", "NX300h"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("雷克萨斯LX")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "LX570");
                seriesNames.add(seriesName);
            }else if(series.equals("雷克萨斯LS")) {
                String[] aliases = {"LS350", "LS500h"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("雷克萨斯UX")) {
                String[] aliases = {"UX200", "UX260h", "UX300e"};
                for(String aliase : aliases) {
                    Map<String, Object> seriesName = new HashMap<>();
                    seriesName.put("alias", aliase);
                    seriesNames.add(seriesName);
                }
            }else if(series.equals("雷克萨斯IS")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "IS300");
                seriesNames.add(seriesName);
            }else if(series.equals("雷克萨斯CT")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "CT200h");
                seriesNames.add(seriesName);
            }else if(series.equals("雷克萨斯LM")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "LM300h");
                seriesNames.add(seriesName);
            }else if(series.equals("雷克萨斯RC")) {
                Map<String, Object> seriesName = new HashMap<>();
                seriesName.put("alias", "RC300");
                seriesNames.add(seriesName);
            }
        }

        if(seriesNames.size() > 0) {
            for(Map<String, Object> seriesName : seriesNames) {
                Map<String, Object> query = new HashMap<>();
                Map<String, Object> match = new HashMap<>();
                match.put("alias", seriesName.get("alias"));
                query.put("match_phrase", match);
                seriesName.put("query", query);

                seriesName.put("brand", brand);
                seriesName.put("factoryName", factoryName);
                seriesName.put("series", series);
                seriesName.put("modeType", modeType);
                seriesName.put("weight", source.get("weight"));
            }
            seriesAlias.addAll(seriesNames);
        }

        List<String> analyze = this.analyze(series);
        analyze.removeAll(CarColorCollection.outColorLst2);//排除颜色
        if(analyze.contains("汽车")) {//排除 汽车 车
            analyze.remove("汽车");
        }else if(analyze.contains("车")) {
            analyze.remove("车");
        }
        Set<String> set = new LinkedHashSet<>(analyze);
        String seriesText = String.join(" ", set);
        source.put("seriesText", seriesText);

        //判断是否有连续的字母或数字，提升打分
        List<String> list = new ArrayList<>(set);
        List<String> copy = new ArrayList<>();
        List<String> phrase = new ArrayList<>();
        for(int i = 0,len = list.size(); i < len; i++) {
            String term = list.get(i);
            boolean m = Pattern.matches("[A-Za-z]+|[0-9]+", term);
            if(m) {
                copy.add(term);
                if(i > 0) {
                    if(copy.get(i - 1).equals("-")) {
                        phrase.clear();
                    }
                }
                phrase.add(term);
            }else{
                copy.add("-");
            }
        }

        Map<String, Object> query = new HashMap<>();
        Map<String, Object> match = new HashMap<>();
        Map<String, Object> seriesTextQuery1 = new HashMap<>();
        seriesTextQuery1.put("query", seriesText);
        seriesTextQuery1.put("minimum_should_match", "2<2");
        match.put("seriesText", seriesTextQuery1);
        if(phrase.size() > 1) {
            Map<String, Object> seriesTextQuery = new HashMap<>();
            seriesTextQuery.put("query", String.join(" ", phrase));
            seriesTextQuery.put("slop", 1);
            Map<String, Object> matchPhrase = new HashMap<>();
            matchPhrase.put("seriesText", seriesTextQuery);
            Map<String, Object> Object1 = new HashMap<>();
            Object1.put("match_phrase", matchPhrase);
            List<Map<String, Object>> should = new ArrayList<>();
            should.add(Object1);

            Map<String, Object> Object2 = new HashMap<>();
            Object2.put("match", match);
            List<Map<String, Object>> must = new ArrayList<>();
            must.add(Object2);

            Map<String, Object> bool = new HashMap<>();
            bool.put("must", must);
            bool.put("should", should);
            query.put("bool", bool);
        }else{
            query.put("match", match);
        }
        source.put("query", query);

        Map<String, Object> accurateQuery = new HashMap<>();
        Map<String, Object> accurateMatch = new HashMap<>();
        Map<String, Object> seriesTextQuery2 = new HashMap<>();
        seriesTextQuery2.put("query", seriesText);
        seriesTextQuery2.put("minimum_should_match", "4<4");
        accurateMatch.put("seriesText", seriesTextQuery2);
        accurateQuery.put("match", accurateMatch);
        source.put("accurateQuery", accurateQuery);

        StringBuilder bsBuilder = new StringBuilder();
        bsBuilder.append(brand);
        this.appendFilterString(bsBuilder, this.seriesFilterFactory(brand, series, factoryName));
        this.handleImportCar(bsBuilder, modeType);
        source.put("search_bs", bsBuilder.toString());

        StringBuilder bfsBuilder = new StringBuilder();
        bfsBuilder.append(brand);
        this.appendFilterString(bfsBuilder, factoryName);
        this.appendFilterString(bfsBuilder, series);
        source.put("search_bfs", bfsBuilder.toString());

        return source;
    }

    /**
     * 给进口车添加关键字，方便检索，因为“进口厂家”没有办法搜索
     *
     * @param builder
     * @param modeType
     */
    private void handleImportCar(StringBuilder builder, int modeType) {
        if (modeType == 1) {
            return;
        }

        this.appendString(builder, this.convertExpandModeType(modeType));
        this.appendString(builder, "进口");
    }

    /**
     * 从车系中过滤厂家，防止想搜厂家，反而搜出了车系
     *
     * @param brand
     * @return
     */
    private String seriesFilterFactory(String brand, String series, String factoryName) {
        if (StringUtils.hasLength(factoryName)
                && series.contains(factoryName)
                && !brand.equals(factoryName)) {
            String p = "^[\u4e00-\u9fa5a-zA-Z]+$";
            boolean m = Pattern.matches(p, factoryName);
            if (m) {//字母和中文混合的厂家，单独处理
                factoryName = factoryName.replaceAll("[a-zA-Z]+", "");
            }
            return series.replace(factoryName, "");
        }

        return series;
    }

}
