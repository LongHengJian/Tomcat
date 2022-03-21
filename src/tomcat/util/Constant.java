package tomcat.util;

/**
 * @author 龙恒建
 * @date 2021/03/10
 * @result Reponse
 * 常量工具类，用于存放头信息模板
 * 202响应信息
 */

import cn.hutool.system.SystemUtil;

import java.io.File;

public class Constant {

    /**
     * http返回代码常量
     */
    public static final int CODE_200 = 200;
    public static final int CODE_302 = 302;
    public static final int CODE_404 = 404;
    public static final int CODE_500 = 500;

    /**
     * 头信息模板
     */
    //202响应
    public static final String response_head_200 =
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: {}{};charset=utf-8" +
                    "\r\n\r\n";

    //302响应
    public static final String response_head_302 =
            "HTTP/1.1 302 Found\r\nLocation: {}\r\n\r\n";

    //404
    public static final String response_head_404 =
            "HTTP/1.1 404 Not Found\r\n" +
                    "Content-Type: text/html;charset=utf-8\r\n\r\n";

    //500
    public static final String response_head_500 =
            "HTTP/1.1 500 Internal Server Error\r\n"
                    + "Content-Type: text/html;charset=utf-8\r\n\r\n";

    public static final String response_head_200_gzip =
            "HTTP/1.1 200 OK\r\nContent-Type: {}{};charset=utf-8\r\n" +
                    "Content-Encoding:gzip" +
                    "\r\n\r\n";

    public static final String textFormat_404 =
            "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>" +
                    "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} " +
                    "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} " +
                    "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} " +
                    "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} " +
                    "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} " +
                    "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}" +
                    "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> " +
                    "</head><body><h1>HTTP Status 404 - {}</h1>" +
                    "<HR size='1' noshade='noshade'><p><b>type</b> Status report</p><p><b>message</b> <u>{}</u></p><p><b>description</b> " +
                    "<u>The requested resource is not available.</u></p><HR size='1' noshade='noshade'><h3>Tocmat 1.0.1</h3>" +
                    "</body></html>";

    public static final String textFormat_500 = "<html><head><title>DIY Tomcat/1.0.1 - Error report</title><style>"
            + "<!--H1 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:22px;} "
            + "H2 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:16px;} "
            + "H3 {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;font-size:14px;} "
            + "BODY {font-family:Tahoma,Arial,sans-serif;color:black;background-color:white;} "
            + "B {font-family:Tahoma,Arial,sans-serif;color:white;background-color:#525D76;} "
            + "P {font-family:Tahoma,Arial,sans-serif;background:white;color:black;font-size:12px;}"
            + "A {color : black;}A.name {color : black;}HR {color : #525D76;}--></style> "
            + "</head><body><h1>HTTP Status 500 - An exception occurred processing {}</h1>"
            + "<HR size='1' noshade='noshade'><p><b>type</b> Exception report</p><p><b>message</b> <u>An exception occurred processing {}</u></p><p><b>description</b> "
            + "<u>The server encountered an internal error that prevented it from fulfilling this request.</u></p>"
            + "<p>Stacktrace:</p>" + "<pre>{}</pre>" + "<HR size='1' noshade='noshade'><h3>Tocmat 1.0.1</h3>"
            + "</body></html>";


    /**
     * user.dir ：用户的当前工作目录
     */
    public final static File webappsFolder = new File(SystemUtil.get("user.dir"), "webapps");

    /**
     * 扫描server.xml配置文件
     */
    public final static File confFolder = new File(SystemUtil.get("user.dir"), "conf");
    public final static File serverXmlFile = new File(confFolder, "server.xml");

    /**
     * 指向web.xml文件
     */
    public static final File webXmlFile = new File(confFolder, "web.xml");

    /**
     * 指向context.xml文件
     */
    public static final File contextXmlFile = new File(confFolder, "context.xml");

    /**
     * 按照Tomcat 的逻辑，当一个 jsp 被转译成为 .java 文件之后，
     * 会被保存在 %TOMCAT_HOME%/ work 这个目录下，
     * 所以我们实现准备这个位置： workFolder
     */
    public static final String workFolder = SystemUtil.get("user.dir") + File.separator + "work";
}
