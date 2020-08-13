package com.sxb.lin.es.suggest.db.entity;

import java.io.Serializable;
import java.math.BigDecimal;
import javax.persistence.*;

public class Geographic implements Serializable {
    @Id
    @Column(name = "UNID")
    private Integer unid;

    /**
     * 父级unid
     */
    @Column(name = "PUNID")
    private Integer punid;

    /**
     * unid 路径集合
     */
    @Column(name = "PATHUNID")
    private String pathunid;

    /**
     * 类型
     */
    @Column(name = "TYPE")
    private Integer type;

    /**
     * 信息名称_中文
     */
    @Column(name = "NAME")
    private String name;

    /**
     * 当前结点 在其亲兄弟结点中的位置 序号
     */
    @Column(name = "RANK")
    private Integer rank;

    @Column(name = "PATHTEXT")
    private String pathtext;

    /**
     * 信息名称_英文
     */
    @Column(name = "ENAME")
    private String ename;

    /**
     * 创建日期，精确到毫秒
     */
    @Column(name = "CREATEDATE")
    private Long createdate;

    /**
     * 经度
     */
    @Column(name = "LONGITUDE")
    private BigDecimal longitude;

    /**
     * 纬度
     */
    @Column(name = "LATITUDE")
    private BigDecimal latitude;

    private static final long serialVersionUID = 1L;

    /**
     * @return UNID
     */
    public Integer getUnid() {
        return unid;
    }

    /**
     * @param unid
     */
    public void setUnid(Integer unid) {
        this.unid = unid;
    }

    /**
     * 获取父级unid
     *
     * @return PUNID - 父级unid
     */
    public Integer getPunid() {
        return punid;
    }

    /**
     * 设置父级unid
     *
     * @param punid 父级unid
     */
    public void setPunid(Integer punid) {
        this.punid = punid;
    }

    /**
     * 获取unid 路径集合
     *
     * @return PATHUNID - unid 路径集合
     */
    public String getPathunid() {
        return pathunid;
    }

    /**
     * 设置unid 路径集合
     *
     * @param pathunid unid 路径集合
     */
    public void setPathunid(String pathunid) {
        this.pathunid = pathunid == null ? null : pathunid.trim();
    }

    /**
     * 获取类型
     *
     * @return TYPE - 类型
     */
    public Integer getType() {
        return type;
    }

    /**
     * 设置类型
     *
     * @param type 类型
     */
    public void setType(Integer type) {
        this.type = type;
    }

    /**
     * 获取信息名称_中文
     *
     * @return NAME - 信息名称_中文
     */
    public String getName() {
        return name;
    }

    /**
     * 设置信息名称_中文
     *
     * @param name 信息名称_中文
     */
    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    /**
     * 获取当前结点 在其亲兄弟结点中的位置 序号
     *
     * @return RANK - 当前结点 在其亲兄弟结点中的位置 序号
     */
    public Integer getRank() {
        return rank;
    }

    /**
     * 设置当前结点 在其亲兄弟结点中的位置 序号
     *
     * @param rank 当前结点 在其亲兄弟结点中的位置 序号
     */
    public void setRank(Integer rank) {
        this.rank = rank;
    }

    /**
     * @return PATHTEXT
     */
    public String getPathtext() {
        return pathtext;
    }

    /**
     * @param pathtext
     */
    public void setPathtext(String pathtext) {
        this.pathtext = pathtext == null ? null : pathtext.trim();
    }

    /**
     * 获取信息名称_英文
     *
     * @return ENAME - 信息名称_英文
     */
    public String getEname() {
        return ename;
    }

    /**
     * 设置信息名称_英文
     *
     * @param ename 信息名称_英文
     */
    public void setEname(String ename) {
        this.ename = ename == null ? null : ename.trim();
    }

    /**
     * 获取创建日期，精确到毫秒
     *
     * @return CREATEDATE - 创建日期，精确到毫秒
     */
    public Long getCreatedate() {
        return createdate;
    }

    /**
     * 设置创建日期，精确到毫秒
     *
     * @param createdate 创建日期，精确到毫秒
     */
    public void setCreatedate(Long createdate) {
        this.createdate = createdate;
    }

    /**
     * 获取经度
     *
     * @return LONGITUDE - 经度
     */
    public BigDecimal getLongitude() {
        return longitude;
    }

    /**
     * 设置经度
     *
     * @param longitude 经度
     */
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    /**
     * 获取纬度
     *
     * @return LATITUDE - 纬度
     */
    public BigDecimal getLatitude() {
        return latitude;
    }

    /**
     * 设置纬度
     *
     * @param latitude 纬度
     */
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }
}