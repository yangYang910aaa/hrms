package com.yang.hr.common;

import com.yang.hr.entity.SysMenu;
import com.yang.hr.vo.MenuTreeVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 菜单树构建测试（纯函数，不依赖 Spring / 数据库）
 */
@DisplayName("菜单树构建")
class MenuTreeBuilderTest {

    @Test
    @DisplayName("目录为根、菜单挂到目录下，按钮不进树")
    void buildsTreeAndExcludesButtons() {
        List<MenuTreeVO> tree = MenuTreeBuilder.build(List.of(
                menu(1L, 0L, "系统设置", "M", 1),
                menu(100L, 1L, "组织结构", "C", 1),
                menu(101L, 1L, "用户管理", "C", 2),
                menu(1004L, 101L, "用户新增", "F", 1)
        ));

        assertThat(tree).hasSize(1);
        MenuTreeVO root = tree.get(0);
        assertThat(root.getName()).isEqualTo("系统设置");
        assertThat(root.getChildren()).extracting(MenuTreeVO::getName)
                .containsExactly("组织结构", "用户管理");
        // 按钮不该出现在树里
        assertThat(root.getChildren()).extracting(MenuTreeVO::getName).doesNotContain("用户新增");
    }

    @Test
    @DisplayName("多个根按 sort 升序排列")
    void sortsRootsBySort() {
        List<MenuTreeVO> tree = MenuTreeBuilder.build(List.of(
                menu(3L, 0L, "薪酬管理", "M", 3),
                menu(1L, 0L, "系统设置", "M", 1),
                menu(2L, 0L, "人事管理", "M", 2)
        ));

        assertThat(tree).extracting(MenuTreeVO::getName)
                .containsExactly("系统设置", "人事管理", "薪酬管理");
    }

    @Test
    @DisplayName("sort 为 null 的节点排在最后，不会抛异常")
    void nullSortGoesLast() {
        List<MenuTreeVO> tree = MenuTreeBuilder.build(List.of(
                menu(1L, 0L, "无排序", "M", null),
                menu(2L, 0L, "有序", "M", 1)
        ));

        assertThat(tree).extracting(MenuTreeVO::getName).containsExactly("有序", "无排序");
    }

    @Test
    @DisplayName("隐藏菜单（visible=0）不进树")
    void hidesInvisibleMenus() {
        SysMenu hidden = menu(100L, 1L, "隐藏页", "C", 1);
        hidden.setVisible(0);
        List<MenuTreeVO> tree = MenuTreeBuilder.build(List.of(
                menu(1L, 0L, "系统设置", "M", 1),
                hidden
        ));

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getChildren()).isEmpty();
    }

    @Test
    @DisplayName("父节点未授权时子节点上提为根，不整棵消失")
    void orphanBecomesRoot() {
        List<MenuTreeVO> tree = MenuTreeBuilder.build(List.of(
                menu(105L, 1L, "社保比例", "C", 6)
        ));

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0).getName()).isEqualTo("社保比例");
    }

    @Test
    @DisplayName("空集合与 null 都返回空结果")
    void handlesEmptyInput() {
        assertThat(MenuTreeBuilder.build(List.of())).isEmpty();
        assertThat(MenuTreeBuilder.build(null)).isEmpty();
    }

    @Test
    @DisplayName("三级结构能正确嵌套")
    void supportsNestedLevels() {
        List<MenuTreeVO> tree = MenuTreeBuilder.build(List.of(
                menu(1L, 0L, "一级", "M", 1),
                menu(2L, 1L, "二级", "M", 1),
                menu(3L, 2L, "三级", "C", 1)
        ));

        assertThat(tree).hasSize(1);
        MenuTreeVO level2 = tree.get(0).getChildren().get(0);
        assertThat(level2.getName()).isEqualTo("二级");
        assertThat(level2.getChildren().get(0).getName()).isEqualTo("三级");
    }

    @Test
    @DisplayName("buildFull 保留按钮与隐藏项（菜单管理页要用）")
    void buildFullKeepsButtonsAndHidden() {
        SysMenu hidden = menu(102L, 1L, "隐藏页", "C", 3);
        hidden.setVisible(0);
        SysMenu button = menu(1004L, 101L, "用户新增", "F", 1);

        List<MenuTreeVO> tree = MenuTreeBuilder.buildFull(List.of(
                menu(1L, 0L, "系统设置", "M", 1),
                menu(101L, 1L, "用户管理", "C", 2),
                hidden,
                button
        ));

        assertThat(tree.get(0).getChildren()).extracting(MenuTreeVO::getName)
                .containsExactly("用户管理", "隐藏页");
        MenuTreeVO userMenu = tree.get(0).getChildren().get(0);
        assertThat(userMenu.getChildren()).extracting(MenuTreeVO::getName).containsExactly("用户新增");
    }

    @Test
    @DisplayName("build 与 buildFull 的差异只体现在过滤规则上")
    void buildAndBuildFullDifferOnlyByFilter() {
        List<SysMenu> source = List.of(
                menu(1L, 0L, "系统设置", "M", 1),
                menu(101L, 1L, "用户管理", "C", 2),
                menu(1004L, 101L, "用户新增", "F", 1)
        );

        assertThat(MenuTreeBuilder.build(source).get(0).getChildren()).hasSize(1);
        assertThat(MenuTreeBuilder.buildFull(source).get(0).getChildren()).hasSize(1);
        assertThat(MenuTreeBuilder.buildFull(source).get(0).getChildren().get(0).getChildren()).hasSize(1);
    }

    @Test
    @DisplayName("buildFull 会带上 visible/status 字段")
    void buildFullCarriesVisibleAndStatus() {
        SysMenu hidden = menu(102L, 0L, "隐藏页", "C", 1);
        hidden.setVisible(0);
        hidden.setStatus(0);

        MenuTreeVO node = MenuTreeBuilder.buildFull(List.of(hidden)).get(0);
        assertThat(node.getVisible()).isZero();
        assertThat(node.getStatus()).isZero();
    }

    private static SysMenu menu(Long id, Long parentId, String name, String type, Integer sort) {
        SysMenu m = new SysMenu();
        m.setId(id);
        m.setParentId(parentId);
        m.setMenuName(name);
        m.setMenuType(type);
        m.setSort(sort);
        m.setVisible(1);
        m.setStatus(1);
        return m;
    }
}
