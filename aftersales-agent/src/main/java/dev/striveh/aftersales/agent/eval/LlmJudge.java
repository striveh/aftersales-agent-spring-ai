package dev.striveh.aftersales.agent.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.striveh.aftersales.agent.plan.AgentTurn;
import dev.striveh.aftersales.agent.runtime.JsonSupport;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/** Optional second pass. It scores prose; it does not decide eligibility. */
public final class LlmJudge {

    private final ChatClient chatClient;
    private final ObjectMapper json = JsonSupport.mapper();

    public LlmJudge(ChatModel model) {
        this.chatClient = ChatClient.builder(model).build();
    }

    public String score(EvalCase evalCase, AgentTurn turn) {
        String question = evalCase.turns().get(evalCase.turns().size() - 1).message();
        String citation = turn.citations().isEmpty() ? "无" : turn.citations().get(0).sectionTitle();
        String prompt = """
                TASK: JUDGE
                你是售后答复评测员。只输出 JSON，不要写别的文字。
                字段：faithfulness、citation、helpfulness，都是 1 到 5 的整数；reason 用一句话。
                5 分表示回答和给出的结论代码、引用条款一致，并且没有承诺已经打款。
                问题：%s
                结论代码：%s
                引用条款：%s
                回答：%s
                """.formatted(question, turn.decision(), citation, turn.answer());
        String content = chatClient.prompt().user(prompt).call().content();
        try {
            JsonNode node = json.readTree(PlanExtract(content));
            return "faithfulness=" + node.path("faithfulness").asInt()
                    + " citation=" + node.path("citation").asInt()
                    + " helpfulness=" + node.path("helpfulness").asInt()
                    + " reason=" + node.path("reason").asText();
        } catch (Exception exception) {
            return "judge-parse-failed: " + content;
        }
    }

    private String PlanExtract(String text) {
        int left = text.indexOf('{');
        int right = text.lastIndexOf('}');
        if (left < 0 || right <= left) {
            return text;
        }
        return text.substring(left, right + 1);
    }
}
