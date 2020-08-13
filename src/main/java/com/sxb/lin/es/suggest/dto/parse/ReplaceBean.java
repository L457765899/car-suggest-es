package com.sxb.lin.es.suggest.dto.parse;

public class ReplaceBean {
	
	private Integer lineNumber;

	private String oldCar;
	
	private String newCar;

	public String getOldCar() {
		return oldCar;
	}

	public void setOldCar(String oldCar) {
		this.oldCar = oldCar;
	}

	public String getNewCar() {
		return newCar;
	}

	public void setNewCar(String newCar) {
		this.newCar = newCar;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}
	
}
