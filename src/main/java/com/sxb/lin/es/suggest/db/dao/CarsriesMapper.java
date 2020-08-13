package com.sxb.lin.es.suggest.db.dao;

import com.sxb.lin.es.suggest.db.entity.Carsries;
import tk.mybatis.mapper.common.BaseMapper;

import java.util.List;
import java.util.Map;

public interface CarsriesMapper extends BaseMapper<Carsries> {

    /**
     * 获取所有车系用于es索引
     *
     * @return
     */
    List<Map<String, Object>> getAllForIndex();
}