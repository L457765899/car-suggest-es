package com.sxb.lin.es.suggest.dto.parse;

import java.io.Serializable;
import java.util.Set;

public class DiscernBySeries implements Serializable {

    private String by;

    private Set<String> set;

    public String getBy() {
        return by;
    }

    public void setBy(String by) {
        this.by = by;
    }

    public Set<String> getSet() {
        return set;
    }

    public void setSet(Set<String> set) {
        this.set = set;
    }
}
