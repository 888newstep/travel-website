package travel.service.impl.route_planning;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import travel.entity.route_planning.Route;
import travel.service.route_planning.RouteService;
import travel.service.route_planning.TripCollaborationService;
import travel.utils.CacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TripCollaborationServiceImpl implements TripCollaborationService {

    @Autowired
    private RouteService routeService;

    @Autowired
    private CacheUtil cacheUtil;

    private static final String COLLAB_PREFIX = "collab:";
    private static final String VERSION_PREFIX = "version:";

    @Override
    public Map<String, Object> createCollaborativeTrip(Integer routeId, Integer creatorId, List<Integer> collaborators) {
        Map<String, Object> result = new HashMap<>();

        Route route = routeService.getById(routeId);
        if (route == null) {
            result.put("success", false);
            result.put("message", "路线不存在");
            return result;
        }

        Integer tripId = generateTripId();

        Map<String, Object> trip = new HashMap<>();
        trip.put("id", tripId);
        trip.put("routeId", routeId);
        trip.put("creatorId", creatorId);
        trip.put("collaborators", new ArrayList<>(collaborators));
        trip.put("createTime", LocalDateTime.now());
        trip.put("status", "active");
        trip.put("version", 1);

        // 保存协作行程
        String cacheKey = COLLAB_PREFIX + "trip:" + tripId;
        cacheUtil.set(cacheKey, trip, 30, TimeUnit.DAYS);

        // 初始化版本历史
        saveVersion(tripId, 1, creatorId, "创建协作行程", trip);

        result.put("success", true);
        result.put("tripId", tripId);
        result.put("message", "协作行程创建成功");

        return result;
    }

    @Override
    public Map<String, Object> inviteCollaborator(Integer tripId, Integer inviterId, Integer inviteeId, String role) {
        Map<String, Object> result = new HashMap<>();

        // 检查权限
        if (!hasPermission(tripId, inviterId, "owner")) {
            result.put("success", false);
            result.put("message", "无权限邀请协作者");
            return result;
        }

        Integer invitationId = generateInvitationId();

        Map<String, Object> invitation = new HashMap<>();
        invitation.put("id", invitationId);
        invitation.put("tripId", tripId);
        invitation.put("inviterId", inviterId);
        invitation.put("inviteeId", inviteeId);
        invitation.put("role", role);
        invitation.put("status", "pending");
        invitation.put("createTime", LocalDateTime.now());

        String cacheKey = COLLAB_PREFIX + "invitation:" + invitationId;
        cacheUtil.set(cacheKey, invitation, 7, TimeUnit.DAYS);

        result.put("success", true);
        result.put("invitationId", invitationId);
        result.put("message", "邀请已发送");

        return result;
    }

    @Override
    public boolean acceptInvitation(Integer invitationId, Integer userId) {
        String cacheKey = COLLAB_PREFIX + "invitation:" + invitationId;

        @SuppressWarnings("unchecked")
        Map<String, Object> invitation = cacheUtil.get(cacheKey, Map.class);
        if (invitation == null) {
            return false;
        }

        Integer inviteeId = (Integer) invitation.get("inviteeId");
        if (!userId.equals(inviteeId)) {
            return false;
        }

        invitation.put("status", "accepted");
        invitation.put("acceptTime", LocalDateTime.now());
        cacheUtil.set(cacheKey, invitation, 30, TimeUnit.DAYS);

        // 添加到协作者列表
        Integer tripId = (Integer) invitation.get("tripId");
        String role = (String) invitation.get("role");
        addCollaboratorToTrip(tripId, userId, role);

        return true;
    }

    @Override
    public boolean rejectInvitation(Integer invitationId, Integer userId) {
        String cacheKey = COLLAB_PREFIX + "invitation:" + invitationId;

        @SuppressWarnings("unchecked")
        Map<String, Object> invitation = cacheUtil.get(cacheKey, Map.class);
        if (invitation == null) {
            return false;
        }

        invitation.put("status", "rejected");
        invitation.put("rejectTime", LocalDateTime.now());
        cacheUtil.set(cacheKey, invitation, 7, TimeUnit.DAYS);

        return true;
    }

    @Override
    public boolean removeCollaborator(Integer tripId, Integer ownerId, Integer collaboratorId) {
        if (!hasPermission(tripId, ownerId, "owner")) {
            return false;
        }

        String cacheKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(cacheKey, Map.class);
        if (trip == null) {
            return false;
        }

        @SuppressWarnings("unchecked")
        List<Integer> collaborators = (List<Integer>) trip.get("collaborators");
        collaborators.remove(collaboratorId);
        trip.put("collaborators", collaborators);

        cacheUtil.set(cacheKey, trip, 30, TimeUnit.DAYS);

        return true;
    }

    @Override
    public boolean updateCollaboratorRole(Integer tripId, Integer ownerId, Integer collaboratorId, String newRole) {
        if (!hasPermission(tripId, ownerId, "owner")) {
            return false;
        }

        String roleKey = COLLAB_PREFIX + "role:" + tripId + ":" + collaboratorId;
        cacheUtil.set(roleKey, newRole, 30, TimeUnit.DAYS);

        return true;
    }

    @Override
    public List<Map<String, Object>> getCollaborators(Integer tripId) {
        List<Map<String, Object>> collaborators = new ArrayList<>();

        String cacheKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(cacheKey, Map.class);
        if (trip == null) {
            return collaborators;
        }

        Integer creatorId = (Integer) trip.get("creatorId");

        // 添加创建者
        Map<String, Object> creator = new HashMap<>();
        creator.put("userId", creatorId);
        creator.put("role", "owner");
        creator.put("joinTime", trip.get("createTime"));
        collaborators.add(creator);

        // 添加其他协作者
        @SuppressWarnings("unchecked")
        List<Integer> collaboratorIds = (List<Integer>) trip.get("collaborators");
        for (Integer userId : collaboratorIds) {
            Map<String, Object> collaborator = new HashMap<>();
            collaborator.put("userId", userId);

            String roleKey = COLLAB_PREFIX + "role:" + tripId + ":" + userId;
            String role = cacheUtil.get(roleKey, String.class);
            collaborator.put("role", role != null ? role : "editor");
            collaborator.put("joinTime", LocalDateTime.now());

            collaborators.add(collaborator);
        }

        return collaborators;
    }

    @Override
    public Map<String, Object> syncChanges(Integer tripId, Integer userId, Map<String, Object> changes) {
        Map<String, Object> result = new HashMap<>();

        if (!hasEditPermission(tripId, userId)) {
            result.put("success", false);
            result.put("message", "无编辑权限");
            return result;
        }

        // 保存修改
        String changeKey = COLLAB_PREFIX + "changes:" + tripId + ":" + System.currentTimeMillis();
        changes.put("userId", userId);
        changes.put("timestamp", LocalDateTime.now());
        cacheUtil.set(changeKey, changes, 7, TimeUnit.DAYS);

        // 更新行程版本
        String tripKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(tripKey, Map.class);
        if (trip != null) {
            Integer currentVersion = (Integer) trip.get("version");
            trip.put("version", currentVersion + 1);
            trip.put("lastModified", LocalDateTime.now());
            cacheUtil.set(tripKey, trip, 30, TimeUnit.DAYS);

            // 保存新版本
            saveVersion(tripId, currentVersion + 1, userId, "同步修改", changes);
        }

        result.put("success", true);
        result.put("message", "修改已同步");

        return result;
    }

    @Override
    public List<Map<String, Object>> getChangeHistory(Integer tripId) {
        List<Map<String, Object>> history = new ArrayList<>();

        // 模拟获取修改历史
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> change = new HashMap<>();
            change.put("id", i);
            change.put("userId", i * 10);
            change.put("action", i % 2 == 0 ? "修改景点" : "调整时间");
            change.put("timestamp", LocalDateTime.now().minusHours(i));
            history.add(change);
        }

        return history;
    }

    @Override
    public Map<String, Object> addComment(Integer tripId, Integer userId, String content, String targetType, Integer targetId) {
        Map<String, Object> result = new HashMap<>();

        Integer commentId = generateCommentId();

        Map<String, Object> comment = new HashMap<>();
        comment.put("id", commentId);
        comment.put("tripId", tripId);
        comment.put("userId", userId);
        comment.put("content", content);
        comment.put("targetType", targetType);
        comment.put("targetId", targetId);
        comment.put("createTime", LocalDateTime.now());

        String cacheKey = COLLAB_PREFIX + "comment:" + commentId;
        cacheUtil.set(cacheKey, comment, 30, TimeUnit.DAYS);

        result.put("success", true);
        result.put("commentId", commentId);
        result.put("comment", comment);

        return result;
    }

    @Override
    public List<Map<String, Object>> getComments(Integer tripId, String targetType, Integer targetId) {
        List<Map<String, Object>> comments = new ArrayList<>();

        // 模拟获取评论
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> comment = new HashMap<>();
            comment.put("id", i);
            comment.put("userId", i * 5);
            comment.put("content", "评论内容 " + i);
            comment.put("createTime", LocalDateTime.now().minusMinutes(i * 10));
            comments.add(comment);
        }

        return comments;
    }

    @Override
    public boolean completeTask(Integer tripId, Integer userId, Integer taskId) {
        String taskKey = COLLAB_PREFIX + "task:" + taskId;

        @SuppressWarnings("unchecked")
        Map<String, Object> task = cacheUtil.get(taskKey, Map.class);
        if (task == null) {
            return false;
        }

        task.put("status", "completed");
        task.put("completedBy", userId);
        task.put("completedTime", LocalDateTime.now());
        cacheUtil.set(taskKey, task, 30, TimeUnit.DAYS);

        return true;
    }

    @Override
    public Map<String, Object> assignTask(Integer tripId, Integer assignerId, Integer assigneeId, String taskDescription) {
        Map<String, Object> result = new HashMap<>();

        Integer taskId = generateTaskId();

        Map<String, Object> task = new HashMap<>();
        task.put("id", taskId);
        task.put("tripId", tripId);
        task.put("assignerId", assignerId);
        task.put("assigneeId", assigneeId);
        task.put("description", taskDescription);
        task.put("status", "pending");
        task.put("createTime", LocalDateTime.now());

        String cacheKey = COLLAB_PREFIX + "task:" + taskId;
        cacheUtil.set(cacheKey, task, 30, TimeUnit.DAYS);

        result.put("success", true);
        result.put("taskId", taskId);
        result.put("task", task);

        return result;
    }

    @Override
    public List<Map<String, Object>> getTasks(Integer tripId, Integer userId) {
        List<Map<String, Object>> tasks = new ArrayList<>();

        // 模拟获取任务列表
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> task = new HashMap<>();
            task.put("id", i);
            task.put("description", "任务 " + i);
            task.put("status", i == 1 ? "completed" : "pending");
            task.put("assigneeId", userId);
            tasks.add(task);
        }

        return tasks;
    }

    @Override
    public Map<String, Object> realTimeEdit(Integer tripId, Integer userId, Map<String, Object> editData) {
        Map<String, Object> result = new HashMap<>();

        if (!hasEditPermission(tripId, userId)) {
            result.put("success", false);
            result.put("message", "无编辑权限");
            return result;
        }

        // 广播编辑数据给其他协作者
        String editKey = COLLAB_PREFIX + "edit:" + tripId + ":" + System.currentTimeMillis();
        editData.put("userId", userId);
        editData.put("timestamp", LocalDateTime.now());
        cacheUtil.set(editKey, editData, 1, TimeUnit.HOURS);

        result.put("success", true);
        result.put("message", "编辑已广播");

        return result;
    }

    @Override
    public List<Map<String, Object>> getOnlineCollaborators(Integer tripId) {
        List<Map<String, Object>> onlineUsers = new ArrayList<>();

        // 模拟获取在线协作者
        for (int i = 1; i <= 3; i++) {
            Map<String, Object> user = new HashMap<>();
            user.put("userId", i * 10);
            user.put("username", "用户" + i);
            user.put("lastActive", LocalDateTime.now());
            onlineUsers.add(user);
        }

        return onlineUsers;
    }

    @Override
    public boolean lockSection(Integer tripId, Integer userId, String section) {
        String lockKey = COLLAB_PREFIX + "lock:" + tripId + ":" + section;

        // 检查是否已被锁定
        Integer lockedBy = cacheUtil.get(lockKey, Integer.class);
        if (lockedBy != null && !lockedBy.equals(userId)) {
            return false;
        }

        cacheUtil.set(lockKey, userId, 10, TimeUnit.MINUTES);
        return true;
    }

    @Override
    public boolean unlockSection(Integer tripId, Integer userId, String section) {
        String lockKey = COLLAB_PREFIX + "lock:" + tripId + ":" + section;

        Integer lockedBy = cacheUtil.get(lockKey, Integer.class);
        if (lockedBy != null && lockedBy.equals(userId)) {
            cacheUtil.delete(lockKey);
            return true;
        }

        return false;
    }

    @Override
    public Map<String, Object> mergeChanges(Integer tripId, Integer userId, Integer baseVersion, Map<String, Object> changes) {
        Map<String, Object> result = new HashMap<>();

        // 获取当前版本
        String tripKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(tripKey, Map.class);
        if (trip == null) {
            result.put("success", false);
            result.put("message", "行程不存在");
            return result;
        }

        Integer currentVersion = (Integer) trip.get("version");

        if (currentVersion.equals(baseVersion)) {
            // 没有冲突，直接应用修改
            syncChanges(tripId, userId, changes);
            result.put("success", true);
            result.put("hasConflict", false);
        } else {
            // 存在冲突，需要手动解决
            result.put("success", false);
            result.put("hasConflict", true);
            result.put("message", "存在版本冲突，请手动合并");
            result.put("currentVersion", currentVersion);
            result.put("baseVersion", baseVersion);
        }

        return result;
    }

    @Override
    public List<Map<String, Object>> getVersionHistory(Integer tripId) {
        List<Map<String, Object>> versions = new ArrayList<>();

        String versionListKey = VERSION_PREFIX + "list:" + tripId;

        @SuppressWarnings("unchecked")
        List<Integer> versionIds = cacheUtil.get(versionListKey, List.class);
        if (versionIds != null) {
            for (Integer versionId : versionIds) {
                String versionKey = VERSION_PREFIX + tripId + ":" + versionId;

                @SuppressWarnings("unchecked")
                Map<String, Object> version = cacheUtil.get(versionKey, Map.class);
                if (version != null) {
                    versions.add(version);
                }
            }
        }

        return versions;
    }

    @Override
    public boolean rollbackToVersion(Integer tripId, Integer userId, Integer versionId) {
        if (!hasPermission(tripId, userId, "owner")) {
            return false;
        }

        String versionKey = VERSION_PREFIX + tripId + ":" + versionId;

        @SuppressWarnings("unchecked")
        Map<String, Object> version = cacheUtil.get(versionKey, Map.class);
        if (version == null) {
            return false;
        }

        // 回滚到指定版本
        String tripKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(tripKey, Map.class);
        if (trip != null) {
            trip.put("version", versionId);
            trip.put("data", version.get("data"));
            trip.put("lastModified", LocalDateTime.now());
            cacheUtil.set(tripKey, trip, 30, TimeUnit.DAYS);
        }

        return true;
    }

    @Override
    public Map<String, Object> exportCollaborativeTrip(Integer tripId, String format) {
        Map<String, Object> result = new HashMap<>();

        String tripKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(tripKey, Map.class);
        if (trip == null) {
            result.put("success", false);
            result.put("message", "行程不存在");
            return result;
        }

        result.put("success", true);
        result.put("tripId", tripId);
        result.put("format", format);
        result.put("downloadUrl", "/api/export/" + tripId + "." + format.toLowerCase());

        return result;
    }

    @Override
    public boolean endCollaboration(Integer tripId, Integer ownerId) {
        if (!hasPermission(tripId, ownerId, "owner")) {
            return false;
        }

        String tripKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(tripKey, Map.class);
        if (trip != null) {
            trip.put("status", "ended");
            trip.put("endTime", LocalDateTime.now());
            cacheUtil.set(tripKey, trip, 30, TimeUnit.DAYS);
        }

        return true;
    }

    // 辅助方法
    private boolean hasPermission(Integer tripId, Integer userId, String requiredRole) {
        String tripKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(tripKey, Map.class);
        if (trip == null) {
            return false;
        }

        Integer creatorId = (Integer) trip.get("creatorId");
        if (userId.equals(creatorId)) {
            return true;
        }

        if ("owner".equals(requiredRole)) {
            return false;
        }

        return true;
    }

    private boolean hasEditPermission(Integer tripId, Integer userId) {
        String roleKey = COLLAB_PREFIX + "role:" + tripId + ":" + userId;
        String role = cacheUtil.get(roleKey, String.class);

        return "owner".equals(role) || "editor".equals(role);
    }

    private void addCollaboratorToTrip(Integer tripId, Integer userId, String role) {
        String tripKey = COLLAB_PREFIX + "trip:" + tripId;

        @SuppressWarnings("unchecked")
        Map<String, Object> trip = cacheUtil.get(tripKey, Map.class);
        if (trip != null) {
            @SuppressWarnings("unchecked")
            List<Integer> collaborators = (List<Integer>) trip.get("collaborators");
            if (!collaborators.contains(userId)) {
                collaborators.add(userId);
                trip.put("collaborators", collaborators);
                cacheUtil.set(tripKey, trip, 30, TimeUnit.DAYS);
            }
        }

        // 保存角色
        String roleKey = COLLAB_PREFIX + "role:" + tripId + ":" + userId;
        cacheUtil.set(roleKey, role, 30, TimeUnit.DAYS);
    }

    private void saveVersion(Integer tripId, Integer versionId, Integer userId, String description, Object data) {
        Map<String, Object> version = new HashMap<>();
        version.put("id", versionId);
        version.put("tripId", tripId);
        version.put("userId", userId);
        version.put("description", description);
        version.put("data", data);
        version.put("timestamp", LocalDateTime.now());

        String versionKey = VERSION_PREFIX + tripId + ":" + versionId;
        cacheUtil.set(versionKey, version, 90, TimeUnit.DAYS);

        // 更新版本列表
        String versionListKey = VERSION_PREFIX + "list:" + tripId;

        @SuppressWarnings("unchecked")
        List<Integer> versionIds = cacheUtil.get(versionListKey, List.class);
        if (versionIds == null) {
            versionIds = new ArrayList<>();
        }
        versionIds.add(versionId);
        cacheUtil.set(versionListKey, versionIds, 90, TimeUnit.DAYS);
    }

    private Integer generateTripId() {
        return (int) (System.currentTimeMillis() % 1000000);
    }

    private Integer generateInvitationId() {
        return (int) (System.currentTimeMillis() % 1000000) + 1000000;
    }

    private Integer generateCommentId() {
        return (int) (System.currentTimeMillis() % 1000000) + 2000000;
    }

    private Integer generateTaskId() {
        return (int) (System.currentTimeMillis() % 1000000) + 3000000;
    }
}
