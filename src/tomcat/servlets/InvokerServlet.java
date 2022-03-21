package tomcat.servlets;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.log.LogFactory;
import tomcat.catalina.Context;
import tomcat.http.Request;
import tomcat.http.Response;
import tomcat.util.Constant;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author 龙恒建
 * @date 2021/03/13
 * @result InvokerServlet
 * 处理servlet
 * 提供 service 方法，根据 请求的uri 获取 ServletClassName ，
 * 然后实例化，接着调用其 service 方法
 */
public class InvokerServlet extends HttpServlet {
    private static InvokerServlet instance = new InvokerServlet();

    public static synchronized InvokerServlet getInstance() {
        return instance;
    }

    private  InvokerServlet() {

    }

    /**
     * 根据请求的uri获取ServletClassName,然后实例化，接着调用其service方法。
     * 因为目标servlet实现了Httpservlet，所以一定要实现service方法，
     * 这个service方法，会根据request的Method，访问对应的doGrt或者doPost
     * @param httpServletRequest
     * @param httpServletResponse
     * @throws IOException
     * @throws ServletException
     */
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        Request request = (Request) httpServletRequest;
        Response response = (Response) httpServletResponse;

        String uri = request.getUri();
        Context context = request.getContext();
        String servletClassName = context.getServletClassName(uri);

        try {
            Class servletClass = context.getWebappClassLoader().loadClass(servletClassName);
            LogFactory.get().info("servletClass:{}",servletClass);
            LogFactory.get().info("servletClass'classLoader:{}",servletClass.getClassLoader());
            Object servletObject = context.getServlet(servletClass);
            ReflectUtil.invoke(servletObject, "service", request, response);
            if(null!=response.getRedirectPath())
                response.setStatus(Constant.CODE_302);
            else
                response.setStatus(Constant.CODE_200);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
