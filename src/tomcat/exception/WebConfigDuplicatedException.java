package tomcat.exception;
/**
 * @author 龙恒建
 * @date 2021/03/13
 * @result WebConfigDuplicatedException
 * 自定义异常
 * 用于在配置web.xml里面发生servlet重复配祀的时候会抛出
 */
public class WebConfigDuplicatedException extends Exception{
    public WebConfigDuplicatedException (String msg) {
        super(msg);
    }
}
