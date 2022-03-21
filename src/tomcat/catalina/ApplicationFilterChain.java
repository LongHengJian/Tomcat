package tomcat.catalina;

import cn.hutool.core.util.ArrayUtil;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;

/**
 * @author 龙恒建
 * @date 2021-03-16 23:56
 * @ClassName ApplicationFilterChain
 * @description: 使多个对象都有机会处理请求，从而避免请求的发送者和接受者之间的耦合关系， 将这个对象连成一条链，并沿着这条链传递该请求，直到有一个对象处理他为止。
 */
public class ApplicationFilterChain implements FilterChain {

    private Filter[] filters;
    private Servlet servlet;
    int pos;

    public ApplicationFilterChain(List<Filter> filterList, Servlet servlet) {
        this.filters = ArrayUtil.toArray(filterList, Filter.class);
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
        if (pos < filters.length) {
            Filter filter = filters[pos++];
            filter.doFilter(request, response, this);
        } else {
            servlet.service(request, response);
        }
    }
}
