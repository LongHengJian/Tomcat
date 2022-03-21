package tomcat.test;

/**
 * @author 龙恒建
 * @date 2021/03/13
 * @result TestClassLoader
 * 类对象加载测试类
 */
public class TestClassLoader {

    public static void main(String[] args) {
        Object object = new Object();
        System.out.println(object);
        Class<?> clazz = object.getClass();

        System.out.println(clazz);

        System.out.println(Object.class.getClassLoader());

        System.out.println(TestClassLoader.class.getClassLoader());
    }

}
