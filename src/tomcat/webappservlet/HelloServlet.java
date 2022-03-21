package tomcat.webappservlet;
/**
 * @author 龙恒建
 * @date 2021/03/09
 * @result hello Servlet
 *
 */

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class HelloServlet extends HttpServlet {
    @Override
    public void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        try {
            httpServletResponse.getWriter().println("Hello Tomcat from HelloServlet");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
