package com.yang.hr.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 统一响应体 {code,msg,data} 与全局异常处理的契约测试。
 * 用 standalone MockMvc + 内置探针 Controller，不依赖数据库和 Redis，可离线跑。
 */
@DisplayName("响应契约与全局异常处理")
class ApiContractTest {

    private static final Set<String> CONTRACT_FIELDS = Set.of("code", "msg", "data");

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(new ProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("成功响应严格只有 code/msg/data 三个字段，不得出现 success")
    void okResponseKeepsExactlyThreeFields() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(get("/probe/ok"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.SUCCESS);
        assertThat(body.get("msg").asText()).isEqualTo("操作成功");
        assertThat(body.get("data").asText()).isEqualTo("hello");

        List<String> keys = new ArrayList<>();
        body.fieldNames().forEachRemaining(keys::add);
        assertThat(keys).containsExactlyInAnyOrderElementsOf(CONTRACT_FIELDS);
    }

    @Test
    @DisplayName("业务异常按自带 code 返回，不被兜底成 500")
    void bizExceptionKeepsItsOwnCode() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(get("/probe/biz"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.PARAM_ERROR);
        assertThat(body.get("msg").asText()).isEqualTo("工号已存在");
        assertThat(body.get("data").isNull()).isTrue();
    }

    @Test
    @DisplayName("@Valid 校验失败回 400，并带上出错字段名")
    void beanValidationFailureReturns400() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.PARAM_ERROR);
        assertThat(body.get("msg").asText()).contains("name");
    }

    @Test
    @DisplayName("请求体不是合法 JSON 回 400，而不是 500")
    void malformedJsonReturns400() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(post("/probe/valid")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.PARAM_ERROR);
        assertThat(body.get("msg").asText()).isEqualTo("请求体格式错误，无法解析 JSON");
    }

    @Test
    @DisplayName("缺少必填参数回 400 并指出参数名")
    void missingParamReturns400() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(get("/probe/need"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.PARAM_ERROR);
        assertThat(body.get("msg").asText()).contains("keyword");
    }

    @Test
    @DisplayName("参数类型不匹配回 400 并指出参数名")
    void typeMismatchReturns400() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(get("/probe/num").param("n", "abc"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.PARAM_ERROR);
        assertThat(body.get("msg").asText()).contains("n");
    }

    @Test
    @DisplayName("请求方法不对回 400，不是 500")
    void methodNotSupportedReturns400() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(post("/probe/ok"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.PARAM_ERROR);
        assertThat(body.get("msg").asText()).contains("请求方法不支持");
    }

    @Test
    @DisplayName("未预期异常走兜底 500，且不把堆栈细节抛给调用方")
    void unexpectedExceptionFallsBackTo500() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(get("/probe/boom"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.ERROR);
        assertThat(body.get("msg").asText()).isEqualTo("服务异常：IllegalStateException");
    }

    @Test
    @DisplayName("静态资源/路径不存在回 404，而不是被兜底成 500")
    void noResourceReturns404() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(get("/probe/notfound"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.NOT_FOUND);
        assertThat(body.get("msg").asText()).isEqualTo("请求的资源不存在");
    }

    @Test
    @DisplayName("上传文件超过容器限制回 400，而不是被兜底成 500")
    void maxUploadSizeReturns400() throws Exception {
        JsonNode body = jsonOf(mockMvc.perform(get("/probe/toolarge"))
                .andExpect(status().isOk())
                .andReturn());

        assertThat(body.get("code").asInt()).isEqualTo(ResultCode.PARAM_ERROR);
        assertThat(body.get("msg").asText()).contains("过大");
    }

    private JsonNode jsonOf(MvcResult result) throws Exception {
        // 必须显式按 UTF-8 取内容，否则 MockMvc 默认按 ISO-8859-1 解码，中文会乱码
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** 测试专用探针 Controller，只覆盖异常分支，不进主代码 */
    @RestController
    @RequestMapping("/probe")
    static class ProbeController {

        @GetMapping("/ok")
        public R<String> ok() {
            return R.ok("hello");
        }

        @GetMapping("/biz")
        public R<Void> biz() {
            throw new BizException(ResultCode.PARAM_ERROR, "工号已存在");
        }

        @PostMapping("/valid")
        public R<String> valid(@Valid @RequestBody ProbeDto dto) {
            return R.ok(dto.getName());
        }

        @GetMapping("/need")
        public R<String> need(@RequestParam String keyword) {
            return R.ok(keyword);
        }

        @GetMapping("/num")
        public R<String> num(@RequestParam Integer n) {
            return R.ok(String.valueOf(n));
        }

        @GetMapping("/boom")
        public R<Void> boom() {
            throw new IllegalStateException("模拟未预期异常");
        }

        /** NoResourceFoundException 继承 ServletException，是受检异常，方法上要声明 throws */
        @GetMapping("/notfound")
        public R<Void> notFound() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "uploads/not-exist.png");
        }

        @GetMapping("/toolarge")
        public R<Void> tooLarge() {
            throw new MaxUploadSizeExceededException(5 * 1024 * 1024);
        }
    }

    static class ProbeDto {

        @NotBlank(message = "不能为空")
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}
