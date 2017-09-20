package ${testPackage};

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.yaml.snakeyaml.Yaml;

import com.ycd360.backstage.app.test.Exporter;

public class Exporter {

    public static void main(String[] args) {
        Map<String,Object> map = (Map<String, Object>) new Yaml().load(Exporter.class.getResourceAsStream("/application.yml"));
        String root="/config/application,dev/";
        String[] paths=root.split("/");
        String s ="/";
        for (int i=1;i<paths.length;i++) {
            s+=paths[i]+"/";
            System.out.println("create "+s.substring(0,s.length()-1)+" ''");
        } 
        for (String key : map.keySet()) {
            each(root+key,map.get(key));
        }
    }


    private static void each(String key,Object object) {
        if(null==object){
            System.out.println("create "+key+" ''");
            return ;
        }
        if (!(object instanceof Map)) {
            String s=object.toString();
            if(object instanceof List){
                s=s.replace("[", "'").replace("]", "'");
            }
            System.out.println("create "+key+" "+s);
            return;
        }
        Map<String,Object> map = (Map)object;
        for(String k:map.keySet()){
            each(key+"."+k,map.get(k));
        }
    }

}
