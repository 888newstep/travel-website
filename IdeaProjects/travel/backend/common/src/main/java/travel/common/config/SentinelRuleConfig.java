package travel.common.config;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流熔断规则配置
 */
@Slf4j
@Configuration
public class SentinelRuleConfig {

    @PostConstruct
    public void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 景点查询接口限流 - 应对高并发查询
        rules.add(createFlowRule("getAttractionById", RuleConstant.FLOW_GRADE_QPS, 100));
        rules.add(createFlowRule("getAttractionsByCity", RuleConstant.FLOW_GRADE_QPS, 50));
        rules.add(createFlowRule("searchAttractions", RuleConstant.FLOW_GRADE_QPS, 30));

        // 路线规划接口限流 - 保护计算资源
        rules.add(createFlowRule("planOptimalRoute", RuleConstant.FLOW_GRADE_QPS, 10));
        rules.add(createFlowRule("getRouteRecommendations", RuleConstant.FLOW_GRADE_QPS, 20));

        // 用户接口限流
        rules.add(createFlowRule("userLogin", RuleConstant.FLOW_GRADE_QPS, 50));
        rules.add(createFlowRule("userRegister", RuleConstant.FLOW_GRADE_QPS, 20));

        // 收藏接口限流
        rules.add(createFlowRule("collectRoute", RuleConstant.FLOW_GRADE_QPS, 30));

        FlowRuleManager.loadRules(rules);
        log.info("Sentinel 流控规则加载完成，共 {} 条", rules.size());
    }

    @PostConstruct
    public void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 景点查询熔断 - 慢调用比例熔断
        rules.add(createDegradeRule("getAttractionById", RuleConstant.DEGRADE_GRADE_RT, 200, 0.5, 10));
        rules.add(createDegradeRule("getAttractionsByCity", RuleConstant.DEGRADE_GRADE_RT, 300, 0.5, 10));

        // 路线规划熔断 - 异常比例熔断
        rules.add(createDegradeRule("planOptimalRoute", RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO, 0, 0.5, 10));

        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel 熔断规则加载完成，共 {} 条", rules.size());
    }

    private FlowRule createFlowRule(String resource, int grade, double count) {
        FlowRule rule = new FlowRule();
        rule.setResource(resource);
        rule.setGrade(grade);
        rule.setCount(count);
        return rule;
    }

    private DegradeRule createDegradeRule(String resource, int grade, double threshold, double minRequestAmount, int statIntervalMs) {
        DegradeRule rule = new DegradeRule();
        rule.setResource(resource);
        rule.setGrade(grade);
        rule.setCount(threshold);
        rule.setMinRequestAmount((int) minRequestAmount);
        rule.setStatIntervalMs(statIntervalMs * 1000);
        rule.setTimeWindow(10); // 熔断10秒后恢复
        return rule;
    }
}