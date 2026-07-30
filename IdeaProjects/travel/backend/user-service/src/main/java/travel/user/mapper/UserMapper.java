package travel.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import travel.common.entity.user_community.User;
import org.springframework.stereotype.Repository;

@Repository
public interface UserMapper extends BaseMapper<User> {
}