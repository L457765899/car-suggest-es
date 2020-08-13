package com.sxb.lin.es.suggest.db.dao;

import com.sxb.lin.es.suggest.db.entity.Carcategory;
import tk.mybatis.mapper.common.BaseMapper;

import java.util.List;
import java.util.Map;

public interface CarcategoryMapper extends BaseMapper<Carcategory> {

    /**
     * 获取车系的所有官方报价
     *
     * @param id
     * @return
     */
    List<Integer> getOfficialQuoteBySeriesId(int id);

    /**
     * 获取车型颜色
     *
     * @param id
     * @return
     */
    String selectColorById(int id);

    /**
     * 获取所有车型用于es索引
     *
     * @return
     */
    List<Map<String, Object>> getAllForIndex();
}