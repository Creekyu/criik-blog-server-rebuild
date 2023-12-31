package com.criiky0.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.criiky0.pojo.User;
import com.criiky0.pojo.dto.UserDTO;
import com.criiky0.pojo.vo.LoginVo;
import com.criiky0.pojo.vo.UpdatePswVo;
import com.criiky0.service.UserService;
import com.criiky0.mapper.UserMapper;
import com.criiky0.utils.JwtHelper;
import com.criiky0.utils.MD5Util;
import com.criiky0.utils.Result;
import com.criiky0.utils.ResultCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;

/**
 * @author criiky0
 * @description 针对表【user】的数据库操作Service实现
 * @createDate 2023-10-26 14:24:13
 */
@Service
@Transactional
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private UserMapper userMapper;
    private JwtHelper jwtHelper;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, JwtHelper jwtHelper) {
        this.userMapper = userMapper;
        this.jwtHelper = jwtHelper;
    }

    @Override
    public Result login(LoginVo loginVo) {
        // 验证账户
        User loginUser =
            userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, loginVo.getUserinfo()));
        if (loginUser == null) {
            // 如果username为null验证email
            loginUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, loginVo.getUserinfo()));
            if (loginUser == null)
                return Result.build(null, ResultCodeEnum.USERINFO_ERROR);
        }
        if (StringUtils.isEmpty(loginVo.getPassword())
            || !loginUser.getPassword().equals(MD5Util.encrypt(loginVo.getPassword()))) {
            return Result.build(null, ResultCodeEnum.USERINFO_ERROR);
        }

        String token = jwtHelper.createToken(loginUser.getUserId(), loginUser.getRole().toString());
        return Result.ok(token);
    }

    @Override
    public Result register(User user) {
        // 验证用户
        User loginUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, user.getUsername()));
        if (loginUser != null) {
            return Result.build(null, ResultCodeEnum.USER_USED_ERROR);
        }
        // 验证邮箱
        User emailUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, user.getEmail()));
        if (emailUser != null) {
            return Result.build(null, ResultCodeEnum.REGISTER_ERROR);
        }

        // 验证密码
        if (StringUtils.isEmpty(user.getPassword())) {
            return Result.build(null, ResultCodeEnum.REGISTER_ERROR);
        }

        // MD5
        user.setPassword(MD5Util.encrypt(user.getPassword()));
        user.setRole("user"); // 避免后面生成token空指针异常
        int insert = userMapper.insert(user);
        if (insert == 0) {
            return Result.build(null, ResultCodeEnum.UNKNOWN_ERROR);
        }
        String token = jwtHelper.createToken(user.getUserId(), user.getRole().toString());
        return Result.ok(token);
    }

    @Override
    public Result<HashMap<String, UserDTO>> getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        UserDTO userDTO = new UserDTO(user.getUserId(), user.getUsername(), user.getNickname(), user.getBrief(),
            user.getEmail(), user.getAvatar(), user.getRole());
        HashMap<String, UserDTO> map = new HashMap<>();
        map.put("user", userDTO);
        return Result.ok(map);
    }

    @Override
    public Result<ResultCodeEnum> uploadAvatar(String avatar, Long userId) {
        // 保存avatar
        int update = userMapper.update(null,
            new LambdaUpdateWrapper<User>().eq(User::getUserId, userId).set(User::getAvatar, avatar));
        if (update > 0) {
            return Result.ok(null);
        }
        return Result.build(null, ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    public Result<HashMap<String, UserDTO>> updateUserInfo(User user) {
        boolean hasNickName = !StringUtils.isEmpty(user.getNickname());
        boolean hasBrief = !StringUtils.isEmpty(user.getBrief());
        if (!hasNickName && !hasBrief) {
            return Result.build(null, ResultCodeEnum.PARAM_NULL_ERROR);
        }
        int rows = userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getUserId, user.getUserId())
            .set(hasNickName, User::getNickname, user.getNickname()).set(hasBrief, User::getBrief, user.getBrief()));
        if (rows > 0) {
            User updatedUser = userMapper.selectById(user.getUserId());
            UserDTO updatedUserDTO =
                new UserDTO(updatedUser.getUserId(), updatedUser.getUsername(), updatedUser.getNickname(),
                    updatedUser.getBrief(), updatedUser.getEmail(), updatedUser.getAvatar(), updatedUser.getRole());
            HashMap<String, UserDTO> map = new HashMap<>();
            map.put("updatedUser", updatedUserDTO);
            return Result.ok(map);
        }
        return Result.build(null, ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    public Result<ResultCodeEnum> updateuserRole(User user) {
        User selectUser = userMapper.selectById(user.getUserId());
        if (selectUser == null) {
            return Result.build(null, ResultCodeEnum.CANNOT_FIND_ERROR);
        }
        boolean hasRole = !StringUtils.isEmpty(user.getRole().toString());
        if (!hasRole) {
            return Result.build(null, ResultCodeEnum.PARAM_NULL_ERROR);
        }
        int rows = userMapper.update(null,
            new LambdaUpdateWrapper<User>().eq(User::getUserId, user.getUserId()).set(User::getRole, user.getRole()));
        if (rows > 0) {
            return Result.ok(null);
        }
        return Result.build(null, ResultCodeEnum.UNKNOWN_ERROR);
    }

    @Override
    public Result<ResultCodeEnum> updatePsw(UpdatePswVo pswVo, Long userId) {
        User user = userMapper.selectById(userId);
        if (!user.getPassword().equals(MD5Util.encrypt(pswVo.getOldPsw()))) {
            return Result.build(null, 400, "旧密码错误，请重新再试！");
        }
        if (!pswVo.getNewPsw().equals(pswVo.getPswConfirm())) {
            return Result.build(null, 400, "两次密码输入不一致，请重新再试！");
        }
        int update = userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getUserId, userId)
            .set(User::getPassword, MD5Util.encrypt(pswVo.getNewPsw())));
        if (update > 0) {
            return Result.ok(null);
        }
        return Result.build(null, 400, "密码因未知原因更新失败！");
    }
}
