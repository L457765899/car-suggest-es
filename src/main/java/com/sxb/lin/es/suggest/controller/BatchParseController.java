package com.sxb.lin.es.suggest.controller;

import com.sxb.lin.es.suggest.service.BatchParseService;
import com.sxb.lin.es.suggest.util.RetUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/batchParse")
public class BatchParseController {

    @Resource(name = "batchParseESService")
    private BatchParseService batchParseESService;

    /**
     * 索引批量解析索引
     * @return
     * @throws Exception
     */
    @RequestMapping("/indexParse.json")
    public Map<String, Object> indexParse() throws Exception{
        batchParseESService.index();
        return RetUtil.getRetValue(true);
    }

    /**
     * 车型术语批量解析
     * @param cars
     * @return
     * @throws Exception
     */
    @RequestMapping("/batchParse.json")
    public Map<String,Object> batchParse(String cars) throws Exception{
        return RetUtil.getRetValue(batchParseESService.batchParse(cars));
    }

    /**
     * 识别语句中的品牌
     * @param cars
     * @return
     */
    @RequestMapping("/discernBrand.json")
    public Map<String,Object> discernBrand(String cars) throws Exception {
        return RetUtil.getRetValue(batchParseESService.discernBrand(cars));
    }
}
