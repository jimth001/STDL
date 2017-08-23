package dm.tools;
import dm.DM;
import dm.state.Constants;
import org.apache.lucene.search.spell.*;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.io.File;
class ConstantsOfNLUAP {

    static String spIntentionTemplatesUrl=ConstantsOfCA.class.getResource("../spIntentions").getFile();
    static String IntentionMapperTemplatesUrl=ConstantsOfCA.class.getResource("../IntentionMapping").getFile();
    static String FILE_ENCODING="utf-8";
}
class SPIntentionTemplate {
    private String intention;//特殊意图。对上文的回应。
    private ArrayList<String> templates=null;//该意图的模板。
    private String condition;//描述什么条件下会有这种特殊意图。对应dm的action。
    private String templatesFileUrl=null;
    public void init(String url) {
        if(url!=null) {
            templatesFileUrl=url;
        }
        init();
    }
    private void init() {//使用该函数可以实现reload
        IOAPI tmpio=new IOAPI(1);
        try {
            tmpio.startRead(templatesFileUrl, ConstantsOfNLUAP.FILE_ENCODING,0);
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation("002\n"+e.getStackTrace());
            return;
        }
        if(templates==null) {
            templates=new ArrayList<>();
        }
        else {
            templates.clear();
        }
        String line=tmpio.readOneSentence(0);
        while(line!=null) {
            int index=line.indexOf("//");
            if(index>0) {
                line=line.substring(0,index);
            }
            else if(index==0) {
                line=tmpio.readOneSentence(0);
                continue;
            }
            line=line.replaceAll("//s+","");
            if(line.length()==0) {
                line=tmpio.readOneSentence(0);
            }
            else {
                int in1=line.indexOf("condition");
                if(in1==0) {
                    this.condition=line.substring(in1+new String("condition:").length());
                }
                else {
                    int in2=line.indexOf("spIntention:");
                    if(in2==0) {
                        this.intention=line.substring(in2+new String("spIntention:").length());
                    }
                    else {
                        templates.add(line);
                    }
                }
                line=tmpio.readOneSentence(0);
            }
        }
        tmpio.endRead(0);
    }
    public SPIntentionTemplate(String templateFileUrl) {
        init(templateFileUrl);
    }
    public ArrayList<String> getTemplates() {
        return templates;
    }
    public String getCondition() {
        return condition;
    }
    public String getIntention() {
        return intention;
    }
}
class IntentionRefineResult {
    private boolean issp;//是否特殊
    private String intention;
    private float sim;
    private String matchedSen;
    public String getIntention() {
        return intention;
    }
    public float getSim() {
        return sim;
    }
    public String getMatchedSen() {
        return matchedSen;
    }
    public IntentionRefineResult(NGDResult rst,String intention) {
        this.issp=true;
        this.intention=intention;
        this.sim=rst.sim;
        this.matchedSen=rst.matchedSen;
    }
    public IntentionRefineResult(String intention) {
        issp=false;
        this.intention=intention;
        this.sim=0;
        this.matchedSen="null";
    }
    public void modify(NGDResult rst,String intention) {
        this.issp=true;
        this.intention=intention;
        this.sim=rst.sim;
        this.matchedSen=rst.matchedSen;
    }
    public void modify(String intention) {
        issp=false;
        this.intention=intention;
        this.sim=0;
        this.matchedSen="null";
    }
    public boolean isSPI() {
        return issp;
    }
}
class NGDResult {
    float sim;//相似度
    String matchedSen;//匹配的句子
    public NGDResult(float similarity,String matchedSentence) {
        this.sim=similarity;
        this.matchedSen=matchedSentence;
    }
}
class IntentionMapperTemplate {
    private ArrayList<String> newIntention=null;
    private ConditionAnalyser ca=null;
    private String urlOfTemplates=null;
    public IntentionMapperTemplate(String url) throws NoSuchMethodException{
        init(url);
    }
    /**
     * 支持reload
     * @param url
     */
    public void init(String url) throws NoSuchMethodException{
        if(url!=null) {
            this.urlOfTemplates=url;
        }
        init();
        System.out.println("IntentionMapper初始化完成");
    }
    private void init() throws NoSuchMethodException{
        IOAPI tmpio=new IOAPI(1);
        try {
            tmpio.startRead(urlOfTemplates,ConstantsOfNLUAP.FILE_ENCODING,0);
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation("003\n"+e.getStackTrace());
            return;
        }
        if(newIntention==null) {
            newIntention=new ArrayList<>();
        }
        else {
            newIntention.clear();
        }
        if(ca==null) {
            Class []args=new Class[1];
            args[0]=String.class;
            ca=new ConditionAnalyser(NLUAfterProcessor.class.getDeclaredMethod("getElementByName",args));
        }
        else {
            ca.clear();
        }
        String line=tmpio.readOneSentence(0);
        while(line!=null) {
            int index=line.indexOf("//");
            if(index>0) {
                line=line.substring(0,index);
            }
            else if(index==0) {
                line=tmpio.readOneSentence(0);
                continue;
            }
            line=line.replaceAll("//s+","");
            if(line.length()==0) {
                line=tmpio.readOneSentence(0);
            }
            else {
                String []strs=line.split("##");
                if(strs.length==1) {//为userFunction，不带意图
                    ca.addOneCondition(strs[0],-1);//此版本的userFunction默认id均为-1
                }
                else if(strs.length!=2) {
                    ErrorHandle.OutputErrorInformation("不合法的规则："+line);
                }
                else {
                    if(ca.addOneCondition(strs[0],newIntention.size())) {//
                        if(!ca.islastOneConditionUserFunction()) {//带意图，不为userFunction
                            newIntention.add(strs[1]);
                        }
                    }
                }
                line=tmpio.readOneSentence(0);
            }
        }
        tmpio.endRead(0);
    }

    public ArrayList<String> getNewMappingIntention() {
        ArrayList<Integer> index=ca.match();
        ArrayList<String> rst=new ArrayList<>(2);
        for(Integer i:index) {
            rst.add(newIntention.get(i));
        }
        return rst;
    }
}
class IntentionRefine {//readOnly.不需要new，要注意先调用init初始化
    private static ArrayList<SPIntentionTemplate> spIntentionTemplates=null;
    private static final float spIntentionSimThre=(float) 0.5;//特殊意图要求的相似度阈值
    private static NGramDistance ngdis=new NGramDistance();
    private static List<File> SPIT_URLs=null;
    private static IntentionMapperTemplate intentionMapper=null;
    private static boolean intentionMapperEnable=true;
    private static boolean isInited=false;
    public static void init() {//初始化
        if(spIntentionTemplates==null) {
            spIntentionTemplates=new ArrayList<>();
        }
        else {
            spIntentionTemplates.clear();
        }
        //todo
        try {
            if(intentionMapper==null) {
                intentionMapper=new IntentionMapperTemplate(ConstantsOfNLUAP.IntentionMapperTemplatesUrl);
            }
            else {
                intentionMapper.init(null);
            }
            intentionMapperEnable=true;
        }
        catch (NoSuchMethodException e) {
            ErrorHandle.OutputErrorInformation("intentionMapper初始化失败！\n");
            intentionMapperEnable=false;
        }
        if(SPIT_URLs==null) {
            SPIT_URLs=new ArrayList<File>();
        }
        else {
            SPIT_URLs.clear();
        }
        if(spIntentionTemplates==null) {
            spIntentionTemplates=new ArrayList<>();
        }
        else {
            spIntentionTemplates.clear();
        }
        try {
            FileOperateAPI.visitDirsAllFiles(ConstantsOfNLUAP.spIntentionTemplatesUrl,SPIT_URLs);
            for(File f:SPIT_URLs) {
                spIntentionTemplates.add(new SPIntentionTemplate(f.getPath()));
            }
        }
        catch (Exception e) {//如果读取失败了
            ErrorHandle.OutputErrorInformation("005\n"+e.getStackTrace());//
        }
    }
    private static void intentionMapping(IntentionRefineResult r) {//意图映射
        if(intentionMapperEnable) {
            ArrayList<String> newIntention=intentionMapper.getNewMappingIntention();
            if(newIntention.size()==0) {//没有匹配，就不映射
                return;
            }
            else if(newIntention.size()==1){
                r.modify(newIntention.get(0));
            }
            else {
                ErrorHandle.OutputErrorInformation("IntentionMapper匹配到多条！");

            }
        }
    }
    private static void spIntentionIdentify(IntentionRefineResult r) {//特殊意图识别。
        NGDResult bestMatch=null;
        String spIntention=null;
        for(int i=0;i<spIntentionTemplates.size();i++) {
            SPIntentionTemplate spit=spIntentionTemplates.get(i);
            if(NLUAfterProcessor.getElementByName("lastaction").equalsIgnoreCase(spit.getCondition())) {
                NGDResult tmpResult=GetNGRAMDistance(NLUAfterProcessor.getElementByName("query"),spit.getTemplates());
                if(bestMatch.sim<tmpResult.sim) {
                    bestMatch=tmpResult;
                    spIntention=spit.getIntention();
                }
            }
            else {
                continue;
            }
        }
        if(bestMatch!=null&&bestMatch.sim>spIntentionSimThre) {//阈值足够
            r.modify(bestMatch,spIntention);
        }
    }
    public static IntentionRefineResult analyse() {
        if(!isInited) {
            init();
        }
        IntentionRefineResult irr=new IntentionRefineResult(NLUAfterProcessor.getElementByName("intention"));
        intentionMapping(irr);
        spIntentionIdentify(irr);
        return irr;
    }
    private static float GetNGRAMDistance(String s1,String s2) {//计算s1和s2的ngram距离
        return ngdis.getDistance(s1,s2);
    }
    private static NGDResult GetNGRAMDistance(String str, ArrayList<String>templates ) {//计算str和模板的相似度，并返回最大值及匹配的sentence
            float sim=0;
            String mostlikelysen="null";
            for(int j=0;j<templates.size();j++) {
                float tmp=GetNGRAMDistance(str,templates.get(j));
                if(tmp>sim) {
                    sim=tmp;
                    mostlikelysen=templates.get(j);
                }
            }
            return new NGDResult(sim,mostlikelysen);
    }
}

//
public class NLUAfterProcessor {
    private static JSONObject nlu1;
    private static JSONObject nlu2;
    private static JSONObject rstnlu1;
    private static JSONObject rstnlu2;
    private static JSONObject state;
    private static HashMap<String,String> varMethodMapper=initVarMethodMapper();
    private static HashMap<String,String> initVarMethodMapper() {
        HashMap<String,String> rst=new HashMap<>();
        Method []methods=NLUAfterProcessor.class.getDeclaredMethods();
        for (Method method : methods) {
            String []strs=method.getName().split("_");
            if(strs[0].equals("get")&&strs.length>1) {
                rst.put(strs[strs.length-1].toLowerCase(),method.getName());
            }
        }
        return rst;
    }

    /**
     *
     * @param key 即name
     * @return 返回值皆为string，如果为null表示没有这种方法。各种get方法不会返回null，必定返回一个字符串。
     * 如果返回值应为boolean类型，则返回的string为"0"或"1"
     */
    public static String getElementByName(String key) {
        try {
            Method method=NLUAfterProcessor.class.getDeclaredMethod(varMethodMapper.get(key.toLowerCase()));
            return (String) method.invoke(NLUAfterProcessor.class);
        }
        catch (NullPointerException e) {

            return null;
        }
        catch (NoSuchMethodException e2) {
            ErrorHandle.OutputErrorInformation("Error in getElementByName");
            ErrorHandle.OutputErrorInformation(e2);
            return null;
        }
        catch (IllegalAccessException e3) {
            ErrorHandle.OutputErrorInformation("Error in getElementByName");
            ErrorHandle.OutputErrorInformation(e3);
            return null;
        }
        catch (InvocationTargetException e4) {
            ErrorHandle.OutputErrorInformation("Error in getElementByName");
            ErrorHandle.OutputErrorInformation(e4);
            return null;
        }
    }

    //get方法必须以get_ElementName命名。get方法返回值不能为null
    private static String get_Intention() {
        String rst="未识别";
        try {
            rst=nlu1.getString("intention");
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation("解析json错误，in get_Original_Intention"+e.getStackTrace());
        }
        return rst;
    }
    private static String get_Query() {//获取原始query
        String rst="";
        try {
            rst=nlu1.getString("query");
            StringBuffer strbuf=new StringBuffer();
            String []strs=rst.substring(1,rst.length()-1).split(",");
            for(String s:strs) {
                strbuf.append(s);
            }
            return  strbuf.toString();
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation("解析json错误,in get_Original_Query"+e.getStackTrace());
        }
        return rst;
    }
    private static String get_LastAction() {
        //todo

        return "";
    }
    private static String get_ifNullAttributes() {
        Iterator keys=nlu2.keys();
        while(keys.hasNext()) {
            try {
                if(nlu2.getString((String) keys.next()).equalsIgnoreCase("null")) {
                    return "1";
                }
            }
            catch (org.json.JSONException e) {
                continue;
            }
        }
        return "0";
    }
    private static String get_ifBrandAttributes() {
        Iterator keys=nlu2.keys();
        while(keys.hasNext()) {
            if(((String)keys.next()).equals("品牌")) {
                return "1";
            }
        }
        return "0";
    }


    //NLUAfterProcess主模块
    public static void process(String nlu1,String nlu2,String jsonState) {
        try {
            NLUAfterProcessor.nlu1=new JSONObject(nlu1);
            NLUAfterProcessor.nlu2=new JSONObject(nlu2);
            NLUAfterProcessor.state=new JSONObject(jsonState);
            NLUAfterProcessor.rstnlu1=new JSONObject(nlu1);
            NLUAfterProcessor.rstnlu2=new JSONObject(nlu2);
        }
        catch (Exception e) {//解析错误就跳过NLUAfterProcess
            ErrorHandle.OutputErrorInformation(e.getMessage()+e.getStackTrace());
            return;
        }
        IntentionRefineResult irr=IntentionRefine.analyse();//精确意图
        try {
            rstnlu1.put("intention",irr.getIntention());
        }
        catch (org.json.JSONException e) {
            ErrorHandle.OutputErrorInformation(e);
        }
    }
    //Test
    public static void main(String []args) {
        String nlu1="{\"query\":\"[小, 蜜蜂, 口红, 有, 什么, 规格, 的]\",\"category\":{\"c3\":\"唇膏\\/口红\",\"c1\":\"彩妆\",\"c2\":\"唇妆\"},\"intention\":\"商品推荐\"}\n" ;
        String nlu2="{\"品牌\":\"小蜜蜂\",\"规格\":\"NULL\"}";
        String token="137269756";
        try {
            DM dm=new DM();
            dm.newStateByToken(token);
            NLUAfterProcessor.process(nlu1,nlu2,dm.getStateByToken(token));
            ErrorHandle.OutputErrorInformation(NLUAfterProcessor.rstnlu1.toString());
        }
        catch (Exception e) {
            ErrorHandle.OutputErrorInformation(e);
        }

    }
}
