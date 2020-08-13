package com.sxb.lin.es.suggest.service.impl;

import com.sxb.lin.es.suggest.db.dao.BrandMapper;
import com.sxb.lin.es.suggest.db.entity.Brand;
import com.sxb.lin.es.suggest.service.BrandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("brandService")
public class BrandServiceImpl implements BrandService {

    @Autowired
    private BrandMapper BrandMapper;


    @Override
    public List<Brand> getBrands() {
        return BrandMapper.getBrands();
    }
}
