package ${commonsPackage};

import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.ControllerAdvice;  
import com.alibaba.fastjson.support.spring.FastJsonpResponseBodyAdvice;   
  
 
@ControllerAdvice(basePackages = "${controllerPackage}")  
public class FastJsonpAdvice extends FastJsonpResponseBodyAdvice{  
  
    public FastJsonpAdvice() {  
        super("callback","jsonp");  
    }
    //jsonp乱码
	@Override
	protected MediaType getContentType(MediaType contentType, ServerHttpRequest request, ServerHttpResponse response) {
		return new MediaType("application", "javascript",contentType.getCharset());
	}  
}  