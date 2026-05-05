package travel.service.route_planning;

import java.util.List;
import java.util.Map;

/**
 * 行程协作服务
 * 支持多人协作规划行程、实时同步、权限管理等功能
 */
public interface TripCollaborationService {

    /**
     * 创建协作行程
     * @param routeId 路线ID
     * @param creatorId 创建者ID
     * @param collaborators 协作者列表
     * @return 协作行程信息
     */
    Map<String, Object> createCollaborativeTrip(Integer routeId, Integer creatorId, List<Integer> collaborators);

    /**
     * 邀请协作者
     * @param tripId 协作行程ID
     * @param inviterId 邀请者ID
     * @param inviteeId 被邀请者ID
     * @param role 角色 (editor/viewer)
     * @return 邀请结果
     */
    Map<String, Object> inviteCollaborator(Integer tripId, Integer inviterId, Integer inviteeId, String role);

    /**
     * 接受邀请
     * @param invitationId 邀请ID
     * @param userId 用户ID
     * @return 接受结果
     */
    boolean acceptInvitation(Integer invitationId, Integer userId);

    /**
     * 拒绝邀请
     * @param invitationId 邀请ID
     * @param userId 用户ID
     * @return 拒绝结果
     */
    boolean rejectInvitation(Integer invitationId, Integer userId);

    /**
     * 移除协作者
     * @param tripId 协作行程ID
     * @param ownerId 拥有者ID
     * @param collaboratorId 协作者ID
     * @return 移除结果
     */
    boolean removeCollaborator(Integer tripId, Integer ownerId, Integer collaboratorId);

    /**
     * 更新协作者权限
     * @param tripId 协作行程ID
     * @param ownerId 拥有者ID
     * @param collaboratorId 协作者ID
     * @param newRole 新角色
     * @return 更新结果
     */
    boolean updateCollaboratorRole(Integer tripId, Integer ownerId, Integer collaboratorId, String newRole);

    /**
     * 获取协作者列表
     * @param tripId 协作行程ID
     * @return 协作者列表
     */
    List<Map<String, Object>> getCollaborators(Integer tripId);

    /**
     * 同步行程修改
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param changes 修改内容
     * @return 同步结果
     */
    Map<String, Object> syncChanges(Integer tripId, Integer userId, Map<String, Object> changes);

    /**
     * 获取修改历史
     * @param tripId 协作行程ID
     * @return 修改历史列表
     */
    List<Map<String, Object>> getChangeHistory(Integer tripId);

    /**
     * 添加评论
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param content 评论内容
     * @param targetType 评论目标类型 (route/day/attraction)
     * @param targetId 评论目标ID
     * @return 评论结果
     */
    Map<String, Object> addComment(Integer tripId, Integer userId, String content, String targetType, Integer targetId);

    /**
     * 获取评论列表
     * @param tripId 协作行程ID
     * @param targetType 评论目标类型
     * @param targetId 评论目标ID
     * @return 评论列表
     */
    List<Map<String, Object>> getComments(Integer tripId, String targetType, Integer targetId);

    /**
     * 标记任务完成
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param taskId 任务ID
     * @return 标记结果
     */
    boolean completeTask(Integer tripId, Integer userId, Integer taskId);

    /**
     * 分配任务
     * @param tripId 协作行程ID
     * @param assignerId 分配者ID
     * @param assigneeId 被分配者ID
     * @param taskDescription 任务描述
     * @return 分配结果
     */
    Map<String, Object> assignTask(Integer tripId, Integer assignerId, Integer assigneeId, String taskDescription);

    /**
     * 获取任务列表
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @return 任务列表
     */
    List<Map<String, Object>> getTasks(Integer tripId, Integer userId);

    /**
     * 实时协作编辑
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param editData 编辑数据
     * @return 编辑结果
     */
    Map<String, Object> realTimeEdit(Integer tripId, Integer userId, Map<String, Object> editData);

    /**
     * 获取在线协作者
     * @param tripId 协作行程ID
     * @return 在线协作者列表
     */
    List<Map<String, Object>> getOnlineCollaborators(Integer tripId);

    /**
     * 锁定编辑区域
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param section 编辑区域
     * @return 锁定结果
     */
    boolean lockSection(Integer tripId, Integer userId, String section);

    /**
     * 解锁编辑区域
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param section 编辑区域
     * @return 解锁结果
     */
    boolean unlockSection(Integer tripId, Integer userId, String section);

    /**
     * 合并修改
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param baseVersion 基础版本
     * @param changes 修改内容
     * @return 合并结果
     */
    Map<String, Object> mergeChanges(Integer tripId, Integer userId, Integer baseVersion, Map<String, Object> changes);

    /**
     * 获取版本历史
     * @param tripId 协作行程ID
     * @return 版本历史列表
     */
    List<Map<String, Object>> getVersionHistory(Integer tripId);

    /**
     * 回滚到指定版本
     * @param tripId 协作行程ID
     * @param userId 用户ID
     * @param versionId 版本ID
     * @return 回滚结果
     */
    boolean rollbackToVersion(Integer tripId, Integer userId, Integer versionId);

    /**
     * 导出协作行程
     * @param tripId 协作行程ID
     * @param format 导出格式
     * @return 导出结果
     */
    Map<String, Object> exportCollaborativeTrip(Integer tripId, String format);

    /**
     * 结束协作
     * @param tripId 协作行程ID
     * @param ownerId 拥有者ID
     * @return 结束结果
     */
    boolean endCollaboration(Integer tripId, Integer ownerId);
}
