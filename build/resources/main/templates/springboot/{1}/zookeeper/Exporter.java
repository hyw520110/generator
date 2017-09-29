package ${zookeeperPackage};

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.yaml.snakeyaml.Yaml;


public class Exporter {
    public static void main(String[] args) throws IOException {
        String zkHost="192.168.40.113:2181";
        URL url = Exporter.class.getResource("/zookeeper.data");
        if(null==url){
            File file = new File(url.getPath());
            Map<String, String> map = export(file);
            importData(zkHost,map);
            file.delete();
            return ;
        }
//        Map<String, String> map=export("/application.yml.bak","/config/application,dev/");
//        importData(zkHost,map);
    }



    private static Map<String, String> export(File dataFile) throws IOException {
        List<String> lines = FileUtils.readLines(dataFile);
        Map<String,String> map=new HashMap<>();
        for (String line : lines) {
            map.put(StringUtils.substringBefore(line, " "), StringUtils.substringAfter(line, " "));
        }
        return map;
    }

    private static Map<String, String> export(String yml, String root) {
        Map<String, Object> map = (Map<String, Object>) new Yaml().load(Exporter.class.getResourceAsStream(yml));
        String[] paths = root.split("/");
        String s = "/";
        Map<String,String> result=new HashMap<>();
        for (int i = 1; i < paths.length; i++) {
            s += paths[i] + "/";
            append(result,s.substring(0, s.length() - 1), "''");
            
        }
        for (String key : map.keySet()) {
            each(result,root + key, map.get(key));
        }
        return result;
    }
    
    private static void each(Map<String,String> result, String key,Object object) {
        if(null==object){
            append(result,key, "''");
            return ;
        }
        if (!(object instanceof Map)) {
            String s=object.toString();
            if(object instanceof List){
                s=s.replace("[", "'").replace("]", "'");
            }
            append(result,key, s);
            return;
        }
        Map<String,Object> map = (Map)object;
        for(String k:map.keySet()){
            each(result,key+"."+k,map.get(k));
        }
    }
    private static void importData(String zkHost,Map<String, String> map) {
        CuratorFramework zk = start(zkHost);
        for (String s:map.keySet()) {
            try {
                String val = map.get(s);
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
    private static CuratorFramework start(String zkHost) {
        CuratorFramework zk = null;
        try {
            if(null==zk){
                zk = CuratorFrameworkFactory.newClient(zkHost, new RetryNTimes(3, 3000));
            }
            zk.start();   
        } catch (Exception e) {
            zk=null;
        }
        return zk;
    }
    
    private static void append(Map<String,String>  map, String s, String val) {
        System.out.println("create " + s + " "+val);
        map.put(s ,val);
    }

}
