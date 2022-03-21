package tomcat.catalina;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.log.LogFactory;
import tomcat.util.ServerXMLUtil;

import java.util.List;

/**
 * @author 龙恒建
 * @date 2021/03/11
 * @result Service
 * 代表tomcat提供的服务
 * 里面会有很多的Connector对象
 */
public class Service {
    private String name;
    private Engine engine;
    private Server server;

    private List<Connector> connectors;
    public Service(Server server) {
        this.server = server;
        this.name = ServerXMLUtil.getServiceName();
        this.engine = new Engine(this);
        this.connectors = ServerXMLUtil.getConnectors(this);
    }

    public Engine getEngine() {
        return engine;
    }

    public Server getServer() {return server;}

    public void start() {
        init();
    }

    /**
     * init方法显示调用了Connector的init,然后调用了它的start。
     */
    private void init() {
        TimeInterval timeInterval = DateUtil.timer();
        for (Connector c : connectors)
            c.init();
        LogFactory.get().info("Initialization processed in {} ms",timeInterval.intervalMs());
        for (Connector c : connectors)
            c.start();
    }
}
