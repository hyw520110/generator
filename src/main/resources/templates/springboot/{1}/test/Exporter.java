package ${testPackage};

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.yaml.snakeyaml.Yaml;


public class Exporter {
    private static CuratorFramework zk ;
    
    public static void main(String[] args) {
        start();
        export("/application.yml.bak");
    }

    private static void export(String yml) {
        Map<String, Object> map = (Map<String, Object>) new Yaml().load(Exporter.class.getResourceAsStream(yml));
        String root = "/config/application,dev/";
        String[] paths = root.split("/");
        String s = "/";
        for (int i = 1; i < paths.length; i++) {
            s += paths[i] + "/";
            printOrImport(s.substring(0, s.length() - 1), "''");
            
        }
        for (String key : map.keySet()) {
            each(root + key, map.get(key));
        }
    }
    
    private static void each(String key,Object object) {
        if(null==object){
            printOrImport(key, "''");
            return ;
        }
        if (!(object instanceof Map)) {
            String s=object.toString();
            if(object instanceof List){
                s=s.replace("[", "'").replace("]", "'");
            }
            printOrImport(key, s);
            return;
        }
        Map<String,Object> map = (Map)object;
        for(String k:map.keySet()){
            each(key+"."+k,map.get(k));
        }
    }
    
    private static void start() {
        try {
            zk = CuratorFrameworkFactory.newClient("192.168.40.113:2181", new RetryNTimes(3, 3000));
            zk.start();   
        } catch (Exception e) {
            zk=null;
        }
    }
    
    private static void printOrImport(String s, String val) {
        try {
            if(null==zk){
                System.out.println("create " + s + " "+val);
                return ;
            }
            if(null==zk.checkExists().forPath(s)){
                System.out.println("create " + s + " "+val);
                zk.create().creatingParentsIfNeeded().forPath(s, val.getBytes("UTF-8"));
                return ;
            }
            System.out.println("set " + s + " "+val);
            zk.setData().forPath(s, val.getBytes("UTF-8"));
        } catch (Exception e) {
        }
    }

}
