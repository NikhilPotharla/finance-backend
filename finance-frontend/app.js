/* ─────────────────────────────────────────
   Finance Dashboard API Tester — app.js
   ─────────────────────────────────────────*/

let jwt = localStorage.getItem('finance_jwt') || null;
let currentUser = JSON.parse(localStorage.getItem('finance_user') || 'null');

const BASE = () => document.getElementById('api-base').value.replace(/\/$/, '');

// ─── Helpers ──────────────────────────────────────────────────────

async function apiFetch(path, options = {}) {
  const headers = { 'Content-Type': 'application/json', ...(options.headers || {}) };
  if (jwt) headers['Authorization'] = `Bearer ${jwt}`;
  const res = await fetch(BASE() + path, { ...options, headers });
  const text = await res.text();
  let json;
  try { json = JSON.parse(text); } catch { json = text; }
  return { ok: res.ok, status: res.status, data: json };
}

function showResponse(elId, res) {
  const el = document.getElementById(elId);
  if (!el) return;
  const pretty = typeof res.data === 'object'
    ? JSON.stringify(res.data, null, 2)
    : res.data;
  el.textContent = `HTTP ${res.status}\n\n${pretty}`;
  el.className = `response-box ${res.ok ? 'success' : 'error'}`;
}

function fmt(n) {
  if (n === null || n === undefined) return '—';
  return '₹ ' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2 });
}

function toast(msg, type = 'success') {
  const t = document.getElementById('toast');
  t.textContent = msg;
  t.className = `toast ${type}`;
  setTimeout(() => { t.className = 'toast hidden'; }, 3000);
}

// ─── Nav ──────────────────────────────────────────────────────────

document.querySelectorAll('.nav-item').forEach(el => {
  el.addEventListener('click', e => {
    e.preventDefault();
    const sec = el.dataset.section;
    switchSection(sec);
  });
});

function switchSection(sec) {
  document.querySelectorAll('.content-section').forEach(s => s.classList.add('hidden'));
  document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
  document.getElementById(`section-${sec}`).classList.remove('hidden');
  document.getElementById(`nav-${sec}`).classList.add('active');

  const titles = { dashboard: ['Dashboard', 'Finance Analytics Overview'],
                   transactions: ['Transactions', 'CRUD & Filtering'],
                   users: ['User Management', 'Admin Controls'],
                   auth: ['Authentication', 'Register & Login'] };
  const [t, s] = titles[sec] || ['', ''];
  document.getElementById('page-title').textContent = t;
  document.getElementById('page-subtitle').textContent = s;
}

// ─── API Status Check ─────────────────────────────────────────────

async function checkApiStatus() {
  const dot  = document.getElementById('status-dot');
  const text = document.getElementById('status-text');
  try {
    const res = await fetch(BASE() + '/api-docs', { method: 'GET', signal: AbortSignal.timeout(3000) });
    if (res.ok || res.status < 500) {
      dot.className = 'status-dot online';
      text.textContent = 'API Online';
    } else { throw new Error(); }
  } catch {
    dot.className = 'status-dot offline';
    text.textContent = 'API Offline';
  }
}

setInterval(checkApiStatus, 10000);
checkApiStatus();

// ─── Auth ─────────────────────────────────────────────────────────

async function register() {
  const body = {
    name: document.getElementById('reg-name').value,
    email: document.getElementById('reg-email').value,
    password: document.getElementById('reg-password').value,
  };
  const role = document.getElementById('reg-role').value;
  if (role) body.role = role;
  const res = await apiFetch('/api/auth/register', { method: 'POST', body: JSON.stringify(body) });
  showResponse('reg-response', res);
  if (res.ok) toast('✅ Registered! You can now log in.');
  else toast('❌ Registration failed', 'error');
}

async function login() {
  const body = {
    email: document.getElementById('login-email').value,
    password: document.getElementById('login-password').value,
  };
  const res = await apiFetch('/api/auth/login', { method: 'POST', body: JSON.stringify(body) });
  showResponse('login-response', res);
  if (res.ok && res.data.token) {
    jwt = res.data.token;
    currentUser = { email: res.data.email, name: res.data.name, role: res.data.role };
    localStorage.setItem('finance_jwt', jwt);
    localStorage.setItem('finance_user', JSON.stringify(currentUser));
    updateUserBadge();
    document.getElementById('token-display').style.display = 'flex';
    document.getElementById('token-preview').textContent = jwt.slice(0, 40) + '...';
    toast(`✅ Logged in as ${currentUser.role}`);
  } else {
    toast('❌ Login failed', 'error');
  }
}

function logout() {
  jwt = null; currentUser = null;
  localStorage.removeItem('finance_jwt');
  localStorage.removeItem('finance_user');
  updateUserBadge();
  toast('👋 Logged out');
}

function updateUserBadge() {
  const badge = document.getElementById('user-badge');
  const logoutBtn = document.getElementById('btn-logout');
  if (currentUser) {
    badge.innerHTML = `<strong>${currentUser.name}</strong><br/>${currentUser.role}`;
    logoutBtn.style.display = 'block';
  } else {
    badge.textContent = 'Not logged in';
    logoutBtn.style.display = 'none';
  }
}

updateUserBadge();
if (jwt) {
  document.getElementById('token-display').style.display = 'flex';
  document.getElementById('token-preview').textContent = jwt.slice(0, 40) + '...';
}

// ─── Dashboard ────────────────────────────────────────────────────

async function loadSummary() {
  const res = await apiFetch('/api/dashboard/summary');
  if (!res.ok) { toast(`❌ ${res.data?.message || 'Failed to load summary'}`, 'error'); return; }
  const d = res.data;

  document.getElementById('stat-income').textContent = fmt(d.totalIncome);
  document.getElementById('stat-expense').textContent = fmt(d.totalExpenses);
  document.getElementById('stat-balance').textContent = fmt(d.netBalance);
  document.getElementById('stat-txn').textContent = d.totalTransactions;

  // Recent transactions
  const container = document.getElementById('recent-transactions');
  if (!d.recentTransactions || d.recentTransactions.length === 0) {
    container.innerHTML = '<p class="empty-state">No transactions yet</p>';
    return;
  }
  container.innerHTML = buildTxTable(d.recentTransactions);
}

async function loadCategories() {
  const res = await apiFetch('/api/dashboard/categories');
  const container = document.getElementById('categories-container');
  if (!res.ok) { container.innerHTML = `<p class="empty-state" style="color:var(--red)">${res.data?.message || 'Error'}</p>`; return; }
  if (!res.data || res.data.length === 0) { container.innerHTML = '<p class="empty-state">No categories</p>'; return; }

  container.innerHTML = res.data.map(c => {
    const pct = parseFloat(c.percentage) || 0;
    return `<div class="cat-row">
      <span class="cat-name">${c.category}</span>
      <div class="cat-bar-wrap"><div class="cat-bar" style="width:${pct}%"></div></div>
      <span class="cat-pct">${c.percentage}</span>
      <span class="cat-total">${fmt(c.total)}</span>
    </div>`;
  }).join('');
}

async function loadTrends() {
  const res = await apiFetch('/api/dashboard/trends');
  const container = document.getElementById('trends-container');
  if (!res.ok) {
    container.innerHTML = `<p class="empty-state" style="color:var(--red)">${res.data?.message || 'Access denied — ANALYST+ only'}</p>`;
    return;
  }
  if (!res.data || res.data.length === 0) { container.innerHTML = '<p class="empty-state">No trend data</p>'; return; }

  container.innerHTML = `
    <div class="trend-row" style="font-size:.7rem;color:var(--text-muted);font-weight:600;">
      <span>MONTH</span><span style="text-align:right">INCOME</span><span style="text-align:right">EXPENSE</span><span style="text-align:right">NET</span>
    </div>` +
    res.data.map(t => `<div class="trend-row">
      <span class="trend-month">${t.monthName} '${String(t.year).slice(2)}</span>
      <span class="trend-val trend-income">${fmt(t.income)}</span>
      <span class="trend-val trend-expense">${fmt(t.expenses)}</span>
      <span class="trend-val trend-net">${fmt(t.net)}</span>
    </div>`).join('');
}

// Auto-load on tab switch
document.getElementById('nav-dashboard').addEventListener('click', () => {
  setTimeout(loadSummary, 100);
});

// ─── Transactions ─────────────────────────────────────────────────

async function createTransaction() {
  const body = {
    title: document.getElementById('tx-title').value,
    amount: parseFloat(document.getElementById('tx-amount').value),
    type: document.getElementById('tx-type').value,
    category: document.getElementById('tx-category').value,
    date: document.getElementById('tx-date').value,
    description: document.getElementById('tx-description').value,
  };
  const res = await apiFetch('/api/transactions', { method: 'POST', body: JSON.stringify(body) });
  showResponse('create-tx-response', res);
  if (res.ok) toast('✅ Transaction created!');
  else toast(`❌ ${res.data?.message || 'Failed'}`, 'error');
}

async function listTransactions() {
  const params = new URLSearchParams({ page: 0, size: document.getElementById('filter-size').value || 10 });
  const keyword = document.getElementById('filter-keyword').value;
  const type =    document.getElementById('filter-type').value;
  const cat =     document.getElementById('filter-category').value;
  const from =    document.getElementById('filter-from').value;
  const to =      document.getElementById('filter-to').value;
  if (keyword) params.set('keyword', keyword);
  if (type)    params.set('type', type);
  if (cat)     params.set('category', cat);
  if (from)    params.set('dateFrom', from);
  if (to)      params.set('dateTo', to);

  const res = await apiFetch(`/api/transactions?${params}`);
  const container = document.getElementById('tx-table-container');
  const badge = document.getElementById('tx-count-badge');

  if (!res.ok) {
    container.innerHTML = `<p class="empty-state" style="color:var(--red)">${res.data?.message || 'Error loading transactions'}</p>`;
    badge.textContent = '';
    return;
  }
  const page = res.data;
  badge.textContent = `${page.totalElements} found`;
  badge.className = 'badge badge-get';

  if (!page.content || page.content.length === 0) {
    container.innerHTML = '<p class="empty-state">No transactions match the filters</p>';
    return;
  }
  container.innerHTML = buildTxTable(page.content);
}

async function getTransactionById() {
  const id = document.getElementById('tx-op-id').value;
  if (!id) { toast('Enter a transaction ID', 'error'); return; }
  const res = await apiFetch(`/api/transactions/${id}`);
  showResponse('tx-op-response', res);
}

async function deleteTransaction() {
  const id = document.getElementById('tx-op-id').value;
  if (!id) { toast('Enter a transaction ID', 'error'); return; }
  if (!confirm(`Delete transaction #${id}?`)) return;
  const res = await apiFetch(`/api/transactions/${id}`, { method: 'DELETE' });
  const el = document.getElementById('tx-op-response');
  el.textContent = `HTTP ${res.status}\n${res.ok ? '✅ Deleted' : JSON.stringify(res.data, null, 2)}`;
  el.className = `response-box ${res.ok ? 'success' : 'error'}`;
  if (res.ok) toast(`✅ Transaction #${id} deleted`);
  else toast(`❌ Delete failed`, 'error');
}

function openUpdateModal() {
  const id = document.getElementById('tx-op-id').value;
  if (!id) { toast('Enter a transaction ID first', 'error'); return; }
  document.getElementById('update-modal').classList.remove('hidden');
}
function closeUpdateModal() {
  document.getElementById('update-modal').classList.add('hidden');
}

async function updateTransaction() {
  const id = document.getElementById('tx-op-id').value;
  const body = {
    title: document.getElementById('upd-title').value,
    amount: parseFloat(document.getElementById('upd-amount').value),
    type: document.getElementById('upd-type').value,
    category: document.getElementById('upd-category').value,
    date: document.getElementById('upd-date').value,
    description: document.getElementById('upd-description').value,
  };
  const res = await apiFetch(`/api/transactions/${id}`, { method: 'PUT', body: JSON.stringify(body) });
  showResponse('update-response', res);
  if (res.ok) { toast('✅ Transaction updated!'); closeUpdateModal(); }
  else toast('❌ Update failed', 'error');
}

function buildTxTable(items) {
  return `<table>
    <thead>
      <tr>
        <th>ID</th><th>Title</th><th>Amount</th>
        <th>Type</th><th>Category</th><th>Date</th><th>Owner</th>
      </tr>
    </thead>
    <tbody>
      ${items.map(t => `<tr>
        <td>${t.id}</td>
        <td>${t.title}</td>
        <td style="font-family:monospace">${fmt(t.amount)}</td>
        <td><span class="pill pill-${t.type?.toLowerCase()}">${t.type}</span></td>
        <td>${t.category || '—'}</td>
        <td>${t.date}</td>
        <td style="font-size:.75rem;color:var(--text-muted)">${t.ownerEmail || '—'}</td>
      </tr>`).join('')}
    </tbody>
  </table>`;
}

// ─── Users ────────────────────────────────────────────────────────

async function loadUsers() {
  const res = await apiFetch('/api/users');
  const container = document.getElementById('users-table-container');
  if (!res.ok) {
    container.innerHTML = `<p class="empty-state" style="color:var(--red)">${res.data?.message || 'Access denied — ADMIN only'}</p>`;
    return;
  }
  if (!res.data || res.data.length === 0) {
    container.innerHTML = '<p class="empty-state">No users found</p>';
    return;
  }
  container.innerHTML = `<table>
    <thead>
      <tr><th>ID</th><th>Name</th><th>Email</th><th>Role</th><th>Status</th></tr>
    </thead>
    <tbody>
      ${res.data.map(u => `<tr>
        <td>${u.id}</td>
        <td>${u.name}</td>
        <td>${u.email}</td>
        <td><span class="pill pill-${u.role?.toLowerCase()}">${u.role}</span></td>
        <td><span class="pill ${u.active ? 'pill-active' : 'pill-inactive'}">${u.active ? 'Active' : 'Inactive'}</span></td>
      </tr>`).join('')}
    </tbody>
  </table>`;
}

async function updateUserStatus() {
  const id = document.getElementById('status-user-id').value;
  const active = document.getElementById('status-active').value === 'true';
  if (!id) { toast('Enter user ID', 'error'); return; }
  const res = await apiFetch(`/api/users/${id}/status`, { method:'PUT', body: JSON.stringify({ active }) });
  showResponse('status-response', res);
  if (res.ok) toast(`✅ Status updated`);
  else toast('❌ Failed', 'error');
}

async function updateUserRole() {
  const id = document.getElementById('role-user-id').value;
  const role = document.getElementById('role-new').value;
  if (!id) { toast('Enter user ID', 'error'); return; }
  const res = await apiFetch(`/api/users/${id}/role`, { method:'PUT', body: JSON.stringify({ role }) });
  showResponse('role-response', res);
  if (res.ok) toast(`✅ Role updated to ${role}`);
  else toast('❌ Failed', 'error');
}

// ─── Init ─────────────────────────────────────────────────────────

// Set today's date defaults
document.getElementById('tx-date').value = new Date().toISOString().split('T')[0];

// Close modal on overlay click
document.getElementById('update-modal').addEventListener('click', e => {
  if (e.target === e.currentTarget) closeUpdateModal();
});
