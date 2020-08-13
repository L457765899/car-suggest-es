
CREATE TABLE geographic (
  unid int(11) NOT NULL IDENTITY PRIMARY KEY,
  punid int(11) DEFAULT NULL COMMENT '父级unid',
  pathunid varchar(1000) NOT NULL DEFAULT '' COMMENT 'unid 路径集合',
  type int(11) NOT NULL DEFAULT 0 COMMENT '类型',
  name varchar(500) NOT NULL DEFAULT '' COMMENT '信息名称_中文',
  rank int(11) DEFAULT 0 COMMENT '当前结点 在其亲兄弟结点中的位置 序号',
  pathtext varchar(1000) DEFAULT NULL,
  ename varchar(500) NOT NULL DEFAULT '' COMMENT '信息名称_英文',
  createDate bigint(20) DEFAULT 0 COMMENT '创建日期，精确到毫秒',
  longitude DECIMAL(40,8) DEFAULT NULL COMMENT '经度',
  latitude DECIMAL(40,8) DEFAULT NULL COMMENT '纬度'
);
CREATE INDEX name_index ON geographic(name);
CREATE INDEX pathuid_index ON geographic(pathunid);
CREATE INDEX g_punid_index ON geographic(punid);


CREATE TABLE brand (
  id int(11) NOT NULL IDENTITY PRIMARY KEY,
  auto_brand_id int(11),
  py char(1),
  name varchar(64) NOT NULL,
  image varchar(255) DEFAULT '',
  createDate timestamp,
  supportInput varchar(255) NOT NULL DEFAULT '' COMMENT '类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版'
);
CREATE INDEX brand_name_index ON brand(name);


CREATE TABLE carsries (
  id int(11) NOT NULL IDENTITY PRIMARY KEY,
  factory_name varchar(128) DEFAULT NULL COMMENT '工厂名称',
  name varchar(128) NOT NULL COMMENT '车系名称',
  auto_series_id int(11) DEFAULT NULL COMMENT '汽车之家 车系id',
  auto_brand_id int(11) DEFAULT NULL COMMENT '汽车之家 品牌id',
  brand_id int(11) DEFAULT NULL COMMENT '表brand的id',
  createDate timestamp,
  level_name varchar(64) DEFAULT NULL COMMENT '车辆型号',
  mode_type int(11) NOT NULL DEFAULT 1 COMMENT '类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版'
);
CREATE INDEX carsries_name_index ON carsries(name);
CREATE INDEX brand_id_index ON carsries(brand_id);


CREATE TABLE carcategory (
  id int(11) NOT NULL IDENTITY PRIMARY KEY,
  name varchar(127) DEFAULT NULL,
  officialQuote int(11) DEFAULT NULL,
  auto_series_id int(11) DEFAULT NULL,
  auto_category_id int(11) DEFAULT NULL,
  carSries_id int(11) DEFAULT NULL,
  stop_sell int(1) DEFAULT 0,
  createDate timestamp,
  outer_color varchar(500) DEFAULT '' COMMENT '外观颜色',
  mode_type int(11) NOT NULL DEFAULT 1 COMMENT '类型 1-国产 2-中规 3-老中规进口 4-美版 6-中东 8-加版 10-欧版 12-墨西哥版',
  inner_color varchar(500) DEFAULT '' COMMENT '内饰颜色'
);
CREATE INDEX carcategory_name_index ON carcategory(name);
CREATE INDEX carSries_id_index ON carcategory(carSries_id);
