package tomcat.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tomcat.catalina.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author 龙恒建
 * @date 2021/03/11
 * @result ServerXMLUtil
 * 工具类，使用第三方库jsoup
 * 用于解析xml
 */

public class ServerXMLUtil {

    /**
     * 获取应用地址，解析xml文件获取
     * @return
     */
    public static List<Context> getContexts(Host host) {
        List<Context> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);

        Elements elements = document.select("Context");
        for (Element element : elements) {
            String path = element.attr("path");
            String docBase = element.attr("docBase");
            boolean reloadable = Convert.toBool(element.attr("reloadable"),true);
            Context context = new Context(path, docBase,host,reloadable);
            result.add(context);
        }
        return result;
    }

    /**
     * 获取 Connectors 集合
     * @param service
     * @return
     */
    public static List<Connector> getConnectors(Service service) {
        List<Connector> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("Connector");
        for (Element element :elements) {
            int port = Convert.toInt(element.attr("port"));
            String compression = element.attr("compression");
            int compressionMinSize = Convert.toInt(element.attr("compressionMinSize"), 0);
            String noCompressionUserAgents = element.attr("noCompressionUserAgents");
            String compressableMimeType = element.attr("compressableMimeType");
            Connector connector = new Connector(service);
            connector.setPort(port);
            connector.setCompression(compression);
            connector.setCompressableMimeType(compressableMimeType);
            connector.setNoCompressionUserAgents(noCompressionUserAgents);
            connector.setCompressableMimeType(compressableMimeType);
            connector.setCompressionMinSize(compressionMinSize);
            result.add(connector);
        }
        return result;
    }

    /**
     * 获取服务名字
     */
    public static String getServiceName() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);
        Element host = document.select("Service").first();
        return host.attr("name");
    }

    /**
     * 获取Engine表示servlet引擎
     * @return
     */
    public static String getEngineDefaultHost() {
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);
        Element host = document.select("Engine").first();
        return host.attr("defaultHost");
    }

    /**
     *获取主机
     */
    public static List<Host> getHosts(Engine engine) {
        List<Host> result = new ArrayList<>();
        String xml = FileUtil.readUtf8String(Constant.serverXmlFile);
        Document document = Jsoup.parse(xml);

        Elements elements = document.select("Host");
        for (Element element : elements) {
            String name = element.attr("name");
            Host host = new Host(name, engine);
            result.add(host);
        }
        return result;
    }


}
