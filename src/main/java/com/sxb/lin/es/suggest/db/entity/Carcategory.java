package com.sxb.lin.es.suggest.db.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

public class Carcategory implements Serializable {
    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "NAME")
    private String name;

    @Column(name = "OFFICIALQUOTE")
    private Integer officialquote;

    @Column(name = "AUTO_SERIES_ID")
    private Integer autoSeriesId;

    @Column(name = "AUTO_CATEGORY_ID")
    private Integer autoCategoryId;

    @Column(name = "CARSRIES_ID")
    private Integer carsriesId;

    @Column(name = "STOP_SELL")
    private Integer stopSell;

    @Column(name = "CREATEDATE")
    private Date createdate;

    /**
     * 外观颜色
     */
    @Column(name = "OUTER_COLOR")
    private String outerColor;

    /**
     * 类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     */
    @Column(name = "MODE_TYPE")
    private Integer modeType;

    /**
     * 内饰颜色
     */
    @Column(name = "INNER_COLOR")
    private String innerColor;

    private static final long serialVersionUID = 1L;

    /**
     * @return ID
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return NAME
     */
    public String getName() {
        return name;
    }

    /**
     * @param name
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * @return OFFICIALQUOTE
     */
    public Integer getOfficialquote() {
        return officialquote;
    }

    /**
     * @param officialquote
     */
    public void setOfficialquote(Integer officialquote) {
        this.officialquote = officialquote;
    }

    /**
     * @return AUTO_SERIES_ID
     */
    public Integer getAutoSeriesId() {
        return autoSeriesId;
    }

    /**
     * @param autoSeriesId
     */
    public void setAutoSeriesId(Integer autoSeriesId) {
        this.autoSeriesId = autoSeriesId;
    }

    /**
     * @return AUTO_CATEGORY_ID
     */
    public Integer getAutoCategoryId() {
        return autoCategoryId;
    }

    /**
     * @param autoCategoryId
     */
    public void setAutoCategoryId(Integer autoCategoryId) {
        this.autoCategoryId = autoCategoryId;
    }

    /**
     * @return CARSRIES_ID
     */
    public Integer getCarsriesId() {
        return carsriesId;
    }

    /**
     * @param carsriesId
     */
    public void setCarsriesId(Integer carsriesId) {
        this.carsriesId = carsriesId;
    }

    /**
     * @return STOP_SELL
     */
    public Integer getStopSell() {
        return stopSell;
    }

    /**
     * @param stopSell
     */
    public void setStopSell(Integer stopSell) {
        this.stopSell = stopSell;
    }

    /**
     * @return CREATEDATE
     */
    public Date getCreatedate() {
        return createdate;
    }

    /**
     * @param createdate
     */
    public void setCreatedate(Date createdate) {
        this.createdate = createdate;
    }

    /**
     * 获取外观颜色
     *
     * @return OUTER_COLOR - 外观颜色
     */
    public String getOuterColor() {
        return outerColor;
    }

    /**
     * 设置外观颜色
     *
     * @param outerColor 外观颜色
     */
    public void setOuterColor(String outerColor) {
        this.outerColor = outerColor == null ? null : outerColor.trim();
    }

    /**
     * 获取类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     *
     * @return MODE_TYPE - 类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     */
    public Integer getModeType() {
        return modeType;
    }

    /**
     * 设置类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     *
     * @param modeType 类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     */
    public void setModeType(Integer modeType) {
        this.modeType = modeType;
    }

    /**
     * 获取内饰颜色
     *
     * @return INNER_COLOR - 内饰颜色
     */
    public String getInnerColor() {
        return innerColor;
    }

    /**
     * 设置内饰颜色
     *
     * @param innerColor 内饰颜色
     */
    public void setInnerColor(String innerColor) {
        this.innerColor = innerColor == null ? null : innerColor.trim();
    }
}