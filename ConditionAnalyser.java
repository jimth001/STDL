package dm.tools;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
class ConstantsOfCA {
    public static String UserFunctionNameBegin="$";
    public static String UserFunctionNameEnd=":=";
    public static String regStr="(\\$([a-zA-Z0-9]|[\u4e00-\u9fa5])+:=([@a-zA-Z0-9\\.\u4e00-\u9fa5]+(==|!=|>>|<<|>=|<=)[@a-zA-Z0-9\\.\u4e00-\u9fa5]+[,\\|\\|]?)+)|([@a-zA-Z0-9\\.\u4e00-\u9fa5]+(==|!=|>>|<<|>=|<=)[@a-zA-Z0-9\\.\u4e00-\u9fa5]+[,\\|\\|]?)+";
    public static String DisjunctionSperator="%";
    public static String []operators={"==","!=",">>","<<",">=","<="};
}
/**
 * 这个类记载了若干条件表达式，通过getElementBy
 */
public class ConditionAnalyser {//一种rule的一类condition的解析需要一个这样的对象。
    private Method getElementByName=null;
    private boolean enable=true;//是否可用
    class DisjunctionUnit {//析取式单元
        public ArrayList<String> left;
        public ArrayList<String> option;
        public ArrayList<String> right;
        public DisjunctionUnit(String s) throws IllegalConditionException{
            left=new ArrayList<>();
            option=new ArrayList<>();
            right=new ArrayList<>();
            String []strs=s.split(ConstantsOfCA.DisjunctionSperator);
            for(String su:strs) {
                addSingleUnit(su);

            }
        }
        private void addSingleUnit(String su) throws IllegalConditionException{
            int index=-1;
            int i=0;
            for(;i<ConstantsOfCA.operators.length;i++) {
                if((index=su.indexOf(ConstantsOfCA.operators[i]))>-1) {
                    break;
                }
            }
            if(i<ConstantsOfCA.operators.length) {
                option.add(ConstantsOfCA.operators[i]);
                left.add(su.substring(0,index));
                right.add(su.substring(index+ConstantsOfCA.operators[i].length()));
            }
            else {
                throw new IllegalConditionException("不合语法的Condition:"+su);
            }
        }
    }
    class OneCondition {//一个完整的条件表达式：合取范式
        public ArrayList<DisjunctionUnit> disjunctionUnits;//包含若干析取式单元
        public String name;//条件表达式名称
        public boolean isUserDefined;//是否为用户定义的条件表达式函数
        public int id;//表示该条件表达式在文件中的id
        public OneCondition(String onecondition,String name,int id) throws IllegalConditionException{
            disjunctionUnits=new ArrayList<>();
            String []strs=onecondition.split(",");
            this.name=name;
            this.isUserDefined=true;
            this.id=id;
            for(String s:strs) {
                disjunctionUnits.add(new DisjunctionUnit(s));
            }
        }
        public OneCondition(String onecondition,int id) throws IllegalConditionException{
            disjunctionUnits=new ArrayList<>();
            String []strs=onecondition.split(",");
            this.name=String.valueOf(onecondition.hashCode());
            this.isUserDefined=false;
            this.id=id;
            for(String s:strs) {
                disjunctionUnits.add(new DisjunctionUnit(s));
            }
        }
    }
    private ArrayList<OneCondition> conditionsFuntions;
    private HashMap<String,Integer> functionRegistry;
    public boolean addOneCondition(String s,int index) {
        s=s.replaceAll("\\s+","");
        if(!s.matches(ConstantsOfCA.regStr)) {
            ErrorHandle.OutputErrorInformation("condition语法错误："+s);
            return false;
        }
        int indexBegin=s.indexOf(ConstantsOfCA.UserFunctionNameBegin);
        if(indexBegin==0) {//定义函数
            int indexEnd=s.indexOf(":=");
            try {
                String funcName=s.substring(indexEnd+ConstantsOfCA.UserFunctionNameEnd.length());
                OneCondition tmp=new OneCondition(funcName,s.substring(indexBegin+ConstantsOfCA.UserFunctionNameBegin.length(),indexEnd),index);
                conditionsFuntions.add(tmp);
                functionRegistry.put(funcName,conditionsFuntions.size()-1);//注册userFunction
            }
            catch (IllegalConditionException e) {
                ErrorHandle.OutputErrorInformation(e);
                return false;
            }
        }
        else {//普通的condition
            try {
                OneCondition tmp=new OneCondition(s,index);
                conditionsFuntions.add(tmp);
            }
            catch (IllegalConditionException e) {
                ErrorHandle.OutputErrorInformation(e);
                ErrorHandle.OutputErrorInformation(s);
                return false;
            }
        }
        return true;
    }
    public boolean islastOneConditionUserFunction() {
        return conditionsFuntions.get(conditionsFuntions.size()-1).isUserDefined;
    }
    public ArrayList<Integer> match() {//
        ArrayList<Integer> rst=new ArrayList<>(2);
        for(OneCondition onec:conditionsFuntions) {
            if(!onec.isUserDefined&&isConditionITure(onec)) {//如果不是用户函数，并且该条件表达式为真
                rst.add(onec.id);
            }
        }
        return rst;
    }
    private boolean isConditionITure(OneCondition onec) {
        if(!enable) {
            ErrorHandle.OutputErrorInformation("ConditionAnalyser不可用");
            return false;
        }
        for(int i=0;i<onec.disjunctionUnits.size();i++) {
            try {
                if(!isOneDisjunctionTure(onec.disjunctionUnits.get(i))) {
                    return false;
                }
            }
            catch (IllegalOperatorException e) {
                ErrorHandle.OutputErrorInformation(e);
                return false;
            }
            catch (NotNumberException e2) {
                ErrorHandle.OutputErrorInformation(e2);
                return false;
            }
            catch (IllegalAccessException e3) {
                ErrorHandle.OutputErrorInformation("调用getElementByName失败");
                ErrorHandle.OutputErrorInformation(e3);
                return false;
            }
            catch (IllegalArgumentException e4) {
                ErrorHandle.OutputErrorInformation("调用getElementByName失败");
                ErrorHandle.OutputErrorInformation(e4);
                return false;
            }
            catch (InvocationTargetException e5) {
                ErrorHandle.OutputErrorInformation("调用getElementByName失败");
                ErrorHandle.OutputErrorInformation(e5);
                return false;
            }
            catch (CallingUndefinedUserFunctionException e6) {
                ErrorHandle.OutputErrorInformation(e6);
            }
        }
        return true;
    }
    private boolean isOneDisjunctionTure(DisjunctionUnit du) throws IllegalOperatorException,NotNumberException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException,CallingUndefinedUserFunctionException {
        for(int i=0;i<du.option.size();i++) {
            if(singleJudge(du.left.get(i),du.right.get(i),du.option.get(i))) {
                return true;
            }
        }
        return false;
    }
    public ConditionAnalyser(Method getElementByName) {
        conditionsFuntions=new ArrayList<>();
        functionRegistry=new HashMap<>();
        this.getElementByName=getElementByName;
        enable=true;
    }
    public void clear() {
        getElementByName=null;
        conditionsFuntions.clear();
        functionRegistry.clear();
        enable=false;
    }
    private boolean singleJudge(String left,String right,String option) throws IllegalOperatorException,NotNumberException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException,CallingUndefinedUserFunctionException {//判断一个left option right三元组是否为ture
        if(option.equals("@")) {
            try {
                return isConditionITure(conditionsFuntions.get(functionRegistry.get(left)));
            }
            catch (NullPointerException e) {
                throw new CallingUndefinedUserFunctionException("调用了未定义的UserFunction："+left);
            }
        }
        String leftReflectValue=null;
        String rightReflectValue=null;
        Object []objs=new Object[1];
        objs[0]=left;
        String tempGetValue=(String)getElementByName.invoke(getElementByName.getClass(),objs);
        leftReflectValue=tempGetValue;
        objs[0]=right;
        tempGetValue=(String)getElementByName.invoke(getElementByName.getClass(),objs);
        rightReflectValue=tempGetValue;
        if(leftReflectValue!=null) {
            left=leftReflectValue;
        }
        if(rightReflectValue!=null) {
            right=rightReflectValue;
        }
        return compare(left,right,option);
    }
    private boolean compare(String left,String right,String operator) throws NotNumberException,IllegalOperatorException{
        if(operator.equals("==")) {
            return left.equals(right);
        }
        else if(operator.equals("!=")) {
            return !left.equals(right);
        }
        //todo
        else if(operator.equals(">>")){
            try {
                return Double.parseDouble(left)>Double.parseDouble(right);
            }
            catch (NumberFormatException e) {
                try {
                    return Integer.parseInt(left)>Integer.parseInt(right);
                }
                catch (NumberFormatException e2) {
                    throw new NotNumberException("非数值类型："+left+"or"+right);
                }
            }
        }
        else if(operator.equals("<<")) {
            return false;
        }
        else if(operator.equals(">=")) {
            return false;
        }
        else if(operator.equals("<=")) {
            return false;
        }
        else{
            throw new IllegalOperatorException("不合法的condition，没有标准的比较运算符。Operator:"+operator);
        }
    }
    class NotNumberException extends Exception {
        public NotNumberException(String msg) {
            super(msg);
        }
    }
    class IllegalOperatorException extends Exception {
        public IllegalOperatorException(String msg)
        {
            super(msg);
        }
    }
    class CallingUndefinedUserFunctionException extends  Exception {
        public CallingUndefinedUserFunctionException(String msg) {
            super(msg);
        }
    }
    class IllegalConditionException extends Exception {
        public IllegalConditionException(String msg) {
            super(msg);
        }
    }
}
