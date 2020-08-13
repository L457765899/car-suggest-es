package com.sxb.lin.es.suggest.dto.parse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BatchParseBean {
	
	private Integer lineNumber;
	
	private String carInfo;
	
	private boolean canParse;
	
	private boolean canPass;
	
	private List<String> remarks;
	
	private String reason;
	
	private List<CategoryBean> categorys;
	
	public String getCarInfo() {
		return carInfo;
	}

	public void setCarInfo(String carInfo) {
		this.carInfo = carInfo;
	}

	public boolean isCanParse() {
		return canParse;
	}

	public void setCanParse(boolean canParse) {
		this.canParse = canParse;
	}

	public List<CategoryBean> getCategorys() {
		return categorys;
	}

	public void setCategorys(List<CategoryBean> categorys) {
		this.categorys = categorys;
	}
	
	public void addCategory(CategoryBean categoryBean) {
		if(this.categorys == null) {
			this.categorys = new ArrayList<>();
		}
		this.categorys.add(categoryBean);
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public List<String> getRemarks() {
		if(remarks != null && remarks.size() > 0 && remarks.contains(null)) {
			String previousRemark = null;
			List<String> realRemarks = new ArrayList<>();
			int phoneCount = 0;
			for(int i = 0,len = remarks.size(); i < len; i++) {
				String remark = remarks.get(i);
				if(i == 0) {
					if(remark != null) {
						realRemarks.add(remark);
						previousRemark = remark;
					}
				}else {
					if(previousRemark != null && remark != null) {
						realRemarks.add(remark);
						previousRemark = remark;
					}else {
						previousRemark = null;
					}
				}
				if(remark != null) {
					if(this.hasPhone(remark)) {
						phoneCount++;
					}
				}
			}
			
			if(phoneCount > 1) {//有多个电话，就不展示
				return null;
			}
			
			previousRemark = null;
			Collections.reverse(remarks);//倒置
			for(int i = 0,len = remarks.size(); i < len; i++) {
				String remark = remarks.get(i);
				if(i == 0) {
					if(remark != null) {
						realRemarks.add(remark);
						previousRemark = remark;
					}
				}else {
					if(previousRemark != null && remark != null) {
						realRemarks.add(remark);
						previousRemark = remark;
					}else {
						previousRemark = null;
					}
				}
			}
			return realRemarks.stream().distinct().collect(Collectors.toList());//去重
		}
		return remarks;
	}

	public void setRemarks(List<String> remarks) {
		this.remarks = remarks;
	}

	private boolean hasPhone(String car) {
		return Pattern.matches(".*1\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d.*", car);
	}

	public boolean isCanPass() {
		return canPass;
	}

	public void setCanPass(boolean canPass) {
		this.canPass = canPass;
	}

	public Integer getLineNumber() {
		return lineNumber;
	}

	public void setLineNumber(Integer lineNumber) {
		this.lineNumber = lineNumber;
	}
}
