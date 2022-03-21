package tomcat.http;

import tomcat.catalina.HttpProcessor;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author 龙恒建
 * @date 2021-03-16 17:42
 * @ClassName ApplicationRequestDispatcher
 * @description: 实现了 RequestDispatcher接口，用于进行服务端跳转。
 *
 * 我们做服务端跳转的思路是，修改 request的uri,
 * 然后通过 HttpProcessor 的 execute 再执行一次。
 * 相当于在服务器内部再次访问了某个页面
 */
public class ApplicationRequestDispatcher implements RequestDispatcher {
    private String uri;
    public ApplicationRequestDispatcher(String uri) {
        if (!uri.startsWith("/"))
            uri = "/" + uri;
        this.uri = uri;
    }

    @Override
    public void forward(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        Request request = (Request) servletRequest;
        Response response = (Response) servletResponse;

        request.setUri(uri);

        HttpProcessor processor = new HttpProcessor();
        processor.execute(request.getSocket(), request,response);
        request.setForwarded(true);
    }

    @Override
    public void include(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

    }
}
