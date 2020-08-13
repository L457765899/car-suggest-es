package com.sxb.lin.es.suggest.db.dao;

import com.sxb.lin.es.suggest.db.entity.Brand;
import tk.mybatis.mapper.common.BaseMapper;

import java.util.List;

public interface BrandMapper extends BaseMapper<Brand> {

    /**
     * 获取所有品牌
     *
     * @return
     */
    List<Brand> getBrands();
}