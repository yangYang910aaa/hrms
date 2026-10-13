package com.yang.hr.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yang.hr.common.R;
import com.yang.hr.dto.DictDataDTO;
import com.yang.hr.dto.DictTypeDTO;
import com.yang.hr.entity.SysDictData;
import com.yang.hr.entity.SysDictType;
import com.yang.hr.service.DictDataService;
import com.yang.hr.service.DictTypeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 数据字典接口（管理端）：字典类型 + 字典数据两套 CRUD。
 *
 * 全部要求 system:dict:* 权限。业务表单要"按类型取字典项"请用 /api/common/dict/{type}（仅需登录），
 * 不要把本 Controller 的权限放宽给业务角色。
 */
@RestController
@RequestMapping("/api/system/dict")
public class DictController {

    private final DictTypeService dictTypeService;
    private final DictDataService dictDataService;

    public DictController(DictTypeService dictTypeService, DictDataService dictDataService) {
        this.dictTypeService = dictTypeService;
        this.dictDataService = dictDataService;
    }

    // ------------------------------------------------------------------ 字典类型

    @GetMapping("/type/page")
    @SaCheckPermission("system:dict:list")
    public R<Page<SysDictType>> typePage(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String dictName,
                                         @RequestParam(required = false) String dictType,
                                         @RequestParam(required = false) Integer status) {
        return R.ok(dictTypeService.page(current, size, dictName, dictType, status));
    }

    /** 启用中的字典类型（管理页类型下拉用） */
    @GetMapping("/type/list")
    @SaCheckPermission("system:dict:list")
    public R<List<SysDictType>> typeList() {
        return R.ok(dictTypeService.listEnabled());
    }

    @GetMapping("/type/{id}")
    @SaCheckPermission("system:dict:list")
    public R<SysDictType> typeDetail(@PathVariable Long id) {
        return R.ok(dictTypeService.getById(id));
    }

    @PostMapping("/type")
    @SaCheckPermission("system:dict:add")
    public R<Long> typeCreate(@Valid @RequestBody DictTypeDTO dto) {
        return R.ok("新增成功", dictTypeService.create(dto));
    }

    @PutMapping("/type")
    @SaCheckPermission("system:dict:edit")
    public R<Void> typeUpdate(@Valid @RequestBody DictTypeDTO dto) {
        dictTypeService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/type/{id}")
    @SaCheckPermission("system:dict:remove")
    public R<Void> typeDelete(@PathVariable Long id) {
        dictTypeService.delete(id);
        return R.ok("删除成功", null);
    }

    // ------------------------------------------------------------------ 字典数据

    @GetMapping("/data/page")
    @SaCheckPermission("system:dict:list")
    public R<Page<SysDictData>> dataPage(@RequestParam(defaultValue = "1") long current,
                                         @RequestParam(defaultValue = "10") long size,
                                         @RequestParam(required = false) String dictType,
                                         @RequestParam(required = false) String dictLabel,
                                         @RequestParam(required = false) Integer status) {
        return R.ok(dictDataService.page(current, size, dictType, dictLabel, status));
    }

    @GetMapping("/data/{id}")
    @SaCheckPermission("system:dict:list")
    public R<SysDictData> dataDetail(@PathVariable Long id) {
        return R.ok(dictDataService.getById(id));
    }

    @PostMapping("/data")
    @SaCheckPermission("system:dict:add")
    public R<Long> dataCreate(@Valid @RequestBody DictDataDTO dto) {
        return R.ok("新增成功", dictDataService.create(dto));
    }

    @PutMapping("/data")
    @SaCheckPermission("system:dict:edit")
    public R<Void> dataUpdate(@Valid @RequestBody DictDataDTO dto) {
        dictDataService.update(dto);
        return R.ok("修改成功", null);
    }

    @DeleteMapping("/data/{id}")
    @SaCheckPermission("system:dict:remove")
    public R<Void> dataDelete(@PathVariable Long id) {
        dictDataService.delete(id);
        return R.ok("删除成功", null);
    }
}
