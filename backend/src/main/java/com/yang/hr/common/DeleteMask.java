package com.yang.hr.common;

import org.springframework.util.StringUtils;

/**
 * 逻辑删除前给唯一键「让路」的公共口径（规划书 §9-16、交接文档 §5 硬约定）。
 *
 * 主档表删除时把业务唯一键改写成「原值 + #del# + id」再置 deleted=1，
 * 避免与被删行撞唯一索引。
 *
 * ⚠️ 拼接结果**不能超过列宽**：strict 模式下超长会 Data too long，
 * 删除接口直接 500 且反复删不掉（2026-10-13 全面复查发现，此前 8 处各自拼串都没截断）。
 * 故统一收口到本类，按列宽截断原值后再拼。
 */
public final class DeleteMask {

    /** 让路标记 */
    public static final String MARK = "#del#";

    private DeleteMask() {
    }

    /**
     * 生成让路值：原业务键 + #del# + id；超列宽时按列宽截断原值。
     *
     * 截断不破坏唯一性：后缀里的 id 本就唯一，两个不同的已删行后缀必然不同。
     *
     * @param businessKey  原业务键；为空、已含标记或 id 为空时原样返回（调用方照旧跳过）
     * @param id           行ID
     * @param columnLength 业务键列宽（VARCHAR(50) 传 50）
     */
    public static String masked(String businessKey, Long id, int columnLength) {
        if (!StringUtils.hasText(businessKey) || businessKey.contains(MARK) || id == null) {
            return businessKey;
        }
        String suffix = MARK + id;
        int keep = columnLength - suffix.length();
        String base = businessKey.length() <= keep ? businessKey : businessKey.substring(0, keep);
        return base + suffix;
    }
}
