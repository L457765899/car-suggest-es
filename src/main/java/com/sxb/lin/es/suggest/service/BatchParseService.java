package com.sxb.lin.es.suggest.service;

import com.sxb.lin.es.suggest.dto.parse.BatchParseBean;
import com.sxb.lin.es.suggest.dto.parse.DiscernBrand;

import java.util.List;

public interface BatchParseService {

    void index() throws Exception;

    List<BatchParseBean> batchParse(String cars) throws Exception;

    DiscernBrand discernBrand(String cars) throws Exception;
}
