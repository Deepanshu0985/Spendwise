package com.finance.application.assistant.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.finance.application.ai.ToolSpec;
import com.finance.application.ai.UntrustedText;
import com.finance.application.assistant.AssistantTool;
import com.finance.application.assistant.ToolArguments;
import com.finance.application.assistant.ToolResult;
import com.finance.application.goal.GoalService;
import com.finance.application.goal.GoalView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
public class GoalStatusTool implements AssistantTool {

    private final GoalService goalService;
    private final ObjectMapper mapper;

    public GoalStatusTool(GoalService goalService, ObjectMapper mapper) {
        this.goalService = goalService;
        this.mapper = mapper;
    }

    @Override
    public ToolSpec spec() {
        return new ToolSpec("goal_status",
                "The user's savings goals with target, saved so far, remaining, percent complete and target date.",
                ToolSchemas.object(ToolSchemas.props("name", ToolSchemas.text("Only goals whose name contains this text. Omit for all goals."))));
    }

    @Override
    public ToolResult execute(UUID userId, String argumentsJson) {
        String name = ToolArguments.parse(mapper, argumentsJson, "name").optionalText("name", 100);
        List<GoalView> goals = goalService.list(userId).stream()
                .filter(view -> name == null || view.goal().getName().toLowerCase(Locale.ROOT).contains(name.toLowerCase(Locale.ROOT)))
                .toList();
        ObjectNode out = ToolOutput.object();
        ArrayNode list = out.putArray("goals");
        for (GoalView view : goals) {
            ObjectNode node = list.addObject();
            node.put("name", UntrustedText.of(view.goal().getName()));
            node.put("currency", view.goal().getCurrency());
            node.put("target", ToolOutput.money(view.goal().getTargetAmount()));
            node.put("saved", ToolOutput.money(view.goal().getCurrentAmount()));
            node.put("remaining", ToolOutput.money(view.progress().remaining()));
            node.put("percentComplete", view.progress().percentComplete());
            node.put("status", view.goal().getStatus().name());
            if (view.goal().getTargetDate() != null) {
                node.put("targetDate", view.goal().getTargetDate().toString());
            }
            if (view.progress().requiredPerMonth() != null) {
                node.put("requiredPerMonth", ToolOutput.money(view.progress().requiredPerMonth()));
            }
            node.put("overdue", view.progress().overdue());
        }
        if (goals.isEmpty()) {
            out.put("note", "No goals found.");
        }
        return new ToolResult(out, "Goal status");
    }
}
