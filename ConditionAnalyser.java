package dm.tools;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
class ConstantsOfCA {
    public static String UserFunctionNameBegin="$";
    public static String UserFunctionNameEnd=":=";
    public static String UserFunctionCalling="@";
    //public static String regStr="(\\$([a-zA-Z0-9]|[\u4e00-\u9fa5])+:=([@a-zA-Z0-9\\.\u4e00-\u9fa5]+(==|!=|>>|<<|>=|<=)[@a-zA-Z0-9\\.\u4e00-\u9fa5]+[,\\|\\|]?)+)|([@a-zA-Z0-9\\.\u4e00-\u9fa5]+(==|!=|>>|<<|>=|<=)[@a-zA-Z0-9\\.\u4e00-\u9fa5]+[,\\|\\|]?)+";
    public static String regStr=".+";
    public static String DisjunctionSperator="\\|\\|";
    public static String []operators={"==","!=",">>","<<",">=","<="};
    public static String varFieldSeparator="\\.";
}
/**
 * 这个类记载了若干条件表达式，通过getElementBy
 */
public class ConditionAnalyser {//一种rule的一类condition的解析需要一个这样的对象。
    //private Method getElementByName=null;
    private Var varTable=null;
    private boolean enable=true;//是否可用
    class DisjunctionUnit {//析取式单元
        public ArrayList<Var> left;
        public ArrayList<String> option;
        public ArrayList<Var> right;
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
            int index=su.indexOf(ConstantsOfCA.UserFunctionCalling);;
            int i=0;
            if(index==0) {
                option.add(ConstantsOfCA.UserFunctionCalling);
                left.add(Var.parseVar(su.substring(index+ConstantsOfCA.UserFunctionCalling.length())));
                right.add(null);
                return;
            }
            index=-1;
            for(;i<ConstantsOfCA.operators.length;i++) {
                if((index=su.indexOf(ConstantsOfCA.operators[i]))>-1) {
                    break;
                }
            }
            if(i<ConstantsOfCA.operators.length) {
                option.add(ConstantsOfCA.operators[i]);
                left.add(Var.parseVar(su.substring(0,index)));
                right.add(Var.parseVar(su.substring(index+ConstantsOfCA.operators[i].length())));
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
    public ConditionAnalyser(Var varTable) {
        this.varTable=varTable;
        conditionsFuntions=new ArrayList<>();
        functionRegistry=new HashMap<>();
        //this.getElementByName=getElementByName;
        enable=true;
    }
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
                return false;
            }
            catch (TypeNotMatchException e7) {
                ErrorHandle.OutputErrorInformation(e7);
                return false;
            }
        }
        return true;
    }
    private boolean isOneDisjunctionTure(DisjunctionUnit du) throws IllegalOperatorException,NotNumberException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            CallingUndefinedUserFunctionException,TypeNotMatchException {
        for(int i=0;i<du.option.size();i++) {
            if(singleJudge(du.left.get(i),du.right.get(i),du.option.get(i))) {
                return true;
            }
        }
        return false;
    }
    public void clear() {
        //getElementByName=null;
        conditionsFuntions.clear();
        functionRegistry.clear();
        enable=false;
    }
    private boolean singleJudge(Var left,Var right,String option) throws IllegalOperatorException,NotNumberException,
            IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            CallingUndefinedUserFunctionException,TypeNotMatchException {//判断一个left option right三元组是否为ture
        if(option.equals(ConstantsOfCA.UserFunctionCalling)) {
            try {
                if(left.type!=VarType.Str) {
                    throw new TypeNotMatchException("UserFunctionName不是标准的字符串类型："+left.toString());
                }
                return isConditionITure(conditionsFuntions.get(functionRegistry.get(left.strVal)));
            }
            catch (NullPointerException e) {
                throw new CallingUndefinedUserFunctionException("调用了未定义的UserFunction："+left);
            }
        }
        Var tempGetValue=null;
        if(left.type==VarType.Str) {
            tempGetValue=varTable.getElementByName(left.strVal.split(ConstantsOfCA.varFieldSeparator));
            if(tempGetValue!=null) {
                left=tempGetValue;
            }
        }
        tempGetValue=null;
        if(right.type==VarType.Str) {
            java.lang.Object[]args=new Object[1];
            args[0]=right.strVal;
            tempGetValue=varTable.getElementByName(right.strVal.split(ConstantsOfCA.varFieldSeparator));
            if(tempGetValue!=null) {
                right=tempGetValue;
            }
        }
        return left.compareTo(right,option);
    }
    class NotNumberException extends Exception {
        public NotNumberException(String msg) {
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
