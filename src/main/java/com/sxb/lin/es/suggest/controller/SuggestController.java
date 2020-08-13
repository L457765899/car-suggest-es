package com.sxb.lin.es.suggest.controller;

import com.sxb.lin.es.suggest.dto.Key;
import com.sxb.lin.es.suggest.service.SuggestService;
import com.sxb.lin.es.suggest.util.RetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tk.mybatis.mapper.util.StringUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Map;

@RestController
@RequestMapping("/suggest")
public class SuggestController {

    private Logger logger = LoggerFactory.getLogger(SuggestController.class);

    @Resource(name = "aSuggestService")
    private SuggestService suggestService;

    @RequestMapping("/indexSuggest.json")
    public Map<String,Object> indexSuggest() throws Exception{
        suggestService.indexSuggest();
        return RetUtil.getRetValue(true);
    }

    @RequestMapping("/searchSuggestWeb.json")
    public Map<String,Object> searchSuggestWeb(String key) throws Exception{
        logger.info("key:" + key);
        if(StringUtils.hasText(key) && key.length() >= 2){
            return suggestService.searchSuggest(new Key(key, null),null);
        }
        return RetUtil.getRetValue(true,new ArrayList<>());
    }

}
