package com.sxb.lin.es.suggest.service.impl;

import com.sxb.lin.es.suggest.config.CarColorCollection;
import com.sxb.lin.es.suggest.config.IndexAttributes;
import com.sxb.lin.es.suggest.db.dao.CarcategoryMapper;
import com.sxb.lin.es.suggest.dto.Area;
import com.sxb.lin.es.suggest.dto.parse.*;
import com.sxb.lin.es.suggest.service.BatchParseService;
import org.apache.commons.lang3.ArrayUtils;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.*;
import org.elasticsearch.percolator.PercolateQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.BucketOrder;
import org.elasticsearch.search.aggregations.PipelineAggregatorBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.Filter;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms.Bucket;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.Max;
import org.elasticsearch.search.aggregations.metrics.MaxAggregationBuilder;
import org.elasticsearch.search.aggregations.metrics.TopHits;
import org.elasticsearch.search.aggregations.metrics.TopHitsAggregationBuilder;
import org.elasticsearch.search.aggregations.pipeline.BucketSortPipelineAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tk.mybatis.mapper.util.StringUtil;

import javax.annotation.Resource;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service("batchParseESService")
public class BatchParseESServiceImpl implements BatchParseService {
	
	private static final Logger logger = LoggerFactory.getLogger(BatchParseESServiceImpl.class);
	
	private static final String[] KEYS = {"店车","店票","现车","交强","店保","裸车","库存",
			"增票","普票","可增可普","票证随车","手续随车"};
	
	@Resource(name = "bSSuggestService")
	private BSSuggestServiceImpl suggestService;

	@Autowired
	private CarcategoryMapper carcategoryMapper;
	
	@Autowired
	private RestHighLevelClient client;

	@Override
	public List<BatchParseBean> batchParse(String cars) throws Exception {
		//解析行，替换emoji
		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cars.getBytes(StandardCharsets.UTF_8));
		InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String line = null;
		List<ReplaceBean> carList = new ArrayList<>();
		int i = 0;
		while ((line = bufferedReader.readLine()) != null) {
			i++;
			String oldCar = line.trim().replaceAll("\\p{Cf}", "");//过滤看不见的字符
			if(oldCar.equals("")){
				ReplaceBean replaceBean = new ReplaceBean();
				replaceBean.setOldCar(null);
				replaceBean.setNewCar(null);
				replaceBean.setLineNumber(i);
				carList.add(replaceBean);
			}else {
				String newCar = oldCar.replace("↓", "下").replace("⬇", "下").replace("👇", "下")
						   .replace("↑", "上").replace("⬆", "上").replace("👆", "上")
						   .replace("优惠", "下").replace("-", "!")
						   .replace("国六", "国VI").replace("国五", "国V")
						   .replace("国6", "国VI").replace("国5", "国V");
				
				ReplaceBean replaceBean = new ReplaceBean();
				replaceBean.setOldCar(oldCar);
				replaceBean.setNewCar(newCar);
				replaceBean.setLineNumber(i);
				carList.add(replaceBean);
			}
		}
		
		List<BatchParseBean> batchParseBeans = new ArrayList<>();
		SeriesBean previousSeriesBean = null;
		Area previousArea = null;
		List<String> remarks = null;
		String country = null;
		for(ReplaceBean replaceBean : carList) {
			if(remarks == null) {//只有第一行remarks为null
				remarks = new ArrayList<>();
				if(replaceBean.getNewCar() == null) {
					continue;
				}
			}else if(replaceBean.getNewCar() == null) {
				remarks = new ArrayList<>();
				country = null;
				previousArea = null;
				continue;
			}
			
			Integer newCarType = null;
			if(replaceBean.getNewCar().contains("国V")) {
				if(replaceBean.getNewCar().contains("国VI")) {
					newCarType = 2;
				}else {
					newCarType = 1;
				}
			}
			
			CategoryBean categoryBean = null;
			MarketBean marketBean = null;
			SeriesBean seriesBean = null;
			List<String> newCarTerms = new ArrayList<>();
			boolean hasNumber = this.hasNumber(replaceBean.getNewCar());
			if(hasNumber) {
				//先提取行情，会删除行情字符串
				marketBean = this.extractMarket(replaceBean);
				if(marketBean == null) {
					//没有行情，提取车系
					seriesBean = this.searchSeries(replaceBean.getNewCar(), newCarType, country, null);
				}
			}else {
				//不可能是行情，提取车系
				seriesBean = this.searchSeries(replaceBean.getNewCar(), newCarType, country, newCarTerms);
			}
			
			BatchParseBean batchParseBean = new BatchParseBean();
			batchParseBean.setCarInfo(replaceBean.getOldCar());
			batchParseBean.setLineNumber(replaceBean.getLineNumber());
			batchParseBeans.add(batchParseBean);
			if(marketBean == null && seriesBean == null) {//没有行情又不是车系
				if(previousSeriesBean != null) {
					//上面的车系过滤国5国6
					Integer replaceNewCarType = newCarType;
					String previousCarInfo = previousSeriesBean.getCarInfo();
					if(previousSeriesBean.getNewCarType() != null && (newCarType != null || country != null)) {
						if(previousSeriesBean.getNewCarType() == 1) {
							previousCarInfo = previousCarInfo.replace("国V", "");
						}else {
							previousCarInfo = previousCarInfo.replace("国VI", "");
						}
					}else if(previousSeriesBean.getNewCarType() != null && newCarType == null) {
						replaceNewCarType = previousSeriesBean.getNewCarType();
					}
					seriesBean = this.searchSeries(previousCarInfo + " " + replaceBean.getNewCar(), replaceNewCarType, country, null);
					if(seriesBean != null && (seriesBean.getGroupCount() < 10 || seriesBean.getMaxScore() >= 2)) {
						if(seriesBean.getOfficialQuoteCount() > 0) {//没有行情但是有车型
							List<String> numbers = this.extractNumbers(replaceBean.getNewCar());
							List<String> officialQuotes = seriesBean.getCategoryBean().getOfficialQuotes();
							List<String> extractNumbers = this.extractNumbers(seriesBean.getSeries());
							numbers.removeAll(officialQuotes);
							numbers.removeAll(extractNumbers);
							int maxNumber = this.maxNumber(numbers);
							marketBean = new MarketBean();
							marketBean.setType(0);
							marketBean.setPrice(-maxNumber);
							marketBean.setCarInfo(replaceBean.getNewCar());
							categoryBean = seriesBean.getCategoryBean();
							//看上面是否有定义地区
							if(categoryBean != null && categoryBean.getCarLocation() == null && previousArea != null) {
								categoryBean.setCarLocation(previousArea.getArea());
								categoryBean.setCarLocationIdPath(previousArea.getAreaIdPath());
							}
						}else {
							batchParseBean.setReason("无官方报价");
							String cou = this.getCountry(newCarType, hasNumber);
							Area area = this.getArea(replaceBean.getNewCar(), newCarTerms, hasNumber);
							if(cou != null || area != null) {
								batchParseBean.setCanPass(true);
								if(cou != null) {
									country = cou;
									batchParseBean.setReason(batchParseBean.getReason() + "：" + country);
								}
								if(area != null) {
									previousArea = area;
									batchParseBean.setReason(batchParseBean.getReason() + "：" + previousArea.getArea());
								}
							}else {
								this.addRemark(remarks, replaceBean.getOldCar());
							}
							continue;
						}
					}else {
						batchParseBean.setReason("匹配结果太多1");
						String cou = this.getCountry(newCarType, hasNumber);
						Area area = this.getArea(replaceBean.getNewCar(), newCarTerms, hasNumber);
						if(cou != null || area != null) {
							batchParseBean.setCanPass(true);
							if(cou != null) {
								country = cou;
								batchParseBean.setReason(batchParseBean.getReason() + "：" + country);
							}
							if(area != null) {
								previousArea = area;
								batchParseBean.setReason(batchParseBean.getReason() + "：" + previousArea.getArea());
							}
						}else {
							this.addRemark(remarks, replaceBean.getOldCar());
						}
						continue;
					}
				}else {
					batchParseBean.setReason("什么都不是");
					String cou = this.getCountry(newCarType, hasNumber);
					Area area = this.getArea(replaceBean.getNewCar(), newCarTerms, hasNumber);
					if(cou != null || area != null) {
						batchParseBean.setCanPass(true);
						if(cou != null) {
							country = cou;
							batchParseBean.setReason(batchParseBean.getReason() + "：" + country);
						}
						if(area != null) {
							previousArea = area;
							batchParseBean.setReason(batchParseBean.getReason() + "：" + previousArea.getArea());
						}
					}else {
						this.addRemark(remarks, replaceBean.getOldCar());
					}
					continue;
				}
			}else if(marketBean == null && seriesBean != null) {//没有行情但是有车系
				if(seriesBean.getGroupCount() < 10 || seriesBean.getMaxScore() >= 2) {
					if(seriesBean.getOfficialQuoteCount() > 0) {//没有行情但是有车型
						List<String> numbers = this.extractNumbers(seriesBean.getCarInfo());
						List<String> officialQuotes = seriesBean.getCategoryBean().getOfficialQuotes();
						List<String> extractNumbers = this.extractNumbers(seriesBean.getSeries());
						numbers.removeAll(officialQuotes);
						numbers.removeAll(extractNumbers);
						int maxNumber = this.maxNumber(numbers);
						marketBean = new MarketBean();
						marketBean.setType(0);
						marketBean.setPrice(-maxNumber);
						marketBean.setCarInfo(seriesBean.getCarInfo());
						categoryBean = seriesBean.getCategoryBean();
						if(categoryBean != null && categoryBean.getCarLocation() == null && previousArea != null) {
							//看上面是否有定义地区
							categoryBean.setCarLocation(previousArea.getArea());
							categoryBean.setCarLocationIdPath(previousArea.getAreaIdPath());
						}else if(categoryBean != null && categoryBean.getCarLocation() == null 
								&& previousSeriesBean != null && previousSeriesBean.getSeries().equals(categoryBean.getSeries())) {
							//车型里没有车源区域，看车系里是否有
							Area area = this.searchArea(previousSeriesBean.getCarInfo(), previousSeriesBean.getTerms(), 
									categoryBean.getBrand(), categoryBean.getFactoryName());
							if(area != null) {
								categoryBean.setCarLocation(area.getArea());
								categoryBean.setCarLocationIdPath(area.getAreaIdPath());
							}
						}
					}else {
						remarks = new ArrayList<>();
						country = null;
						previousArea = null;
						previousSeriesBean = seriesBean;//记录为上一次的车系
						previousSeriesBean.setNewCarType(newCarType);
						batchParseBean.setCanPass(true);
						batchParseBean.setReason("识别为车系：" + seriesBean.getSeries());
						if(this.hasRemark(seriesBean.getCarInfo())) {
							batchParseBean.setReason("有备注" + batchParseBean.getReason());
							remarks.add(replaceBean.getOldCar());
						}
						continue;
					}
				}else {
					batchParseBean.setReason("匹配结果太多2");
					String cou = this.getCountry(newCarType, hasNumber);
					Area area = this.getArea(replaceBean.getNewCar(), newCarTerms, hasNumber);
					if(cou != null || area != null) {
						batchParseBean.setCanPass(true);
						if(cou != null) {
							country = cou;
							batchParseBean.setReason(batchParseBean.getReason() + "：" + country);
						}
						if(area != null) {
							previousArea = area;
							batchParseBean.setReason(batchParseBean.getReason() + "：" + previousArea.getArea());
						}
					}else {
						if(this.hasRemark(seriesBean.getCarInfo())) {
							batchParseBean.setReason("有备注" + batchParseBean.getReason());
							remarks.add(replaceBean.getOldCar());
						}
					}
					continue;
				}
			}
			remarks.add(null);
			
			//再匹配车型
			if(categoryBean == null) {
				categoryBean = this.searchCategory(marketBean.getCarInfo(), newCarType, country);
				if(categoryBean == null) {
					if(previousSeriesBean != null) {
						//上面的车系过滤国5国6
						Integer replaceNewCarType = newCarType;
						String previousCarInfo = previousSeriesBean.getCarInfo();
						if(previousSeriesBean.getNewCarType() != null && (newCarType != null || country != null)) {
							if(previousSeriesBean.getNewCarType() == 1) {
								previousCarInfo = previousCarInfo.replace("国V", "");
							}else {
								previousCarInfo = previousCarInfo.replace("国VI", "");
							}
						}else if(previousSeriesBean.getNewCarType() != null && newCarType == null) {
							replaceNewCarType = previousSeriesBean.getNewCarType();
						}
						categoryBean = this.searchCategory(previousCarInfo + " " + marketBean.getCarInfo(), replaceNewCarType, country);
						//看上面是否有定义地区
						if(categoryBean != null && categoryBean.getCarLocation() == null && previousArea != null) {
							categoryBean.setCarLocation(previousArea.getArea());
							categoryBean.setCarLocationIdPath(previousArea.getAreaIdPath());
						}
					}
					if(categoryBean == null) {
						batchParseBean.setReason("无匹配车型");
						continue;
					}
				}else {
					if(categoryBean.getCarLocation() == null && previousArea != null) {
						//看上面是否有定义地区
						categoryBean.setCarLocation(previousArea.getArea());
						categoryBean.setCarLocationIdPath(previousArea.getAreaIdPath());
					}else if(categoryBean.getCarLocation() == null && previousSeriesBean != null
							&& previousSeriesBean.getSeries().equals(categoryBean.getSeries())) {
						//车型里没有车源区域，看车系里是否有
						Area area = this.searchArea(previousSeriesBean.getCarInfo(), previousSeriesBean.getTerms(), 
								categoryBean.getBrand(), categoryBean.getFactoryName());
						if(area != null) {
							categoryBean.setCarLocation(area.getArea());
							categoryBean.setCarLocationIdPath(area.getAreaIdPath());
						}
					}
				}
			}
			
			//行情转换，验证行情
			if(marketBean.getType() == 2) {
				int price = marketBean.getSell() - categoryBean.getOfficialQuote();
				if(Math.abs(price) <= categoryBean.getOfficialQuote() * 0.3) {
					marketBean.setType(0);
					marketBean.setPrice(price);
				}else {
					batchParseBean.setReason("行情偏离过大");
					continue;
				}
			}else if(marketBean.getType() == 0) {
				if(Math.abs(marketBean.getPrice()) >= categoryBean.getOfficialQuote()) {
					batchParseBean.setReason("行情偏离过大");
					continue;
				}
			}else if(marketBean.getType() == 1) {
				if(marketBean.getDot().abs().intValue() >= 100) {
					batchParseBean.setReason("行情偏离过大");
					continue;
				}
			}
			
			batchParseBean.setCanParse(true);
			batchParseBean.setCanPass(true);
			batchParseBean.setRemarks(remarks);
			List<String> categoryRemarks = new ArrayList<>();
			for(String key : KEYS) {
				if(replaceBean.getOldCar().contains(key)) {
					categoryRemarks.add(key);
				}
			}
			
			//最后提取颜色
			List<ColorBean> extractColor = this.extractColor(marketBean.getCarInfo(), categoryBean);
			if(extractColor == null) {
				categoryBean.setInnerColor("色全");
				categoryBean.setOuterColor("色全");
				categoryBean.setMarketBean(marketBean);
				categoryBean.setRemarks(categoryRemarks);
				batchParseBean.addCategory(categoryBean);
				continue;
			}
			
			for(ColorBean colorBean : extractColor) {
				CategoryBean clone = (CategoryBean) categoryBean.clone();
				clone.setOuterColor(colorBean.getOutColor());
				if(StringUtils.hasText(colorBean.getInnerColor())) {
					clone.setInnerColor(colorBean.getInnerColor());
				}else {
					clone.setInnerColor("色全");
				}
				clone.setMarketBean(marketBean);
				clone.setRemarks(categoryRemarks);
				batchParseBean.addCategory(clone);
			}
		}
		return batchParseBeans;
	}
	
	/*GET /sxb_parse_category/_search
	{
	  "query": {
	    "bool": {
	      "must": [
	        {
	          "match": {
	            "series": "马自达 阿特兹 1758"
	          }
	        },
	        {
	          "terms": {
	            "officialQuote": [
	              "马自达",
	              "阿特",
	              "兹",
	              "1758"
	            ]
	          }
	        }
	      ],
	      "should": [
	        {
	          "terms": {
	            "year": [
	              "马自达",
	              "阿特",
	              "兹",
	              "1758"
	            ]
	          }
	        },
	        {
	          "match": {
	            "brand": "马自达 阿特兹 1758"
	          }
	        },
	        {
	          "match": {
	            "factoryName": "马自达 阿特兹 1758"
	          }
	        },
	        {
	          "match": {
	            "category": "马自达 阿特兹 1758"
	          }
	        }
	      ]
	    }
	  },
	  "sort": [
	    "_score",
	    {
	      "weight": {
	        "order": "desc"
	      }
	    }
	  ]
	}*/
	protected CategoryBean searchCategory(String car, Integer newCarType, String country) throws Exception {
		if(!StringUtils.hasText(car)) {
			return null;
		}
		List<String> terms = suggestService.analyze(car);
		String newCar = car;
		if(newCarType != null && newCarType == 1) {
			newCar = car.replace("国V", "");
		}else if(newCarType != null && newCarType == 2) {
			newCar = car.replace("国VI", "");
		} 
		
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		BoolQueryBuilder seriesQueryBuilder = QueryBuilders.boolQuery();
		seriesQueryBuilder.should(QueryBuilders.matchQuery("seriesText", newCar));
		seriesQueryBuilder.should(QueryBuilders.matchQuery("seriesAlias", newCar + " xxxxxxxxx")
				.minimumShouldMatch("3"));
		seriesQueryBuilder.boost(2);
		boolQueryBuilder.must(seriesQueryBuilder);
		boolQueryBuilder.must(QueryBuilders.termsQuery("officialQuote", terms));
		
		boolQueryBuilder.should(QueryBuilders.termsQuery("year", terms));
		boolQueryBuilder.should(QueryBuilders.matchQuery("brand", newCar));
		boolQueryBuilder.should(QueryBuilders.matchQuery("factoryName", newCar));
		if(newCarType == null && country != null && country.equals("国V")) {
			boolQueryBuilder.should(QueryBuilders.matchQuery("categoryText", car + " 国V"));
		}else if(newCarType == null && country != null && country.equals("国VI")) {
			boolQueryBuilder.should(QueryBuilders.matchQuery("categoryText", car + " 国VI"));
		}else {
			boolQueryBuilder.should(QueryBuilders.matchQuery("categoryText", car));
		}
		
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(1);
        searchSourceBuilder.sort("_score");
        searchSourceBuilder.sort("weight", SortOrder.DESC);
        
        SearchRequest searchRequest = new SearchRequest(IndexAttributes.SXB_PARSE_CATEGORY);
        searchRequest.source(searchSourceBuilder);
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
		if(searchHits.getTotalHits().value > 0) {
			SearchHit[] hits = searchHits.getHits();
			Map<String, Object> sourceAsMap = hits[0].getSourceAsMap();
			CategoryBean categoryBean = this.converToCategoryBean(sourceAsMap);
			
			if(!this.isConfirmSeries(terms, categoryBean.getSeries())) {
				return null;
			}
			
			//查询车源区域
			Area area = this.searchArea(car, terms, categoryBean.getBrand(), categoryBean.getFactoryName());
			if(area != null) {
				categoryBean.setCarLocation(area.getArea());
				categoryBean.setCarLocationIdPath(area.getAreaIdPath());
			}
			return categoryBean;
		}
		return null;
	}
	
	/*GET /sxb_parse_category/_search
	{
	  "size": 0, 
	  "query": {
	    "bool": {
	      "must": [
	        {
	          "bool": {
		           "should": [
		             {
		               "constant_score": {
		                 "filter": {
		                   "term": {
		                     "seriesText": "阿特"
		                   }
		                 },
		                 "boost": 1
		               }
		             },
		             {
		               "constant_score": {
		                 "filter": {
		                   "term": {
		                     "seriesText": "兹"
		                   }
		                 },
		                 "boost": 1
		               }
		             }
		           ]
		         }
	        }
	      ],
	      "should": [
	        {
	          "constant_score": {
	            "filter": {
	              "match": {
	                "brand": "阿特兹"
	              }
	            },
	            "boost": 1
	          }
	        },
	        {
	          "constant_score": {
	            "filter": {
	              "terms": {
	                "officialQuote": [
	                  "1758"
	                ]
	              }
	            },
	            "boost": 1
	          }
	        },
	        {
	         "constant_score": {
	            "filter": {
	              "match": {
	                "categoryText": "阿特兹"
	              }
	            },
	            "boost": 1
	          }
	        }
	      ]
	    }
	  },
	  "aggs": {
	    "seriesGroup": {
	      "terms": {
	        "field": "series",
	        "size": 10,
	        "order": [
	          {
	            "maxScore": "desc"
	          },
	          {
	            "officialQuoteFilter.doc_count": "desc"
	          }
	        ]
	      },
	      "aggs": {
	        "officialQuoteFilter": {
	          "filter": {
	            "terms": {
	              "officialQuote": [
	                "1758"
	              ]
	            }
	          },
	          "aggs": {
	            "topData": {
	              "top_hits": {
	                "size": 1,
	                "sort": [
	                  "_score",
	                  {
	                    "weight": {
	                      "order": "desc"
	                    }
	                  }
	                ]
	              }
	            }
	          }
	        },
	        "maxScore": {
	          "max": {
	            "script": "_score"
	          }
	        },
	        "scorebucketSort": {
	          "bucket_sort": {
	            "sort": [
	              {
	                "maxScore": {
	                  "order": "desc"
	                },
	                "officialQuoteFilter._count": {
	                  "order": "desc"
	                }
	              }
	            ],
	            "size": 10
	          }
	        }
	      }
	    }
	  }
	}*/
	protected SeriesBean searchSeries(String car, Integer newCarType, String country, List<String> newCarTerms) throws Exception {
		if(!StringUtils.hasText(car)) {
			return null;
		}
		
		List<String> terms = suggestService.analyze(car);
		if(newCarTerms != null && newCarTerms.size() == 0) {
			newCarTerms.addAll(terms);
		}
		
		List<String> newTerms = new ArrayList<>(terms);
		if(newCarType != null && newCarType == 1) {
			newTerms.remove("国");
			newTerms.remove("v");
		}else if(newCarType != null && newCarType == 2) {
			newTerms.remove("国");
			newTerms.remove("vi");
		} 
		
		BoolQueryBuilder seriesBoolQueryBuilder = QueryBuilders.boolQuery();
		for (String term : newTerms) {
            TermQueryBuilder termQuery = QueryBuilders.termQuery("seriesText", term);
            ConstantScoreQueryBuilder constantScoreQuery = QueryBuilders.constantScoreQuery(termQuery);
            seriesBoolQueryBuilder.should(constantScoreQuery);
        }
		ConstantScoreQueryBuilder seriesAliasConstantScoreQuery = QueryBuilders.constantScoreQuery(
				QueryBuilders.matchQuery("seriesAlias", car + " xxxxxxxxx").minimumShouldMatch("3"));
		seriesAliasConstantScoreQuery.boost(3);
		seriesBoolQueryBuilder.should(seriesAliasConstantScoreQuery);
		
		ConstantScoreQueryBuilder brandQueryBuilder = QueryBuilders.constantScoreQuery(QueryBuilders.matchQuery("brand", car));
		brandQueryBuilder.boost(0.2f);
		
		ConstantScoreQueryBuilder officialQuoteQueryBuilder = QueryBuilders.constantScoreQuery(QueryBuilders.termsQuery("officialQuote", newTerms));
		
		BoolQueryBuilder categoryQueryBuilder = QueryBuilders.boolQuery();
		for (String term : terms) {
			TermQueryBuilder termQuery = QueryBuilders.termQuery("categoryText", term);
			ConstantScoreQueryBuilder constantScoreQuery = QueryBuilders.constantScoreQuery(termQuery);
			constantScoreQuery.boost(0.1f);
			categoryQueryBuilder.should(constantScoreQuery);
		}
		if(newCarType == null && country != null && country.equals("国V")) {
			TermQueryBuilder termQuery1 = QueryBuilders.termQuery("categoryText", "国");
			ConstantScoreQueryBuilder constantScoreQuery1 = QueryBuilders.constantScoreQuery(termQuery1);
			constantScoreQuery1.boost(0.1f);
			categoryQueryBuilder.should(constantScoreQuery1);
			
			TermQueryBuilder termQuery2 = QueryBuilders.termQuery("categoryText", "v");
			ConstantScoreQueryBuilder constantScoreQuery2 = QueryBuilders.constantScoreQuery(termQuery2);
			constantScoreQuery2.boost(0.1f);
			categoryQueryBuilder.should(constantScoreQuery2);
		} else if(newCarType == null && country != null && country.equals("国VI")) {
			TermQueryBuilder termQuery1 = QueryBuilders.termQuery("categoryText", "国");
			ConstantScoreQueryBuilder constantScoreQuery1 = QueryBuilders.constantScoreQuery(termQuery1);
			constantScoreQuery1.boost(0.1f);
			categoryQueryBuilder.should(constantScoreQuery1);
			
			TermQueryBuilder termQuery2 = QueryBuilders.termQuery("categoryText", "vi");
			ConstantScoreQueryBuilder constantScoreQuery2 = QueryBuilders.constantScoreQuery(termQuery2);
			constantScoreQuery2.boost(0.1f);
			categoryQueryBuilder.should(constantScoreQuery2);
		}
		
		BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
		boolQueryBuilder.must(seriesBoolQueryBuilder);
		boolQueryBuilder.should(brandQueryBuilder);
		boolQueryBuilder.should(officialQuoteQueryBuilder);
		boolQueryBuilder.should(categoryQueryBuilder);
		
		int size = 10;
		List<BucketOrder> orders = new ArrayList<>();
		orders.add(BucketOrder.aggregation("maxScore", false));
		orders.add(BucketOrder.aggregation("officialQuoteFilter.doc_count", false));
		TermsAggregationBuilder seriesGroup = AggregationBuilders.terms("seriesGroup");
		seriesGroup.field("series");
		seriesGroup.size(size);
		seriesGroup.order(orders);
		
		TermsQueryBuilder filter = QueryBuilders.termsQuery("officialQuote", terms);
		FilterAggregationBuilder officialQuoteFilter = AggregationBuilders.filter("officialQuoteFilter", filter);
		
		List<SortBuilder<?>> topSorts = new ArrayList<>();
		topSorts.add(SortBuilders.scoreSort());
		topSorts.add(SortBuilders.fieldSort("weight").order(SortOrder.DESC));
		TopHitsAggregationBuilder topData = AggregationBuilders.topHits("topData");
		topData.size(1);
		topData.sorts(topSorts);
		officialQuoteFilter.subAggregation(topData);
		seriesGroup.subAggregation(officialQuoteFilter);
		
		Script script = new Script("_score");
		MaxAggregationBuilder maxScore = AggregationBuilders.max("maxScore");
		maxScore.script(script);
		seriesGroup.subAggregation(maxScore);
		
		List<FieldSortBuilder> sorts = new ArrayList<>();
		FieldSortBuilder score = new FieldSortBuilder("maxScore");
		score.order(SortOrder.DESC);
        sorts.add(score);
        FieldSortBuilder count = new FieldSortBuilder("officialQuoteFilter._count");
        count.order(SortOrder.DESC);
        sorts.add(count);
		BucketSortPipelineAggregationBuilder scorebucketSort = PipelineAggregatorBuilders.bucketSort("scorebucketSort", sorts);
		scorebucketSort.size(size);
		seriesGroup.subAggregation(scorebucketSort);
		
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(boolQueryBuilder);
        searchSourceBuilder.size(0);
        searchSourceBuilder.sort("_score");
        searchSourceBuilder.aggregation(seriesGroup);
        
        SearchRequest searchRequest = new SearchRequest(IndexAttributes.SXB_PARSE_CATEGORY);
        searchRequest.source(searchSourceBuilder);
        
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        SearchHits searchHits = searchResponse.getHits();
		if(searchHits.getTotalHits().value > 0) {
			Aggregations aggregations = searchResponse.getAggregations();
			Terms seriesTerms = aggregations.get("seriesGroup");
			List<? extends Bucket> buckets = seriesTerms.getBuckets();
			if(buckets.size() > 0) {
				Bucket bucket = buckets.get(0);
				Aggregations seriesAggs = bucket.getAggregations();
				String series = bucket.getKeyAsString();
				
				if(!this.isConfirmSeries(terms, series)) {
					return null;
				}
				
				Filter officialQuote = seriesAggs.get("officialQuoteFilter");
				Aggregations officialQuoteAggs = officialQuote.getAggregations();
				TopHits topHits = officialQuoteAggs.get("topData");
				SearchHits topSearchHits = topHits.getHits();
				
				SeriesBean seriesBean = new SeriesBean();
				if(topSearchHits.getTotalHits().value > 0) {
					SearchHit[] hits = topSearchHits.getHits();
					Map<String, Object> sourceAsMap = hits[0].getSourceAsMap();
					CategoryBean categoryBean = this.converToCategoryBean(sourceAsMap);
					//查询车源区域
					Area area = this.searchArea(car, terms, categoryBean.getBrand(), categoryBean.getFactoryName());
					if(area != null) {
						categoryBean.setCarLocation(area.getArea());
						categoryBean.setCarLocationIdPath(area.getAreaIdPath());
					}
					seriesBean.setCategoryBean(categoryBean);
				}
				
				Max max = seriesAggs.get("maxScore");
				double maxScoreDouble = max.getValue();
				int groupCount = buckets.size();
				seriesBean.setMaxScore(maxScoreDouble);
				seriesBean.setCarInfo(car);
				seriesBean.setGroupCount(groupCount);
				seriesBean.setOfficialQuoteCount(officialQuote.getDocCount());
				seriesBean.setSeries(series);
				seriesBean.setTerms(terms);
				return seriesBean;
			}
		}
		return null;
	}
	
	private boolean isConfirmSeries(List<String> terms, String series) throws Exception {
		Set<String> retainKeywords = this.retainKeywords(terms, series);
		//如果搜索条件只命中搜索结果的一个词元，则判断命中的词元是否是以下情况
		if(retainKeywords.size() == 1) {
			String term = retainKeywords.iterator().next();
			if(CarColorCollection.outColorLst2.contains(term)) {
				return false;//命中的词元是颜色词
			}
			if(term.matches("^[0-9]{1,3}$")) {
				return false;//命中的词元是1到3位的数字
			}
			if(term.equals("车")) {
				return false;
			}
			if(term.equals("小")) {
				return false;
			}
		}
		return true;
	}
	
	private Set<String> retainKeywords(List<String> terms, String series) throws Exception {
		String seriesText = this.getSeriesText(series);
		List<String> seriesTerms = suggestService.analyze(seriesText);
		seriesTerms.retainAll(terms);//交集
		return new HashSet<String>(seriesTerms);
	}
	
	private void addRemark(List<String> remarks, String remark) {
		String join = String.join(".*|.*", CarColorCollection.outColorLst2);
    	join = ".*" + join + ".*";
    	if(Pattern.matches(join, remark)) {
    		return;
    	}
    	if(remark.matches(".*\\D\\d{3,5}\\D.*|.*\\D\\d{3,5}|\\d{3,5}\\D.*")) {
    		return;
    	}
    	remarks.add(remark);
	}
	
	private Area searchArea(String car, List<String> terms, String brand, String factoryName) throws Exception {
		BoolQueryBuilder allQuery = QueryBuilders.boolQuery();
		BoolQueryBuilder boolQuery1 = QueryBuilders.boolQuery();
		boolQuery1.should(QueryBuilders.rangeQuery("type").lte(4));
		List<String> values = new ArrayList<>();
		values.add("重庆");
		values.add("天津");
		values.add("上海");
		values.add("北京");
		boolQuery1.should(QueryBuilders.termsQuery("name", values));
		allQuery.must(boolQuery1);
		
		BoolQueryBuilder boolQuery2 = QueryBuilders.boolQuery();
        for(String term : terms){
            if(term.length() >= 2){
            	boolQuery2.should(QueryBuilders.termQuery("name", term));
            }
        }
        if(boolQuery2.should().size() > 0){
        	allQuery.must(boolQuery2);
        	SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
            searchSourceBuilder.query(allQuery);
            searchSourceBuilder.from(0); 
            searchSourceBuilder.size(2);
            searchSourceBuilder.sort("_score");
            
            SearchRequest searchRequest = new SearchRequest(IndexAttributes.SXB_AREA);
            searchRequest.source(searchSourceBuilder);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            SearchHits hits = searchResponse.getHits();
            if(hits.getTotalHits().value > 0){
                SearchHit hit = hits.getAt(0);
                Map<String, Object> sourceAsMap = hit.getSourceAsMap();
                String name = (String) sourceAsMap.get("name");
                String pathunid = (String) sourceAsMap.get("pathunid");
                
                Area area = new Area();
                area.setArea(name);
                area.setAreaIdPath(pathunid);
                
                if(brand == null && factoryName == null) {
                	return area;
                }
                
                String replace = name.replace("市", "").replace("省", "");
                factoryName = factoryName == null ? "" : factoryName;
                
                if(hits.getTotalHits().value == 1) {
                	String p = ".*北京市.*|.*天津市.*|.*吉林省.*|.*四川省.*|.*江西省.*|.*陕西省.*";
                    if (!Pattern.matches(p, car)) {
                    	if(factoryName.contains(replace) || brand.contains(replace)) {
                    		return null;
                    	}
                    }
                }else if(hits.getTotalHits().value > 1) {
                	if(factoryName.contains(replace) || brand.contains(replace)) {
                		hit = hits.getAt(1);
                		sourceAsMap = hit.getSourceAsMap();
                		area.setArea((String) sourceAsMap.get("name"));
                        area.setAreaIdPath((String) sourceAsMap.get("pathunid"));
                	}
                }
                return area;
            }
        }
		return null;
	}
	
	private CategoryBean converToCategoryBean(Map<String, Object> sourceAsMap) {
		CategoryBean categoryBean = new CategoryBean();
		String brand = (String) sourceAsMap.get("brand");
		String factoryName = (String) sourceAsMap.get("factoryName");
		String series = (String) sourceAsMap.get("series");
		String category = (String) sourceAsMap.get("category");
		int modeType = (int) sourceAsMap.get("modeType");
		String outColor = (String) sourceAsMap.get("outColor");
		
		categoryBean.setBrand(brand);
		categoryBean.setFactoryName(factoryName);
		categoryBean.setSeries(series);
		categoryBean.setCategory(category);
		categoryBean.setModeType(modeType);
		categoryBean.setOuterColor(outColor);
		
		@SuppressWarnings("unchecked")
		List<String> officialQuotes = (List<String>) sourceAsMap.get("officialQuote");
		Integer maxOfficialQuote = this.maxNumber(officialQuotes);
		categoryBean.setOfficialQuote(maxOfficialQuote);
		categoryBean.setOfficialQuotes(officialQuotes);
		return categoryBean;
	}
	
	/**
	 * 提取颜色
	 * @param car
	 * @return
	 */
	protected List<ColorBean> extractColor(String car, CategoryBean categoryBean) {
		if(!this.hasColor(car)) {
			return null;
		}
		if(this.hasWhitespace(car)) {
			String[] split = car.split("\\s");
			List<String> parts = new ArrayList<>();
			for(String part : split) {
				if(StringUtils.hasText(part)) {
					parts.add(part);
				}
			}
			if(parts.size() == 1) {
				return this.singleColor(car);
			}
			return this.multipleColor(parts, categoryBean);
		}else {
			return this.singleColor(car);
		}
	}
	
	private List<ColorBean> singleColor(String text) {
		if(text.contains("/")) {
			String[] parts = text.split("/");
			return this.partsColor(parts);
		}
		if(text.contains("|")) {
			String[] parts = text.split("\\|");
			return this.partsColor(parts);
		}
		return this.textColor(text);
	}
	
	private List<String> retainColor(String text) {
		String[] split = text.split("");
		List<String> asList = new ArrayList<>(split.length);
    	Collections.addAll(asList, split);
    	asList.retainAll(CarColorCollection.outColorLst2);//交集
    	Map<Integer, String> colorMap = new HashMap<>();
    	for(int i = 0,len = asList.size(); i < len; i++) {
    		String color = asList.get(i) + "色";
    		if(text.contains(color)) {
    			colorMap.put(i, color);
    		}
    	}
    	if(colorMap.size() > 0) {
    		Set<Entry<Integer, String>> entrySet = colorMap.entrySet();
    		for(Entry<Integer, String> entry : entrySet) {
    			asList.set(entry.getKey(), entry.getValue());
    		}
    	}
    	return asList;
	}
	
	private List<ColorBean> textColor(String text) {
		List<String> asList = this.retainColor(text);
    	if(asList.size() > 0) {
    		ColorBean colorBean = new ColorBean();
			colorBean.setOutColor(String.join(",", asList));
			List<ColorBean> list = new ArrayList<>();
			list.add(colorBean);
			return list;
    	}
    	return null;
	}
	
	private List<ColorBean> partsColor(String[] parts) {
		List<String> colors = new ArrayList<>();
		for(String part : parts) {
			List<String> asList = this.retainColor(part);
	    	if(asList.size() > 0) {
	    		colors.add(String.join(",", asList));
	    	}
		}
		if(colors.size() == 1) {
			ColorBean colorBean = new ColorBean();
			colorBean.setOutColor(colors.get(0));
			List<ColorBean> list = new ArrayList<>();
			list.add(colorBean);
			return list;
		}else if(colors.size() > 1) {
			ColorBean colorBean = new ColorBean();
			colorBean.setOutColor(colors.get(0));
			colorBean.setInnerColor(colors.get(1));
			List<ColorBean> list = new ArrayList<>();
			list.add(colorBean);
			return list;
		}
		return null;
	}
	
	private List<ColorBean> multipleColor(List<String> parts, CategoryBean categoryBean) {
		String[] defaultColors = null;
		if(StringUtils.hasText(categoryBean.getOuterColor())) {
			defaultColors = categoryBean.getOuterColor().split(",");
		}
		List<ColorBean> colorBeans = new ArrayList<>();
		for(String part : parts) {
			if(part.contains("/")) {
				String[] multipleParts = part.split("/");
				if(defaultColors != null && multipleParts.length > 0 
						&& ArrayUtils.contains(defaultColors, multipleParts[0])) {
					ColorBean colorBean = new ColorBean();
					colorBean.setOutColor(multipleParts[0]);
					if(multipleParts.length > 1) {
						List<String> asList = this.retainColor(multipleParts[1]);
						if(asList.size() > 0) {
							colorBean.setInnerColor(String.join(",", asList));
						}
					}
					colorBeans.add(colorBean);
				}else {
					List<ColorBean> partsColor = this.partsColor(multipleParts);
					if(partsColor != null) {
						colorBeans.addAll(partsColor);
					}
				}
			}else if(part.contains("|")) {
				String[] multipleParts = part.split("\\|");
				if(defaultColors != null && multipleParts.length > 0 
						&& ArrayUtils.contains(defaultColors, multipleParts[0])) {
					ColorBean colorBean = new ColorBean();
					colorBean.setOutColor(multipleParts[0]);
					if(multipleParts.length > 1) {
						List<String> asList = this.retainColor(multipleParts[1]);
						if(asList.size() > 0) {
							colorBean.setInnerColor(String.join(",", asList));
						}
					}
					colorBeans.add(colorBean);
				}else {
					List<ColorBean> partsColor = this.partsColor(multipleParts);
					if(partsColor != null) {
						colorBeans.addAll(partsColor);
					}
				}
			}else {
				if(defaultColors != null && ArrayUtils.contains(defaultColors, part)) {
					ColorBean colorBean = new ColorBean();
					colorBean.setOutColor(part);
					colorBeans.add(colorBean);
				}else {
					List<ColorBean> textColor = this.textColor(part);
					if(textColor != null) {
						colorBeans.addAll(textColor);
					}
				}
			}
		}
		
		return colorBeans.size() == 0 ? null : colorBeans;
	}
	
	private boolean hasColor(String text) {
		String join = String.join(".*|.*", CarColorCollection.outColorLst2);
    	join = ".*" + join + ".*";
		return Pattern.matches(join, text);
	}
	
	private boolean hasWhitespace(String car) {
		return Pattern.matches(".+\\s.+", car);
	}
	
	private boolean hasNumber(String car) {
		return Pattern.matches(".*\\d+.*", car);
	}
	
	private boolean hasRemark(String car) {
		if(Pattern.matches(".*1\\d\\d\\d\\d\\d\\d\\d\\d\\d\\d.*", car)) {
			return true;
		}
		String join = String.join(".*|.*", KEYS);
    	join = ".*" + join + ".*";
    	if(Pattern.matches(join, car)) {
    		return true;
    	}
		return false;
	}

	/**
	 * 提取行情
	 * @param replaceBean
	 * @return
	 */
	protected MarketBean extractMarket(ReplaceBean replaceBean) {
		String car = replaceBean.getNewCar();
		
		Pattern pattern10 = Pattern.compile("((下|!)\\s*(\\d+\\.?\\d*)\\s*万)");
        Matcher matcher10 = pattern10.matcher(car);
        if(matcher10.find()) {
        	String priceStr = matcher10.group(1);
        	int price = new BigDecimal(matcher10.group(3)).multiply(BigDecimal.valueOf(10000)).intValue();
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(car.replace(priceStr, ""));
        	bean.setType(0);
        	bean.setPrice(-price);
        	replaceBean.setNewCar(bean.getCarInfo());
        	return bean;
        }
        
        Pattern pattern11 = Pattern.compile("((下|!)\\s*(\\d+\\.?\\d*)\\s*(点|%))");
        Matcher matcher11 = pattern11.matcher(car);
        if(matcher11.find()) {
        	String dotStr = matcher11.group(1);
        	BigDecimal dot = new BigDecimal(matcher11.group(3));
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(car.replace(dotStr, ""));
        	bean.setType(1);
        	bean.setDot(dot.negate());
        	replaceBean.setNewCar(bean.getCarInfo());
        	return bean;
        }
        
        Pattern pattern20 = Pattern.compile("((上|加|\\+)\\s*(\\d+\\.?\\d*)\\s*万)");
        Matcher matcher20 = pattern20.matcher(car);
        if(matcher20.find()) {
        	String priceStr = matcher20.group(1);
        	int price = new BigDecimal(matcher20.group(3)).multiply(BigDecimal.valueOf(10000)).intValue();
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(car.replace(priceStr, ""));
        	bean.setType(0);
        	bean.setPrice(price);
        	replaceBean.setNewCar(bean.getCarInfo());
        	return bean;
        }
        
        Pattern pattern21 = Pattern.compile("((上|加|\\+)\\s*(\\d+\\.?\\d*)\\s*(点|%))");
        Matcher matcher21 = pattern21.matcher(car);
        if(matcher21.find()) {
        	String dotStr = matcher21.group(1);
        	BigDecimal dot = new BigDecimal(matcher21.group(3));
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(car.replace(dotStr, ""));
        	bean.setType(1);
        	bean.setDot(dot);
        	replaceBean.setNewCar(bean.getCarInfo());
        	return bean;
        }
        
        Pattern pattern31 = Pattern.compile("((下|!)\\s*(\\d+\\.?\\d*))");
        Matcher matcher31 = pattern31.matcher(car);
        if(matcher31.find()) {
        	String numberStr = matcher31.group(1);
        	BigDecimal number = new BigDecimal(matcher31.group(3));
        	String carInfo = car.replace(numberStr, "");
        	replaceBean.setNewCar(carInfo);
        	return this.guessUnderMarket(number, carInfo);
        }
        
        Pattern pattern32 = Pattern.compile("((上|加|\\+)\\s*(\\d+\\.?\\d*))");
        Matcher matcher32 = pattern32.matcher(car);
        if(matcher32.find()) {
        	String numberStr = matcher32.group(1);
        	BigDecimal number = new BigDecimal(matcher32.group(3));
        	String carInfo = car.replace(numberStr, "");
        	replaceBean.setNewCar(carInfo);
        	return this.guessUpperMarket(number, carInfo);
        }
        
        Pattern pattern40 = Pattern.compile("((卖|售)\\s*(\\d+\\.?\\d*)\\s*万)");
        Matcher matcher40 = pattern40.matcher(car);
        if(matcher40.find()) {
        	String numberStr = matcher40.group(1);
        	BigDecimal number = new BigDecimal(matcher40.group(3));
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(car.replace(numberStr, ""));
        	bean.setType(2);
        	bean.setSell(number.multiply(BigDecimal.valueOf(10000)).intValue());
        	replaceBean.setNewCar(bean.getCarInfo());
        	return bean;
        }
        
        Pattern pattern41 = Pattern.compile("((卖|售)\\s*(\\d+\\.?\\d*))");
        Matcher matcher41 = pattern41.matcher(car);
        if(matcher41.find()) {
        	String numberStr = matcher41.group(1);
        	BigDecimal number = new BigDecimal(matcher41.group(3));
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(car.replace(numberStr, ""));
        	bean.setType(2);
        	replaceBean.setNewCar(bean.getCarInfo());
        	if(number.compareTo(BigDecimal.valueOf(100)) == -1) {
        		bean.setSell(number.multiply(BigDecimal.valueOf(10000)).intValue());
        		return bean;
        	}else if(number.compareTo(BigDecimal.valueOf(10000)) == 1) {
        		bean.setSell(number.intValue());
        		return bean;
        	}
        }
        
        Pattern pattern50 = Pattern.compile("((\\d+\\.?\\d*)\\s*万)");
        Matcher matcher50 = pattern50.matcher(car);
        if(matcher50.find()) {
        	String priceStr = matcher50.group(1);
        	int price = new BigDecimal(matcher50.group(2)).multiply(BigDecimal.valueOf(10000)).intValue();
        	String carInfo = car.replace(priceStr, "");
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(carInfo);
        	bean.setType(0);
        	bean.setPrice(-price);
        	replaceBean.setNewCar(bean.getCarInfo());
        	return bean;
        }
        
        Pattern pattern51 = Pattern.compile("((\\d+\\.?\\d*)\\s*(点|%))");
        Matcher matcher51 = pattern51.matcher(car);
        if(matcher51.find()) {
        	String dotStr = matcher51.group(1);
        	BigDecimal dot = new BigDecimal(matcher51.group(2));
        	String carInfo = car.replace(dotStr, "");
        	MarketBean bean = new MarketBean();
        	bean.setCarInfo(carInfo);
        	bean.setType(1);
        	bean.setDot(dot.negate());
        	replaceBean.setNewCar(bean.getCarInfo());
        	return bean;
        }
        
		return null;
	}
	
	/**
	 * 下行情
	 * @param number
	 * @param carInfo
	 * @return
	 */
	private MarketBean guessUnderMarket(BigDecimal number, String carInfo) {
		MarketBean bean = new MarketBean();
    	bean.setCarInfo(carInfo);
    	if(number.compareTo(BigDecimal.valueOf(300)) == 1 
    			|| number.compareTo(BigDecimal.valueOf(300)) == 0) {
    		bean.setType(0);
    		bean.setPrice(-number.intValue());
    		return bean;//300及以上都算价格
    	}
    	
    	Integer extractOfficialQuote = this.extractOfficialQuote(carInfo);
    	if(extractOfficialQuote == null) {
    		return null;
    	}
    	extractOfficialQuote = extractOfficialQuote * 100;
    	bean.setNumber(number);
    	if(extractOfficialQuote > 10000 && extractOfficialQuote <= 100000) {
    		if(number.compareTo(BigDecimal.valueOf(100)) == 1 
    				|| number.compareTo(BigDecimal.valueOf(100)) == 0) {
        		bean.setType(0);
        		bean.setPrice(-number.intValue());
        		return bean;//十万以内，大于等于100算成价格
        	}else if(number.compareTo(BigDecimal.valueOf(30)) == 1) {
        		return null;//十万以内，大于30小于100既不算价格也不算点数
        	}else if((number.compareTo(BigDecimal.valueOf(30)) == -1 
        			|| number.compareTo(BigDecimal.valueOf(30)) == 0)
        			&& number.compareTo(BigDecimal.valueOf(4)) == 1) {
        		bean.setType(1);
        		bean.setDot(number.negate());
        		return bean;//十万以内，小于等于30，大于4算成点数
        	}else if(number.compareTo(BigDecimal.valueOf(4)) == -1 
        			|| number.compareTo(BigDecimal.valueOf(4)) == 0) {
        		bean.setType(0);
        		bean.setPrice(-number.multiply(BigDecimal.valueOf(10000)).intValue());
        		return bean;//十万以内，小于等于4算成价格
        	}
    	}else {
    		BigDecimal multiply = number.multiply(BigDecimal.valueOf(10000));
    		if(multiply.compareTo(BigDecimal.valueOf(extractOfficialQuote * 0.3)) == -1) {
    			bean.setType(0);
        		bean.setPrice(multiply.negate().intValue());
        		return bean;
    		}else if(number.compareTo(BigDecimal.valueOf(30)) == -1 
    				|| number.compareTo(BigDecimal.valueOf(30)) == 0) {
    			bean.setType(1);
    			bean.setDot(number.negate());
    			return bean;
    		}
    	}
		return null;
	}
	
	/**
	 * 上行情
	 * @param number
	 * @param carInfo
	 * @return
	 */
	private MarketBean guessUpperMarket(BigDecimal number, String carInfo) {
		//加价都算成价格
		MarketBean bean = new MarketBean();
    	bean.setCarInfo(carInfo);
    	if(number.compareTo(BigDecimal.valueOf(100)) == 1 
    			|| number.compareTo(BigDecimal.valueOf(100)) == 0) {
    		bean.setType(0);
    		bean.setPrice(number.intValue());
    		return bean;
    	}
    	Integer extractOfficialQuote = this.extractOfficialQuote(carInfo);
    	if(extractOfficialQuote == null) {
    		return null;
    	}
    	extractOfficialQuote = extractOfficialQuote * 100;
    	bean.setNumber(number);
    	BigDecimal multiply = number.multiply(BigDecimal.valueOf(10000));
		if(multiply.compareTo(BigDecimal.valueOf(extractOfficialQuote * 0.3)) == -1) {
			bean.setType(0);
    		bean.setPrice(multiply.intValue());
    		return bean;//加价不超过百分之30
		}
		return null;
	}
	
	/**
	 * 提取官方报价
	 * @param car
	 * @return
	 */
	private Integer extractOfficialQuote(String car) {
		Pattern pattern = Pattern.compile("(\\d{3,6})");
        Matcher matcher = pattern.matcher(car);
        Integer officialQuote = null;
        while(matcher.find()) {
        	String group = matcher.group(1);
        	if(!group.startsWith("0")) {
        		int parseInt = Integer.parseInt(group);
        		if(officialQuote == null || parseInt > officialQuote) {
        			officialQuote = parseInt;
        		}
        	}
        }
        return officialQuote;
	}
	
	private List<String> extractNumbers(String car) {
		List<String> numbers = new ArrayList<>();
		
		/*Pattern pattern = Pattern.compile("(\\d{4,8})");
		Matcher matcher = pattern.matcher(car);
		while(matcher.find()) {
			String group = matcher.group(1);
			if(!group.startsWith("0")) {
				numbers.add(group);
			}
		}*/
        
        Pattern pattern1 = Pattern.compile("(\\d+(?:\\.\\d+)?)");
        Matcher matcher1 = pattern1.matcher(car);
        while(matcher1.find()) {
        	String str = matcher1.group(1);
        	double group = Double.parseDouble(str);
        	if(group > 0 && group < 10) {
        		numbers.add(str);
        	}else if(group >= 1000 && group < 10000000) {
        		numbers.add(str);
        	}
        }
		return numbers;
	}
	
	private int maxNumber(List<String> numbers) {
		double maxNumber = 0;
		for(String number : numbers) {
			double off = Double.parseDouble(number);
			if(off > maxNumber) {
				maxNumber = off;
			}
		}
		if(maxNumber > 0 && maxNumber < 10) {
			return BigDecimal.valueOf(maxNumber).multiply(BigDecimal.valueOf(10000)).intValue();
		}
		return (int) maxNumber;
	}

	@Override
	public void index() throws Exception {
		//删除原来的索引
        GetIndexRequest seriesRequest = new GetIndexRequest(IndexAttributes.SXB_PARSE_CATEGORY);
        boolean sexists = client.indices().exists(seriesRequest, RequestOptions.DEFAULT);
        if (sexists) {
            DeleteIndexRequest deleteIndexRequest = new DeleteIndexRequest(IndexAttributes.SXB_PARSE_CATEGORY);
			AcknowledgedResponse deleteIndexResponse = client.indices().delete(deleteIndexRequest, RequestOptions.DEFAULT);
            if (!deleteIndexResponse.isAcknowledged()) {
                throw new Exception("删除" + IndexAttributes.SXB_PARSE_CATEGORY + "索引失败");
            }
        }
        
        //创建索引，创建映射
        XContentBuilder builder = XContentFactory.jsonBuilder();
        builder.startObject();

        //start properties
        builder.startObject("properties");

        builder.startObject("brand");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();

        builder.startObject("factoryName");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();
        
        builder.startObject("series");
        builder.field("type", "keyword");
        builder.endObject();
        
        builder.startObject("seriesAlias");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();

        builder.startObject("seriesText");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();
        
        builder.startObject("category");
        builder.field("type", "keyword");
        builder.endObject();
        
        builder.startObject("categoryText");
        builder.field("type", "text");
        builder.field("analyzer", "query_ansj");
        builder.field("search_analyzer", "query_ansj");
        builder.endObject();

        builder.startObject("modeType");
        builder.field("type", "integer");
        builder.endObject();
        
        builder.startObject("officialQuote");
        builder.field("type", "keyword");
        builder.endObject();
        
        builder.startObject("year");
        builder.field("type", "keyword");
        builder.endObject();

        builder.startObject("weight");
        builder.field("type", "integer");
        builder.endObject();
        
        builder.startObject("outColor");
        builder.field("type", "keyword");
        builder.endObject();

        builder.endObject();
        //end properties

        builder.endObject();

        CreateIndexRequest createSeriesRequest = new CreateIndexRequest(IndexAttributes.SXB_PARSE_CATEGORY);
        createSeriesRequest.mapping(builder);
        CreateIndexResponse createSeriesResponse = client.indices().create(createSeriesRequest, RequestOptions.DEFAULT);
        if (!createSeriesResponse.isAcknowledged()) {
            throw new Exception("创建" + IndexAttributes.SXB_PARSE_CATEGORY + "索引失败");
        }
        
        //索引数据
        List<Map<String, Object>> categoryList = carcategoryMapper.getAllForIndex();
        List<Map<String, Object>> carList = new ArrayList<>();
        for (Map<String, Object> categoryMap : categoryList) {
        	Integer officialQuote = (Integer) categoryMap.get("OFFICIALQUOTE");
        	if(officialQuote == null || officialQuote <= 0) {
        		continue;
        	}
        	
            String brand = (String) categoryMap.get("BRAND");
            String factoryName = (String) categoryMap.get("FACTORYNAME");
            String series = (String) categoryMap.get("SERIES");
            String category = (String) categoryMap.get("CATEGORY");
            int modeType = (int) categoryMap.get("MODETYPE");
            String outColor = (String) categoryMap.get("OUTCOLOR");
            
            Map<String, Object> car = new HashMap<>();
            car.put("brand", brand);
            car.put("factoryName", factoryName);
            car.put("series", series);
            car.put("modeType", modeType);
            car.put("category", category);
            
        	List<String> officialQuotes = new ArrayList<>();
        	Integer shortQuote = officialQuote / 100;
        	officialQuotes.add(shortQuote.toString());
        	officialQuotes.add(officialQuote.toString());
        	car.put("officialQuote", officialQuotes);
            	
            String year = suggestService.getYear(category);
            if(StringUtils.hasText(year)) {
            	car.put("year", Integer.parseInt(year));
            }
            if(StringUtils.hasText(outColor)) {
            	car.put("outColor", outColor);
            }
            car.put("weight", this.getWeight(modeType, year, category));
            
            //需求https://shimo.im/docs/hHGCxqQvpcxG8KTt/
            //用户常用的别名
            String seriesAlias = null;
            String addText = " xxxxxxxxx";
            if(brand.equals("宝马") && series.equals("宝马3系") && modeType == 1) {
            	String[] aliases = {"320i", "320Li", "325i", "325Li", "330i", "330Li"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias + addText;
            			break;
            		}
            	}
            }else if(brand.equals("宝马") && series.equals("宝马5系") && modeType == 1) {
            	String[] aliases = {"525Li", "530Li"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias + addText;
            			break;
            		}
            	}
            }else if(brand.equals("宝马") && series.equals("宝马5系(进口)") && modeType == 2) {
            	String[] aliases = {"525i", "530i", "540i"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias + addText;
            			break;
            		}
            	}
            }else if(brand.equals("宝马") && series.equals("宝马5系新能源") && modeType == 1) {
            	String alias = "530Le";
            	if(category.contains(alias)) {
        			seriesAlias = alias + addText;
        		}
            }else if(brand.equals("宝马") && series.equals("宝马7系") && modeType == 2) {
            	String[] aliases = {"730Li", "740Li", "750Li", "760Li"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias + addText;
            			break;
            		}
            	}
            }else if(brand.equals("宝马") && series.equals("宝马4系") && modeType == 2) {
            	String[] aliases = {"425i", "430i"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias + addText;
            			break;
            		}
            	}
            }else if(brand.equals("奔驰") && series.equals("奔驰E级") && modeType == 1) {
            	String[] aliases = {"E 260 L", "E 300 L", "E 350 L"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias;
            			break;
            		}
            	}
            }else if(brand.equals("奔驰") && series.equals("奔驰E级(进口)") && modeType == 2) {
            	String[] aliases = {"E260", "E300", "E350"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias + addText;
            			break;
            		}
            	}
            }else if(brand.equals("奔驰") && series.equals("奔驰E级新能源") && modeType == 1) {
            	String alias = "E 300 e L";
            	if(category.contains(alias)) {
        			seriesAlias = "E300eL";
        		}
            }else if(brand.equals("奔驰") && series.equals("奔驰S级") && modeType == 2) {
            	String[] aliases = {"S320L", "S350L", "S450L", "S500L"};
            	for(String alias : aliases) {
            		if(category.contains(alias)) {
            			seriesAlias = alias;
            			break;
            		}
            	}
            }
            if(seriesAlias != null) {
            	car.put("seriesAlias", seriesAlias);
            }
            car.put("seriesText", this.getSeriesText(series));
            
            if(modeType == 2 || modeType == 3) {
            	category = category + " 进口";
            }
            category = category + " " + year;
            //需求https://shimo.im/docs/hHGCxqQvpcxG8KTt/
            if(brand.equals("奥迪")) {
            	if(category.contains("quattro")) {
            		category = category + " 四驱";
            	}else if(category.contains("Sportback")) {
            		category = category + " 五门 两厢";
            	}else if(category.contains("Limousine")) {
            		category = category + " 三厢";
            	}else if(category.contains("Coupe")) {
            		category = category + " 三门 硬顶";
            	}else if(category.contains("Cabriolet")) {
            		category = category + " 敞篷";
            	}else if(category.contains("Roadster")) {
            		category = category + " 敞篷";
            	}
            }else if(brand.equals("奔驰")) {
            	if(category.contains("运动")) {
            		category = category + " 大标";
            	}else {
            		category = category + " 小标";
            	}
            	if(category.contains("4MATIC")) {
            		category = category + " 四驱";
            	}
            }else if(brand.equals("宝马")) {
            	if(category.contains("xDrive")) {
            		category = category + " 四驱";
            	}else if(category.contains("Gran Coupe")) {
            		category = category + " 四门";
            	}
            }else if(brand.equals("保时捷")) {
            	if(category.contains("Cayenne")) {
            		category = category + " 卡宴";
            	}
            } else if(brand.equals("JEEP")) {
            	if(category.contains("Sahara")) {
            		category = category + " 撒哈拉";
            	}else if(category.contains("Rubicon")) {
            		category = category + " 罗宾汉";
            	}
            }
            car.put("categoryText", category);
            
            carList.add(car);
        }
        
        int len = carList.size();
        int pageCount = (len - 1) / 5000 + 1;
        Map<Integer, BulkRequest> pageMap = new HashMap<>();
        for (int i = 1; i <= pageCount; i++) {
            BulkRequest request = new BulkRequest();
            pageMap.put(i, request);
        }
        for (int i = 1; i <= len; i++) {
            IndexRequest indexRequest = new IndexRequest(IndexAttributes.SXB_PARSE_CATEGORY);
            indexRequest.source(carList.get(i - 1));

            int page = (i - 1) / 5000 + 1;
            BulkRequest bulkRequest = pageMap.get(page);
            bulkRequest.add(indexRequest);
        }
        for (BulkRequest bulkRequest : pageMap.values()) {
            BulkResponse bulkResponse = client.bulk(bulkRequest, RequestOptions.DEFAULT);
            if (bulkResponse.hasFailures()) {
                throw new Exception(IndexAttributes.SXB_PARSE_CATEGORY + "索引数据失败");
            } else {
                logger.info(IndexAttributes.SXB_PARSE_CATEGORY + "索引" + bulkRequest.numberOfActions() + "条数据");
            }
        }
        logger.info(IndexAttributes.SXB_PARSE_CATEGORY + "总共索引" + len + "条数据");
	}
	
	private String getSeriesText(String series) {
        if(series.contains("-")) {
        	series = series + " " + series.replace("-", "");
        }
        return series;
	}
	
	private int getWeight(int modeType, String year, String category) {
		int time = 0;
		int vi = 60;
		if(StringUtils.hasText(year)) {
			time = Integer.parseInt(year) * 100;
		}
		if(category.contains("国V") && !category.contains("国VI")) {
			vi = 50;
		}
        if (modeType == 2 || modeType == 3) {
            return 1000000 + time + vi;
        } else if (modeType > 3) {
            return time + vi;
        }
        return 2000000 + time + vi;
    }
	
	private String getCountry(Integer newCarType, boolean hasNumber) {
		String country = null;
		if(newCarType != null && !hasNumber) {
			if(newCarType == 2) {
				country = "国VI";
			}else if(newCarType == 1) {
				country = "国V";
			}
		}
		return country;
	}
	
	private Area getArea(String car, List<String> terms, boolean hasNumber) throws Exception {
		Area area = null;
		if(!hasNumber && terms != null && terms.size() > 0) {
			area = this.searchArea(car, terms, null, null);
			if(area != null) {
				switch (area.getArea()) {
				case "北京市":
					/*if(car.contains("北京现代")) {
						area = null;
					}else if(car.contains("北京奔驰")) {
						area = null;
					}else if(car.contains("北京汽车")) {
						area = null;
					}else if(car.contains("北京越野")) {
						area = null;
					}else if(car.contains("北京吉普")) {
						area = null;
					}else if(car.contains("北京克莱斯勒")) {
						area = null;
					}else if(car.contains("北京清行")) {
						area = null;
					}*/
					if(car.matches(".*北京现代.*|.*北京奔驰.*|.*北京汽车.*|.*北京越野.*|.*北京吉普.*|.*北京克莱斯勒.*|.*北京清行.*")) {
						area = null;
					}
					break;
				case "天津市":
					if(car.contains("天津一汽")) {
						area = null;
					}
					break;
				case "吉林省":
					if(car.contains("一汽吉林")) {
						area = null;
					}
					break;
				case "四川省":
					if(car.contains("四川现代")) {
						area = null;
					}
					break;
				case "江西省":
					if(car.contains("江西五十铃")) {
						area = null;
					}
					break;
				case "陕西省":
					if(car.contains("陕西通家")) {
						area = null;
					}
					break;
				default:
					break;
				}
			}
		}
		return area;
	}

	@Override
	public DiscernBrand discernBrand(String cars) throws Exception {
		if(StringUtils.isEmpty(cars)) {
			return null;
		}

		cars = cars.replace("吉普", "jeep");

		Set<String> discernByBrand = this.discernByBrand(cars);
		Set<String> brands = new HashSet<>(discernByBrand);

		ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cars.getBytes(StandardCharsets.UTF_8));
		InputStreamReader inputStreamReader = new InputStreamReader(byteArrayInputStream, StandardCharsets.UTF_8);
		BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
		String line = null;
		Map<String, Set<String>> discernByAliasMap = new LinkedHashMap<>();
		Map<String, DiscernBySeries> discernBySeriesMap = new LinkedHashMap<>();
		while ((line = bufferedReader.readLine()) != null) {
			//过滤看不见的字符
			String car = line.trim().replaceAll("\\p{Cf}", "");
			if(car.length() > 0) {
				Set<String> discernByAlias = this.discernByAlias(car);
				DiscernBySeries discernBySeries = this.discernBySeries(car);
				brands.addAll(discernByAlias);
				brands.addAll(discernBySeries.getSet());

				if(discernByAlias.size() > 0) {
					discernByAliasMap.put(car, discernByAlias);
				}
				if(discernBySeries.getSet().size() > 0) {
					discernBySeriesMap.put(car, discernBySeries);
				}
			}
		}

		DiscernBrand discernBrand = new DiscernBrand();
		discernBrand.setBrands(brands);
		discernBrand.setDiscernByBrand(discernByBrand);
		discernBrand.setDiscernByAlias(discernByAliasMap);
		discernBrand.setDiscernBySeries(discernBySeriesMap);

		return discernBrand;
	}

	private Set<String> discernByBrand(String car) throws Exception {
		Set<String> set = new HashSet<>();
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		builder.field("nameText", car);
		builder.endObject();
		BytesReference bytesReference = BytesReference.bytes(builder);
		PercolateQueryBuilder percolateQueryBuilder = new PercolateQueryBuilder("query", bytesReference, XContentType.JSON);

		SearchSourceBuilder brandSearchSourceBuilder = new SearchSourceBuilder();
		brandSearchSourceBuilder.query(percolateQueryBuilder);
		brandSearchSourceBuilder.size(20);

		SearchRequest brandSearchRequest = new SearchRequest(IndexAttributes.SXB_BRAND);
		brandSearchRequest.source(brandSearchSourceBuilder);
		SearchResponse brandSearchResponse = client.search(brandSearchRequest, RequestOptions.DEFAULT);
		SearchHits brandSearchHits = brandSearchResponse.getHits();
		if(brandSearchHits.getTotalHits().value > 0) {
			for(SearchHit hit : brandSearchHits.getHits()) {
				Map<String, Object> sourceAsMap = hit.getSourceAsMap();
				set.add(sourceAsMap.get("name").toString());
			}
		}
		return set;
	}

	private DiscernBySeries discernBySeries(String car) throws Exception {
		DiscernBySeries discernBySeries = new DiscernBySeries();
		Set<String> set = new HashSet<>();
		discernBySeries.setSet(set);

		//至少4个词的精确匹配
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		builder.field("seriesText", car);
		builder.endObject();
		BytesReference bytesReference = BytesReference.bytes(builder);
		PercolateQueryBuilder percolateQueryBuilder = new PercolateQueryBuilder("accurateQuery", bytesReference, XContentType.JSON);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(percolateQueryBuilder);
		searchSourceBuilder.size(10);
		searchSourceBuilder.sort("_score");
		searchSourceBuilder.sort("sxbCount", SortOrder.DESC);

		SearchRequest searchRequest = new SearchRequest(IndexAttributes.SXB_SERIES);
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		SearchHits hits = searchResponse.getHits();
		if(hits.getTotalHits().value > 0) {
			for(SearchHit searchHit : hits.getHits()) {
				Map<String, Object> sourceAsMap = searchHit.getSourceAsMap();
				set.add(sourceAsMap.get("brand").toString());
			}
			discernBySeries.setBy("精确");
		}else{
			//至少2个词的模糊匹配
			XContentBuilder builder1 = XContentFactory.jsonBuilder();
			builder1.startObject();
			builder1.field("seriesText", car);
			builder1.endObject();
			BytesReference bytesReference1 = BytesReference.bytes(builder1);
			PercolateQueryBuilder percolateQueryBuilder1 = new PercolateQueryBuilder("query", bytesReference1, XContentType.JSON);

			SearchSourceBuilder searchSourceBuilder1 = new SearchSourceBuilder();
			searchSourceBuilder1.query(percolateQueryBuilder1);
			searchSourceBuilder1.size(1);

			SearchRequest searchRequest1 = new SearchRequest(IndexAttributes.SXB_SERIES);
			searchRequest1.source(searchSourceBuilder1);
			SearchResponse searchResponse1 = client.search(searchRequest1, RequestOptions.DEFAULT);
			SearchHits searchHits = searchResponse1.getHits();
			if(searchHits.getTotalHits().value > 0) {
				Map<String, Object> sourceAsMap = searchHits.getAt(0).getSourceAsMap();
				set.add(sourceAsMap.get("brand").toString());
				discernBySeries.setBy("模糊");
			}
		}
		return discernBySeries;
	}

	private Set<String> discernByAlias(String car) throws Exception {
		Set<String> set = new HashSet<>();
		XContentBuilder builder = XContentFactory.jsonBuilder();
		builder.startObject();
		builder.field("alias", car);
		builder.endObject();
		BytesReference bytesReference = BytesReference.bytes(builder);
		PercolateQueryBuilder percolateQueryBuilder = new PercolateQueryBuilder("query", bytesReference, XContentType.JSON);

		TermsAggregationBuilder brandGroup = AggregationBuilders.terms("brandGroup");
		brandGroup.field("brand");
		brandGroup.size(10);

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(percolateQueryBuilder);
		searchSourceBuilder.size(0);
		searchSourceBuilder.aggregation(brandGroup);

		SearchRequest searchRequest = new SearchRequest(IndexAttributes.SXB_SERIES_ALIAS);
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
		Aggregations aggregations = searchResponse.getAggregations();
		Terms brandGroupTerms = aggregations.get("brandGroup");
		List<? extends Bucket> brandGroupBuckets = brandGroupTerms.getBuckets();
		if(brandGroupBuckets.size() > 0) {
			for(Bucket brandGroupBucket : brandGroupBuckets) {
				set.add(brandGroupBucket.getKeyAsString());
			}
		}
		return set;
	}
}
