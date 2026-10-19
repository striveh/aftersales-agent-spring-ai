const customer = document.querySelector("#customer");
const hint = document.querySelector("#hint");
const log = document.querySelector("#log");
const form = document.querySelector("#composer");
const message = document.querySelector("#message");
const error = document.querySelector("#error");
const plan = document.querySelector("#plan");
const citations = document.querySelector("#citations");
const approvalCard = document.querySelector("#approval-card");
const approvalText = document.querySelector("#approval-text");
const mode = document.querySelector("#mode");
const send = document.querySelector("#send");

let conversationId = null;
let approvalId = null;
let customers = [];

customer.addEventListener("change", () => {
  conversationId = null;
  approvalId = null;
  approvalCard.hidden = true;
  log.innerHTML = "";
  const selected = customers.find((item) => item.phone === customer.value);
  hint.textContent = selected ? selected.hint : "";
  if (selected?.sampleQuestion) {
    message.value = selected.sampleQuestion;
  }
});

form.addEventListener("submit", async (event) => {
  event.preventDefault();
  const text = message.value.trim();
  if (!text) {
    showError("先写一句问题。");
    return;
  }
  showError("");
  append("你", text, "user");
  message.value = "";
  send.disabled = true;
  try {
    const body = await post("/api/chat", {
      conversationId,
      message: text,
      customerPhone: customer.value
    });
    render(body);
  } catch (exception) {
    showError(exception.message);
  } finally {
    send.disabled = false;
  }
});

document.querySelector("#approve").addEventListener("click", () => decide(true));
document.querySelector("#reject").addEventListener("click", () => decide(false));

loadCustomers();

async function loadCustomers() {
  const response = await fetch("/api/demo/customers");
  customers = await response.json();
  customer.innerHTML = customers.map((item) => `<option value="${item.phone}">${item.name} · ${item.phone}</option>`).join("");
  customer.dispatchEvent(new Event("change"));
}

async function decide(approved) {
  if (!approvalId) return;
  showError("");
  try {
    const body = await post(`/api/approvals/${approvalId}`, { approved });
    approvalCard.hidden = true;
    approvalId = null;
    append("澄光", body.answer, "agent");
  } catch (exception) {
    showError(exception.message);
  }
}

function render(body) {
  conversationId = body.conversationId;
  mode.textContent = body.mode === "openai" ? "在线模型规划" : "离线确定性规划";
  append("澄光", body.answer, "agent");
  plan.innerHTML = (body.trace || []).map((event) => {
    const status = (event.status || "").toLowerCase();
    return `<li class="${status}"><b>${event.type}</b> ${event.action || ""} ${event.status || ""}<br><span>${escapeHtml(event.detail || "")}</span></li>`;
  }).join("") || "<li class=\"muted\">没有步骤</li>";
  citations.innerHTML = (body.citations || []).map((item) =>
    `<article class="cite"><b>${escapeHtml(item.docTitle)} · ${escapeHtml(item.sectionTitle)}</b><span>${escapeHtml(item.quote || "")}</span></article>`
  ).join("") || "<p class=\"muted\">这条答复没有引用条款。</p>";
  if (body.pendingApproval) {
    approvalId = body.pendingApproval.id;
    approvalCard.hidden = false;
    approvalText.textContent = `${body.pendingApproval.customerName} 的订单 ${body.pendingApproval.orderId}，金额 ¥${body.pendingApproval.amount}。确认后才会记入演示账本。`;
  } else {
    approvalId = null;
    approvalCard.hidden = true;
  }
}

function append(who, text, kind) {
  const node = document.createElement("article");
  node.className = `bubble ${kind}`;
  node.innerHTML = `<strong>${who}</strong>${escapeHtml(text)}`;
  log.appendChild(node);
  log.scrollTop = log.scrollHeight;
}

async function post(url, payload) {
  const response = await fetch(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload)
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(body.message || "请求失败");
  }
  return body;
}

function showError(text) {
  error.hidden = !text;
  error.textContent = text;
}

function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    "\"": "&quot;",
    "'": "&#39;"
  }[char]));
}
