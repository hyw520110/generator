package ${packagePath};

import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

@Component
public class SampleXxlJob {
    @XxlJob("sampleJobHandler")
    public void sampleJobHandler() throws Exception {
        System.out.println("XXL-JOB, Hello World.");
    }
}
