const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => [...document.querySelectorAll(selector)];
const state = { userId: localStorage.getItem("paylendUserId") || "", users: [], loans: [], transactions: [], config: { maxLoanAmount: 10000, flatInterestRate: 10 } };
const money = (amount) => new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR", maximumFractionDigits: 2 }).format(Number(amount || 0));
const dateText = (value) => value ? value.replace("T", " ") : "—";
const activeUser = () => state.users.find(user => user.userId === state.userId);
const esc = (value) => String(value ?? "").replace(/[&<>"']/g, ch => ({ "&":"&amp;", "<":"&lt;", ">":"&gt;", '"':"&quot;", "'":"&#39;" }[ch]));

async function api(path, options = {}) {
  const response = await fetch(`/api${path}`, {
    ...options,
    headers: { "Content-Type": "application/json", ...(options.headers || {}) }
  });
  const body = response.status === 204 ? null : await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(body?.error || body?.message || "Something went wrong. Please try again.");
  return body;
}
function showNotice(message, kind = "success") {
  const box = $("#notice");
  box.textContent = message;
  box.className = `notice ${kind}`;
  box.hidden = false;
  clearTimeout(showNotice.timer);
  showNotice.timer = setTimeout(() => box.hidden = true, 5200);
}
function requireUser() {
  if (!activeUser()) { showNotice("Register a user and select a wallet before doing that.", "error"); return false; }
  return true;
}
function currentView(view) {
  $$(".view").forEach(el => el.hidden = el.id !== `view-${view}`);
  $$(".nav-link").forEach(el => el.classList.toggle("active", el.dataset.view === view));
  const names = { overview:"Overview", wallet:"Wallet actions", loans:"Micro-loans", transactions:"Transactions" };
  $("#page-title").textContent = names[view] || "Overview";
  if (view === "transactions") renderTransactions();
  if (view === "loans") renderLoans();
}
function setUserOptions() {
  const select = $("#current-user");
  if (!state.users.length) {
    select.innerHTML = '<option value="">Register a user first</option>';
    state.userId = "";
  } else {
    if (!state.users.some(u => u.userId === state.userId)) state.userId = state.users[0].userId;
    select.innerHTML = state.users.map(u => `<option value="${esc(u.userId)}">${esc(u.name)} · ${esc(u.mobileNumber)}</option>`).join("");
    select.value = state.userId;
  }
  localStorage.setItem("paylendUserId", state.userId);
  const receiver = $("#receiver");
  const recipients = state.users.filter(u => u.userId !== state.userId);
  receiver.innerHTML = recipients.length
    ? recipients.map(u => `<option value="${esc(u.userId)}">${esc(u.name)} · ${esc(u.mobileNumber)}</option>`).join("")
    : '<option value="">Register another user first</option>';
  receiver.disabled = recipients.length === 0;
}
async function refreshAll() {
  const [users, config] = await Promise.all([api("/users"), api("/config")]);
  state.users = users;
  state.config = config;
  setUserOptions();
  $("#loan-limit").textContent = money(config.maxLoanAmount);
  $("#loan-rate").textContent = `${config.flatInterestRate}%`;
  if (state.userId) {
    [state.loans, state.transactions] = await Promise.all([
      api(`/loans?userId=${encodeURIComponent(state.userId)}`),
      api(`/transactions?userId=${encodeURIComponent(state.userId)}`)
    ]);
  } else { state.loans = []; state.transactions = []; }
  renderOverview();
  renderLoans();
  renderTransactions();
}
function renderOverview() {
  const user = activeUser();
  const activeLoans = state.loans.filter(l => l.remainingBalance > 0);
  $("#stat-balance").textContent = money(user?.balance || 0);
  $("#stat-loan").textContent = money(activeLoans.reduce((sum, loan) => sum + loan.remainingBalance, 0));
  $("#stat-transactions").textContent = state.transactions.length;
  $("#today-label").textContent = user ? `WALLET · ${user.walletId}` : "PERSONAL WALLET";
  const recent = state.transactions.slice(0, 5);
  $("#recent-list").innerHTML = recent.length ? recent.map(t => `
    <div class="activity-item">
      <span class="activity-symbol">${symbol(t.type)}</span>
      <div class="activity-copy"><strong>${esc(t.description || label(t.type))}</strong><small>${esc(dateText(t.dateTime))}</small></div>
      <span class="activity-amount ${isCredit(t.type) ? "positive" : "negative"}">${isCredit(t.type) ? "+" : "−"}${money(t.amount)}</span>
    </div>`).join("") : '<div class="empty-state">No activity yet. Add money or try a wallet action.</div>';
}
function symbol(type) {
  return ({ ADD_MONEY:"＋", TRANSFER_SENT:"↗", TRANSFER_RECEIVED:"↙", MOBILE_RECHARGE:"▣", LOAN_DISBURSEMENT:"₹", LOAN_REPAYMENT:"↘", REGISTRATION:"✦" })[type] || "•";
}
function label(type) {
  return ({ ADD_MONEY:"Money added", TRANSFER_SENT:"Transfer sent", TRANSFER_RECEIVED:"Transfer received", MOBILE_RECHARGE:"Mobile recharge", LOAN_DISBURSEMENT:"Loan disbursed", LOAN_REPAYMENT:"Loan repayment", REGISTRATION:"Wallet created" })[type] || type;
}
function isCredit(type) { return ["ADD_MONEY", "TRANSFER_RECEIVED", "LOAN_DISBURSEMENT"].includes(type); }
function renderTransactions() {
  const list = state.transactions;
  $("#history-count").textContent = `${list.length} record${list.length === 1 ? "" : "s"}`;
  $("#history-caption").textContent = activeUser() ? `Activity for ${activeUser().name} · Most recent first` : "Register a user to view activity";
  $("#transaction-rows").innerHTML = list.length ? list.map(t => `
    <tr><td><div class="txn-name">${esc(label(t.type))}</div><div class="txn-id">${esc(t.transactionId)}</div></td>
    <td>${esc(t.description || "—")}</td><td>${esc(dateText(t.dateTime))}</td>
    <td><span class="status-tag">${esc(t.status || "SUCCESS")}</span></td>
    <td class="align-right"><strong class="${isCredit(t.type) ? "positive" : "negative"}">${isCredit(t.type) ? "+" : (t.type === "REGISTRATION" ? "" : "−")}${money(t.amount)}</strong></td></tr>`).join("")
    : '<tr><td colspan="5" class="empty-state">No transactions yet for this wallet.</td></tr>';
}
function renderLoans() {
  const list = state.loans;
  $("#loans-list").innerHTML = list.length ? list.map(l => `
    <div class="loan-row"><div><strong>${esc(l.loanId)}</strong><small>Requested ${esc(dateText(l.createdAt))}</small></div>
    <div><small>PRINCIPAL</small><strong>${money(l.principalAmount)}</strong></div>
    <div><small>REMAINING</small><strong>${money(l.remainingBalance)}</strong></div>
    <span class="status ${l.status === "PAID" ? "paid" : ""}">${esc(l.status)}</span></div>`).join("") : '<div class="empty-state">No loans yet. Request a micro-loan to see it here.</div>';
}
async function runAction(action, successMessage) {
  if (!requireUser()) return;
  try { await action(); await refreshAll(); showNotice(successMessage); }
  catch (error) { showNotice(error.message, "error"); }
}
$("#register-form").addEventListener("submit", async event => {
  event.preventDefault();
  const form = new FormData(event.currentTarget);
  try {
    const user = await api("/users", { method:"POST", body:JSON.stringify({ name:form.get("name"), mobileNumber:form.get("mobileNumber") }) });
    state.userId = user.userId;
    event.currentTarget.reset();
    await refreshAll();
    showNotice(`Wallet created for ${user.name}. You can now add money.`);
  } catch (error) { showNotice(error.message, "error"); }
});
$("#current-user").addEventListener("change", async event => {
  state.userId = event.target.value;
  localStorage.setItem("paylendUserId", state.userId);
  await refreshAll();
});
$("#add-form").addEventListener("submit", event => {
  event.preventDefault();
  runAction(async () => {
    await api("/wallet/add", { method:"POST", body:JSON.stringify({ userId:state.userId, amount:Number($("#add-amount").value) }) });
    event.target.reset();
  }, "Money added to your demo wallet.");
});
$("#transfer-form").addEventListener("submit", event => {
  event.preventDefault();
  runAction(async () => {
    await api("/wallet/transfer", { method:"POST", body:JSON.stringify({ userId:state.userId, receiverId:$("#receiver").value, amount:Number($("#transfer-amount").value) }) });
    event.target.reset();
  }, "Transfer completed successfully.");
});
$("#recharge-form").addEventListener("submit", event => {
  event.preventDefault();
  runAction(async () => {
    await api("/wallet/recharge", { method:"POST", body:JSON.stringify({ userId:state.userId, mobileNumber:$("#recharge-mobile").value, operator:$("#operator").value, amount:Number($("#recharge-amount").value) }) });
    event.target.reset();
  }, "Recharge recorded. This is a simulated transaction.");
});
$("#loan-form").addEventListener("submit", event => {
  event.preventDefault();
  runAction(async () => {
    await api("/loans", { method:"POST", body:JSON.stringify({ userId:state.userId, amount:Number($("#loan-amount").value) }) });
    event.target.reset(); updateEstimate();
  }, "Loan created and demo funds added to your wallet.");
});
$("#repay-form").addEventListener("submit", event => {
  event.preventDefault();
  runAction(async () => {
    await api("/loans/repay", { method:"POST", body:JSON.stringify({ userId:state.userId, amount:Number($("#repay-amount").value) }) });
    event.target.reset();
  }, "Loan repayment recorded.");
});
function updateEstimate() {
  const amount = Math.max(0, Number($("#loan-amount").value || 0));
  const interest = amount * state.config.flatInterestRate / 100;
  $("#estimated-interest").textContent = money(interest);
  $("#estimated-total").textContent = money(amount + interest);
}
$("#loan-amount").addEventListener("input", updateEstimate);
$$("[data-view]").forEach(button => button.addEventListener("click", () => currentView(button.dataset.view)));
$$("[data-go]").forEach(button => button.addEventListener("click", () => {
  currentView(button.dataset.go);
  if (button.dataset.action) setTimeout(() => document.getElementById(`${button.dataset.action}-card`)?.scrollIntoView({ behavior:"smooth", block:"center" }), 60);
}));
async function start() {
  $("#today-label").textContent = new Date().toLocaleDateString("en-IN", { day:"2-digit", month:"short", year:"numeric" }).toUpperCase();
  try { await refreshAll(); updateEstimate(); }
  catch (error) { showNotice(`Could not connect to the backend: ${error.message}. Check that Spring Boot is running.`, "error"); }
}
start();
