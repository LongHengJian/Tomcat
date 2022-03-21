package tomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.LogFactory;
import org.apache.jasper.JspC;
import org.apache.jasper.compiler.JspRuntimeContext;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import tomcat.classloader.WebappClassLoader;
import tomcat.exception.WebConfigDuplicatedException;
import tomcat.http.ApplicationContext;
import tomcat.http.StandardServletConfig;
import tomcat.util.ContextXMLUtil;
import tomcat.watcher.ContextFileChangeWatcher;

import javax.servlet.*;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.util.*;

/**
 * @author 龙恒建
 * @date 2021/03/10
 * @result 用于代表一个应用；有俩个属性
 * path和docBase
 * path：表示访问的路径
 * docbase: 表示对应在文件系统中的位置
 * context用于存Servlet的映射信息
 */

public class Context {
    private String path;
    private String docBase;
    private File contextWebXmlFile;

    private Map<String, String> url_servletClassName;  //地址对应 Servlet 的类名
    private Map<String, String> url_servletName;  //地址对应 Servlet 的名称
    private Map<String, String> servletName_className; //Servlet 的名称对应类名
    private Map<String, String> className_servletName; //Servlet 类名对应名称

    private List<String> loadOnStartupServletClassNames; //用于存放哪些类需要做自启动

    private WebappClassLoader webappClassLoader;

    private Host host;
    private boolean reloadable;
    private ContextFileChangeWatcher contextFileChangeWatcher;
    //声明servlet_className_init_params用于存放初始化信息
    private Map<String, Map<String, String>> servlet_className_init_params;

    private ServletContext servletContext;
    private Map<Class<?>, HttpServlet> servletPool; //准备一个map作为存放servlet的池子

    //过滤器相关配置
    private Map<String, List<String>> url_filterClassName;
    private Map<String, List<String>> url_FilterNames;
    private Map<String, String> filterName_className;
    private Map<String, String> className_filterName;
    private Map<String, Map<String, String>> filter_className_init_params;
    private Map<String, Filter> filterPool;

    //监听器
    private List<ServletContextListener> listeners;

    public Context(String path, String docBase, Host host, boolean reloadable) {
        TimeInterval timeInterval = DateUtil.timer();
        this.host = host;
        this.reloadable = reloadable;
        this.path = path;
        this.docBase = docBase;
        this.contextWebXmlFile = new File(docBase, ContextXMLUtil.getWatchedResource());
        this.url_servletClassName = new HashMap<>();
        this.url_servletName = new HashMap<>();
        this.servletName_className = new HashMap<>();
        this.className_servletName = new HashMap<>();
        this.servlet_className_init_params = new HashMap<>();

        this.servletPool = new HashMap<>();

        this.servletContext = new ApplicationContext(this);
        this.loadOnStartupServletClassNames = new ArrayList<>();

        ClassLoader commonClassLoader = Thread.currentThread().getContextClassLoader();
        this.webappClassLoader = new WebappClassLoader(docBase, commonClassLoader);

        this.url_filterClassName = new HashMap<>();
        this.url_FilterNames = new HashMap<>();
        this.filterName_className = new HashMap<>();
        this.className_filterName = new HashMap<>();
        this.filter_className_init_params = new HashMap<>();
        this.filterPool = new HashMap<>();

        listeners=new ArrayList<ServletContextListener>();

        LogFactory.get().info("Deploying web application directory {}", this.docBase);
        deploy();
        LogFactory.get().info("Deployment of web application directory {} has finished in {} ms", this.docBase,timeInterval.intervalMs());
    }

    public void reload() {
        host.reload(this);
    }

    /**
     * 部署方法
     */
    private void deploy(){
        loadListeners();
        TimeInterval timeInterval = DateUtil.timer();
        init();
        if (reloadable) {
            contextFileChangeWatcher = new ContextFileChangeWatcher(this);
            contextFileChangeWatcher.start();
        }
        JspC c = new JspC();
        new JspRuntimeContext(servletContext, c);
    }

    /**
     * 初始化方法
     */
    private void init() {
        if (!contextWebXmlFile.exists())
            return;
        try {
            checkDuplicated();
        } catch (WebConfigDuplicatedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document document = Jsoup.parse(xml);
        parseServletMapping(document);
        parseFilterMapping(document);

        parseFilterInitParams(document);
        parseServletInitParams(document);

        initFilter();

        parseLoadOnStartUp(document);
        handleLoadOnStartup();

        fireEvent("init");
    }

    /**
     * parseServletMapping 方法，把这些信息从 web.xml 中解析出来
     * @param document
     */
    private void parseServletMapping(Document document) {
        // url_ServletName
        Elements mappingurlElements = document.select("servlet-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String servletName = mappingurlElement.parent().select("servlet-name").first().text();
            url_servletName.put(urlPattern, servletName);
        }
        // servletName_className / className_servletName
        Elements servletNameElements = document.select("servlet servlet-name");
        for (Element servletNameElement : servletNameElements) {
            String servletName = servletNameElement.text();
            String servletClass = servletNameElement.parent().select("servlet-class").first().text();
            servletName_className.put(servletName, servletClass);
            className_servletName.put(servletClass, servletName);
        }
        // url_servletClassName
        Set<String> urls = url_servletName.keySet();
        for (String url : urls) {
            String servletName = url_servletName.get(url);
            String servletClassName = servletName_className.get(servletName);
            url_servletClassName.put(url, servletClassName);
        }
    }

    /**
     * 判断是否有重复，如果有就会抛出 WebConfigDuplicatedException 异常
     * @param document
     * @param mapping
     * @param desc
     * @throws WebConfigDuplicatedException
     */
    private void checkDuplicated(Document document, String mapping, String desc) throws WebConfigDuplicatedException {
        Elements elements = document.select(mapping);
        // 判断逻辑是放入一个集合，然后把集合排序之后看两临两个元素是否相同
        List<String> contents = new ArrayList<>();
        for (Element e : elements) {
            contents.add(e.text());
        }
        Collections.sort(contents);
        for (int i = 0; i < contents.size() - 1; i++) {
            String contentPre = contents.get(i);
            String contentNext = contents.get(i + 1);
            if (contentPre.equals(contentNext)) {
                throw new WebConfigDuplicatedException(StrUtil.format(desc, contentPre));
            }
        }
    }

    private void checkDuplicated() throws WebConfigDuplicatedException {
        String xml = FileUtil.readUtf8String(contextWebXmlFile);
        Document document = Jsoup.parse(xml);
        checkDuplicated(document, "servlet-mapping url-pattern", "servlet url 重复,请保持其唯一性:{} ");
        checkDuplicated(document, "servlet servlet-name", "servlet 名称重复,请保持其唯一性:{} ");
        checkDuplicated(document, "servlet servlet-class", "servlet 类名重复,请保持其唯一性:{} ");
    }

    /**
     * parseServletInitParams 方法从 web.xml 中解析初始化参数
     * @param document
     */
    private void parseServletInitParams(Document document) {
        Elements servletClassNameElements = document.select("servlet-class");
        for (Element servletClassNameElement : servletClassNameElements) {
            String servletClassName = servletClassNameElement.text();
            Elements initElements = servletClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;
            Map<String,String> initParams = new HashMap<>();
            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name,value);
            }
            servlet_className_init_params.put(servletClassName, initParams);
        }
    }

    /**
     *  提供 getServlet 方法，根据类对象来获取 servlet 对象。
     *  修改 getServlet，让 servlet 对象放进池子之前做初始化
     * @param clazz
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ServletException
     */
    public synchronized HttpServlet getServlet(Class<?> clazz)
            throws InstantiationException, IllegalAccessException, ServletException {
        HttpServlet servlet = servletPool.get(clazz);
        if (null == servlet) {
            servlet = (HttpServlet) clazz.newInstance();
            ServletContext servletContext = this.getServletContext();
            String className = clazz.getName();
            String servletName = className_servletName.get(className);
            Map<String, String> initParameters = servlet_className_init_params.get(className);
            ServletConfig servletConfig = new StandardServletConfig(servletContext, servletName, initParameters);
            servlet.init(servletConfig);
            servletPool.put(clazz, servlet);
        }
        return servlet;
    }

    /**
     * 销毁Servlets
     */
    private void destroyServlets() {
        Collection<HttpServlet> servlets = servletPool.values();
        for (HttpServlet servlet : servlets) {
            servlet.destroy();
        }
    }

    /**
     * parseLoadOnStartUp() 解析哪些类需要做自启动
     * @param document
     */
    public void parseLoadOnStartUp(Document document) {
        Elements elements = document.select("load-on-startup");
        for (Element element : elements) {
            String loadOnStartupServletClassName = element.parent().select("servlet-class").text();
            loadOnStartupServletClassNames.add(loadOnStartupServletClassName);
        }
    }

    /**
     * 提供parseFilterMapping方法，解析web.xml里面的filter信息
     * @param document
     */
    public void parseFilterMapping(Document document) {

        // filter_url_name
        Elements mappingurlElements = document.select("filter-mapping url-pattern");
        for (Element mappingurlElement : mappingurlElements) {
            String urlPattern = mappingurlElement.text();
            String filterName = mappingurlElement.parent().select("filter-name").first().text();

            List<String> filterNames= url_FilterNames.get(urlPattern);
            if(null==filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(urlPattern, filterNames);
            }
            filterNames.add(filterName);
        }

        // class_name_filter_name
        Elements filterNameElements = document.select("filter filter-name");
        for (Element filterNameElement : filterNameElements) {
            String filterName = filterNameElement.text();
            String filterClass = filterNameElement.parent().select("filter-class").first().text();
            filterName_className.put(filterName, filterClass);
            className_filterName.put(filterClass, filterName);
        }

        // url_filterClassName
        Set<String> urls = url_FilterNames.keySet();
        for (String url : urls) {
            List<String> filterNames = url_FilterNames.get(url);
            if(null == filterNames) {
                filterNames = new ArrayList<>();
                url_FilterNames.put(url, filterNames);
            }
            for (String filterName : filterNames) {
                String filterClassName = filterName_className.get(filterName);
                List<String> filterClassNames = url_filterClassName.get(url);
                if(null==filterClassNames) {
                    filterClassNames = new ArrayList<>();
                    url_filterClassName.put(url, filterClassNames);
                }
                filterClassNames.add(filterClassName);
            }
        }
    }

    /**
     * 提供 parseFilterInitParams 方法用于解析参数信息
     * @param document
     */
    private void parseFilterInitParams(Document document) {
        Elements filterClassNameElements = document.select("filter-class");
        for (Element filterClassNameElement : filterClassNameElements) {
            String filterClassName = filterClassNameElement.text();

            Elements initElements = filterClassNameElement.parent().select("init-param");
            if (initElements.isEmpty())
                continue;


            Map<String, String> initParams = new HashMap<>();

            for (Element element : initElements) {
                String name = element.select("param-name").get(0).text();
                String value = element.select("param-value").get(0).text();
                initParams.put(name, value);
            }

            filter_className_init_params.put(filterClassName, initParams);

        }
    }

    /**
     * 初始化Filter
     */
    private void initFilter() {
        Set<String> classNames = className_filterName.keySet();
        for (String className : classNames) {
            try {
                Class clazz =  this.getWebClassLoader().loadClass(className);
                Map<String,String> initParameters = filter_className_init_params.get(className);
                String filterName = className_filterName.get(className);
                FilterConfig filterConfig = new StandardFilterConfig(servletContext, filterName, initParameters);
                Filter filter = filterPool.get(clazz);
                if(null==filter) {
                    filter = (Filter) ReflectUtil.newInstance(clazz);
                    filter.init(filterConfig);
                    filterPool.put(className, filter);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 获取匹配了的过滤器集合
     * @param uri
     * @return
     */
    public List<Filter> getMatchedFilters(String uri) {
        List<Filter> filters = new ArrayList<>();
        Set<String> patterns = url_filterClassName.keySet();
        Set<String> matchedPatterns = new HashSet<>();

        for (String pattern : patterns) {
            if(match(pattern,uri)) {
                matchedPatterns.add(pattern);
            }
        }

        Set<String> matchedFilterClassNames = new HashSet<>();
        for (String pattern : matchedPatterns) {
            List<String> filterClassName = url_filterClassName.get(pattern);
            matchedFilterClassNames.addAll(filterClassName);
        }
        for (String filterClassName : matchedFilterClassNames) {
            Filter filter = filterPool.get(filterClassName);
            filters.add(filter);
        }
        return filters;
    }

    /**
     * 三种匹配模式
     * @param pattern
     * @param uri
     * @return
     */
    private boolean match(String pattern, String uri) {
        // 完全匹配
        if(StrUtil.equals(pattern, uri))
            return true;
        // /* 模式
        if(StrUtil.equals(pattern, "/*"))
            return true;
        // 后缀名 /*.jsp
        if(StrUtil.startWith(pattern, "/*.")) {
            String patternExtName = StrUtil.subAfter(pattern, '.', false);
            String uriExtName = StrUtil.subAfter(uri, '.', false);
            if(StrUtil.equals(patternExtName, uriExtName))
                return true;
        }
        // 其他模式就懒得管了
        return false;
    }

    /**
     * 新增监听器
     * @param listener
     */
    public void addListener(ServletContextListener listener){
        listeners.add(listener);
    }

    /**
     * 从web.xml 中扫描监听器类
     */
    private void loadListeners()  {
        try {
            if(!contextWebXmlFile.exists())
                return;
            String xml = FileUtil.readUtf8String(contextWebXmlFile);
            Document d = Jsoup.parse(xml);

            Elements es = d.select("listener listener-class");
            for (Element e : es) {
                String listenerClassName = e.text();

                Class<?> clazz= this.getWebClassLoader().loadClass(listenerClassName);
                ServletContextListener listener = (ServletContextListener) clazz.newInstance();
                addListener(listener);

            }
        } catch (IORuntimeException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param type
     */
    private void fireEvent(String type) {
        ServletContextEvent event = new ServletContextEvent(servletContext);
        for (ServletContextListener servletContextListener : listeners) {
            if("init".equals(type))
                servletContextListener.contextInitialized(event);
            if("destroy".equals(type))
                servletContextListener.contextDestroyed(event);
        }
    }

    /**
     * 对这些类做自启动
     */
    public void handleLoadOnStartup() {
        for (String loadOnStartupServletClassName : loadOnStartupServletClassNames) {
            try {
                Class<?> clazz = webappClassLoader.loadClass(loadOnStartupServletClassName);
                getServlet(clazz);
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | ServletException e) {
                e.printStackTrace();
            }
        }
    }

    public String getServletClassName(String uri) {
        return url_servletClassName.get(uri);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDocBase() {
        return docBase;
    }

    public void setDocBase(String docBase) {
        this.docBase = docBase;
    }

    public WebappClassLoader getWebappClassLoader() {
        return webappClassLoader;
    }

    /**
     * 停止
     */
    public void stop() {
        webappClassLoader.stop();
        contextFileChangeWatcher.stop();
        destroyServlets();
        fireEvent("destroy");
    }

    public boolean isReloadable() {
        return reloadable;
    }

    public void setReloadable(boolean reloadable) {
        this.reloadable = reloadable;
    }

    public ServletContext getServletContext() {
        return servletContext;
    }

    public WebappClassLoader getWebClassLoader() {
        return webappClassLoader;
    }
}