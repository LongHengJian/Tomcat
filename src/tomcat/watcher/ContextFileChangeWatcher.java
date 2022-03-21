package tomcat.watcher;

import cn.hutool.core.io.watch.WatchMonitor;
import cn.hutool.core.io.watch.WatchUtil;
import cn.hutool.core.io.watch.Watcher;
import cn.hutool.log.LogFactory;
import tomcat.catalina.Context;
import tomcat.catalina.Host;

import java.nio.file.Path;
import java.nio.file.WatchEvent;

/**
 * @author 龙恒建
 * @date 2021/03/14
 * @result ContextFileChangeWatcher
 * Context文件改变监听器
 */
public class ContextFileChangeWatcher {

    private WatchMonitor monitor;//真正起作用的监听器

    private boolean stop = false;//标记是否暂停

    /**
     * ContextFileChangeWatcher 构造方法带上 Context 对象，方便后续重载
     * @param context
     * 通过WatchUtil.createAll 创建 监听器。
     * context.getDocBase() 代表监听的文件夹
     * Integer.MAX_VALUE 代表监听的深入，如果是0或者1，就表示只监听当前目录，而不监听子目录
     * new Watcher 当有文件发生变化，那么就会访问 Watcher 对应的方法
     */
    public ContextFileChangeWatcher(Context context){

        this.monitor = WatchUtil.createAll(context.getDocBase(), Integer.MAX_VALUE, new Watcher() {

            /**
             * watcher 声明的方法，就是当文件发生创建，修改，删除 和 出错的时候。 所谓的出错比如文件不能删除，磁盘错误等等。
             * 这些方法，我都归置归置，放进了 dealWith里。
             * 首先加上 synchronized 同步。 因为这是一个异步处理的，当文件发生变化，会发过来很多次事件。 所以我们得一个一个事件的处理，否则搞不好就会让 Context 重载多次。
             * String fileName = event.context().toString(); 取得当前发生变化的文件或者文件夹名称
             * if(stop) return; 当 stop 的时候，就表示已经重载过了，后面再来的消息就别搭理了。
             * if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) 表示只应对 jar class 和 xml 发生的变化，其他的不需要重启，比如 html ,txt等，没必要重启
             * stop = true; 标记一下，后续消息就别处理了
             * LogFactory.get().info(ContextFileChangeWatcher.this + " 检测到了Web应用下的重要文件变化 {} " , fileName); 打印下日志
             * 调用 context.reload(); 进行重载
             * @param event
             */
            private void dealWith(WatchEvent<?> event)  {
                synchronized (ContextFileChangeWatcher.class) {
                    String fileName = event.context().toString();
                    if (stop)
                        return;
                    if (fileName.endsWith(".jar") || fileName.endsWith(".class") || fileName.endsWith(".xml")) {
                        stop = true;
                        LogFactory.get().info(ContextFileChangeWatcher.this + " 检测到了Web应用下的重要文件变化 {} " , fileName);
                        //Thread.sleep(500);
                        context.reload();
                    }

                }
            }

            @Override
            public void onCreate(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

            @Override
            public void onModify(WatchEvent<?> event, Path currentPath) {
                dealWith(event);

            }

            @Override
            public void onDelete(WatchEvent<?> event, Path currentPath) {
                dealWith(event);

            }

            @Override
            public void onOverflow(WatchEvent<?> event, Path currentPath) {
                dealWith(event);
            }

        });

        this.monitor.setDaemon(true);
    }

    //启动
    public void start() {
        monitor.start();
    }

    //停止
    public void stop() {
        monitor.close();
    }
}
