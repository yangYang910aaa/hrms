package com.yang.hr.controller;

import com.yang.hr.common.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * M1 连通性验证接口：/api/ping
 * 同时探活 MySQL 与 Redis，任一不通也能返回 200 并标明状态，方便定位环境问题。
 */
@RestController
@RequestMapping("/api")
public class PingController {

    private static final Logger log = LoggerFactory.getLogger(PingController.class);
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final DataSource dataSource;
    private final StringRedisTemplate stringRedisTemplate;

    public PingController(DataSource dataSource, StringRedisTemplate stringRedisTemplate) {
        this.dataSource = dataSource;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/ping")
    public R<Map<String, Object>> ping() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("app", "hr-backend");
        data.put("time", LocalDateTime.now().format(FMT));
        data.put("mysql", probeMysql());
        data.put("redis", probeRedis());

        boolean allUp = "UP".equals(((Map<?, ?>) data.get("mysql")).get("status"))
                && "UP".equals(((Map<?, ?>) data.get("redis")).get("status"));
        data.put("allUp", allUp);

        return R.ok(allUp ? "MySQL 与 Redis 均连通" : "服务已启动，但有组件未连通，详见 data", data);
    }

    private Map<String, Object> probeMysql() {
        Map<String, Object> m = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION(), DATABASE(), CURRENT_USER()")) {
            if (rs.next()) {
                m.put("status", "UP");
                m.put("version", rs.getString(1));
                m.put("database", rs.getString(2));
                m.put("user", rs.getString(3));
            } else {
                m.put("status", "UP");
            }
        } catch (Exception e) {
            log.error("[ping] MySQL 探活失败", e);
            m.put("status", "DOWN");
            m.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return m;
    }

    private Map<String, Object> probeRedis() {
        Map<String, Object> m = new LinkedHashMap<>();
        String key = "hr:ping";
        try {
            String stamp = LocalDateTime.now().format(FMT);
            // 带 TTL：既验证了读+写，又不会在 Redis 里留下永不过期的探活垃圾键
            stringRedisTemplate.opsForValue().set(key, stamp, Duration.ofMinutes(5));
            String back = stringRedisTemplate.opsForValue().get(key);
            m.put("status", stamp.equals(back) ? "UP" : "DOWN");
            m.put("key", key);
            m.put("value", back);
            m.put("ttlSeconds", stringRedisTemplate.getExpire(key));
        } catch (Exception e) {
            log.error("[ping] Redis 探活失败", e);
            m.put("status", "DOWN");
            m.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
        return m;
    }
}
