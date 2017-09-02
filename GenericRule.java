package dm.tools;

import org.json.JSONException;
import org.json.JSONObject;
import java.lang.reflect.Method;
import java.util.*;

/*keywords:
区分大小写
 */
class ConstantsOfGR {
    static String methodCallingReg="[0-9a-zA-Z_\\.]+\\((([0-9a-zA-Z_\\.]|[\u4e00-\u9fa5]|\\.)+;)*([0-9a-zA-Z_\\.]|[\u4e00-\u9fa5]|\\.)*\\)";
    static String argsListSeparator=";";//参数分隔符
    static String varFieldSeparator="\\.";//变量域分隔符
    static String defineSeparator="::";//定义分隔符
    static String fileEncodingType="utf-8";
    static String varDeclareSeparator=":=";//变量声明符号。强制赋值符号(变量不存在则创建)
    static String blockSeparator="##";//pattern的区块分隔符
}
enum VarType {Str,Int,Double,Dict,Undef}
class Var {
    public VarType type=VarType.Undef;
    private HashMap <String,Var> dict=null;
    String strVal=null;
    int intVal=0;
    double floVal=0;
    String name="";
    public String toJsonString() throws TypeNotMatchException {
        String js=this.toJsonStringWithName();
        int index=js.indexOf(this.name+"\":");
        return js.substring(index+this.name.length()+2);
    }
    private String toJsonStringWithName() throws TypeNotMatchException{
        StringBuffer strbuf=new StringBuffer();
        switch (this.type) {
            case Str:strbuf.append("\""+name+"\":"+"\""+strVal+"\"");return strbuf.toString();
            case Double:strbuf.append("\""+name+"\":"+"\""+floVal+"\"");return strbuf.toString();
            case Int:strbuf.append("\""+name+"\":"+"\""+intVal+"\"");return strbuf.toString();
            case Undef:throw new TypeNotMatchException("变量类型未定义");
            case Dict:{
                strbuf.append("\""+name+"\":{");
                Iterator iter=dict.keySet().iterator();
                while(iter.hasNext()) {
                    Var tmpvar=dict.get((String)iter.next());
                    strbuf.append(tmpvar.toJsonStringWithName());
                    strbuf.append(",");
                }
                strbuf.delete(strbuf.length()-1,strbuf.length());//删除最后一个逗号
                strbuf.append("}");
                return strbuf.toString();
            }
            default:throw new TypeNotMatchException("fatal error,不应走入的逻辑分支，in toJsonString");
        }
    }
    public boolean compareTo(Var v,String operator) throws TypeNotMatchException,IllegalOperatorException{
        if(this.type!=v.type) {
            throw new TypeNotMatchException("要比较的变量类型不匹配："+this.type.toString()+","+v.type.toString());
        }
        switch (operator) {
            case "==":return this.equals(v);
            case "!=":return !this.equals(v);
            case ">>":{
                switch (this.type) {
                    case Int:return this.intVal>v.intVal;
                    case Double:return this.floVal>v.floVal;
                    default:throw new TypeNotMatchException("非数值类型，无法使用比较运算符："+operator);
                }
            }
            case "<<":{
                switch (this.type) {
                    case Int:return this.intVal<v.intVal;
                    case Double:return this.floVal<v.floVal;
                    default:throw new TypeNotMatchException("非数值类型，无法使用比较运算符："+operator);
                }
            }
            case ">=":{
                switch (this.type) {
                    case Int:return this.intVal>=v.intVal;
                    case Double:return this.floVal>=v.floVal;
                    default:throw new TypeNotMatchException("非数值类型，无法使用比较运算符："+operator);
                }
            }
            case "<=":{
                switch (this.type) {
                    case Int:return this.intVal<=v.intVal;
                    case Double:return this.floVal<=v.floVal;
                    default:throw new TypeNotMatchException("非数值类型，无法使用比较运算符："+operator);
                }
            }
            default:throw new IllegalOperatorException("不合法的condition，没有标准的比较运算符。Operator:"+operator);
        }
    }
    public Var(HashMap<String,Var> v,String name) {
        this.dict=v;
        type=VarType.Dict;
        this.name=name;
    }
    public static Var parseVar(String s,String name) {
        try {
            return new Var(Integer.parseInt(s),name);
        }
        catch (NumberFormatException e) {
            try {
                return new Var(Double.parseDouble(s),name);
            }
            catch (NumberFormatException e2) {
                try {
                    return new Var(new JSONObject(s),name);
                }
                catch (JSONException e3) {
                    return new Var(s,name);
                }
            }
        }
    }
    public static Var parseVar(String s) {
        return parseVar(s,"unname");
    }
    public void copyToSelf(Var v) {
        this.type=v.type;
        this.name=v.name;
        this.dict=v.dict;
        this.floVal=v.floVal;
        this.strVal=v.strVal;
        this.intVal=v.intVal;
    }
    public Var(String s,String name) {
        this.strVal=s;
        type=VarType.Str;
        this.name=name;
    }
    public Var(int i,String name) {
        this.intVal=i;
        type=VarType.Int;
        this.name=name;
    }
    public Var(double f,String name) {
        this.floVal=f;
        type=VarType.Double;
        this.name=name;
    }
    public Var(JSONObject v,String name){//以JSONObject为基础生成变量
        type=VarType.Dict;
        this.name=name;
        dict=new HashMap<>();
        try {
            setJsonAsDict(v);
        }
        catch (Json2VarException e) {
            ErrorHandle.OutputErrorInformation("fatal error:wrong logic routes");//不应该走到这个分支
            ErrorHandle.OutputErrorInformation(e);
        }
    }
    public boolean equals(Var v) {
        if(type!=v.type) {
            return false;
        }
        else {
            switch (type) {
                case Int:return intVal==v.intVal;
                case Str:return strVal.equals(v.strVal);
                case Dict:return this.equals(v.dict);
                case Double:return floVal==v.floVal;
                case Undef:return false;
                default:return false;
            }
        }
    }
    public boolean equals(String s) {
        return equals(this.parseVar(s,"rt"));
    }
    public boolean equals(HashMap<String,Var> m) {//判断两个dict是否相等
        if(this.dict.size()!=m.size()) {
            return false;
        }
        else {
            Iterator<Map.Entry<String,Var>> i=this.dict.entrySet().iterator();
            while(i.hasNext()) {
                Map.Entry<String,Var> e=i.next();
                String s=e.getKey();
                Var v=e.getValue();
                if(v==null) {
                    if(!(m.get(s)==null&&m.containsKey(s))) {
                        return false;
                    }
                }
                else {
                    if(!v.equals(m.get(s))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }
    public Var getElementByName(String []name) {
        if(name[name.length-1].matches(ConstantsOfGR.methodCallingReg)) {//调用dict的内置方法
            String []str=new String[name.length-1];
            for(int i=0;i<str.length;i++) {
                str[i]=name[i];
            }
            String methodCallingBody=name[name.length-1];
            int index=methodCallingBody.indexOf("(");
            String methodName=methodCallingBody.substring(0,index);
            String argsList=methodCallingBody.substring(index+1,methodCallingBody.length()-1);
            try {
                return getElementByName(str,0,dict).dictSearchMethodExtend(methodName,argsList.split(ConstantsOfGR.argsListSeparator));
            }
            catch (Exception e) {//发生exception即找不到对应的element，因此返回null
                return null;
            }
        }
        else {//是单纯地在查Var树里的变量
            return getElementByName(name,0,dict);
        }
    }
    private Var dictSearchMethodExtend(String methodName,String []args) throws ArgsNotMatchException,NoSuchMethodForDictSearchException{
        if(methodName.equals("containsKey")) {
            if(args.length!=1) {
                throw new ArgsNotMatchException("参数列表长度不匹配，method："+methodName+",argsLength:"+args.length);
            }
            else {
                if(this.dict.containsKey(args[0])) {
                    return new Var(1,"rt");
                }
                else {
                    return new Var(0,"rt");
                }
            }
        }
        else if(methodName.equals("containsValue")) {
            if(args.length!=1) {
                throw new ArgsNotMatchException("参数列表长度不匹配，method："+methodName+",argsLength:"+args.length);
            }
            else {
                Iterator iter=this.dict.entrySet().iterator();
                Var right=Var.parseVar(args[0]);
                while(iter.hasNext()) {
                    Map.Entry<String,Var> entry=(Map.Entry<String,Var>)iter.next();
                    if(right.equals(entry.getValue())) {
                        return new Var(1,"rt");
                    }
                }
                return new Var(0,"rt");
            }
        }
        else if(methodName.equals("containsKeyAndValue")) {
            if(args.length!=2) {
                throw new ArgsNotMatchException("参数列表长度不匹配，method："+methodName+",argsLength:"+args.length);
            }
            else {
                if(this.dict.containsKey(args[0])) {
                    Var v=this.dict.get(args[0]);
                    if(v.equals(this.parseVar(args[1],"rt"))) {
                        return new Var(1,"rt");
                    }
                    else {
                        return new Var(0,"rt");
                    }
                }
                else {
                    return new Var(0,"rt");
                }
            }
        }
        else {
            throw new NoSuchMethodForDictSearchException("找不到方法："+methodName);
        }
    }
    private Var getElementByName(String []name,int index,HashMap<String,Var> m) {//是单纯地在查Var树里的变量
        if(name.length-1==index) {
            try {
                return m.get(name[index]);
            }
            catch (NullPointerException e) {//m==null,没有相应的变量
                return null;
            }
        }
        else {
            try {
                return getElementByName(name,index+1,m.get(name[index]).dict);
            }
            catch (NullPointerException e) {
                return null;
            }
        }
    }
    public void addVarInDict(String name,Var var) throws TypeNotMatchException{//覆盖式插入
        if(type!=VarType.Dict) {
            throw new TypeNotMatchException("变量"+name+"不为Dict类型，无法执行addVarInDict。类型为："+this.type.toString());
        }
        else {
            dict.put(name,var);
        }
    }
    private void setJsonAsDict(JSONObject o) throws Json2VarException{//根据json对象生成一个新的dict，取代旧的this.dict
        if(type!=VarType.Dict) {
            new Json2VarException("变量"+this.name+"不是Dict类型！类型为："+this.type.toString());
        }
        HashMap<String,Var> tmp=dict;
        dict.clear();
        Iterator i=o.keys();
        while(i.hasNext()) {
            String key=(String) i.next();
            try {
                int a=o.getInt(key);
                dict.put(key,new Var(a,key));
            }
            catch (JSONException e) {
                try {
                    double a=o.getDouble(key);
                    dict.put(key,new Var(a,key));
                }
                catch (JSONException e2) {
                    try {
                        dict.put(key,new Var(o.getJSONObject(key),key));
                    }
                    catch (JSONException e3) {
                        try {
                            dict.put(key,new Var(o.getString(key),key));
                        }
                        catch (JSONException e4) {
                            ErrorHandle.OutputErrorInformation(e4);
                            dict=tmp;//复原到未操作前
                            throw new Json2VarException("Json.getString error,key:"+key);
                        }
                    }
                }
            }
        }
    }
    public void setElementByName(String []name,String value) throws SetUndefinedVarException,
            CallingUserMethodException,TypeNotMatchException{
        /*规则文件形式：path.method(arg1;arg2;...;argn) or path=value
        其中path必须存在，本函数将取得this.path作为target,对target进行操作。
        不同的method的参数说明见更详细的使用文档
        */
        //如果是调用方法，那么形参value将不会用到
        //[]name中是从this开始的路径，如果是普通赋值，name[len-1]是最终的变量名；如果是方法调用，
        //name[len-1]为：方法名(参数1;参数2;...)
        if(name[name.length-1].matches(ConstantsOfGR.methodCallingReg)) {//如果调用的是修改dict的扩展方法
            Var target=null;
            if(name.length==1) {
                target=this;
                if(target.type!=VarType.Dict) {
                    throw new TypeNotMatchException("调用的方法不属于Dict类型变量，"+this.name+",type:"+target.type.toString());
                }
            }
            else {
                String []str=new String[name.length-1];
                for(int i=0;i<str.length;i++) {
                    str[i]=name[i];
                }
                target=this.getElementByName(str);
                if(target==null) {
                    StringBuffer strbuf=new StringBuffer();
                    for(int i=0;i<str.length;i++) {
                        strbuf.append(str[i]+ConstantsOfGR.varFieldSeparator);
                    }
                    throw new SetUndefinedVarException("调用未定义的变量下的方法"+strbuf.toString());
                }
                if(target.type!=VarType.Dict) {
                    StringBuffer strbuf=new StringBuffer();
                    for(int i=0;i<str.length;i++) {
                        strbuf.append(str[i]+ConstantsOfGR.varFieldSeparator);
                    }
                    throw new TypeNotMatchException("调用的方法不属于Dict类型变量，"+strbuf.toString()+",type:"+target.type.toString());
                }
            }
            String methodCallingBody=name[name.length-1];
            int index=methodCallingBody.indexOf("(");
            String methodName=methodCallingBody.substring(0,index);
            String []argsList=methodCallingBody.substring(index+1,methodCallingBody.length()-1).split(ConstantsOfGR.argsListSeparator);
            if(isInnerExtendMethod(methodName)) {//内置方法
                target.dictModifyMethodExtend(methodName,argsList);//调用target下的内置方法
            }
            else {//用户扩展方法
                Method e=UserCreateRegion.methodRegistry.get(methodName);
                ArrayList<Object> args=new ArrayList<>();
                args.add(target);//使用用户扩展方法操作target
                args.add(argsList);
                try {
                    e.invoke(UserCreateRegion.class,args);
                }
                catch (NullPointerException e3) {
                    ErrorHandle.OutputErrorInformation(e3);
                    throw new CallingUserMethodException("调用了未定义的方法："+methodName);
                }
                catch (Exception e2) {
                    ErrorHandle.OutputErrorInformation(e2);
                    throw new CallingUserMethodException("修改varTable时调用userMethod出错");
                }
            }
        }
        else {//如果只是简单地set Var树中的变量
            Var target=getElementByName(name);
            if(target==null) {//未定义的变量
                throw new SetUndefinedVarException("无法给未定义的变量赋值："+name);
            }
            else {
                target.copyToSelf(Var.parseVar(value,target.name));
            }
        }
    }
    private void dictModifyMethodExtend(String methodName,String args[]) throws TypeNotMatchException{
        //todo
        //create方法：参数args[]中，最后一个元素是value,前面的元素构成从this开始的完整路径。如this.intention等。
        //在rulefile中，create方法可以写为create(path,value),也可以写为path:=value
        //
        //本函数中的所有方法接受的路径都认为是相对于this的，调用此函数前需要整理好路径参数
        if(methodName.equals("create")) {//创建变量，如果路径不存在就创建路径  currentDir\path
            //参数：path，value
            String []path=new String [args.length-1];
            String value=args[args.length-1];
            for(int i=0;i<path.length;i++) {
                path[i]=args[i];
            }
            createInDict(this,path,value,0);
        }
    }
    public static void createInDict(Var dt,String []path,String value,int index) throws TypeNotMatchException{
        if(dt.type!=VarType.Dict) {
            throw new TypeNotMatchException("非Dict类型，无法在其中创建新变量。name："+dt.name+",type:"+dt.type.toString());
        }
        if(index==path.length-1) {
            Var tmp=dt.dict.get(path[index]);
            if(tmp!=null) {
                tmp.copyToSelf(Var.parseVar(value,tmp.name));
            }
            else {
                dt.dict.put(path[path.length-1],Var.parseVar(value,path[path.length-1]));
            }
        }
        else {
            Var tmp=dt.dict.get(path[index]);
            if(tmp!=null) {
                createInDict(tmp,path,value,index+1);
            }
            else {
                tmp=new Var(new HashMap<String,Var>(),path[index]);
                dt.addVarInDict(path[index],tmp);
                createInDict(tmp,path,value,index+1);
            }
        }
    }
    private static boolean isInnerExtendMethod(String methodName) {
        if(methodName.equals("create"))
            return true;
        return false;
    }
    public String toString() {
        String rst="name:"+name+",type:"+type.toString()+",value:";
        switch (type) {
            case Int:return rst+intVal;
            case Double:return rst+floVal;
            case Str:return rst+strVal;
            case Dict:return rst+dict.toString();
            case Undef:return rst+"undef";
            default:return rst+"undef";
        }
    }
}
class ArgsNotMatchException extends Exception {
    public ArgsNotMatchException(String msg) {
        super(msg);
    }
}
class NoSuchMethodForDictSearchException extends Exception {
    public NoSuchMethodForDictSearchException(String msg) {
        super(msg);
    }
}
class Json2VarException extends Exception {
    public Json2VarException(String msg) {
        super(msg);
    }
}
class TypeNotMatchException extends Exception {
    public TypeNotMatchException(String msg) {
        super(msg);
    }
}
class InitFailedException extends Exception {
    public InitFailedException(String msg) {
        super(msg);
    }
}
class SetUndefinedVarException extends Exception {
    public SetUndefinedVarException(String msg) {
        super(msg);
    }
}
class RuleInputFormatException extends Exception{
    public RuleInputFormatException(String msg) {
        super(msg);
    }
}
class IllegalOperatorException extends Exception {
    public IllegalOperatorException(String msg) {
        super(msg);
    }
}
class IllegalPatternException extends Exception {
    public IllegalPatternException(String msg) {
        super(msg);
    }
}
class RuleMatchingException extends Exception {
    public RuleMatchingException(String msg) {
        super(msg);
    }
}
public class GenericRule {
    enum blockType{condition,action};
    private ConditionAnalyser conditionAnalyser;
    private ActionExecutor actionExecutor;
    private String urlOfRuleFile;
    private ArrayList <blockType> descriptionOfRuleStructure;//描述一条rule的组织结构
    private Var varTable;//存储着一套规则中的所有变量，包括input和inner，即输入变量和内部变量
    private boolean enable=true;
    private ArrayList<String> LoadRule(String urlOfRuleFile) {
        IOAPI tmpio=new IOAPI(1);
        tmpio.startRead(urlOfRuleFile,ConstantsOfGR.fileEncodingType,0);
        String line=tmpio.readOneSentence(0);
        ArrayList<String> rst=new ArrayList<>();
        while(line!=null) {
            line=line.replaceAll("\\s+","");
            int index=line.indexOf("//");
            if(index==0) {
                line=tmpio.readOneSentence(0);
                continue;
            }
            else if(index<0){
                if(line.length()>0) {
                    rst.add(line);
                }
            }
            else {
                rst.add(line.substring(0,index));
            }
            line=tmpio.readOneSentence(0);
        }
        tmpio.endRead(0);
        return rst;
    }
    public Var getElementByName(String key) {
        return varTable.getElementByName(key.split(ConstantsOfGR.varFieldSeparator));
    }
    public GenericRule(String urlOfRF) throws InitFailedException{//构造时生成inner表
        this.urlOfRuleFile=urlOfRF;
        descriptionOfRuleStructure=new ArrayList<>();
        try {
            init();
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation(e);
            enable=false;
            throw new InitFailedException("Rule初始化失败");
        }
        enable=true;
    }
    private ArrayList<String> getFileBlock(ArrayList<String> fileContent,String start,String end) {
        int indexStart=-1;
        int indexEnd=-1;
        for(int i=0;i<fileContent.size()&&(indexStart<0||indexEnd<0);i++) {
            if(fileContent.get(i).equals(start)) {
                indexStart=i;
            }
            if(fileContent.get(i).equals(end)) {
                indexEnd=i;
            }
        }
        ArrayList<String> rst=new ArrayList<>();
        for(int i=indexStart+1;i<indexEnd;i++) {
            rst.add(fileContent.get(i));
        }
        return rst;
    }
    private void defineReplace(ArrayList<String> target,ArrayList<String> defContent) throws InitFailedException{
        for(String s:defContent) {
            String []strs=s.split(ConstantsOfGR.defineSeparator);
            if(strs.length!=2) {
                throw new InitFailedException("错误的语法，in define block："+s);
            }
            for(int i=0;i<target.size();i++) {
                target.set(i,target.get(i).replaceAll(strs[0],strs[1]));
            }
        }
    }
    public void init() throws InitFailedException{//调用此函数可重新初始化，即支持重编辑文件后reload
        descriptionOfRuleStructure.clear();
        varTable=new Var(new HashMap<String,Var>(),"varTable");
        try {
            varTable.addVarInDict("inner",new Var(new HashMap<String,Var>(),"inner"));
        }
        catch (TypeNotMatchException e) {
            enable=false;
            ErrorHandle.OutputErrorInformation(e);
            throw new InitFailedException("Rule初始化失败");
        }
        conditionAnalyser=new ConditionAnalyser(varTable);
        actionExecutor=new ActionExecutor(varTable);
        ArrayList<String> ruleFileContent=LoadRule(urlOfRuleFile);
        //取得文件中的四个Block：
        ArrayList<String> defBlock=getFileBlock(ruleFileContent,"==DEFBEGIN","==DEFEND");
        ArrayList<String> innerVarBlock=getFileBlock(ruleFileContent,"==VARBEGIN","==VAREND");
        ArrayList<String> patternBlock=getFileBlock(ruleFileContent,"==PATTERNBEGIN","==PATTERNEND");
        ArrayList<String> actionBlock=getFileBlock(ruleFileContent,"==ACTIONBEGIN","==ACTIONEND");
        //define替换：
        defineReplace(innerVarBlock,defBlock);
        defineReplace(patternBlock,defBlock);
        defineReplace(actionBlock,defBlock);
        //innerVar注册：
        try {
            for(String str:innerVarBlock) {
                String []s=str.split(ConstantsOfGR.varDeclareSeparator);
                StringBuffer strbuf=new StringBuffer();
                strbuf.append("create(");
                for(String s1:s[0].split(ConstantsOfGR.varFieldSeparator)) {
                    strbuf.append(s1+ConstantsOfGR.argsListSeparator);
                }
                strbuf.append(s[1]+")");
                String []name={"inner",strbuf.toString()};
                varTable.setElementByName(name,null);
            }
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation(e);
            throw new InitFailedException("rule初始化失败。解析变量定义时出错。");
        }
        //todo：简易版已完成。待重构。
        //解析patternBlock：
        try {
            //首句话必须是bsd:BlockStructureDescription
            confirmBlockStructure(patternBlock.get(0));
            int pid=0;
            int ucid=-1;
            for(int i=1;i<patternBlock.size();i++) {
                String pat=patternBlock.get(i);
                String []contents=pat.split(ConstantsOfGR.blockSeparator);
                if(contents.length==descriptionOfRuleStructure.size()) {
                    for(int j=0;j<contents.length;j++) {
                        if(descriptionOfRuleStructure.get(j)==blockType.action) {
                            actionExecutor.addOneGroupAction(contents[j],pid);
                        }
                        else if(descriptionOfRuleStructure.get(j)==blockType.condition) {
                            conditionAnalyser.addOneCondition(contents[j],pid);
                        }
                        else {
                            throw new IllegalPatternException("不支持的block类型："+descriptionOfRuleStructure.get(j).toString());
                        }
                    }
                    pid++;
                }
                else if(contents.length==1){//用户定义的condition函数
                    conditionAnalyser.addOneCondition(contents[0],ucid);
                    ucid--;
                }
                else {
                    for(String s:contents) {
                        System.out.println(s);
                    }
                    throw new IllegalPatternException("无法解析的pattern："+patternBlock.get(i).toString());
                }
            }
        }
        catch (Exception e) {
            //一个规则文件必须有pattern，所以如果patternBlock.Size==0的话也会抛出异常
            ErrorHandle.OutputErrorInformation(e);
            throw new InitFailedException("rule初始化失败。解析pattern时出错。");
        }
        //解析actionBlock：暂时不实现
    }
    private void confirmBlockStructure(String bsd) throws IllegalPatternException{//bsd:BlockStructureDescription
        //todo:more flexible
        String []blocks=bsd.split(ConstantsOfGR.blockSeparator);
        for(int i=0;i<blocks.length;i++) {
            if(blocks[i].equals("condition")) {
                descriptionOfRuleStructure.add(blockType.condition);
            }
            else if(blocks[i].equals("action")) {
                descriptionOfRuleStructure.add(blockType.action);
            }
            else {
                throw new IllegalPatternException("BlockStructureDescription不合法："+blocks[i]);
            }
        }
    }
    public void Analyze(ArrayList<JSONObject> inputs,ArrayList<String> inputsName) throws TypeNotMatchException,
            RuleInputFormatException,RuleMatchingException,ActionExecutingException {
        //将输入转化为变量：
        if(inputs.size()!=inputsName.size()) {
            throw new RuleInputFormatException("inputs和inputsName列表长度不一致："+inputs.size()+" "+inputsName.size());
        }
        for(int i=0;i<inputsName.size();i++) {
            varTable.addVarInDict(inputsName.get(i),new Var(inputs.get(i),inputsName.get(i)));
        }
        //todo：简易版已完成。待重构。
        //匹配条件：
        ArrayList<Integer> matchedIndex=conditionAnalyser.match();
        if(matchedIndex.size()>1) {
            throw new RuleMatchingException("匹配到了多条规则或未匹配到规则:"+matchedIndex.size());
        }
        else if(matchedIndex.size()==0) {

        }
        //执行action：
        actionExecutor.exec(matchedIndex.get(0));
    }
    public static void main(String []args) {
        try {
            //GenericRule gr=new GenericRule(ConstantsOfCA.class.getResource("../IntentionMapping").getFile());
            Var var=new Var(new JSONObject("{\"query\":\"[22]\",\"category\":{\"c3\":\"\",\"c1\":\"\",\"c2\":\"\"},\"intention\":\"未识别\"}"),"nlu1");
            System.out.println(var.toJsonString());
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation(e);
        }
    }
}
