package dev.striveh.aftersales.agent.answer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

/**
 * Optional prose pass. The decision and the citation objects stay with the
 * deterministic composer; this only rewrites the wording.
 */
public final class AnswerPolisher {

    private static final Logger log = LoggerFactory.getLogger(AnswerPolisher.class);

    private final ChatClient chatClient;

    public AnswerPolisher(ChatModel model) {
        this.chatClient = ChatClient.builder(model).build();
    }

    public String polish(String question, String draft, String decision) {
        try {
            String polished = chatClient.prompt().user("""
                    把下面的售后答复改写得更自然。不得改变结论代码对应的资格，不得声称已经打款或退款已到账。
                    必须保留原答复里的订单号、金额，以及《制度名》「条款名」。
                    只输出答复正文。
                    结论代码：%s
                    用户问题：%s
                    原答复：%s
                    """.formatted(decision, question, draft)).call().content();
            if (polished == null || polished.isBlank()) {
                return draft;
            }
            return polished.trim();
        } catch (RuntimeException exception) {
            log.warn("答复改写失败，保留规则稿。{}", exception.getMessage());
            return draft;
        }
    }
}
