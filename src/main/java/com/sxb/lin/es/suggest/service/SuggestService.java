package com.sxb.lin.es.suggest.service;


import com.sxb.lin.es.suggest.dto.Area;
import com.sxb.lin.es.suggest.dto.Key;

import java.util.List;
import java.util.Map;

public interface SuggestService {
	
	final static int GROUP_SIZE = 200;

    void indexSuggest() throws Exception ;

    Map<String, Object> searchSuggest(Key key, Area area) throws Exception;

    StringBuilder appendString(StringBuilder builder, String str);

    StringBuilder appendFilterString(StringBuilder builder, String str);

    String getYear(String category);

    void handleImportCar(StringBuilder builder, int modeType, String factoryName);

    List<String> analyze(String key) throws Exception;
    
}
