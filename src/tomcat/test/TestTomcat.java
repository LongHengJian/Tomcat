package tomcat.test;

/**
 * @author 龙恒建
 * @date 2021/03/09
 * @result 单元测试
 * 对已实现功能进行测试
 */

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.NetUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.log.LogFactory;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import tomcat.Bootstrap;
import tomcat.util.MiniBrowser;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestTomcat {
    private static int port = 18080;
    private static String ip = "127.0.0.1";

    @BeforeClass
    public static void beforeClass() throws InterruptedException {
        //所有的测试开始前都要先看tomcat是否已经启动了
        String[] args = new String[1];
        //设置一个线程启动tomcat;并且让他休眠两秒，保证启动成功
        new Thread(() -> {
            try {
                Bootstrap.main(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        Thread.sleep(2000);
        //判断是否启动成功
        if (NetUtil.isUsableLocalPort(port)) {
            LogFactory.get().error("请先启动位与端口："+port+" 的tomcat,否则无法进行单元测试");
            System.exit(1);
        } else {
            LogFactory.get().info("检测到tomcat已经启动，开始进行单元测试");
        }
    }

    /**
     * 对于进行单元测试
     */
    @Test
    public void testHelloTomcat() {
        String html = getContentString("/");
        Assert.assertEquals(html,"Hello Tomcat");
    }

    /**
     * 对于文本文件的读取进行单元测试
     */
    @Test
    public void testHtml() {
        String html = getContentString("/a.html");
        Assert.assertEquals(html,"Hello Tomcat from a.html");
    }

    /**
     * 测试线程池
     * @throws InterruptedException
     */
    @Test
    public void testTimeConsumeHtml() throws InterruptedException {
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(20,20,60, TimeUnit.SECONDS,new LinkedBlockingQueue<Runnable>(10));
        TimeInterval timeInterval = DateUtil.timer();

        for (int i = 0; i<3; i++) {
            threadPoolExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    getContentString("/timeConsume.html");
                }
            });
        }
        threadPoolExecutor.shutdown();
        threadPoolExecutor.awaitTermination(1,TimeUnit.HOURS);

        long duration = timeInterval.intervalMs();
        Assert.assertTrue(duration > 3000);
    }

    @Test
    public void testIndex() {
        String html = getContentString("/a");
        Assert.assertEquals(html,"Hello Tomcat from index.html@a");
    }

    /**
     *测试多应用
     */

    @Test
    public void testbIndex() {
        String html = getContentString("/b");
        Assert.assertEquals(html,"Hello Tomcat from index.html@b");
    }

    /**
     * 对404进行测试
     */
    @Test
    public void test404() {
        String response  = getHttpString("/not_exist.html");
        containAssert(response, "HTTP/1.1 404 Not Found");
    }

    /**
     * 对500进行测试
     */
    @Test
    public void test500() {
        String response  = getHttpString("/500.html");
        containAssert(response, "HTTP/1.1 500 Internal Server Error");
    }

    /**
     * 对不同文件格式的处理
     */
    @Test
    public void testTxt() {
        String response  = getHttpString("/a.txt");
        containAssert(response, "Content-Type: text/plain");
    }

    /**
     * 读取PNG图片格式的测试
     */
    @Test
    public void testPNG() {
        byte[] bytes = getContentBytes("/logo.png");
        int pngFileLength = 1672;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    /**
     * 测试读取pdf格式的文件
     */
    @Test
    public void testPDF() {
        byte[] bytes = getContentBytes("/etf.pdf");
        int pngFileLength = 3590775;
        Assert.assertEquals(pngFileLength, bytes.length);
    }

    /**
     * 测试servlet的访问
     */
    @Test
    public void testhello() {
        String html = getContentString("/j2ee/hello");
        Assert.assertEquals(html,"Hello Tomcat from HelloServlet");
    }

    /**
     * 测试javaWeb
     */
    @Test
    public void testJavawebHello() {
        String html = getContentString("/javaWeb/hello");
        System.out.println("Html   "+html);
        containAssert(html,"Hello Tomcat from HelloServlet@javaweb");
    }

    /**
     * 测试是否同样servlet单例模式
     */
    @Test
    public void testJavawebHelloSingleton() {
        String html1 = getContentString("/javaweb/hello");
        String html2 = getContentString("/javaweb/hello");
        Assert.assertEquals(html1,html2);
    }

    /**
     * 增加get方式
     */
    @Test
    public void testgetParam() {
        String uri = "/javaweb/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser.getContentString(url, params, true);
        Assert.assertEquals(html,"get name:meepo");
    }

    /**
     * post方式的测试
     */
    @Test
    public void testpostParam() {
        String uri = "/javaweb/param";
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        Map<String,Object> params = new HashMap<>();
        params.put("name","meepo");
        String html = MiniBrowser.getContentString(url, params, false);
        Assert.assertEquals(html,"post name:meepo");
    }

    /**
     * 测试头信息
     */
    @Test
    public void testheader() {
        String html = getContentString("/javaweb/header");
        Assert.assertEquals(html,"mini brower / java1.8");
    }

    private byte[] getContentBytes(String uri) {
        return getContentBytes(uri,false);
    }
    private byte[] getContentBytes(String uri,boolean gzip) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        return MiniBrowser.getContentBytes(url,gzip);
    }

    /**
     * 测试cookie
     */
    @Test
    public void testsetCookie() {
        String html = getHttpString("/javaweb/setCookie");
        containAssert(html,"Set-Cookie: name=Gareen(cookie); Expires=");
    }

    /**
     * testgetCookie 测试
     * @throws IOException
     */
    @Test
    public void testgetCookie() throws IOException {
        String url = StrUtil.format("http://{}:{}{}", ip,port,"/javaweb/getCookie");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","name=Gareen(cookie)");
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, "utf-8");
        containAssert(html,"name:Gareen(cookie)");
    }

    /**
     * 测试gzip
     */
    @Test
    public void testGzip() {
        byte[] gzipContent = getContentBytes("/",true);
        byte[] unGzipContent = ZipUtil.unGzip(gzipContent);
        String html = new String(unGzipContent);
        Assert.assertEquals(html, "Hello Tomcat");
    }

    /**
     *测试JspServlet
     */
    @Test
    public void testJsp() {
        String html = getContentString("/javaweb/");
        Assert.assertEquals(html, "hello jsp@javaweb");
    }

    /**
     * 先通过访问 setSession，设置 name_in_session,
     * 并且得到 jsessionid, 然后 把 jsessionid 作为 Cookie 的值提交到 getSession，
     * 就获取了session 中的数据了。
     */
    @Test
    public void testSession() throws IOException {
        String jsessionid = getContentString("/javaweb/setSession");
        if(null!=jsessionid)
            jsessionid = jsessionid.trim();
        String url = StrUtil.format("http://{}:{}{}", ip,port,"/javaweb/getSession");
        URL u = new URL(url);
        HttpURLConnection conn = (HttpURLConnection) u.openConnection();
        conn.setRequestProperty("Cookie","JSESSIONID="+jsessionid);
        conn.connect();
        InputStream is = conn.getInputStream();
        String html = IoUtil.read(is, "utf-8");
        containAssert(html,"Gareen(session)");
    }

    /**
     * 测试客户端跳转
     */
    @Test
    public void testClientJump(){
        String http_servlet = getHttpString("/javaweb/jump1");
        containAssert(http_servlet,"HTTP/1.1 302 Found");
        String http_jsp = getHttpString("/javaweb/jump1.jsp");
        containAssert(http_jsp,"HTTP/1.1 302 Found");
    }

    /**
     * 测试服务端跳转
     */

    @Test
    public void testServerJump(){
        String http_servlet = getHttpString("/javaweb/jump2");
        containAssert(http_servlet,"Hello Tomcat from HelloServlet");
    }

    /**
     * 测试服务端传参
     */
    @Test
    public void testServerJumpWithAttributes(){
        String http_servlet = getHttpString("/javaweb/jump2");
        containAssert(http_servlet,"Hello DIY Tomcat from HelloServlet@javaweb, the name is gareen");
    }

    /**
     * 测试war
     */
    @Test
    public void testJavaweb0Hello() {
        String html = getContentString("/javaweb0/hello");
        containAssert(html,"Hello Tomcat from HelloServlet@javaweb");
    }



    /**
     * 使用miniBrower对http:127.0.0.1:18080/进行模拟访问
     * 并且返回浏览器信息和相关内容
     *
     * @param uri
     * @return content
     */
    private String getContentString(String uri) {
        String url = StrUtil.format("http://{}:{}{}",ip,port,uri);
        String content = MiniBrowser.getContentString(url);
        return content;
    }

    /**
     * 增加一个 getHttpString 方法来获取 Http 响应，而非仅仅有内容
     * @param uri
     * @return
     */
    private String getHttpString(String uri) {
        String url = StrUtil.format("http://{}:{}{}", ip,port,uri);
        String http = MiniBrowser.getHttpString(url);
        return http;
    }

    /**
     * 增加一个 containAssert 断言，来判断html 里是否包含某段字符串的断言
     * @param html
     * @param string
     */
    private void containAssert(String html, String string) {
        boolean match = StrUtil.containsAny(html, string);
        Assert.assertTrue(match);
    }


}
