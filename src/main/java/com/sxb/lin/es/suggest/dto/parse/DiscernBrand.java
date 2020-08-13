package com.sxb.lin.es.suggest.dto.parse;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

public class DiscernBrand implements Serializable {

    private Set<String> brands;

    private Set<String> discernByBrand;

    private Map<String, Set<String>> discernByAlias;

    private Map<String, DiscernBySeries> discernBySeries;

    public Set<String> getBrands() {
        return brands;
    }

    public void setBrands(Set<String> brands) {
        this.brands = brands;
    }

    public Set<String> getDiscernByBrand() {
        return discernByBrand;
    }

    public void setDiscernByBrand(Set<String> discernByBrand) {
        this.discernByBrand = discernByBrand;
    }

    public Map<String, Set<String>> getDiscernByAlias() {
        return discernByAlias;
    }

    public void setDiscernByAlias(Map<String, Set<String>> discernByAlias) {
        this.discernByAlias = discernByAlias;
    }

    public Map<String, DiscernBySeries> getDiscernBySeries() {
        return discernBySeries;
    }

    public void setDiscernBySeries(Map<String, DiscernBySeries> discernBySeries) {
        this.discernBySeries = discernBySeries;
    }
}
