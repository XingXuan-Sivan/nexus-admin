package com.nexusadmin.plugin.system.user.mapper;

import com.mybatisflex.core.BaseMapper;
import com.nexusadmin.core.domain.identity.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 用户持久化层接口，占位用于集成具体的 ORM 框架（如 MyBatis-Flex）。
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    List<User> selectUsers();

    User selectUserById(String userId);

    void insertUser(User user);

    void updateUser(User user);

    void deleteUser(String userId);
}
