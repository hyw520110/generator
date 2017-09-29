package ${redisPackage};

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StopWatch;

import ${rootPackage}.${projectName}.${moduleName}.Booter;

@RunWith(SpringRunner.class)
//@ContextConfiguration or @SpringBootTest(classes=Booter.class)
@SpringBootTest(classes=Booter.class)
public class RedisTemplateTest {

    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    private int loop=10;
    /**
     * 测试redis字符串数据结构 
     * @author:  heyiwu 
     * @throws Exception
     */
    @Test
    public void testString() throws Exception {
        String key="String";
        ValueOperations<String, String> operation = redisTemplate.opsForValue();
        StopWatch watch=new StopWatch(key);
        for (int i = 0; i < loop; i++) {
            String value = String.valueOf(i);
            watch.start("set");
            operation.set(key, value);
            watch.stop();
            watch.start("get");
            Assert.assertEquals(value, operation.get(key));
            watch.stop();
            watch.start("get2");
            Assert.assertEquals(value,stringRedisTemplate.opsForValue().get(key));
            watch.stop();
            watch.start("del");
            stringRedisTemplate.delete(key);
            watch.stop();
        }
        System.out.println(watch.prettyPrint());
    }
    
   /**
    * 测试redis的hash数据结构
    * @author:  heyiwu 
    * @throws Exception
    */
    @Test
    public void testHash() throws Exception {
        HashOperations<String, Object, Object> operation = redisTemplate.opsForHash();
        String key="Hash";
        StopWatch watch=new StopWatch(key);
        for (int i = 0; i < loop; i++) {
            String s = String.valueOf(i);
            watch.start("put");
            operation.put(key,s, s);
            watch.stop();
            watch.start("get");
            Assert.assertEquals(s, operation.get(key,s));
            watch.stop();
        }
        System.out.println(watch.shortSummary());
    }
    
    /**
     * 测试redis的list数据结构
     * @author:  heyiwu 
     * @throws Exception
     */
    @Test
    public void testList() throws Exception {
        ListOperations<String, String> operater = redisTemplate.opsForList();
        String value="sfd";
        operater.leftPush("List", value);
        operater.rightPush("list2", value + "2");
        Assert.assertEquals(value, operater.leftPop("List"));
        Assert.assertEquals(value + "2", operater.leftPop("list2"));
    }
    
    /**
     * 测试redis的set数据结构
     * @author:  heyiwu 
     * @throws Exception
     */
    @Test
    public void testSet() throws Exception {
        SetOperations<String, String> operater = redisTemplate.opsForSet();
        operater.add("Set", "2", "1");

        String value = "2";
        Assert.assertEquals(value, operater.pop("Set"));
    }
    
    /**
     * 测试redis的有序set结构
     * @author:  heyiwu 
     * @throws Exception
     */
    @Test
    public void testZSet() throws Exception {
        ZSetOperations<String, String> operater = redisTemplate.opsForZSet();
        for (int i = 10; i > 0; i--) {
            operater.add("zSet",  String.valueOf(i), i);
        }
        System.out.println(operater.range("zSet", 0, -1));
    }
}
