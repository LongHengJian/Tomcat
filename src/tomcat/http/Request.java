package tomcat.http;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.log.LogFactory;
import tomcat.catalina.Connector;
import tomcat.catalina.Context;
import tomcat.catalina.Engine;
import tomcat.catalina.Service;
import tomcat.util.MiniBrowser;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;

/**
 * @author 龙恒建
 * @date 2021/03/09
 * @result Request
 * 处理request请求对象
 * 创建 Request 对象用来解析 requestString 和 uri。
 */

public class Request extends BaseRequest{
    private String requestString; //请求的uri
    private String uri;
    private Socket socket;
    private Context context;//代表应用
    private Connector connector;
    private String method;
    private String queryString; //查询字符串
    private HttpSession session; //session属性
    private Cookie[] cookies; //cookie属性
    private Map<String,String[]> parameterMap; //参数Map
    private Map<String,String> headerMap; //声明headerMap用于存放头信息
    private boolean forwarded; //服务端跳转
    private Map<String, Object> attributesMap; //用于存放参数

    public Request(Socket socket, Connector connector) throws IOException {
        this.socket = socket;
        this.connector = connector;

        this.parameterMap = new HashMap<>();
        this.headerMap = new HashMap<>();
        this.attributesMap = new HashMap<>();
        parseHttpRequest();
        if(StrUtil.isEmpty(requestString))
            return;
        parseUri();
        parseContext();
        parseMethod();
        if (!"/".equals(context.getPath())) {
            uri = StrUtil.removePrefix(uri, context.getPath());
            if (StrUtil.isEmpty(uri))
                uri = "/";
        }

        parseParameters();
        parseHeaders();
        parseCookies();

        //需要观察请求对象处理的时候可以把注释去掉
        /*System.out.println();
        LogFactory.get().info("RequestHead is\n{}",headerMap);
        System.out.println();
        LogFactory.get().info("RequestString is\n{}",requestString);*/
    }

    /**
     *  parseHttpRequest 用于解析 http请求字符串，
     *  这里面就调用了 MiniBrowser里重构的 readBytes 方法。
     * @throws IOException
     */
    private void parseHttpRequest() throws IOException {
        InputStream inputStream = this.socket.getInputStream();
        byte[] bytes = MiniBrowser.readBytes(inputStream,false);
        requestString = new String(bytes,"utf-8");
    }

    /**
     * 解析uri
     */
    private void parseUri() {
        String temp;

        temp = StrUtil.subBetween(requestString," "," ");
        //System.out.println("temp:"+ temp);
        //无参数时
        if(!StrUtil.contains(temp,'?')) {
            uri = temp;
            System.out.println("uri是"+uri);
            System.out.println(requestString);
            return;
        }
        //有参数时
        temp = StrUtil.subBefore(temp,'?',false);
        uri = temp;
    }

    /**
     * 解析Context的方法，
     * 通过uri的信息来得到path，
     * 然后根据这个path来获取Context对象，
     * 如果获取不到，那就获取“/”对应的ROOT Context
     */
    private void parseContext() {
        Service service = connector.getService();
        Engine engine = service.getEngine();
        context = engine.getDefaultHost().getContext(uri);
        if (null!=context)
            return;
        String path = StrUtil.subBetween(uri,"/","/");
        if (null == path)
            path = "/";
        else
            path = "/" + path;

        context = engine.getDefaultHost().getContext(path);
        if (null == context) {
            context =engine.getDefaultHost().getContext("/");
        }
    }

    /**
     * 解析方法
     */
    private void parseMethod() {
        method = StrUtil.subBefore(requestString, " ", false);
    }

    /**
     * 根据get和post方式分别解析参数，参数Map里存放的值是字符串数组类型
     */
    private void parseParameters() {
        if ("GET".equals(this.getMethod())) {
            String url = StrUtil.subBetween(requestString, " "," ");
            if (StrUtil.contains(url,'?')) {
                queryString = StrUtil.subAfter(url, '?', false);
            }
        }
        if ("POST".equals(this.getMethod())) {
            queryString = StrUtil.subAfter(requestString, "\r\n\r\n", false);
        }
        if (null == queryString || 0==queryString.length())
            return;
        queryString = URLUtil.decode(queryString);
        String[] parameterValues = queryString.split("&");
        if (null != parameterValues) {
            for (String parameterValue : parameterValues) {
                String[] nameValues = parameterValue.split("=");
                String name = nameValues[0];
                String value = nameValues[1];
                String values[] = parameterMap.get(name);

                if (null == values) {
                    values = new String[] {value};
                    parameterMap.put(name, values);
                }
                else {
                    values = ArrayUtil.append(values, value);
                    parameterMap.put(name, values);
                }
            }
        }
    }

    /**
     * 从requestString中解析出来这些header
     */
    public void parseHeaders() {
        StringReader stringReader = new StringReader(requestString);
        List<String> lines = new ArrayList<>();
        IoUtil.readLines(stringReader, lines);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            if (0 == line.length())
                break;
            String[] segs = line.split(":");
            String headerName = segs[0].toLowerCase();
            String headerValue = segs[1];
            headerMap.put(headerName, headerValue);
            //System.out.println(line);
        }
        System.out.println();
    }

    /**
     * 从http请求协议中解析出Cookie
     */
    private void parseCookies() {
        List<Cookie> cookieList = new ArrayList<>();
        String cookies = headerMap.get("cookie");
        if (null != cookies) {
            String[] pairs = StrUtil.split(cookies,";");
            for (String pair : pairs) {
                if (StrUtil.isBlank(pair))
                    continue;
                String[] segs = StrUtil.split(pair,"=");
                String name = segs[0].trim();
                String value = segs[1].trim();
                Cookie cookie = new Cookie(name,value);
                cookieList.add(cookie);
            }
        }
        this.cookies = ArrayUtil.toArray(cookieList,Cookie.class);
    }

    /**
     * 返回ApplicationRequestDispatcher 对象
     * @param uri
     * @return
     */
    public RequestDispatcher getRequestDispatcher(String uri) {
        return new ApplicationRequestDispatcher(uri);
    }

    public void removeAttribute(String name) {
        attributesMap.remove(name);
    }
    public void setAttribute(String name, Object value) {
        attributesMap.put(name, value);
    }
    public Object getAttribute(String name) {
        return attributesMap.get(name);
    }
    public Enumeration<String> getAttributeNames() {
        Set<String> keys = attributesMap.keySet();
        return Collections.enumeration(keys);
    }

    public Socket getSocket() {
        return socket;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    /**
     * 从cookie中获取sessionid
     * @return
     */
    public String getJSessionIdFromCookie() {
        if (null == cookies)
            return null;
        for (Cookie cookie : cookies) {
            if ("JSESSIONID".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public Connector getConnector() {
        return connector;
    }

    public String getHeader(String name) {
        if(null==name)
            return null;
        name = name.toLowerCase();
        return headerMap.get(name);
    }
    public Enumeration getHeaderNames() {
        Set keys = headerMap.keySet();
        return Collections.enumeration(keys);
    }
    public int getIntHeader(String name) {
        String value = headerMap.get(name);
        return Convert.toInt(value, 0);
    }

    public String getParameter(String name) {
        String values[] = parameterMap.get(name);
        if (null != values && 0 != values.length)
            return values[0];
        return null;
    }

    public Map getParameterMap() {
        return parameterMap;
    }

    public Enumeration getParameterNames() {
        return Collections.enumeration(parameterMap.keySet());
    }

    public String[] getParameterValues(String name) {
        return parameterMap.get(name);
    }

    public String getUri() {
        return uri;
    }

    public String getRequestString() {
        return requestString;
    }

    public Context getContext() {
        return context;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public ServletContext getServletContext() {
        return context.getServletContext();
    }

    @Override
    public String getRealPath(String path) {
        return getServletContext().getRealPath(path);
    }

    public String getLocalAddr() {
        return socket.getLocalAddress().getHostAddress();
    }

    public String getLocalName() {
        return socket.getLocalAddress().getHostName();
    }

    public int getLocalPort() {

        return socket.getLocalPort();
    }

    public String getProtocol() {

        return "HTTP:/1.1";
    }

    public String getRemoteAddr() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        String temp = isa.getAddress().toString();
        return StrUtil.subAfter(temp, "/", false);
    }

    public String getRemoteHost() {
        InetSocketAddress isa = (InetSocketAddress) socket.getRemoteSocketAddress();
        return isa.getHostName();
    }

    public int getRemotePort() {
        return socket.getPort();
    }

    public String getScheme() {
        return "http";
    }

    public String getServerName() {
        return getHeader("host").trim();
    }

    public int getServerPort() {
        return getLocalPort();
    }

    public String getContextPath() {
        String result = this.context.getPath();
        if ("/".equals(result))
            return "";
        return result;
    }

    public String getRequestURI() {
        return uri;
    }

    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        if (port < 0) {
            port = 80; // Work around java.net.URL bug
        }
        url.append(scheme);
        url.append("://");
        url.append(getServerName());
        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("https") && (port != 443))) {
            url.append(':');
            url.append(port);
        }
        url.append(getRequestURI());

        return url;
    }

    public String getServletPath() {
        return uri;
    }

    @Override
    public Cookie[] getCookies() {
        return cookies;
    }

    public HttpSession getSession() {
        return session;
    }
    public void setSession(HttpSession session) {
        this.session = session;
    }
}
