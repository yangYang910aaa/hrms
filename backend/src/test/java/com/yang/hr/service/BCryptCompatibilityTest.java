package com.yang.hr.service;

import cn.dev33.satoken.secure.BCrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * BCrypt 兼容性测试。
 *
 * 背景：库里的 admin 口令哈希是**用 Python bcrypt 的 `$2a$` 前缀**生成的，
 * 而运行时校验用的是 Sa-Token 内置的 BCrypt 实现（jBCrypt 血统，只认 `$2a$`/`$2$`）。
 * 这里用同一个工具链生成一个**非敏感**哈希来守住这条兼容性 —— 一旦哪天换实现把它弄坏了，测试会立刻红。
 *
 * 注意：本测试里刻意不使用真实的种子口令，避免口令明文进入源码。
 */
@SuppressWarnings("deprecation")   // 整篇就在测 Sa-Token 的 BCrypt（已 @Deprecated 且无替代实现）；功能正常，仅消除编辑器告警
@DisplayName("BCrypt 哈希兼容性")
class BCryptCompatibilityTest {

    /** 由 Python bcrypt 生成（prefix=2a），明文 unit-test-only，非敏感 */
    private static final String PYTHON_GENERATED_HASH =
            "$2a$10$C47GuIRtNjkuzg8jnxjaX.DJ.w2d895.X3iaK4/SAJE.1l3SmAtBC";
    private static final String PLAIN = "unit-test-only";

    @Test
    @DisplayName("Sa-Token 能校验 Python bcrypt 生成的 $2a$ 哈希")
    void acceptsPythonGenerated2aHash() {
        assertThat(PYTHON_GENERATED_HASH).startsWith("$2a$").hasSize(60);
        assertThat(BCrypt.checkpw(PLAIN, PYTHON_GENERATED_HASH)).isTrue();
    }

    @Test
    @DisplayName("错误口令必须校验失败")
    void rejectsWrongPassword() {
        assertThat(BCrypt.checkpw("wrong-password", PYTHON_GENERATED_HASH)).isFalse();
        assertThat(BCrypt.checkpw("", PYTHON_GENERATED_HASH)).isFalse();
    }

    @Test
    @DisplayName("自身加密再校验可往返，且生成的哈希带 $2a$ 前缀")
    void roundTrip() {
        String hash = BCrypt.hashpw(PLAIN, BCrypt.gensalt(10));
        assertThat(hash).startsWith("$2a$").hasSize(60);
        assertThat(BCrypt.checkpw(PLAIN, hash)).isTrue();
        assertThat(BCrypt.checkpw(PLAIN + "x", hash)).isFalse();
    }
}
