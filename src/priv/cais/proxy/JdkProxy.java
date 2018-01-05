package priv.cais.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * @auther CaiS
 */
public class JdkProxy {

    public static void main(String[] args) throws ClassNotFoundException {
        ArrayList<Integer> arrayList = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Integer> list = (List<Integer>) Proxy.newProxyInstance(arrayList.getClass().getClassLoader(),
                arrayList.getClass().getInterfaces(), new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if (method.getName().equals("add")) {
                            System.out.println("proxy");
                        }
                        return method.invoke(arrayList, args);
                    }

                });
        list.add(1);
    }

}
