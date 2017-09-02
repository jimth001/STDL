package dm.tools;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

public class UserCreateRegion {
    //todo
    public static HashMap<String,Method> methodRegistry=getMethodRegistry();
    public static HashMap<String,Method> getMethodRegistry() {
        HashMap<String,Method> rst=new HashMap<>();
        Method[]methods=UserCreateRegion.class.getDeclaredMethods();
        for(Method e:methods) {
            rst.put(e.getName(),e);
        }
        return rst;
    }
}
class CallingUserMethodException extends Exception {
    public CallingUserMethodException(String msg) {
        super(msg);
    }
}