package xyz.dowenwork.lucene.analyzer;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * 范围扩展分词过滤器，也可作词义扩大器
 *
 * @author liufl / 2014再调用年6月30日
 */
public class ScopeExpandTokenFilter extends TokenFilter {

    private Map<String, Set<String>> expands; // 扩展字典
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class); // 词元记录
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class); // 词元属性记录
    private char[] curTermBuffer; // 底层词元输入缓存
    private Set<String> curExpands = null; // 当前输入词元对应的扩展集
    private Iterator<String> iterator = null; // 扩展集迭代器
    private boolean keepInput = true; // 保留输入开关
    private boolean notOutInput = true; // 是否输出当前输入开关

    /**
     * 构造器
     *
     * @param in       词元输入
     * @param rootRefs 扩展字典
     */
    public ScopeExpandTokenFilter(TokenStream in, Map<String, Set<String>> rootRefs) {
        super(in);
        this.expands = rootRefs;
    }

    /**
     * 是否保留词元输入
     *
     * @return true保留;false不保留
     */
    public boolean isKeepInput() {
        return keepInput;
    }

    /**
     * 设置是否保留词元输入。默认保留
     *
     * @param keepInput true保留;false不保留
     */
    public void setKeepInput(boolean keepInput) {
        this.keepInput = keepInput;
    }

    /**
     * 分词过滤。<br/>
     * 该方法在上层调用中被循环调用，直到该方法返回false
     */
    @Override
    public boolean incrementToken() throws IOException {
        while (true) {
            if (this.curTermBuffer == null) { // 开始处理或上一输入词元已被处理完成
                if (!this.input.incrementToken()) { // 获取下一词元输入
                    return false; // 没有后继词元输入，处理完成，返回false，结束上层调用
                }
                // 缓存词元输入
                this.curTermBuffer = this.termAtt.buffer().clone();
                char[] temp = new char[this.termAtt.length()];
                System.arraycopy(this.curTermBuffer, 0, temp, 0, temp.length);
                this.curTermBuffer = temp;
            }
            // 判断是否匹配扩展词
            if (this.curExpands == null) { // 未匹配，是新一轮输入（不会是第二轮循环，下个if说明）
                // 匹配扩展词
                if (this.expands != null) { // 完全扩展集存在
                    // 获取当前输入对应扩展词
                    this.curExpands = this.expands.get(new String(this.curTermBuffer));
                }
            }
            if (this.curExpands == null || this.curExpands.isEmpty()) {
                // 匹配扩展词，结果不能扩展
                // 清理，准备接受下一输入
                this.curExpands = null;
                this.curTermBuffer = null; // 缓存置为null，下一循环获取下一词元输入
                return true; // 返回true,上层调用控制进入下次循环
            }
            // 存在当前扩展词
            // 迭代当前扩展词
            if (this.iterator == null) {
                this.iterator = this.curExpands.iterator();
            }
            if (this.iterator.hasNext()) {
                String expand = this.iterator.next();
                this.termAtt.copyBuffer(expand.toCharArray(), 0, expand.length()); // 写入扩展词元
                this.typeAtt.setType("EXPANSION"); // 词元属性为EXPANSION
                return true; // 继续
            }
            // 扩展输出完毕
            // 处理输入原词元
            if (this.keepInput && this.notOutInput) { // 准许输出输入词元且当前没有输出原输入词元
                this.notOutInput = false; // 标记已输出
                this.termAtt.copyBuffer(this.curTermBuffer, 0,
                        this.curTermBuffer.length); // 写入原输入词元
                return true; // 继续
            }
            // 扩展输出完毕且原输入词元处理完毕，清理，准备进行接受下一输入词元
            this.notOutInput = true;
            this.curExpands = null;
            this.curTermBuffer = null;
            this.iterator = null;
        }
    }

}
