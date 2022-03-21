package tomcat.http;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 龙恒建
 * @date 2021-03-15 01:07
 * @ClassName StandardServletConfig
 * @description: ServletConfig 是在 Servlet 初始化的时候，传递进去的参数对象
 */
public class StandardServletConfig implements ServletConfig {
    private ServletContext servletContext;
    private Map<String, String> initParameters;
    private String servletName;

    public StandardServletConfig(ServletContext servletContext, String servletName, Map<String, String> initParameters) {
        this.servletContext = servletContext;
        this.servletName = servletName;
        this.initParameters = initParameters;
        if (null == this.initParameters) {
            this.initParameters = new HashMap<>();
        }
    }

    @Override
    public String getInitParameter(String name) {
        // TODO Auto-generated method stub
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getServletName() {
        return servletName;
    }
}
