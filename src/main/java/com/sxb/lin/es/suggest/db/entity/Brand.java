package com.sxb.lin.es.suggest.db.entity;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;

public class Brand implements Serializable {
    @Id
    @Column(name = "ID")
    private Integer id;

    @Column(name = "AUTO_BRAND_ID")
    private Integer autoBrandId;

    @Column(name = "PY")
    private String py;

    @Column(name = "NAME")
    private String name;

    @Column(name = "IMAGE")
    private String image;

    @Column(name = "CREATEDATE")
    private Date createdate;

    /**
     * 类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     */
    @Column(name = "SUPPORTINPUT")
    private String supportinput;

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
     * @return AUTO_BRAND_ID
     */
    public Integer getAutoBrandId() {
        return autoBrandId;
    }

    /**
     * @param autoBrandId
     */
    public void setAutoBrandId(Integer autoBrandId) {
        this.autoBrandId = autoBrandId;
    }

    /**
     * @return PY
     */
    public String getPy() {
        return py;
    }

    /**
     * @param py
     */
    public void setPy(String py) {
        this.py = py == null ? null : py.trim();
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
     * @return IMAGE
     */
    public String getImage() {
        return image;
    }

    /**
     * @param image
     */
    public void setImage(String image) {
        this.image = image == null ? null : image.trim();
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
     * 获取类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     *
     * @return SUPPORTINPUT - 类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     */
    public String getSupportinput() {
        return supportinput;
    }

    /**
     * 设置类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     *
     * @param supportinput 类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版
     */
    public void setSupportinput(String supportinput) {
        this.supportinput = supportinput == null ? null : supportinput.trim();
    }
}