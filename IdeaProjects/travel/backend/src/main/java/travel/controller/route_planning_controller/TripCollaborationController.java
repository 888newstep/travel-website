package travel.controller.route_planning_controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.service.route_planning.TripCollaborationService;
import travel.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 行程协作控制器
 * 支持多人协作规划行程、实时同步、权限管理等功能
 */
@RestController
@RequestMapping("/trip-collaboration")
@Slf4j
@RequiredArgsConstructor
public class TripCollaborationController {

    private final TripCollaborationService tripCollaborationService;

    /**
     * 创建协作行程
     * POST /api/trip-collaboration/create
     */
    @PostMapping("/create")
    public Result<Map<String, Object>> createCollaborativeTrip(@RequestBody Map<String, Object> request) {
        try {
            Integer routeId = (Integer) request.get("routeId");
            Integer creatorId = (Integer) request.get("creatorId");
            @SuppressWarnings("unchecked")
            List<Integer> collaborators = (List<Integer>) request.get("collaborators");
            log.info("创建协作行程请求: routeId={}, creatorId={}", routeId, creatorId);
            Map<String, Object> trip = tripCollaborationService.createCollaborativeTrip(routeId, creatorId, collaborators);
            return Result.success("创建协作行程成功", trip);
        } catch (Exception e) {
            log.error("创建协作行程失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 邀请协作者
     * POST /api/trip-collaboration/invite
     */
    @PostMapping("/invite")
    public Result<Map<String, Object>> inviteCollaborator(@RequestBody Map<String, Object> request) {
        try {
            Integer tripId = (Integer) request.get("tripId");
            Integer inviterId = (Integer) request.get("inviterId");
            Integer inviteeId = (Integer) request.get("inviteeId");
            String role = (String) request.get("role");
            log.info("邀请协作者请求: tripId={}, inviterId={}, inviteeId={}", tripId, inviterId, inviteeId);
            Map<String, Object> result = tripCollaborationService.inviteCollaborator(tripId, inviterId, inviteeId, role);
            return Result.success("邀请协作者成功", result);
        } catch (Exception e) {
            log.error("邀请协作者失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 接受邀请
     * POST /api/trip-collaboration/accept/{invitationId}
     */
    @PostMapping("/accept/{invitationId}")
    public Result<Boolean> acceptInvitation(@PathVariable Integer invitationId,
                                             @RequestParam Integer userId) {
        try {
            log.info("接受邀请请求: invitationId={}, userId={}", invitationId, userId);
            boolean result = tripCollaborationService.acceptInvitation(invitationId, userId);
            if (result) {
                return Result.success("接受邀请成功", true);
            }
            return Result.error("接受邀请失败");
        } catch (Exception e) {
            log.error("接受邀请失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 拒绝邀请
     * POST /api/trip-collaboration/reject/{invitationId}
     */
    @PostMapping("/reject/{invitationId}")
    public Result<Boolean> rejectInvitation(@PathVariable Integer invitationId,
                                             @RequestParam Integer userId) {
        try {
            log.info("拒绝邀请请求: invitationId={}, userId={}", invitationId, userId);
            boolean result = tripCollaborationService.rejectInvitation(invitationId, userId);
            if (result) {
                return Result.success("拒绝邀请成功", true);
            }
            return Result.error("拒绝邀请失败");
        } catch (Exception e) {
            log.error("拒绝邀请失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 移除协作者
     * DELETE /api/trip-collaboration/{tripId}/collaborator/{collaboratorId}
     */
    @DeleteMapping("/{tripId}/collaborator/{collaboratorId}")
    public Result<Boolean> removeCollaborator(@PathVariable Integer tripId,
                                               @RequestParam Integer ownerId,
                                               @PathVariable Integer collaboratorId) {
        try {
            log.info("移除协作者请求: tripId={}, ownerId={}, collaboratorId={}", tripId, ownerId, collaboratorId);
            boolean result = tripCollaborationService.removeCollaborator(tripId, ownerId, collaboratorId);
            if (result) {
                return Result.success("移除协作者成功", true);
            }
            return Result.error("移除协作者失败");
        } catch (Exception e) {
            log.error("移除协作者失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新协作者权限
     * PUT /api/trip-collaboration/{tripId}/collaborator/{collaboratorId}/role
     */
    @PutMapping("/{tripId}/collaborator/{collaboratorId}/role")
    public Result<Boolean> updateCollaboratorRole(@PathVariable Integer tripId,
                                                  @RequestParam Integer ownerId,
                                                  @PathVariable Integer collaboratorId,
                                                  @RequestParam String newRole) {
        try {
            log.info("更新协作者权限请求: tripId={}, collaboratorId={}, newRole={}", tripId, collaboratorId, newRole);
            boolean result = tripCollaborationService.updateCollaboratorRole(tripId, ownerId, collaboratorId, newRole);
            if (result) {
                return Result.success("更新协作者权限成功", true);
            }
            return Result.error("更新协作者权限失败");
        } catch (Exception e) {
            log.error("更新协作者权限失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取协作者列表
     * GET /api/trip-collaboration/{tripId}/collaborators
     */
    @GetMapping("/{tripId}/collaborators")
    public Result<List<Map<String, Object>>> getCollaborators(@PathVariable Integer tripId) {
        try {
            log.info("获取协作者列表请求: tripId={}", tripId);
            List<Map<String, Object>> collaborators = tripCollaborationService.getCollaborators(tripId);
            return Result.success("获取协作者列表成功", collaborators);
        } catch (Exception e) {
            log.error("获取协作者列表失败: tripId={}, error={}", tripId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 同步行程修改
     * POST /api/trip-collaboration/{tripId}/sync
     */
    @PostMapping("/{tripId}/sync")
    public Result<Map<String, Object>> syncChanges(@PathVariable Integer tripId,
                                                    @RequestParam Integer userId,
                                                    @RequestBody Map<String, Object> changes) {
        try {
            log.info("同步行程修改请求: tripId={}, userId={}", tripId, userId);
            Map<String, Object> result = tripCollaborationService.syncChanges(tripId, userId, changes);
            return Result.success("同步行程修改成功", result);
        } catch (Exception e) {
            log.error("同步行程修改失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取修改历史
     * GET /api/trip-collaboration/{tripId}/history
     */
    @GetMapping("/{tripId}/history")
    public Result<List<Map<String, Object>>> getChangeHistory(@PathVariable Integer tripId) {
        try {
            log.info("获取修改历史请求: tripId={}", tripId);
            List<Map<String, Object>> history = tripCollaborationService.getChangeHistory(tripId);
            return Result.success("获取修改历史成功", history);
        } catch (Exception e) {
            log.error("获取修改历史失败: tripId={}, error={}", tripId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 添加评论
     * POST /api/trip-collaboration/{tripId}/comment
     */
    @PostMapping("/{tripId}/comment")
    public Result<Map<String, Object>> addComment(@PathVariable Integer tripId,
                                                   @RequestParam Integer userId,
                                                   @RequestBody Map<String, Object> request) {
        try {
            String content = (String) request.get("content");
            String targetType = (String) request.get("targetType");
            Integer targetId = (Integer) request.get("targetId");
            log.info("添加评论请求: tripId={}, userId={}", tripId, userId);
            Map<String, Object> result = tripCollaborationService.addComment(tripId, userId, content, targetType, targetId);
            return Result.success("添加评论成功", result);
        } catch (Exception e) {
            log.error("添加评论失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取评论列表
     * GET /api/trip-collaboration/{tripId}/comments
     */
    @GetMapping("/{tripId}/comments")
    public Result<List<Map<String, Object>>> getComments(@PathVariable Integer tripId,
                                                         @RequestParam String targetType,
                                                         @RequestParam Integer targetId) {
        try {
            log.info("获取评论列表请求: tripId={}, targetType={}, targetId={}", tripId, targetType, targetId);
            List<Map<String, Object>> comments = tripCollaborationService.getComments(tripId, targetType, targetId);
            return Result.success("获取评论列表成功", comments);
        } catch (Exception e) {
            log.error("获取评论列表失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 标记任务完成
     * POST /api/trip-collaboration/{tripId}/task/{taskId}/complete
     */
    @PostMapping("/{tripId}/task/{taskId}/complete")
    public Result<Boolean> completeTask(@PathVariable Integer tripId,
                                      @RequestParam Integer userId,
                                      @PathVariable Integer taskId) {
        try {
            log.info("标记任务完成请求: tripId={}, userId={}, taskId={}", tripId, userId, taskId);
            boolean result = tripCollaborationService.completeTask(tripId, userId, taskId);
            if (result) {
                return Result.success("标记任务完成成功", true);
            }
            return Result.error("标记任务完成失败");
        } catch (Exception e) {
            log.error("标记任务完成失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 分配任务
     * POST /api/trip-collaboration/{tripId}/task/assign
     */
    @PostMapping("/{tripId}/task/assign")
    public Result<Map<String, Object>> assignTask(@PathVariable Integer tripId,
                                                   @RequestParam Integer assignerId,
                                                   @RequestBody Map<String, Object> request) {
        try {
            Integer assigneeId = (Integer) request.get("assigneeId");
            String taskDescription = (String) request.get("taskDescription");
            log.info("分配任务请求: tripId={}, assignerId={}, assigneeId={}", tripId, assignerId, assigneeId);
            Map<String, Object> result = tripCollaborationService.assignTask(tripId, assignerId, assigneeId, taskDescription);
            return Result.success("分配任务成功", result);
        } catch (Exception e) {
            log.error("分配任务失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取任务列表
     * GET /api/trip-collaboration/{tripId}/tasks
     */
    @GetMapping("/{tripId}/tasks")
    public Result<List<Map<String, Object>>> getTasks(@PathVariable Integer tripId,
                                                       @RequestParam Integer userId) {
        try {
            log.info("获取任务列表请求: tripId={}, userId={}", tripId, userId);
            List<Map<String, Object>> tasks = tripCollaborationService.getTasks(tripId, userId);
            return Result.success("获取任务列表成功", tasks);
        } catch (Exception e) {
            log.error("获取任务列表失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 实时协作编辑
     * POST /api/trip-collaboration/{tripId}/edit
     */
    @PostMapping("/{tripId}/edit")
    public Result<Map<String, Object>> realTimeEdit(@PathVariable Integer tripId,
                                                    @RequestParam Integer userId,
                                                    @RequestBody Map<String, Object> editData) {
        try {
            log.info("实时协作编辑请求: tripId={}, userId={}", tripId, userId);
            Map<String, Object> result = tripCollaborationService.realTimeEdit(tripId, userId, editData);
            return Result.success("实时协作编辑成功", result);
        } catch (Exception e) {
            log.error("实时协作编辑失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取在线协作者
     * GET /api/trip-collaboration/{tripId}/online
     */
    @GetMapping("/{tripId}/online")
    public Result<List<Map<String, Object>>> getOnlineCollaborators(@PathVariable Integer tripId) {
        try {
            log.info("获取在线协作者请求: tripId={}", tripId);
            List<Map<String, Object>> collaborators = tripCollaborationService.getOnlineCollaborators(tripId);
            return Result.success("获取在线协作者成功", collaborators);
        } catch (Exception e) {
            log.error("获取在线协作者失败: tripId={}, error={}", tripId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 锁定编辑区域
     * POST /api/trip-collaboration/{tripId}/lock
     */
    @PostMapping("/{tripId}/lock")
    public Result<Boolean> lockSection(@PathVariable Integer tripId,
                                      @RequestParam Integer userId,
                                      @RequestBody Map<String, Object> request) {
        try {
            String section = (String) request.get("section");
            log.info("锁定编辑区域请求: tripId={}, userId={}, section={}", tripId, userId, section);
            boolean result = tripCollaborationService.lockSection(tripId, userId, section);
            if (result) {
                return Result.success("锁定编辑区域成功", true);
            }
            return Result.error("锁定编辑区域失败");
        } catch (Exception e) {
            log.error("锁定编辑区域失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 解锁编辑区域
     * POST /api/trip-collaboration/{tripId}/unlock
     */
    @PostMapping("/{tripId}/unlock")
    public Result<Boolean> unlockSection(@PathVariable Integer tripId,
                                        @RequestParam Integer userId,
                                        @RequestBody Map<String, Object> request) {
        try {
            String section = (String) request.get("section");
            log.info("解锁编辑区域请求: tripId={}, userId={}, section={}", tripId, userId, section);
            boolean result = tripCollaborationService.unlockSection(tripId, userId, section);
            if (result) {
                return Result.success("解锁编辑区域成功", true);
            }
            return Result.error("解锁编辑区域失败");
        } catch (Exception e) {
            log.error("解锁编辑区域失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 合并修改
     * POST /api/trip-collaboration/{tripId}/merge
     */
    @PostMapping("/{tripId}/merge")
    public Result<Map<String, Object>> mergeChanges(@PathVariable Integer tripId,
                                                    @RequestParam Integer userId,
                                                    @RequestBody Map<String, Object> request) {
        try {
            Integer baseVersion = (Integer) request.get("baseVersion");
            @SuppressWarnings("unchecked")
            Map<String, Object> changes = (Map<String, Object>) request.get("changes");
            log.info("合并修改请求: tripId={}, userId={}, baseVersion={}", tripId, userId, baseVersion);
            Map<String, Object> result = tripCollaborationService.mergeChanges(tripId, userId, baseVersion, changes);
            return Result.success("合并修改成功", result);
        } catch (Exception e) {
            log.error("合并修改失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取版本历史
     * GET /api/trip-collaboration/{tripId}/versions
     */
    @GetMapping("/{tripId}/versions")
    public Result<List<Map<String, Object>>> getVersionHistory(@PathVariable Integer tripId) {
        try {
            log.info("获取版本历史请求: tripId={}", tripId);
            List<Map<String, Object>> versions = tripCollaborationService.getVersionHistory(tripId);
            return Result.success("获取版本历史成功", versions);
        } catch (Exception e) {
            log.error("获取版本历史失败: tripId={}, error={}", tripId, e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 回滚到指定版本
     * POST /api/trip-collaboration/{tripId}/rollback/{versionId}
     */
    @PostMapping("/{tripId}/rollback/{versionId}")
    public Result<Boolean> rollbackToVersion(@PathVariable Integer tripId,
                                            @RequestParam Integer userId,
                                            @PathVariable Integer versionId) {
        try {
            log.info("回滚到指定版本请求: tripId={}, userId={}, versionId={}", tripId, userId, versionId);
            boolean result = tripCollaborationService.rollbackToVersion(tripId, userId, versionId);
            if (result) {
                return Result.success("回滚到指定版本成功", true);
            }
            return Result.error("回滚到指定版本失败");
        } catch (Exception e) {
            log.error("回滚到指定版本失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导出协作行程
     * POST /api/trip-collaboration/{tripId}/export
     */
    @PostMapping("/{tripId}/export")
    public Result<Map<String, Object>> exportCollaborativeTrip(@PathVariable Integer tripId,
                                                              @RequestParam String format) {
        try {
            log.info("导出协作行程请求: tripId={}, format={}", tripId, format);
            Map<String, Object> result = tripCollaborationService.exportCollaborativeTrip(tripId, format);
            return Result.success("导出协作行程成功", result);
        } catch (Exception e) {
            log.error("导出协作行程失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 结束协作
     * POST /api/trip-collaboration/{tripId}/end
     */
    @PostMapping("/{tripId}/end")
    public Result<Boolean> endCollaboration(@PathVariable Integer tripId,
                                           @RequestParam Integer ownerId) {
        try {
            log.info("结束协作请求: tripId={}, ownerId={}", tripId, ownerId);
            boolean result = tripCollaborationService.endCollaboration(tripId, ownerId);
            if (result) {
                return Result.success("结束协作成功", true);
            }
            return Result.error("结束协作失败");
        } catch (Exception e) {
            log.error("结束协作失败: error={}", e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}
