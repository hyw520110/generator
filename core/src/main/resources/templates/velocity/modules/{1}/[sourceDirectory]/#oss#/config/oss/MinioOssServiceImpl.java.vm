package ${packagePath};
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
@Service
public class MinioOssServiceImpl implements OssService {
    @Override
    public String uploadFile(MultipartFile file) {
        // MinIO 或 阿里云 OSS 上传逻辑
        return "http://oss.example.com/" + file.getOriginalFilename();
    }
}
