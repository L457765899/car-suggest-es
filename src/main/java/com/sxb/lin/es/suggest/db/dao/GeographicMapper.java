package com.sxb.lin.es.suggest.db.dao;

import com.sxb.lin.es.suggest.db.entity.Geographic;
import tk.mybatis.mapper.common.BaseMapper;

import java.util.List;

public interface GeographicMapper extends BaseMapper<Geographic> {

    List<Geographic> getListByType(int type);
}