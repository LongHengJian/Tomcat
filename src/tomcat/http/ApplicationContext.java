package tomcat.http;

import tomcat.catalina.Context;

import java.io.File;
import java.util.*;

/**
 * @author 龙恒建
 * @date 2021/03/10
 * @result ApplicationContext
 * 创建一个 attributesMap 属性，用于存放 属性
 * 内置一个 context， ApplicationContext 的很多方法，其实就是调用的它
 */
public class ApplicationContext extends BaseServletContext {
    private Map<String, Object> attributesMap;
    private Context context;

    public ApplicationContext(Context context) {
        this.attributesMap = new HashMap<>();
        this.context = context;
    }

    /**
     * 然后围绕 attributesMap 重写一一批方法，
     * 这部分在 jsp 里的用法就是那个 <% application.setAttribute()%> ，
     * 那个 application 内置对象就是这个 ApplicationContext
     * @param name
     */
    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }

    public void setAttributesMap(String name, Object value) {
        attributesMap.put(name, value);
    }

    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }

    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    /**
     * 重写了 getRealPath , 来获取硬盘上的真是路径。
     * @param path
     * @return
     */
    public String getRealPath(String path) {
        return new File(context.getDocBase(), path).getAbsolutePath();
    }
}
