package com.sxb.lin.es.suggest.dto;

import java.util.List;

public class Key {

    private String key;
    
    private List<String> terms;
    
    public Key() {
        
    }

    public Key(String key, List<String> terms) {
        this.key = key;
        this.terms = terms;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getTerms() {
        return terms;
    }

    public void setTerms(List<String> terms) {
        this.terms = terms;
    }
    
    
}
