package com.sxb.lin.es.suggest.controller;

import com.sxb.lin.es.suggest.service.BrandService;
import com.sxb.lin.es.suggest.util.RetUtil;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/brand")
public class BrandController {

    @Resource(name = "brandService")
    private BrandService brandService;

    @RequestMapping("/getBrands.json")
    public Map<String, Object> getBrands() {
        return RetUtil.getRetValue(brandService.getBrands());
    }
}
