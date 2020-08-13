package com.sxb.lin.es.suggest.dto.parse;

import java.util.List;

public class SeriesBean {
	
	private Integer groupCount;
	
	private Long officialQuoteCount;

	private String carInfo;
	
	private Double maxScore;
	
	private CategoryBean categoryBean;
	
	private String series;
	
	private List<String> terms;
	
	private Integer newCarType;

	public String getSeries() {
		return series;
	}

	public void setSeries(String series) {
		this.series = series;
	}

	public String getCarInfo() {
		return carInfo;
	}

	public void setCarInfo(String carInfo) {
		this.carInfo = carInfo;
	}

	public Double getMaxScore() {
		return maxScore;
	}

	public void setMaxScore(Double maxScore) {
		this.maxScore = maxScore;
	}

	public Integer getGroupCount() {
		return groupCount;
	}

	public void setGroupCount(Integer groupCount) {
		this.groupCount = groupCount;
	}

	public Long getOfficialQuoteCount() {
		return officialQuoteCount;
	}

	public void setOfficialQuoteCount(Long officialQuoteCount) {
		this.officialQuoteCount = officialQuoteCount;
	}

	public CategoryBean getCategoryBean() {
		return categoryBean;
	}

	public void setCategoryBean(CategoryBean categoryBean) {
		this.categoryBean = categoryBean;
	}

	public List<String> getTerms() {
		return terms;
	}

	public void setTerms(List<String> terms) {
		this.terms = terms;
	}

	public Integer getNewCarType() {
		return newCarType;
	}

	public void setNewCarType(Integer newCarType) {
		this.newCarType = newCarType;
	}

}