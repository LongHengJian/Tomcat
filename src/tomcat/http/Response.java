package tomcat.http;

/**
 * @author 龙恒建
 * @date 2021/03/10
 * @result Reponse
 * 用于存放返回的html文本
 */

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.log.LogFactory;

import javax.servlet.http.Cookie;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Response extends BaseResponse {
    private StringWriter stringWriter;
    private PrintWriter printWriter;
    private String contentType;
    private byte[] body;
    private int status;
    private String redirectPath; //用于保存客户端跳转路径
    private List<Cookie> cookies;

    /**
     * 构造方法
     */
    public Response() {
        this.stringWriter = new StringWriter();
        /**
         * 而这个PrintWriter 其实是建立在 stringWriter的基础上的，
         * 所以 response.getWriter().println();
         * 写进去的数据最后都写到 stringWriter 里面去了。
         * contentType就是对应响应头信息里的 Content-type ，默认是 "text/html"。
         */
        this.printWriter = new PrintWriter(stringWriter);
        this.contentType = "text/html";
        this.cookies = new ArrayList<>();
    }

    /**
     * 把Cookie集合转换成 cookie Header
     * @return  cookie Header
     */
    public String getCookiesHeader() {
        if (null==cookies)
            return "";
        String pattern = "EEE,d MMM yyyy HH:mm:ss 'GMT'";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern, Locale.ENGLISH);
        StringBuffer stringBuffer = new StringBuffer();
        for (Cookie cookie : getCookies()) {
            stringBuffer.append("\r\n");
            stringBuffer.append("Set-Cookie: ");
            stringBuffer.append(cookie.getName() + "=" + cookie.getValue() + "; ");
            if (-1!=cookie.getMaxAge()) { //-1 mean forever
                stringBuffer.append("Expires=");
                Date now = new Date();
                Date expire = DateUtil.offset(now, DateField.MINUTE, cookie.getMaxAge());
                stringBuffer.append(simpleDateFormat.format(expire));
                stringBuffer.append("; ");
            }
            if (null!=cookie.getPath()) {
                stringBuffer.append("Path=" +cookie.getPath());
            }
        }
        return stringBuffer.toString();
    }

    public String getRedirectPath() {
        return this.redirectPath;
    }

    public void sendRedirect(String redirect) throws IOException {
        this.redirectPath = redirect;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * 提供getWrite()方法，
     * 这样就可以像HttpServletResponse 那样写成 response.getWriter().println();
     * 这种风格了。
     * @return
     */
    public PrintWriter getWriter() {
        return printWriter;
    }

    /**
     * getBody方法返回html的字节数组。
     * @return
     * @throws UnsupportedEncodingException
     */

    public byte[] getBody() throws UnsupportedEncodingException {
        if(null==body) {
            String content = stringWriter.toString();
            body = content.getBytes("utf-8");
        }
        return body;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public int getStatus() {
        return status;
    }

    public void addCookie(Cookie cookie) {
        cookies.add(cookie);
    }

    public List<Cookie> getCookies() {
        return this.cookies;
    }

}
