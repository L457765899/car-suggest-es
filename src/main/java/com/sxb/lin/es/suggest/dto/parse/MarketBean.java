package com.sxb.lin.es.suggest.dto.parse;

import java.math.BigDecimal;

public class MarketBean {

	private Integer type;
	
	private Integer price;
	
	private BigDecimal dot;
	
	private Integer sell;
	
	private String carInfo;
	
	private BigDecimal number;

	public Integer getType() {
		return type;
	}

	public void setType(Integer type) {
		this.type = type;
	}

	public Integer getPrice() {
		return price;
	}

	public void setPrice(Integer price) {
		this.price = price;
	}

	public BigDecimal getDot() {
		return dot;
	}

	public void setDot(BigDecimal dot) {
		this.dot = dot;
	}

	public String getCarInfo() {
		return carInfo;
	}

	public void setCarInfo(String carInfo) {
		this.carInfo = carInfo;
	}

	public Integer getSell() {
		return sell;
	}

	public void setSell(Integer sell) {
		this.sell = sell;
	}

	public BigDecimal getNumber() {
		return number;
	}

	public void setNumber(BigDecimal number) {
		this.number = number;
	}
	
}
