package ${packagePath};
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class IdempotentAspect {
    // 实际项目中应注入 RedisTemplate 或 RedissonClient
    @Around("@annotation(idem)")
    public Object around(ProceedingJoinPoint point, Idempotent idem) throws Throwable {
        // 获取锁逻辑：若获取失败则抛出 "重复提交" 异常
        // String key = idem.prefix() + hashCode;
        // boolean lock = redisTemplate.opsForValue().setIfAbsent(key, "1", idem.expire(), TimeUnit.MILLISECONDS);
        // if (!lock) throw new RuntimeException("请勿重复提交");
        try {
            return point.proceed();
        } finally {
            // 业务执行完毕可选择是否释放锁
        }
    }
}
