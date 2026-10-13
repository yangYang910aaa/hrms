package com.yang.hr.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.SaTokenInfo;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yang.hr.common.BizException;
import com.yang.hr.common.MenuTreeBuilder;
import com.yang.hr.common.ResultCode;
import com.yang.hr.common.SecurityUtils;
import com.yang.hr.dto.LoginDTO;
import com.yang.hr.entity.SysMenu;
import com.yang.hr.entity.SysUser;
import com.yang.hr.mapper.SysMenuMapper;
import com.yang.hr.mapper.SysUserMapper;
import com.yang.hr.service.AuthService;
import com.yang.hr.vo.LoginVO;
import com.yang.hr.vo.MenuTreeVO;
import com.yang.hr.vo.UserInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 鉴权服务实现
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    /** 登录失败计数键前缀 */
    private static final String LOGIN_FAIL_KEY = "hr:login:fail:";

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final StringRedisTemplate redis;
    private final int loginMaxFail;
    private final int loginLockMinutes;

    public AuthServiceImpl(SysUserMapper userMapper,
                           SysMenuMapper menuMapper,
                           StringRedisTemplate redis,
                           @Value("${hr.security.login-max-fail:3}") int loginMaxFail,
                           @Value("${hr.security.login-lock-minutes:5}") int loginLockMinutes) {
        this.userMapper = userMapper;
        this.menuMapper = menuMapper;
        this.redis = redis;
        this.loginMaxFail = loginMaxFail;
        this.loginLockMinutes = loginLockMinutes;
    }

    @Override
    @SuppressWarnings("deprecation")   // Sa-Token 的 BCrypt 整类已 @Deprecated 且无替代实现；功能正常，仅消除编辑器告警
    public LoginVO login(LoginDTO dto) {
        String username = dto.getUsername().trim();
        assertNotLocked(username);

        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));

        // 账号不存在与口令错误返回同一句话，避免被拿来枚举账号
        if (user == null || !BCrypt.checkpw(dto.getPassword(), user.getPassword())) {
            throw new BizException(ResultCode.PARAM_ERROR, registerFail(username));
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.FORBIDDEN, "账号已停用，请联系管理员");
        }

        redis.delete(LOGIN_FAIL_KEY + username);

        // 签发 token（会话数据由 sa-token-redis-jackson 落到 Redis）
        StpUtil.login(user.getId());

        SysUser touch = new SysUser();
        touch.setId(user.getId());
        touch.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(touch);

        SaTokenInfo info = StpUtil.getTokenInfo();
        LoginVO vo = new LoginVO();
        vo.setTokenName(info.getTokenName());
        vo.setTokenValue(info.getTokenValue());
        vo.setExpiresIn(info.getTokenTimeout());
        vo.setUserInfo(buildUserInfo(user));
        log.info("用户登录成功：{}（id={}）", username, user.getId());
        return vo;
    }

    @Override
    public void logout() {
        Object loginId = StpUtil.getLoginIdDefaultNull();
        StpUtil.logout();
        log.info("用户退出登录：{}", loginId);
    }

    @Override
    public UserInfoVO currentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "用户不存在或已被删除，请重新登录");
        }
        // 已登录之后账号被停用，不能继续放行（token 还没过期，这里必须再挡一道）
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BizException(ResultCode.UNAUTHORIZED, "账号已停用，请联系管理员");
        }
        return buildUserInfo(user);
    }

    @Override
    public List<MenuTreeVO> currentUserMenus() {
        Long userId = StpUtil.getLoginIdAsLong();
        return MenuTreeBuilder.build(visibleMenus(userId));
    }

    // ------------------------------------------------------------------ 内部方法

    private UserInfoVO buildUserInfo(SysUser user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setDeptId(user.getDeptId());

        List<String> roles = userMapper.selectRoleCodesByUserId(user.getId());
        vo.setRoles(roles);

        List<SysMenu> menus = menusFor(user.getId(), roles);
        vo.setPerms(menus.stream()
                .map(SysMenu::getPerms)
                .filter(p -> p != null && !p.isBlank())
                .distinct()
                .toList());
        return vo;
    }

    private List<SysMenu> visibleMenus(Long userId) {
        return menusFor(userId, userMapper.selectRoleCodesByUserId(userId));
    }

    private List<SysMenu> menusFor(Long userId, List<String> roles) {
        return SecurityUtils.isSuperAdmin(roles)
                ? menuMapper.selectAllEnabled()
                : menuMapper.selectEnabledByUserId(userId);
    }

    /** 前置检查：锁定期内直接拒绝，不再比对口令 */
    private void assertNotLocked(String username) {
        String key = LOGIN_FAIL_KEY + username;
        String raw = redis.opsForValue().get(key);
        if (raw == null) {
            return;
        }
        int fails;
        try {
            fails = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            redis.delete(key);
            return;
        }
        if (fails < loginMaxFail) {
            return;
        }
        Long ttlMinutes = redis.getExpire(key, TimeUnit.MINUTES);
        if (ttlMinutes == null || ttlMinutes <= 0) {
            // 异常数据（计数键没有过期时间）：宁可清掉放行，也不要造出一把永远打不开的锁
            log.warn("登录失败计数键缺少 TTL，已清除：{}", key);
            redis.delete(key);
            return;
        }
        throw new BizException(ResultCode.FORBIDDEN, "密码错误次数过多，请 " + ttlMinutes + " 分钟后再试");
    }

    /**
     * 记一次失败并返回给前端的提示语。
     * 首次失败时设 TTL，避免"不停试错就把锁定期无限顺延"；TTL 意外丢失时补设，避免锁死。
     */
    private String registerFail(String username) {
        String key = LOGIN_FAIL_KEY + username;
        Long counter = redis.opsForValue().increment(key);
        long fails = counter == null ? 1 : counter;
        Long ttl = redis.getExpire(key);
        if (fails == 1 || ttl == null || ttl < 0) {
            redis.expire(key, Duration.ofMinutes(loginLockMinutes));
        }
        if (fails < loginMaxFail) {
            return "用户名或密码错误，还可尝试 " + (loginMaxFail - fails) + " 次";
        }
        return "用户名或密码错误次数过多，账号将锁定 " + loginLockMinutes + " 分钟";
    }
}
