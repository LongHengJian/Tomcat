package tomcat.watcher;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import tomcat.catalina.Host;
import tomcat.util.Constant;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * @author 龙恒建
 * @date 2021-03-17 00:23
 * @ClassName WarFileWatcher
 * @description: 创建一个监控类 WarFileWatcher ，监控 webapps 目录， 当发现新创建了 war 文件的时候，就调用 host 现成的 loadWar 方法即可。
 */
public class WarFileWatcher {
    private WatchMonitor monitor;
    public WarFileWatcher(Host host) {
        this.monitor = WatchUtil.createAll(Constant.webappsFolder, 1, new Watcher() {
            private void dealWith(WatchEvent<?> event, Path currentPath){
                synchronized (WarFileWatcher.class) {
                    String fileName = event.context().toString();
                    if(fileName.toLowerCase().endsWith(".war")  && ENTRY_CREATE.equals(event.kind())) {
                        File warFile = FileUtil.file(Constant.webappsFolder, fileName);
                        host.loadWar(warFile);
                    }
                }
            }
            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);

            }
            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);
            }
            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event, currentPath);
            }

        });
    }

    public void start() {
        monitor.start();
    }

    public void stop() {
        monitor.interrupt();
    }

}
