package me.dowen.solr.analyzers;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

/**
 * 范围扩展分词过滤器工厂类
 * @author liufl / 2014年6月30日
 *
 */
public class ScopeExpandTokenFilterFactory extends TokenFilterFactory
		implements ResourceLoaderAware {

	private String expandsFile; // 扩展字典文件
	private Map<String, Set<String>> refs = new HashMap<String, Set<String>>(); // 扩展字典存储结构
	private boolean keepInput = true; // 保留原输入词元开关

	/**
	 * 构造器，供Solr/Lucene调用
	 * @param args 分词配置
	 */
	public ScopeExpandTokenFilterFactory(Map<String, String> args) {
		super(args);
		this.expandsFile = get(args, "expands");
		this.keepInput = getBoolean(args, "keepInput", true); // 默认保留原输入
	}

	/**
	 * 自动加载字典文件
	 */
	@Override
	public void inform(ResourceLoader loader) throws IOException {
		if (this.expandsFile != null) { // 配置了字典文件参数
			List<String> lines = getLines(loader, this.expandsFile); // 字典文件内容，一行一条
			if (lines != null) {
				for (String line : lines) {
					String[] parts = line.split("=>"); // [原词]=>[扩展] 同一原词可有多行扩展配置，一行配置只能配置一种扩展结果
					// 过滤合法数据
					if (parts.length != 2) {
						continue; // 结构不对
					}
					if (parts[0] == null || "".equals(parts[0].trim())) {
						continue; // 原词空白
					}
					if (parts[1] == null || "".equals(parts[1].trim())) {
						continue; // 扩展空白
					}
					// 加入扩展集
					Set<String> expands = null;
					expands = this.refs.get(parts[0].trim());
					if (expands == null) {
						expands = new HashSet<String>();
					}
					expands.add(parts[1].trim());
					this.refs.put(parts[0].trim(), expands);
				}
			}
			// 合并级联扩展。如连衣裙>>[裙子]、裙子>>[裙] ==== 连衣裙>>[裙子,裙]
			for (Set<String> expands : this.refs.values()) {
				expands.addAll(fillExpandsChain(expands));
			}
		}
	}

	/**
	 * 合并级联扩展。如连衣裙>>[裙子]、裙子>>[裙] ==== 连衣裙>>[裙子,裙]
	 * @param expands
	 * @return
	 */
	private Collection<String> fillExpandsChain(Set<String> expands) {
		Collection<String> _expands = new HashSet<String>(expands);
		Set<String> temp = new HashSet<String>();
		for (String expand : _expands) {
			this.expands(expand, temp);
		}
		_expands.addAll(temp);
		return _expands;
	}

	private void expands(String expand, Collection<String> expands) {
		if (this.refs.containsKey(expand)) {
			for (String nextExpand : this.refs.get(expand)) {
				if (expands.contains(nextExpand)) {
					continue;
				}
				expands.add(nextExpand);
				this.expands(nextExpand, expands);
			}
		}
	}

	/**
	 * 返回过滤器对象
	 */
	@Override
	public TokenStream create(TokenStream input) {
		ScopeExpandTokenFilter filter = new ScopeExpandTokenFilter(input, this.refs);
		filter.setKeepInput(this.keepInput); // 设置过滤器属性
		return filter;
	}

}
