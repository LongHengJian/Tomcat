package tomcat.catalina;

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import tomcat.http.Request;
import tomcat.http.Response;
import tomcat.servlets.DefaultServlet;
import tomcat.servlets.InvokerServlet;
import tomcat.servlets.JspServlet;
import tomcat.util.Constant;
import tomcat.util.SessionManager;

import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.List;

/**
 * @author 龙恒建
 * @date 2021/03/09
 * @result 处理请求类
 *
 */

public class HttpProcessor {
    /**
     * 处理相应的请求
     * @param socket
     * @param request
     * @param response
     */
    public void execute(Socket socket, Request request, Response response){
        try {
            String uri = request.getUri();
            if(null==uri)
                return;

            prepareSession(request, response);
            Context context = request.getContext();
            String servletClassName = context.getServletClassName(uri);
            HttpServlet workingServlet;

            if(null!=servletClassName){
                workingServlet = InvokerServlet.getInstance();
            }
            else if (uri.endsWith(".jsp")) {
                workingServlet = JspServlet.getInstance();
            }
            else
                workingServlet = DefaultServlet.getInstance();

            List<Filter> filters = request.getContext().getMatchedFilters(request.getRequestURI());
            ApplicationFilterChain filterChain = new ApplicationFilterChain(filters, workingServlet);
            filterChain.doFilter(request, response);

            if(request.isForwarded())
                return;

            if(Constant.CODE_200 == response.getStatus()){
                handle200(socket, request, response);
                return;
            }
            if(Constant.CODE_302 == response.getStatus()){
                handle302(socket, response);
                return;
            }
            if(Constant.CODE_404 == response.getStatus()){
                handle404(socket, uri);
                return;
            }
        } catch (Exception e) {
            LogFactory.get().error(e);
            handle500(socket,e);
        }
        finally{
            try {
                if(!socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  准备session，
     *  先通过cookie拿到jsessionid，
     *  然后通过SessionManager创建session
     *  并且设置request上
     * @param request
     * @param response
     */
    public void prepareSession(Request request, Response response) {
        String jsessionid= request.getJSessionIdFromCookie();
        HttpSession session = SessionManager.getSession(jsessionid, request, response);
        request.setSession(session);
    }

    /**
     * 返回200响应重构成独立的方法
     * @param socket
     * @param response
     * @throws IOException
     */
    private void handle200(Socket socket, Request request, Response response) throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        String contentType = response.getContentType();
        //根据reponse对象上的contentType,组成返回的头信息，并且转化成字节数组
        byte[] body = response.getBody();
        String cookiesHeader = response.getCookiesHeader();
        boolean gzip = isGzip(request, body, contentType);
        String headText;
        if (gzip)
            headText = Constant.response_head_200_gzip;
        else
            headText = Constant.response_head_200;
        headText = StrUtil.format(headText, contentType, cookiesHeader);
        if (gzip)
            body = ZipUtil.gzip(body);
        byte[] head = headText.getBytes();
        //获取主题信息部分，即 html 对应的 字节数组。
        byte[] responseBytes = new byte[head.length + body.length];
        ArrayUtil.copy(head, 0, responseBytes, 0, head.length);
        ArrayUtil.copy(body, 0, responseBytes, head.length, body.length);

        //拼接头信息和主题信息，成为一个响应字节数组
        outputStream.write(responseBytes, 0, responseBytes.length);//最后把这个响应字节数组返回浏览器。
        outputStream.flush();
        outputStream.close();

        //响应信息观察释放
        //LogFactory.get().info("Response is\n{}",responseBytes);
    }

    /**
     * 处理302跳转
     * @param socket
     * @param response
     * @throws IOException
     */
    private void handle302(Socket socket, Response response) throws IOException {
        OutputStream os = socket.getOutputStream();
        String redirectPath = response.getRedirectPath();
        String head_text = Constant.response_head_302;
        String header = StrUtil.format(head_text, redirectPath);
        byte[] responseBytes = header.getBytes("utf-8");
        os.write(responseBytes);
    }

    /**
     * 返回404响应重构成独立的方法
     * @param socket
     * @param uri
     * @throws IOException
     */
    protected void handle404(Socket socket, String uri)throws IOException {
        OutputStream outputStream = socket.getOutputStream();
        String responseText = StrUtil.format(Constant.textFormat_404,uri,uri);
        responseText = Constant.response_head_404 + responseText;
        byte[] responseByte = responseText.getBytes("utf-8");
        outputStream.write(responseByte);
    }

    /**
     * 返回500响应重构成独立的方法
     * @param socket
     * @param exception
     * @throws IOException
     */

    protected void handle500(Socket socket, Exception exception) {
        try {
            OutputStream outputStream = socket.getOutputStream();
            StackTraceElement stackTraceElements[] = exception.getStackTrace();
            StringBuffer stringBuffer = new StringBuffer();
            stringBuffer.append(exception.toString());
            stringBuffer.append("\r\n");
            for (StackTraceElement stackTraceElement : stackTraceElements) {
                stringBuffer.append("\t");
                stringBuffer.append(stackTraceElement.toString());
                stringBuffer.append("\r\n");
            }

            String msg = exception.getMessage();

            if(null != msg && msg.length() > 20)
                msg = msg.substring(0,19);

            String text = StrUtil.format(Constant.textFormat_500,msg,exception.toString(), stringBuffer.toString());
            text = Constant.response_head_500 + text;
            byte[] responsebytes = text.getBytes();
            outputStream.write(responsebytes);
        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  判断是否要进行gzip
     * @param request
     * @param body
     * @param mimeType
     * @return
     */
    private boolean isGzip(Request request, byte[] body, String mimeType) {
        String acceptEncodings=  request.getHeader("Accept-Encoding");
        if(!StrUtil.containsAny(acceptEncodings, "gzip"))
            return false;


        Connector connector = request.getConnector();
        if (mimeType.contains(";"))
            mimeType = StrUtil.subBefore(mimeType, ";", false);
        if (!"on".equals(connector.getCompression()))
            return false;
        if (body.length < connector.getCompressionMinSize())
            return false;
        String userAgents = connector.getNoCompressionUserAgents();
        String[] eachUserAgents = userAgents.split(",");
        for (String eachUserAgent : eachUserAgents) {
            eachUserAgent = eachUserAgent.trim();
            String userAgent = request.getHeader("User-Agent");
            if (StrUtil.containsAny(userAgent, eachUserAgent))
                return false;
        }
        String mimeTypes = connector.getCompressableMimeType();
        String[] eachMimeTypes = mimeTypes.split(",");
        for (String eachMimeType : eachMimeTypes) {
            if (mimeType.equals(eachMimeType))
                return true;
        }
        return false;
    }

}
