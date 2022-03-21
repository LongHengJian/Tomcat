package tomcat.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.log.LogFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 龙恒建
 * @date 2021/03/09
 * @result MiniBrower
 * 模拟浏览器进行访问
 */

/**
 * @date 2021/03/09
 * @result 第一次改进
 * 增加Request对象
 */

public class MiniBrowser {

    public static void main(String[] args) throws Exception{
        //初始化请求地址，这个请求地址就是待会去掉连接的地址
        String url = "http://static.how2j.cn/diytomcat.html";

        String contentString = getContentString(url,false);
        String httpString = getHttpString(url,false);
        LogFactory.get().info("contentString is {}",contentString);
        LogFactory.get().info("httpString is {}",httpString);
    }

    public static byte[] getContentBytes(String url) {
        return getContentBytes(url, false,null,true);
    }

    public static byte[] getContentBytes(String url, boolean gzip) {
        return getContentBytes(url, gzip,null,true);
    }

    public static byte[] getContentBytes(String url, Map<String, Object> params, boolean isGet) {
        return getContentBytes(url,false, params, isGet);
    }

    //返回二进制的http响应内容（可以简单的理解为去掉头的html部分）
    public static byte[] getContentBytes(String url,boolean gzip, Map<String,Object> params, boolean isGet) {
        //这里是真正的逻辑,就是与请求地址建立连接的逻辑,是整个类的核心,其他方法都只是处理这个方法返回值的一些逻辑而已
        byte[] response = getHttpBytes(url,gzip,params,isGet);
        //这个doubleReturnq其实是这样来的:我们获取的返回值正常其实是这样的
        //也就是说响应头部分和具体内容部分其实隔了一行, \r表示回到行首\n表示换到下一行,那么\r\n就相当于说先到了空格一行的那一行的行首,接着又到了具体内容的那部分的行首
        byte[] doubleReturn = "\r\n\r\n".getBytes();

        //接着这里初始化一个记录值,做记录用,往下看
        int pos = -1;

        //开始遍历返回内容
        for (int i = 0; i < response.length-doubleReturn.length;i++) {
            //这里的意思就是不断去初始化一个数组(从原数组进行拷贝),目的其实是为了获取到\r\n这一行的起始位置
            byte[] temp = Arrays.copyOfRange(response, i,i+doubleReturn.length);

            if (Arrays.equals(temp, doubleReturn)) {
                //将pos等于i,记录位置
                pos = i;
                break;
            }
        }
        //如果没记录到,那就说明压根没具体内容,那其实就是null
        if (-1 == pos)
            return null;
        //接着pos就是\r\n\n的第一个\的这个位置,加上\r\n\r\n的长度,相当于来到了具体内容的其实位置
        pos += doubleReturn.length;

        //最后,确定了具体内容是在哪个字节开始,就拷贝这部分内容返回
        byte[] result = Arrays.copyOfRange(response,pos,response.length);
        return result;
    }

    public static String getContentString(String url) {
        return getContentString(url, false, null, true);
    }

    public static String getContentString(String url, boolean gzip) {
        return getContentString(url, gzip, null, true);
    }

    public static String getContentString(String url, Map<String,Object> params, boolean isGet) {
        return getContentString(url,false,params,isGet);
    }

    //返回字符串的字符串的http响应内容（可以简单为去掉头的html部分）
    public static String getContentString(String url, boolean gzip, Map<String,Object> params, boolean isGet) {
        //这里获取返回体具体内容的字节数组
        byte[] result = getContentBytes(url, gzip,params,isGet);
        //getContentString 表示获取内容的字符串,我们获取到具体内容的字节数组后还需要进行编码
        if (null==result)
            return null;
        try {
            //将二进制转化格式成为utf-8
            return new String(result,"utf-8").trim();
        } catch (UnsupportedEncodingException exception) {
            return null;
        }
    }

    //返回字符串的http响应
    public static String getHttpString(String url) {
        //这些重载方法其实就是备用的,以后可以直接调url,默认不gzip
        return getHttpString(url,false, null, true);
    }

    public static String getHttpString(String url,boolean gzip) {
        return getHttpString(url, gzip, null, true);
    }

    public static String getHttpString(String url, Map<String,Object> params, boolean isGet) {
        return getHttpString(url,false,params,isGet);
    }

    public static String getHttpString(String url, boolean gzip, Map<String,Object> params, boolean isGet) {
        //这里也没啥了,就是少了截取内容的那部分操作,直接就返回整个返回值的字节数组出来
        byte[]  bytes=getHttpBytes(url,gzip,params,isGet);
        return new String(bytes).trim();
    }

    //返回二进制的http响应内容
    public static byte[] getHttpBytes(String url,boolean gzip, Map<String, Object> params, boolean isGet) {
        String method = isGet?"GET":"POST";
        //首先初始化一个返回值,这个返回值是一个字节数组,utf-8编码的
        byte[] result = null;
        try {
            //通过Url来new一个URL对象，这样就不用自己去截取他的端口啊或者请求路径啥的，可以直接调他的方法获取
            URL u = new URL(url);
            //开启一个socket链接，client指的就是你现在的这台计算机(客户端)
            Socket client = new Socket();
            //获取到端口号，要是端口号是-1，那就默认取80端口（这个端口也是web常用端口）
            int port = u.getPort();
            if (-1 == port)
                port = 80;

            //这个是socket编程的内容，简单来说就是通过一个host+端口和这个url建立连接
            InetSocketAddress inetSocketAddress = new InetSocketAddress(u.getHost(),port);
            //开启连接了，1000是超过时间，等于说超过1秒就算是你超时了
            client.connect(inetSocketAddress,1000);
            //初始化请求头
            Map<String,String> requestHeaders = new HashMap<>();

            //这几个参数都是http请求时会带上的请求头
            requestHeaders.put("Host",u.getHost()+":"+port);
            requestHeaders.put("Accept","text/html;charset=utf-8");
            requestHeaders.put("Connection","close");
            requestHeaders.put("User-Agent","mini brower / java1.8");

            //gzip是确定客户端或服务器端是否支持压缩
            if(gzip)
                requestHeaders.put("Accept-Encoding","gzip");

            //获取到path，其实就是/diytomcat.html,如果没用的话就默认是/
            String path = u.getPath();
            if (path.length()==0)
                path = "/";

            if(null!=params && isGet){
                String paramsString = HttpUtil.toParams(params);
                path = path + "?" + paramsString;
            }

            //接着开始拼接请求得字符串，其实所谓得请求头和请求内容就是这么一串字符串拼接起来
            //记得空格
            String firstLine = method+ " " + path +" HTTP/1.1\r\n";

            //字符流
            StringBuffer httpRequestString = new StringBuffer();
            //拼接firstLine的内容
            httpRequestString.append(firstLine);
            Set<String> headers = requestHeaders.keySet();
            //遍历header的那个map进行拼接
            for (String header : headers) {
                String headerLine = header + ":" + requestHeaders.get(header)+"\r\n";
                httpRequestString.append(headerLine);
            }
            /**走到这的时候,httpRequestString已经拼接好了,内容是:
             GET /diytomcat.html HTTP/1.1
             Accept:text/html
             Connection:close
             User-Agent:mini browser / java1.8
             Host:127.0.0.1:18080
             */

            if (null!=params && !isGet) {
                String paramsString = HttpUtil.toParams(params);
                httpRequestString.append("\r\n");
                httpRequestString.append(paramsString);
            }
            //通过输出流,将这么一串字符串输出给连接的地址,后面的true是autoFlush,表示开始自动刷新
            PrintWriter printWriter = new PrintWriter(client.getOutputStream(),true);
            printWriter.println(httpRequestString);
            //这时候你已经将需要的请求所需的字符串发给上面那个url了,其实所谓的http协议就是这样,你发给他这么一串符合规范的字符串,他就给你响应,接着他那边就给你返回数据
            InputStream inputStream = client.getInputStream();

            result = readBytes(inputStream,true);
            //这是个好习惯,不过最好是放在finally进行关闭比较好,这里就是关闭连接了
            client.close();
        } catch (Exception exception) {
            exception.printStackTrace();
            //这里是将返回的异常信息进行字节数组编码,其实就是兼容这个方法
            try {
                result = exception.toString().getBytes("utf-8");
            }catch (UnsupportedEncodingException encodingException) {
                encodingException.printStackTrace();
            }
        }
        //返回结果
        return result;
    }


    /**
     * 对mini brower的改进
     * 准备一个1024长度的缓存，不断地从输入流读取数据到这个缓存里面去
     * 如果读取长度是-1就表示到头了，停止循环
     * 如果读取的长度小于buffer_size，说明也读完了
     *
     * 最后把读取到的数据，根据实际长度，写出到一个字节数组输出流里面
     *
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static byte[] readBytes(InputStream inputStream, boolean fully) throws IOException {
        int buffer_size = 1024;
        byte[] buffer = new byte[buffer_size];
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        while (true) {
            //从输入流获取数据,调read方法存到buffer数组中
            int length = inputStream.read(buffer);
            //读到的长度如果是-1,说明没读到数据了,直接退出
            if (-1 == length)
                break;
            //接着先将读到的1m数据输出到我们初始化的那个输出流中
            byteArrayOutputStream.write(buffer, 0, length);
            //这里是一个结束的操作,length != bufferSize,说明已经是最后一次读取了,为什么这么说?
            //举个例子,如果你的数据是1025字节,当你第二次循环的时候就是只有一个字节了,这时候就说明处理完这一个字节的数组就可以结束了,因为已经没数据了
            if (!fully && length!=buffer_size)
                break;
        }
        byte[] result = byteArrayOutputStream.toByteArray();
        return result;

    }
}
