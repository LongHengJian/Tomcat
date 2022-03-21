package tomcat.catalina;

import tomcat.util.ServerXMLUtil;

import java.util.List;

/**
 * @author 龙恒建
 * @date 2021/03/11
 * @result Engine
 * 表示servlet引擎，用来处理servlet的请求
 */

public class Engine {

    private String defaultHost;
    private List<Host> hosts;
    private Service service;

    public Engine(Service service) {
        this.service = service;
        this.defaultHost = ServerXMLUtil.getEngineDefaultHost();
        this.hosts = ServerXMLUtil.getHosts(this);
        checkDefault();
    }

    public Service getService() {
        return service;
    }

    /**
     * 获取默认的Host对象
     */
   private void checkDefault() {
        if(null==getDefaultHost())
            throw new RuntimeException("the defaultHost" + defaultHost + "does not exist!");
   }

    /**
     * 判断默认的是否存在，否则就会抛出异常
     * @return
     */
   public Host getDefaultHost() {
        for (Host host :hosts) {
            if (host.getName().equals(defaultHost))
                return host;
        }
        return null;
   }

}
