package dm.tools;

import dm.state.Constants;

import java.lang.reflect.Method;
import java.util.ArrayList;
class ConstantsOfAE {
    public static String argsListSeparator=";";
    public static String actionsSeparator=",";
    public static String varFieldSeparator="\\.";
}
class SingleAction {
    //public String methodName;//第一版无用
    public String []path;
    public String value;
    public SingleAction(String action) throws IllegalSyntaxOfActionException{
        try {
            int index=action.indexOf("(");
            if(index>-1) {//包含括号
                /*第一版不拆分存储 todo 下一版优化运行时效率
                String []strs=action.split("\\(");
                String []path=strs[0].split(ConstantsOfAE.varFieldSeparator);
                methodName=path[path.length-1];*/
                path=action.split(ConstantsOfAE.varFieldSeparator);
                value=null;
            }
            else {//不包含括号 index==-1
                index=action.indexOf(":=");
                if(index>-1) {
                    StringBuffer strbuf=new StringBuffer();
                    strbuf.append("create(");
                    String []strs=action.split(":=");
                    String []strs2=strs[0].split(ConstantsOfAE.varFieldSeparator);
                    for(String s:strs2) {
                        strbuf.append(s+ ConstantsOfAE.argsListSeparator);
                    }
                    strbuf.append(strs[1]);
                    strbuf.append(")");
                    path=new String[]{strbuf.toString()};
                    value=null;
                }
                else {
                    index=action.indexOf("=");
                    if(index>-1) {
                        String []strs=action.split("=");
                        String []strs2=strs[0].split(ConstantsOfAE.varFieldSeparator);
                        path=strs2;
                        value=strs[1];
                    }
                    else {
                        throw new IllegalSyntaxOfActionException("");
                    }
                }
            }
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation(e);
            throw new IllegalSyntaxOfActionException("错误的action语法："+action);
        }
    }
    public String []getPath() {
        return path;
    }
    public String getValue() {
        return value;
    }
}
class OneGroupAction {
    int index;
    public ArrayList<SingleAction> groupActions;
    public String originGroupAction;
    public OneGroupAction(String s,int index) throws IllegalSyntaxOfActionException{
        this.originGroupAction=s;
        groupActions=new ArrayList<>();
        String []strs=s.split(ConstantsOfAE.actionsSeparator);
        for(String str:strs) {
            groupActions.add(new SingleAction(str));
        }

        this.index=index;
    }
    public String toString() {
        return originGroupAction;

    }
}
public class ActionExecutor {
    //private Method setElementByName;
    private Var varTable;
    private ArrayList<OneGroupAction> actions;
    public ActionExecutor(Var varTable) {
        this.varTable=varTable;
        //this.setElementByName=setElementByName;
        actions=new ArrayList<>();
    }
    public void addOneGroupAction(String s,int index) throws IllegalSyntaxOfActionException{
        actions.add(new OneGroupAction(s,index));
    }
    public void exec(int id) throws ActionExecutingException{
        try {
            OneGroupAction targetActions=actions.get(id);
            for(SingleAction sa:targetActions.groupActions) {
                varTable.setElementByName(sa.getPath(), sa.getValue());
            }
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation(e);
            throw new ActionExecutingException("执行Action时出错："+actions.get(id).toString());
        }
    }
    public static void main(String []args) {
        String s="a;;a";
        String []strs=s.split(";");
        for(String str:strs) {
            System.out.println("ccc"+str);
        }
    }
}
class ActionExecutingException extends Exception{
    public ActionExecutingException(String msg) {
        super(msg);
    }
}
class IllegalSyntaxOfActionException extends Exception {
    public IllegalSyntaxOfActionException(String msg) {
        super(msg);
    }
}