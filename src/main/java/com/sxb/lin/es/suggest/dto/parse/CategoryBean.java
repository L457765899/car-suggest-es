package com.sxb.lin.es.suggest.dto.parse;

import java.util.List;

public class CategoryBean implements Cloneable {

	private Integer modeType;
	
	private String brand;
	
	private String series;
	
	private String factoryName;
	
	private String category;
	
	private Integer officialQuote;
	
	private List<String> officialQuotes;
	
	private String innerColor;
	
	private String outerColor;
	
	private String carLocation;
	
	private String carLocationIdPath;
	
	private MarketBean marketBean;
	
	private List<String> remarks;

	public Integer getModeType() {
		return modeType;
	}

	public void setModeType(Integer modeType) {
		this.modeType = modeType;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getSeries() {
		return series;
	}

	public void setSeries(String series) {
		this.series = series;
	}

	public String getFactoryName() {
		return factoryName;
	}

	public void setFactoryName(String factoryName) {
		this.factoryName = factoryName;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Integer getOfficialQuote() {
		return officialQuote;
	}

	public void setOfficialQuote(Integer officialQuote) {
		this.officialQuote = officialQuote;
	}

	public String getInnerColor() {
		return innerColor;
	}

	public void setInnerColor(String innerColor) {
		this.innerColor = innerColor;
	}

	public String getOuterColor() {
		return outerColor;
	}

	public void setOuterColor(String outerColor) {
		this.outerColor = outerColor;
	}

	public String getCarLocation() {
		return carLocation;
	}

	public void setCarLocation(String carLocation) {
		this.carLocation = carLocation;
	}

	public String getCarLocationIdPath() {
		return carLocationIdPath;
	}

	public void setCarLocationIdPath(String carLocationIdPath) {
		this.carLocationIdPath = carLocationIdPath;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public MarketBean getMarketBean() {
		return marketBean;
	}

	public void setMarketBean(MarketBean marketBean) {
		this.marketBean = marketBean;
	}

	public List<String> getRemarks() {
		return remarks;
	}

	public void setRemarks(List<String> remarks) {
		this.remarks = remarks;
	}

	public List<String> getOfficialQuotes() {
		return officialQuotes;
	}

	public void setOfficialQuotes(List<String> officialQuotes) {
		this.officialQuotes = officialQuotes;
	}

}
