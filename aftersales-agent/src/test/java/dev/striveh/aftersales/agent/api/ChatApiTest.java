package dev.striveh.aftersales.agent.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import dev.striveh.aftersales.bootstrap.AgentApplication;

@SpringBootTest(classes = AgentApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void chatReturnsDecisionCitationsAndTrace() throws Exception {
        mvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"我上周买的耳机能退吗","customerPhone":"13800001001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.decision").value("RETURN_NO_REASON"))
                .andExpect(jsonPath("$.orderId").value("ORD-DEMO-1001"))
                .andExpect(jsonPath("$.citations[0].sectionTitle").value("七天无理由退货"))
                .andExpect(jsonPath("$.pendingApproval").doesNotExist())
                .andExpect(jsonPath("$.trace[0].type").value("PLAN"))
                .andExpect(jsonPath("$.mode").value("stub"));
    }

    @Test
    void blankMessageIsRejected() throws Exception {
        mvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refundStaysPendingUntilApproved() throws Exception {
        MvcResult created = mvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"上周的耳机帮我退款","customerPhone":"13800001001"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pendingApproval.status").value("PENDING"))
                .andReturn();
        String body = created.getResponse().getContentAsString();
        String approvalId = body.replaceAll("(?s).*\"id\"\\s*:\\s*\"(APR-[^\"]+)\".*", "$1");
        String conversationId = body.replaceAll("(?s).*\"conversationId\"\\s*:\\s*\"([^\"]+)\".*", "$1");

        mvc.perform(post("/api/approvals/" + approvalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("不会真实出账")));

        mvc.perform(post("/api/approvals/" + approvalId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value(org.hamcrest.Matchers.containsString("已经处理过")));

        mvc.perform(get("/api/traces/" + conversationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type=='APPROVAL')]").exists());
    }

    @Test
    void unknownApprovalIsNotFound() throws Exception {
        mvc.perform(post("/api/approvals/APR-missing")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"approved\":false}"))
                .andExpect(status().isNotFound());
    }
}
