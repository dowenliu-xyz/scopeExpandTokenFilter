scopeExpandTokenFilter
======================

Apache Solr（语义）范围扩展分词过滤器。  
如底层分词Stream组件返回结果“男靴”时扩展出“鞋”，因为“男靴”定义的语义集合是“鞋”定义语义集合的子集。  
具体扩展规则需要手工指定。

在Solr 4.7.0版本中测试通过。

#用法

##示例

	<fieldType name="text_general" class="solr.TextField"
		positionIncrementGap="100">
		<analyzer type="index">
			<tokenizer class="solr.StandardTokenizerFactory" />
			<filter class="me.dowen.solr.analyzers.ScopeExpandTokenFilterFactory" expands="se.txt"/>
			<filter class="solr.StopFilterFactory" ignoreCase="true"
				words="stopwords.txt" />
			<filter class="solr.LowerCaseFilterFactory" />
			<filter class="solr.RemoveDuplicatesTokenFilterFactory"/>
		</analyzer>
		<analyzer type="query">
			<tokenizer class="solr.StandardTokenizerFactory" />
			<filter class="solr.StopFilterFactory" ignoreCase="true"
				words="stopwords.txt" />
			<filter class="solr.SynonymFilterFactory" synonyms="synonyms.txt"
				ignoreCase="true" expand="true" />
			<filter class="solr.LowerCaseFilterFactory" />
		</analyzer>
	</fieldType>

##配置项

###expands

指定扩展规则的配置文件名。文件通常设置在对应core的conf/下。此参数无默认值，且必需在schema fieldType声明中指定，否则分词器加载失败、对应的core无法使用。示例中指定的文件名为se.txt  
se.txt中部分内容如下所示:

	西服=>正装
	凉拖=>鞋
	男靴=>男鞋
	男靴=>靴子
	靴子=>鞋
	男鞋=>鞋
	...

扩展规则书写规则：

1. {原词元}=>{扩展词元}  
2. "=>"前后不含空白

如*西服=>正装*表示“‘西服’是‘正装’”，输出效果为{西服} --> {西服, 正装}

扩展支持多向和级联。  
多向：如“男靴”可扩展出“男鞋”和“靴子”两个词。  
级联：如“男靴”可扩展出“靴子”而“靴子”又可扩展出“鞋”，故“男靴”可扩展出“靴子”和“鞋”两个词。  
综合：“男靴”可扩展出“男鞋”，“靴子”，“鞋”三个词。

扩展的多个方向或存在相同结果，自动去重。

此分词过滤器适合用在索引过程扩词，不适合用在查询过程，因为可能会使查询结果分散（QueryOperater是OR的情况）且影响查询效率。
