scopeExpandTokenFilter
======================

Apache Solr（语义）范围扩展分词过滤器。  
如底层分词Stream组件返回结果“筒靴”时扩展出“鞋”，因为“筒靴”定义的语义集合是“鞋”定义语义集合的子集。  
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
	无袖背心裙=>无袖裙
	双肩包=>箱包
	凉拖=>鞋
	筒靴=>靴子
	靴子=>鞋
	...

扩展规则书写规则：

1. {原词元}=>{扩展词元}  
2. "=>"前后不含空白

如*西服=>正装*表示“‘西服’是‘正装’”，输出效果为{西服} --> {西服, 正装}
