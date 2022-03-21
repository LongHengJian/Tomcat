package tomcat.util;
/**
 * @author 龙恒建
 * @date 2021/03/12
 * @result webXMLUtil
 * 工具类，使用第三方库jsoup
 * 用于解析xml
 */
import cn.hutool.core.io.FileUtil;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tomcat.catalina.Context;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static tomcat.util.Constant.webXmlFile;

public class WebXMLUtil {
    private static Map<String, String> mimeTypeMapping = new HashMap<>();

    public static synchronized String getMimeType(String extName) {
        if (mimeTypeMapping.isEmpty())
            initMimeType();
        String mimeType = mimeTypeMapping.get(extName);
        if (null==mimeType)
            return "text/html";

        return mimeType;
    }

    private static void initMimeType() {
        String xml = FileUtil.readUtf8String(webXmlFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("mime-mapping");
        for (Element element : elements) {
            String extName = element.select("extension").first().text();
            String mimeType = element.select("mime-type").first().text();
            mimeTypeMapping.put(extName, mimeType);
        }
    }



    /**
     * 获取欢迎页面
     * @param context
     * @return
     */
    public static String getWelcomeFile(Context context) {
        String xml = FileUtil.readUtf8String(webXmlFile);
        Document document = Jsoup.parse(xml);
        Elements elements = document.select("welcome-file");
        for (Element element :elements) {
            String welcomeFileName = element.text();
            File file = new File(context.getDocBase(),welcomeFileName);
            if (file.exists())
                return file.getName();
        }
        return "index.html";
    }

}
