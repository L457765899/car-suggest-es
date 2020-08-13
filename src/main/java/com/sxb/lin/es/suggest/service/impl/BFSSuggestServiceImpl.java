package com.sxb.lin.es.suggest.service.impl;

import com.sxb.lin.es.suggest.config.IndexAttributes;
import com.sxb.lin.es.suggest.db.dao.CarcategoryMapper;
import com.sxb.lin.es.suggest.dto.Area;
import com.sxb.lin.es.suggest.dto.Key;
import com.sxb.lin.es.suggest.dto.SearchField;
import com.sxb.lin.es.suggest.service.SuggestService;
import com.sxb.lin.es.suggest.util.RetUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;


@Service("bFSSuggestService")
public class BFSSuggestServiceImpl extends AbstractSuggestServiceImpl implements SuggestService {

    @Resource(name = "bFSCSuggestService")
    private SuggestService suggestService;

    @Autowired
    private CarcategoryMapper carcategoryMapper;

    @Override
    protected SearchField getSearchField() {
        SearchField searchField = new SearchField();
        searchField.setIndex(IndexAttributes.SXB_SERIES);
        searchField.setField("search_bfs");
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
            firstData.put("factoryName", mapData.get("factoryName"));
            firstData.put("series", mapData.get("series"));
            firstData.put("modeType", modeType);
            firstData.put("match", 1);
            firstData.put("show", mapData.get("factoryName") + " " + mapData.get("series") 
            	+ this.convertModeType(modeType, null));
            resultData.add(firstData);

            //如果品牌工厂车系只有一个结果，需要将报价集合追加到后面
            List<Integer> officialQuotes = carcategoryMapper.getOfficialQuoteBySeriesId(
            		Integer.parseInt(searchHitsArray[0].getId()));
            List<Integer> exist = new ArrayList<>();
            for (Integer officialQuote : officialQuotes) {
                if (officialQuote != null && officialQuote > 0
                		&& !exist.contains(officialQuote)) {
                    exist.add(officialQuote);
                    Map<String, Object> data = new HashMap<>();
                    data.put("brand", mapData.get("brand"));
                    data.put("factoryName", mapData.get("factoryName"));
                    data.put("series", mapData.get("series"));
                    data.put("officialQuote", officialQuote);
                    data.put("modeType", modeType);
                    data.put("match", 1);
                    data.put("show", mapData.get("factoryName") + " " + mapData.get("series")
                            + this.convertModeType(modeType, null) + " " + (officialQuote / 100));
                    resultData.add(data);
                }
            }
        } else {
            Set<String> factoryNameSet = new LinkedHashSet<>();
            for (SearchHit searchHit : searchHitsArray) {
                Map<String, Object> mapData = searchHit.getSourceAsMap();
                int modeType = Integer.parseInt(mapData.get("modeType").toString());
                String factoryName = (String) mapData.get("factoryName");
                if (factoryName.replace("-", "").contains(key.getKey().replace("-", ""))) {
                    factoryNameSet.add(factoryName);
                } else if (key.getKey().replace("-", "").contains(factoryName.replace("-", ""))) {
                    factoryNameSet.add(factoryName);
                }
                Map<String, Object> data = new HashMap<>();
                data.put("brand", mapData.get("brand"));
                data.put("factoryName", factoryName);
                data.put("series", mapData.get("series"));
                data.put("modeType", modeType);
                data.put("match", 1);
                data.put("show", factoryName + " " + mapData.get("series") + this.convertModeType(modeType, null));
                resultData.add(data);
            }

            if (factoryNameSet.size() > 0) {
                List<Map<String, Object>> factoryNameDataList = new ArrayList<>();
                for (String factoryName : factoryNameSet) {
                    Map<String, Object> brandData = new HashMap<>();
                    brandData.put("factoryName", factoryName);
                    brandData.put("show", factoryName);
                    factoryNameDataList.add(brandData);
                }
                factoryNameDataList.addAll(resultData);
                resultData = factoryNameDataList;
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
        // do nothing
    }

    @Override
    protected XContentBuilder mapping() throws Exception {
        // do nothing
        return null;
    }

    @Override
    protected void create() throws Exception {
        // do nothing
    }

    @Override
    protected void bulk() throws Exception {
        // do nothing
    }

}
