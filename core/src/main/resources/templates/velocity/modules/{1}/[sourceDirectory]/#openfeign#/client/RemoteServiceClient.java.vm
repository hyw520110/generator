package ${packagePath};

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(name = "remote-service", fallback = RemoteServiceFallback.class)
public interface RemoteServiceClient {
    @GetMapping("/api/remote/data")
    String getRemoteData();
}
