package ${zookeeperPackage};

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.yaml.snakeyaml.Yaml;


public class ZkTool {
    public static void main(String[] args) throws IOException {
        String zkHost="localhost:2181";
        //zookeeper数据脚本
        URL url = ZkTool.class.getResource("/zookeeper.data");
        List<String> lines = FileUtils.readLines(new File(url.getPath()));
        Map<String,String> map=new HashMap<>();
        for (String line : lines) {
			if(StringUtils.isBlank(line)) {
				continue;
			}
			line=StringUtils.replace(line, "create ", "", 1);
			String[] items = line.split(" ");
			map.put(items[0], items[1]);
		}
        importData(zkHost, map);
//        if(null==url){
//            //根据yml配置文件生成zookeeper初始数据，并导入到zookeeper
//          map=export("/application.yml","/config/application,dev/");
//          importData(zkHost,map);
//          return;
//        }
//        //根据zk数据脚本导入zookeeper初始数据
//        File file = new File(url.getPath());
//        map = export(file);
//        importData(zkHost,map);
//        file.delete();
        
    }



    private static Map<String, String> export(File dataFile) throws IOException {
        List<String> lines = FileUtils.readLines(dataFile);
        Map<String,String> map=new HashMap<>();
        for (String line : lines) {
            if(StringUtils.isBlank(line)){
                continue;
            }
            if(StringUtils.startsWith(line, "create ")){
                line=line.replace("create ", "");
            }
            map.put(StringUtils.substringBefore(line, " "), StringUtils.substringAfter(line, " "));
        }
        return map;
    }

    private static Map<String, String> export(String yml, String root) {
        Map<String, Object> map = (Map<String, Object>) new Yaml().load(ZkTool.class.getResourceAsStream(yml));
        String[] paths = root.split("/");
        String s = "/";
        Map<String,String> result=new HashMap<>();
        for (int i = 1; i < paths.length; i++) {
            s += paths[i] + "/";
            put(result,s.substring(0, s.length() - 1), "''");
            
        }
        for (String key : map.keySet()) {
            each(result,root + key, map.get(key));
        }
        return result;
    }
    
    private static void each(Map<String,String> result, String key,Object object) {
        if(null==object){
            put(result,key, "''");
            return ;
        }
        if (!(object instanceof Map)) {
            String s=object.toString();
            if(object instanceof List){
                s=s.replace("[", "'").replace("]", "'");
            }
            put(result,key, s);
            return;
        }
        Map<String,Object> map = (Map)object;
        for(String k:map.keySet()){
            each(result,key+"."+k,map.get(k));
        }
    }
    private static void importData(String zkHost,Map<String, String> map) {
        CuratorFramework zk = start(zkHost);
        if(null==zk){
        	System.out.println("zkHost:" + zkHost);
            return ;
        }
        Map<String,String> result = map.entrySet().stream()
        	    .sorted(Map.Entry.comparingByKey())
        	    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
        	    (oldValue, newValue) -> oldValue, LinkedHashMap::new));
        for (String s:result.keySet()) {
            try {
                String val = map.get(s);
                if(null==zk.checkExists().forPath(s)){
                    System.out.println("create " + s + " "+val);
                    zk.create().creatingParentsIfNeeded().forPath(s, val.getBytes("UTF-8"));
                    continue ;
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
    
    private static void put(Map<String,String>  map, String s, String val) {
        System.out.println("create " + s + " "+val);
        map.put(s ,val);
    }

}
