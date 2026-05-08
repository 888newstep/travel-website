package travel.service.impl.user_community;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import travel.entity.user_community.TravelNote;
import travel.entity.user_community.TravelNoteTag;
import travel.mapper.user_community_mapper.TravelNoteMapper;
import travel.service.user_community.TravelNoteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelNoteServiceImpl extends ServiceImpl<TravelNoteMapper, TravelNote> implements TravelNoteService {

    private static final Logger log = LoggerFactory.getLogger(TravelNoteServiceImpl.class);

    private final JdbcTemplate jdbcTemplate;
    @Override
    @Transactional
    public TravelNote createTravelNote(TravelNote travelNote, List<String> tags) {
        try {
            save(travelNote);

            if (tags != null && !tags.isEmpty()) {
                List<TravelNoteTag> noteTags = tags.stream()
                        .map(tag -> {
                            TravelNoteTag noteTag = new TravelNoteTag();
                            noteTag.setNoteId(travelNote.getId());
                            noteTag.setTagName(tag);
                            return noteTag;
                        })
                        .collect(Collectors.toList());
                noteTags.forEach(this::saveTag);
            }

            log.info("创建游记成功: id={}, userId={}, title={}", travelNote.getId(), travelNote.getUserId(), travelNote.getTitle());
            return travelNote;
        } catch (Exception e) {
            log.error("创建游记失败: userId={}, title={}", travelNote.getUserId(), travelNote.getTitle(), e);
            throw new RuntimeException("创建游记失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getHotTravelNotes(int limit) {
        try {
            QueryWrapper<TravelNote> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_public", true);
            queryWrapper.orderByDesc("views_count", "likes_count", "comments_count");
            queryWrapper.last("LIMIT " + limit);

            List<TravelNote> travelNotes = list(queryWrapper);

            return travelNotes.stream()
                    .map(note -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("travelNote", note);
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取热门游记失败: limit={}", limit, e);
            throw new RuntimeException("获取热门游记失败", e);
        }
    }
    @Override
    @Transactional
    public TravelNote updateTravelNote(Integer id, TravelNote travelNote, List<String> tags) {
        try {
            TravelNote existingNote = getById(id);
            if (existingNote == null) {
                throw new RuntimeException("游记不存在");
            }

            travelNote.setId(id);
            updateById(travelNote);

            if (tags != null) {
                deleteTagsByNoteId(id);
                List<TravelNoteTag> noteTags = tags.stream()
                        .map(tag -> {
                            TravelNoteTag noteTag = new TravelNoteTag();
                            noteTag.setNoteId(id);
                            noteTag.setTagName(tag);
                            return noteTag;
                        })
                        .collect(Collectors.toList());
                noteTags.forEach(this::saveTag);
            }

            log.info("更新游记成功: id={}, userId={}, title={}", id, travelNote.getUserId(), travelNote.getTitle());
            return travelNote;
        } catch (Exception e) {
            log.error("更新游记失败: id={}, userId={}, title={}", id, travelNote.getUserId(), travelNote.getTitle(), e);
            throw new RuntimeException("更新游记失败", e);
        }
    }

    @Override
    @Transactional
    public boolean deleteTravelNote(Integer id, Integer userId) {
        try {
            // 检查游记是否存在
            TravelNote travelNote = getById(id);
            if (travelNote == null) {
                throw new RuntimeException("游记不存在");
            }
            
            // 检查权限
            if (!travelNote.getUserId().equals(userId)) {
                throw new RuntimeException("无权限删除此游记");
            }
            
            // 删除标签
            deleteTagsByNoteId(id);
            
            // 删除游记
            boolean result = removeById(id);
            
            if (result) {
                log.info("删除游记成功: id={}, userId={}", id, userId);
            }
            
            return result;
        } catch (Exception e) {
            log.error("删除游记失败: id={}, userId={}", id, userId, e);
            throw new RuntimeException("删除游记失败", e);
        }
    }

    @Override
    public Map<String, Object> getTravelNoteDetail(Integer id) {
        try {
            // 获取游记
            TravelNote travelNote = getById(id);
            if (travelNote == null) {
                throw new RuntimeException("游记不存在");
            }
            
            // 增加浏览数
            incrementViews(id);
            
            // 构建返回结果
            Map<String, Object> result = new HashMap<>();
            result.put("travelNote", travelNote);
            // 这里可以添加用户信息、标签信息等
            
            return result;
        } catch (Exception e) {
            log.error("获取游记详情失败: id={}", id, e);
            throw new RuntimeException("获取游记详情失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getTravelNotes(int page, int size, Map<String, Object> filters) {
        try {
            // 构建查询条件
            QueryWrapper<TravelNote> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_public", true);
            
            // 应用过滤条件
            if (filters != null) {
                if (filters.containsKey("cityId")) {
                    queryWrapper.eq("city_id", filters.get("cityId"));
                }
                if (filters.containsKey("userId")) {
                    queryWrapper.eq("user_id", filters.get("userId"));
                }
            }
            
            // 排序
            queryWrapper.orderByDesc("created_at");
            
            // 分页查询
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<TravelNote> pageInfo = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            page(pageInfo, queryWrapper);
            
            // 构建返回结果
            return pageInfo.getRecords().stream()
                    .map(note -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("travelNote", note);
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取游记列表失败: page={}, size={}, filters={}", page, size, filters, e);
            throw new RuntimeException("获取游记列表失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getUserTravelNotes(Integer userId, int page, int size) {
        try {
            // 构建查询条件
            QueryWrapper<TravelNote> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", userId);
            queryWrapper.orderByDesc("created_at");
            
            // 分页查询
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<TravelNote> pageInfo = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            page(pageInfo, queryWrapper);
            
            // 构建返回结果
            return pageInfo.getRecords().stream()
                    .map(note -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("travelNote", note);
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取用户游记列表失败: userId={}, page={}, size={}", userId, page, size, e);
            throw new RuntimeException("获取用户游记列表失败", e);
        }
    }

    @Override
    public boolean likeTravelNote(Integer id, Integer userId) {
        try {
            // 检查游记是否存在
            TravelNote travelNote = getById(id);
            if (travelNote == null) {
                throw new RuntimeException("游记不存在");
            }
            
            // 增加点赞数
            travelNote.setLikesCount(travelNote.getLikesCount() + 1);
            boolean result = updateById(travelNote);
            
            if (result) {
                log.info("点赞游记成功: id={}, userId={}", id, userId);
            }
            
            return result;
        } catch (Exception e) {
            log.error("点赞游记失败: id={}, userId={}", id, userId, e);
            throw new RuntimeException("点赞游记失败", e);
        }
    }

    @Override
    public boolean unlikeTravelNote(Integer id, Integer userId) {
        try {
            // 检查游记是否存在
            TravelNote travelNote = getById(id);
            if (travelNote == null) {
                throw new RuntimeException("游记不存在");
            }
            
            // 减少点赞数
            if (travelNote.getLikesCount() > 0) {
                travelNote.setLikesCount(travelNote.getLikesCount() - 1);
                boolean result = updateById(travelNote);
                
                if (result) {
                    log.info("取消点赞游记成功: id={}, userId={}", id, userId);
                }
                
                return result;
            }
            
            return false;
        } catch (Exception e) {
            log.error("取消点赞游记失败: id={}, userId={}", id, userId, e);
            throw new RuntimeException("取消点赞游记失败", e);
        }
    }

    @Override
    public boolean incrementViews(Integer id) {
        try {
            // 检查游记是否存在
            TravelNote travelNote = getById(id);
            if (travelNote == null) {
                throw new RuntimeException("游记不存在");
            }
            
            // 增加浏览数
            travelNote.setViewsCount(travelNote.getViewsCount() + 1);
            boolean result = updateById(travelNote);
            
            return result;
        } catch (Exception e) {
            log.error("增加游记浏览数失败: id={}", id, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> searchTravelNotes(String keyword, int page, int size) {
        try {
            // 构建查询条件
            QueryWrapper<TravelNote> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_public", true);
            queryWrapper.like("title", keyword).or().like("content", keyword);
            queryWrapper.orderByDesc("created_at");
            
            // 分页查询
            com.baomidou.mybatisplus.extension.plugins.pagination.Page<TravelNote> pageInfo = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
            page(pageInfo, queryWrapper);
            
            // 构建返回结果
            return pageInfo.getRecords().stream()
                    .map(note -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("travelNote", note);
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("搜索游记失败: keyword={}, page={}, size={}", keyword, page, size, e);
            throw new RuntimeException("搜索游记失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> getLatestTravelNotes(int limit) {
        try {
            QueryWrapper<TravelNote> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("is_public", true);
            queryWrapper.orderByDesc("created_at");
            queryWrapper.last("LIMIT " + limit);

            List<TravelNote> travelNotes = list(queryWrapper);

            return travelNotes.stream()
                    .map(note -> {
                        Map<String, Object> map = new HashMap<>();
                        map.put("travelNote", note);
                        return map;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取最新游记失败: limit={}", limit, e);
            throw new RuntimeException("获取最新游记失败", e);
        }
    }

    @Override
    public int countByUserId(Integer userId) {
        if (userId == null || userId <= 0) {
            return 0;
        }

        QueryWrapper<TravelNote> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        return Math.toIntExact(count(queryWrapper));
    }

    @Override
    @Transactional
    public boolean collectNote(Integer noteId, Integer userId) {
        try {
            if (noteId == null || userId == null) {
                throw new RuntimeException("参数不能为空");
            }

            TravelNote note = getById(noteId);
            if (note == null) {
                throw new RuntimeException("游记不存在");
            }

            String sql = "INSERT INTO travel_note_collections (note_id, user_id, created_at) VALUES (?, ?, NOW())";
            jdbcTemplate.update(sql, noteId, userId);

            log.info("收藏游记成功: noteId={}, userId={}", noteId, userId);
            return true;
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate")) {
                throw new RuntimeException("已经收藏过该游记");
            }
            log.error("收藏游记失败: noteId={}, userId={}", noteId, userId, e);
            throw new RuntimeException("收藏失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean uncollectNote(Integer noteId, Integer userId) {
        try {
            if (noteId == null || userId == null) {
                throw new RuntimeException("参数不能为空");
            }

            String sql = "DELETE FROM travel_note_collections WHERE note_id = ? AND user_id = ?";
            int rows = jdbcTemplate.update(sql, noteId, userId);

            if (rows == 0) {
                throw new RuntimeException("未找到收藏记录");
            }

            log.info("取消收藏游记成功: noteId={}, userId={}", noteId, userId);
            return true;
        } catch (Exception e) {
            log.error("取消收藏游记失败: noteId={}, userId={}", noteId, userId, e);
            throw new RuntimeException("取消收藏失败: " + e.getMessage());
        }
    }
    private void saveTag(TravelNoteTag tag) {
        String sql = "INSERT INTO travel_note_tags (note_id, tag_name) VALUES (?, ?)";
        jdbcTemplate.update(sql, tag.getNoteId(), tag.getTagName());
    }

    private void deleteTagsByNoteId(Integer noteId) {
        String sql = "DELETE FROM travel_note_tags WHERE note_id = ?";
        jdbcTemplate.update(sql, noteId);
    }
}
