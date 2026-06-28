package ${packagePath};

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.Deployment;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

/**
 * 自动生成的工作流入口控制器
 */
@RestController
@RequestMapping("/workflow")
public class FlowableController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    // 1. 部署流程
    @PostMapping("/deploy")
    public String deploy(@RequestParam("file") MultipartFile file, @RequestParam(value = "tenantId", defaultValue = "") String tenantId) {
        try {
            InputStream in = file.getInputStream();
            Deployment deployment = repositoryService.createDeployment()
                    .addInputStream(file.getOriginalFilename(), in)
                    .name(file.getOriginalFilename())
                    .tenantId(tenantId)
                    .deploy();
            return "部署成功，部署ID：" + deployment.getId();
        } catch (Exception e) {
            return "部署失败：" + e.getMessage();
        }
    }

    // 2. 发起流程
    @PostMapping("/start")
    public String startProcess(@RequestParam("processDefinitionKey") String processDefinitionKey,
                               @RequestParam("businessKey") String businessKey) {
        runtimeService.startProcessInstanceByKey(processDefinitionKey, businessKey);
        return "流程启动成功";
    }

    // 3. 查询待办任务
    @GetMapping("/tasks")
    public List<Map<String, Object>> getTasks(@RequestParam("assignee") String assignee) {
        List<Task> tasks = taskService.createTaskQuery().taskAssignee(assignee).list();
        return tasks.stream().map(task -> {
            Map<String, Object> map = new HashMap<>();
            map.put("taskId", task.getId());
            map.put("taskName", task.getName());
            map.put("processInstanceId", task.getProcessInstanceId());
            return map;
        }).collect(Collectors.toList());
    }

    // 4. 完成任务
    @PostMapping("/complete")
    public String completeTask(@RequestParam("taskId") String taskId) {
        taskService.complete(taskId);
        return "任务审批完成";
    }
}
