package tomcat;

import tomcat.catalina.Server;
import tomcat.classloader.CommonClassLoader;

import java.lang.reflect.Method;

/**
 * @author 龙恒建
 * @date 2021/03/09
 * @result 简单的搭建tomcat服务器
 */

public class Bootstrap {
    public static void main(String[] args) throws Exception{
        CommonClassLoader commonClassLoader = new CommonClassLoader();

        Thread.currentThread().setContextClassLoader(commonClassLoader);

        String serverClassName = "tomcat.catalina.Server";

        Class<?> serverClazz = commonClassLoader.loadClass(serverClassName);

        Object serverObject = serverClazz.newInstance();

        Method method = serverClazz.getMethod("start");

        method.invoke(serverObject);

    }



    /**
     * 注：为什么要把头和主体分开，而不直接使用合并的 html 呢？
     * 因为在接下来的工作里，会对头部做更复杂的处理，
     * 主体部分也会面对二进制文件和gzip压缩，
     * 现在分开来，后续处理起来更加游刃有余。
     */

}
