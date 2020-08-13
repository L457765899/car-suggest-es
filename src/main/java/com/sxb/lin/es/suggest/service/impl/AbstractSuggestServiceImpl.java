package com.sxb.lin.es.suggest.service.impl;

import com.sxb.lin.es.suggest.dto.Area;
import com.sxb.lin.es.suggest.dto.Key;
import com.sxb.lin.es.suggest.dto.SearchField;
import com.sxb.lin.es.suggest.service.SuggestService;
import com.sxb.lin.es.suggest.util.RetUtil;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.AnalyzeRequest;
import org.elasticsearch.client.indices.AnalyzeResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractSuggestServiceImpl implements SuggestService {

    protected static final Logger logger = LoggerFactory.getLogger(AbstractSuggestServiceImpl.class);

    //public static final String[] SPECIAL_PART_AREA = {"北京", "天津", "郑州", "吉林", "南京", "四川", "苏州", "江西", "陕西"};

    public static final int DEFAULT_DATAS_SIZE = 100;
    
    @Autowired
    protected RestHighLevelClient client;

    protected abstract SearchField getSearchField();

    protected abstract Map<String, Object> handleResult(SearchResponse searchResponse, Key key, Area area);

    public abstract SuggestService getSuggestService();

    @Override
    public void indexSuggest() throws Exception {

        this.doIndexSuggest();

        SuggestService suggestService = this.getSuggestService();
        if (suggestService != null) {
            suggestService.indexSuggest();
        }
    }

    protected void doIndexSuggest() throws Exception {
        this.delete();
        this.create();
        this.bulk();
    }

    protected abstract void delete() throws Exception;

    protected abstract XContentBuilder mapping() throws Exception;

    protected abstract void create() throws Exception;

    protected abstract void bulk() throws Exception;

    protected int getWeight(int modeType) {
        if (modeType == 2 || modeType == 3) {
            return 10;
        } else if (modeType > 3) {
            return 0;
        }
        return 20;
    }
    
    @Override
    public List<String> analyze(String key) throws Exception {
        AnalyzeRequest analyzeRequest = AnalyzeRequest.withGlobalAnalyzer("query_ansj", key);
        AnalyzeResponse analyzeResponse = client.indices().analyze(analyzeRequest, RequestOptions.DEFAULT);
        List<AnalyzeResponse.AnalyzeToken> tokens = analyzeResponse.getTokens();
        List<String> terms = new ArrayList<>();
        for(AnalyzeResponse.AnalyzeToken token : tokens){
            terms.add(token.getTerm());
        }
    	return terms;
    }
    
    protected SearchResponse search(SearchSourceBuilder searchSourceBuilder) throws Exception {
    	SearchField field = this.getSearchField();
    	SearchRequest searchRequest = new SearchRequest(field.getIndex());
        searchRequest.source(searchSourceBuilder);
        return client.search(searchRequest, RequestOptions.DEFAULT);
    }

    @Override
    public Map<String, Object> searchSuggest(Key key, Area area) throws Exception {

    	SearchField field = this.getSearchField();
        SearchSourceBuilder searchSourceBuilder = this.getSearchSourceBuilder(key, area, field);
        SearchResponse searchResponse = this.search(searchSourceBuilder);
        if (searchResponse.getHits().getTotalHits().value > 0) {
            return this.signature(this.handleResult(searchResponse, key, area));
        }

        SuggestService suggestService = this.getSuggestService();
        if (suggestService != null) {
            return suggestService.searchSuggest(key, area);
        } else {
            List<Map<String, Object>> datas = new ArrayList<>();
            if (area != null) {
                Map<String, Object> data = new HashMap<>();
                data.put("areaIdPath", area.getAreaIdPath());
                data.put("show", area.getArea());
                datas.add(data);
            }
            return this.signature(RetUtil.getRetValue(true, datas));
        }

    }

    protected QueryBuilder getQueryBuilder(Key key, Area area, String field) {
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        List<String> terms = key.getTerms();
        int len = terms.size();
        if (area == null) {
            for (int i = 0; i < len; i++) {
                String term = terms.get(i);
                //最后一个词联想补全
                if(i == len - 1){
                    boolQuery.must(QueryBuilders.prefixQuery(field, term));
                    boolQuery.should(QueryBuilders.termQuery(field, term));//增加评分
                }else{
                	//数字联想补全
                    //if (StringUtils.isNumeric(term)) {
                    //    boolQuery.must(QueryBuilders.prefixQuery(field, term));
                    //} else {
                    //    boolQuery.must(QueryBuilders.termQuery(field, term));
                    //}
                    
                    boolQuery.must(QueryBuilders.termQuery(field, term));
                }
            }
        } else {
            List<String> mustTerms = new ArrayList<>();
            for (String term : terms) {
                if (term.length() > 1 && area.getArea().contains(term)) {
                    //#1
                    //if(ArrayUtils.contains(SPECIAL_PART_AREA, term)) {
                    //	boolQuery.should(QueryBuilders.termQuery(field, term));
                    //}

                    //#2
                    boolQuery.should(QueryBuilders.termQuery(field, term));
                } else {
                    mustTerms.add(term);
                }
            }
            for (int i = 0, ll = mustTerms.size(); i < ll; i++) {
                String term = mustTerms.get(i);

                //最后一个词联想补全
                if(i == ll - 1){
                    boolQuery.must(QueryBuilders.prefixQuery(field, term));
                    boolQuery.should(QueryBuilders.termQuery(field, term));//增加评分
                }else{
                	//数字联想补全
                    //if (StringUtils.isNumeric(term)) {
                    //    boolQuery.must(QueryBuilders.prefixQuery(field, term));
                    //} else {
                    //    boolQuery.must(QueryBuilders.termQuery(field, term));
                    //}
                	
                	boolQuery.must(QueryBuilders.termQuery(field, term));
                }
            }
            //#1
            //if(!boolQuery.hasClauses()) {
            //	for(String term : terms) {
            //		boolQuery.must(QueryBuilders.termQuery(field, term));
            //	}
            //}
        }
        return boolQuery;
    }

    protected SearchSourceBuilder getSearchSourceBuilder(Key key, Area area, SearchField field) {
        QueryBuilder queryBuilder = this.getQueryBuilder(key, area, field.getField());
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(queryBuilder);
        searchSourceBuilder.fetchSource(field.getIncludeFields(), null);
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(500);
        searchSourceBuilder.sort("weight", SortOrder.DESC);
        searchSourceBuilder.sort("_score");
        return searchSourceBuilder;
    }

    protected Map<String, Object> signature(Map<String, Object> resMap) {
        resMap.put("signature", this.getClass().getName());
        return resMap;
    }

    protected List<Map<String, Object>> subDatas(
    		List<Map<String, Object>> datas, String key, Area area) {
        int size = Math.min(datas.size(), DEFAULT_DATAS_SIZE);
        List<Map<String, Object>> subData = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> first = null;
            if (i == 0) {
                first = new HashMap<>();
            }
            Map<String, Object> data = datas.get(i);
            this.appendArea(data, key, area, first, subData);
            subData.add(data);
        }
        return subData;
    }

    private boolean appendArea(Map<String, Object> data, String key, Area area,
        Map<String, Object> first, List<Map<String, Object>> subDatas) {
        if (area != null) {
            if (area.getArea().equals("九龙")) {
                return false;
            }
            //特殊处理（品牌+厂家+车系）包含地名的情况
            String show = data.get("show").toString();
            String p1 = ".*(北京).*|.*(天津).*|.*(郑州).*|.*(吉林).*|.*(南京).*|.*(四川).*|.*(苏州).*|.*(江西).*|.*(陕西).*";
            Pattern pattern = Pattern.compile(p1);
            Matcher m = pattern.matcher(show);
            if (m.find()) {
                String p2 = ".*北京市.*|.*天津市.*|.*郑州市.*|.*吉林省.*|.*南京市.*|.*四川省.*|.*苏州市.*|.*江西省.*|.*陕西省.*";
                if (Pattern.matches(p2, key)) {
                    if (key.length() != 3) {
                        data.put("areaIdPath", area.getAreaIdPath());
                        data.put("show", data.get("show") + " " + area.getArea());
                        return true;
                    }
                    if (first != null) {
                        first.put("areaIdPath", area.getAreaIdPath());
                        first.put("show", area.getArea());
                        subDatas.add(first);
                    }
                } else {
                    boolean isShow = false;
                    for (int i = 1; i <= m.groupCount(); i++) {
                        String group = m.group(i);
                        if (group != null && !area.getArea().contains(group)) {
                            isShow = true;
                            break;
                        }
                    }
                    if (isShow) {
                        data.put("areaIdPath", area.getAreaIdPath());
                        data.put("show", data.get("show") + " " + area.getArea());
                        return true;
                    } else {
                        if (first != null) {
                            first.put("areaIdPath", area.getAreaIdPath());
                            first.put("show", area.getArea());
                            subDatas.add(first);
                        }
                    }
                }
            } else {
            	if (first != null) {
                    first.put("areaIdPath", area.getAreaIdPath());
                    first.put("show", area.getArea());
                    subDatas.add(first);
                }
                data.put("areaIdPath", area.getAreaIdPath());
                data.put("show", data.get("show") + " " + area.getArea());
                return true;
            }
        }
        return false;
    }


    @PostConstruct
    protected void init() throws IOException {
        this.initAfter();
    }


    protected void initAfter() throws IOException {

    }

    @PreDestroy
    protected void destroy() {
        this.destroyAfter();
    }

    protected void destroyAfter() {

    }

    protected String convertModeType(Integer modeType, Object series) {
        String text = "";
        if (modeType == null) {
            return text;
        }
        switch (modeType) {
        	case 2:
            case 3:
                if(series != null && !series.toString().contains("进口")) {
            		text = "(进口)";
            	}
        		break;
            case 4:
                text = "(美版)";
                break;
            case 6:
                text = "(中东)";
                break;
            case 8:
                text = "(加版)";
                break;
            case 10:
                text = "(欧版)";
                break;
            case 12:
                text = "(墨西哥版)";
                break;
            default:
                text = "";
                break;
        }
        return text;
    }

    protected String convertExpandModeType(int modeType) {
        String text = "";
        switch (modeType) {
            case 1:
                text = "国产";
                break;
            case 2:
            case 3:
                text = "中规";
                break;
            case 4:
                text = "美版" + " " + "美规";
                break;
            case 6:
                text = "中东";
                break;
            case 8:
                text = "加版";
                break;
            case 10:
                text = "欧版" + " " + "欧规";
                break;
            case 12:
                text = "墨西哥版";
                break;
            default:
                break;
        }
        return text;
    }

    @Override
    public StringBuilder appendString(StringBuilder builder, String str) {
    	if (StringUtils.isEmpty(str)) {
    		return builder;
    	}
        builder.append(" ");
        builder.append(str);
        return builder;
    }
    
    @Override
    public StringBuilder appendFilterString(StringBuilder builder,String str){
    	if (StringUtils.isEmpty(str)) {
    		return builder;
    	}
        this.appendString(builder, str);
        if (str.contains("-")) {
            builder.append(" ");
            builder.append(str.replace("-", ""));
        }
        return builder;
    }

    /**
     * 有些平行进口车，厂家字段上没有“平行进口”
     * 给车系加上规格
     * @param builder
     * @param modeType
     * @param factoryName
     */
    @Override
    public void handleImportCar(StringBuilder builder, int modeType, String factoryName) {
        if (modeType == 1) {
            return;
        }

        if (modeType > 3 && factoryName != null && !factoryName.contains("平行进口")) {
            this.appendString(builder, "平行进口");
        } else if (factoryName != null && !factoryName.contains("进口")) {
            this.appendString(builder, "进口");
        }
        this.appendString(builder, this.convertExpandModeType(modeType));
    }

    /**
     * 获取车型年份
     *
     * @param category
     * @return
     */
    @Override
    public String getYear(String category) {
        //boolean m = Pattern.matches("^2\\d\\d\\d.*", category);
        //if(m){
        //    return category.substring(0, 4);
        //}
        Pattern p = Pattern.compile(".*(\\d\\d)款.*");
        Matcher m = p.matcher(category);
        if (m.matches()) {
            String group = m.group(1);
            return group == null ? "" : group;
        }
        return "";
    }
    
    /**
     * 获取车系数字
     *
     * @param series
     * @return
     */
    protected String getNumber(String series) {
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(series);
        return m.replaceAll("").trim();
    }

    /**
     * 获取车型排量
     *
     * @param category
     * @return
     */
    protected String getDisplacement(String category) {
        String p = ".*(\\d\\.\\d).*";
        Pattern pa = Pattern.compile(p);
        Matcher m = pa.matcher(category);
        if (m.matches()) {
            String value = m.group(1);
            return value == null ? "" : value;
        }
        return "";
    }
    
    /**
     * 缩减车型，因为默认的名字太长
     * @param category
     * @return
     */
    protected String reduceCategory(int index, String category) {
        if(index > DEFAULT_DATAS_SIZE) {
            return category;
        }
        String reduceCategory = this.reduceCategory(category);
        if (reduceCategory.length() > 25) {
            reduceCategory = reduceCategory.substring(0, 25);
        }
        return reduceCategory;
    }

    private String reduceCategory(String category) {
        Pattern p = Pattern.compile("^\\d\\d(\\d\\d款).*");
        Matcher m = p.matcher(category);
        if(m.matches()){
            String group = m.group(1);
            category = category.replaceAll("\\d\\d\\d\\d款", group);
        }
        category = category.replace("年型", "").replace("型", "")
                .replace("TFSI", "").replace("tfsi", "")
                .replace("TDI", "").replace("tdi", "")
                .replace("TSI", "").replace("tsi", "")
                .replace("套装", "").replace("设计", "")
                .replace("EcoBoost", "").replace("版", "")
                .replace("quattro", "四驱").replace("xDrive", "四驱")
                .replace("Gran Coupe", "四门").replace("4MATIC", "四驱");

        return category;
    }
}
