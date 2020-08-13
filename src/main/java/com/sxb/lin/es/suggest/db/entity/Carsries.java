package com.sxb.lin.es.suggest.db.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

public class Carsries implements Serializable {
    @Id
    @Column(name = "ID")
    private Integer id;

    /**
     * 工厂名称
     */
    @Column(name = "FACTORY_NAME")
    private String factoryName;

    /**
     * 车系名称
     */
    @Column(name = "NAME")
    private String name;

    /**
     * 汽车之家 车系id
     */
    @Column(name = "AUTO_SERIES_ID")
    private Integer autoSeriesId;

    /**
     * 汽车之家 品牌id
     */
    @Column(name = "AUTO_BRAND_ID")
    private Integer autoBrandId;

    /**
     * 表brand的id
     */
    @Column(name = "BRAND_ID")
    private Integer brandId;

    @Column(name = "CREATEDATE")
    private Date createdate;

    /**
     * 车辆型号
     */
    @Column(name = "LEVEL_NAME")
    private String levelName;

    /**
     * 类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     */
    @Column(name = "MODE_TYPE")
    private Integer modeType;

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
     * 获取工厂名称
     *
     * @return FACTORY_NAME - 工厂名称
     */
    public String getFactoryName() {
        return factoryName;
    }

    /**
     * 设置工厂名称
     *
     * @param factoryName 工厂名称
     */
    public void setFactoryName(String factoryName) {
        this.factoryName = factoryName == null ? null : factoryName.trim();
    }

    /**
     * 获取车系名称
     *
     * @return NAME - 车系名称
     */
    public String getName() {
        return name;
    }

    /**
     * 设置车系名称
     *
     * @param name 车系名称
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 获取汽车之家 车系id
     *
     * @return AUTO_SERIES_ID - 汽车之家 车系id
     */
    public Integer getAutoSeriesId() {
        return autoSeriesId;
    }

    /**
     * 设置汽车之家 车系id
     *
     * @param autoSeriesId 汽车之家 车系id
     */
    public void setAutoSeriesId(Integer autoSeriesId) {
        this.autoSeriesId = autoSeriesId;
    }

    /**
     * 获取汽车之家 品牌id
     *
     * @return AUTO_BRAND_ID - 汽车之家 品牌id
     */
    public Integer getAutoBrandId() {
        return autoBrandId;
    }

    /**
     * 设置汽车之家 品牌id
     *
     * @param autoBrandId 汽车之家 品牌id
     */
    public void setAutoBrandId(Integer autoBrandId) {
        this.autoBrandId = autoBrandId;
    }

    /**
     * 获取表brand的id
     *
     * @return BRAND_ID - 表brand的id
     */
    public Integer getBrandId() {
        return brandId;
    }

    /**
     * 设置表brand的id
     *
     * @param brandId 表brand的id
     */
    public void setBrandId(Integer brandId) {
        this.brandId = brandId;
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
     * 获取车辆型号
     *
     * @return LEVEL_NAME - 车辆型号
     */
    public String getLevelName() {
        return levelName;
    }

    /**
     * 设置车辆型号
     *
     * @param levelName 车辆型号
     */
    public void setLevelName(String levelName) {
        this.levelName = levelName == null ? null : levelName.trim();
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
}