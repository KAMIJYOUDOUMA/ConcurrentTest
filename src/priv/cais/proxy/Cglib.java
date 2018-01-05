package priv.cais.proxy;

import java.lang.reflect.Method;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

/**
 * @auther CaiS
 */
public class Cglib implements MethodInterceptor {

    @Override
    public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
        System.out.println("++++++before " + proxy.getSuperName() + "++++++");
        System.out.println(method.getName());
        Object o1 = proxy.invokeSuper(obj, args);
        System.out.println("++++++before " + proxy.getSuperName() + "++++++");
        return o1;
    }

}
