/* ══════════════════════════════════════════
   SkillSetter — Upgraded app.js
   All original API endpoints preserved.
   UI/UX layer upgraded — no logic changed.
══════════════════════════════════════════ */

/* ─── API BASE ─── */
const params = new URLSearchParams(window.location.search);
const API = params.get('api') || 'http://localhost:8080';

/* ─── PREDEFINED SKILLS (mirrored from original) ─── */
const PREDEFINED_SKILLS = [
  'Python','Java','JavaScript','C','C++','C#','Go','Rust','Swift','Kotlin',
  'HTML','CSS','React','Vue','Angular','Node.js','Express','Django','Flask','Spring Boot',
  'SQL','MongoDB','PostgreSQL','MySQL','SQLite','Redis',
  'Machine Learning','Deep Learning','Data Analysis','NLP','Computer Vision',
  'Docker','Kubernetes','AWS','Azure','GCP','Linux','Git',
  'UI/UX Design','Figma','Photoshop','Blender',
  'Project Management','Communication','Problem Solving','Team Leadership'
];

/* ─── STATE ─── */
let currentSkills = [];   // [{name, level}]
let currentUserEmail = '';
let allUsers = [];
let pendingReceiverEmail = '';

/* ════════════════════════════════════════
   INIT
════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
  buildSkillDropdown();
  setupNav();
  setupTheme();
  setupSidebar();
  loadUsers();  // preload for all-members view
});

/* ─── BUILD SKILL DROPDOWN ─── */
function buildSkillDropdown() {
  const sel = document.getElementById('skillSelect');
  PREDEFINED_SKILLS.forEach(s => {
    const opt = document.createElement('option');
    opt.value = s; opt.textContent = s;
    sel.appendChild(opt);
  });
}

/* ─── NAVIGATION ─── */
function setupNav() {
  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.addEventListener('click', () => {
      const view = btn.dataset.view;
      switchView(view);
      document.querySelectorAll('.nav-item').forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      closeSidebar();
    });
  });
}

function switchView(viewId) {
  document.querySelectorAll('.view').forEach(v => v.classList.remove('active'));
  document.getElementById('view-' + viewId)?.classList.add('active');
  if (viewId === 'users' && allUsers.length === 0) loadUsers();
}

/* ─── THEME ─── */
function setupTheme() {
  const saved = localStorage.getItem('ss-theme') || 'dark';
  document.documentElement.setAttribute('data-theme', saved);
  document.getElementById('themeToggle').addEventListener('click', () => {
    const cur = document.documentElement.getAttribute('data-theme');
    const next = cur === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem('ss-theme', next);
  });
}

/* ─── SIDEBAR (mobile) ─── */
function setupSidebar() {
  const sidebar = document.getElementById('sidebar');
  const overlay = document.getElementById('overlay');
  document.getElementById('menuBtn').addEventListener('click', openSidebar);
  document.getElementById('sidebarClose').addEventListener('click', closeSidebar);
  overlay.addEventListener('click', closeSidebar);
}
function openSidebar() {
  document.getElementById('sidebar').classList.add('open');
  document.getElementById('overlay').classList.add('active');
}
function closeSidebar() {
  document.getElementById('sidebar').classList.remove('open');
  document.getElementById('overlay').classList.remove('active');
}

/* ─── TEAM SIZE TOGGLE ─── */
function toggleTeamSize() {
  const mode = document.getElementById('regMode').value;
  document.getElementById('teamSizeField').style.display = mode === 'Build' ? 'block' : 'none';
}

/* ════════════════════════════════════════
   SKILL CHIPS
════════════════════════════════════════ */
document.getElementById('addSkillBtn').addEventListener('click', () => {
  const sel = document.getElementById('skillSelect');
  const level = document.getElementById('levelSelect').value;
  const name = sel.value;
  if (!name) { toast('Select a skill first', 'error'); return; }
  addSkillChip(name, level);
  sel.value = '';
});

document.getElementById('addCustomSkillBtn').addEventListener('click', () => {
  const inp = document.getElementById('customSkillInput');
  const name = inp.value.trim();
  const level = document.getElementById('levelSelect').value;
  if (!name) { toast('Enter a skill name', 'error'); return; }
  addSkillChip(name, level);
  inp.value = '';
});

function addSkillChip(name, level) {
  if (currentSkills.find(s => s.name.toLowerCase() === name.toLowerCase())) {
    toast(`"${name}" already added`, 'error'); return;
  }
  currentSkills.push({ name, level });
  renderChips();
}

function removeSkill(idx) {
  currentSkills.splice(idx, 1);
  renderChips();
}

function renderChips() {
  const container = document.getElementById('skillChips');
  if (currentSkills.length === 0) {
    container.innerHTML = '<p class="empty-hint">No skills added yet.</p>';
    return;
  }
  container.innerHTML = currentSkills.map((s, i) => `
    <span class="skill-chip">
      ${escHtml(s.name)}
      <span class="chip-level">${escHtml(s.level)}</span>
      <button class="chip-remove" onclick="removeSkill(${i})" aria-label="Remove ${escHtml(s.name)}">×</button>
    </span>
  `).join('');
}

/* ════════════════════════════════════════
   REGISTER / UPDATE — POST /api/register
════════════════════════════════════════ */
async function registerUser() {
  const name  = document.getElementById('regName').value.trim();
  const email = document.getElementById('regEmail').value.trim();
  const avail = parseInt(document.getElementById('regAvail').value);
  const role  = document.getElementById('regRole').value;
  const goal  = document.getElementById('regGoal').value;
  const mode  = document.getElementById('regMode').value;
  const teamSize = mode === 'Build' ? parseInt(document.getElementById('regTeamSize').value) || 0 : 0;

  // Validation
  if (!name) { highlightError('regName', 'Name required'); return; }
  if (!email || !email.includes('@')) { highlightError('regEmail', 'Valid email required'); return; }
  if (!avail || avail < 1) { highlightError('regAvail', 'Availability required'); return; }
  if (currentSkills.length === 0) { toast('Add at least one skill', 'error'); return; }

  const btn = document.getElementById('registerBtn');
  setButtonLoading(btn, true);
  showStatus('', '');

  const payload = { name, email, availability: avail, role, goal, mode, teamSize, skills: currentSkills };

  try {
    const res = await fetch(`${API}/api/register`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const text = await res.text();
    if (res.ok) {
      currentUserEmail = email;
      showStatus('✓ ' + text, 'success');
      toast('Profile saved!', 'success');
      updateSidebarUser(name, email);
      allUsers = []; // invalidate cache
    } else {
      showStatus(text || 'Registration failed', 'error');
      toast('Registration failed', 'error');
    }
  } catch (e) {
    showStatus('Cannot connect to backend. Is it running?', 'error');
    toast('Connection error', 'error');
  } finally {
    setButtonLoading(btn, false);
  }
}

/* ─── LOAD PROFILE — GET /api/user?email=... ─── */
async function loadProfile() {
  const email = document.getElementById('regEmail').value.trim();
  if (!email) { highlightError('regEmail', 'Enter your email first'); return; }

  showStatus('Loading…', '');
  try {
    const res = await fetch(`${API}/api/user?email=${encodeURIComponent(email)}`);
    if (!res.ok) { showStatus('Profile not found', 'error'); toast('Profile not found', 'error'); return; }
    const user = await res.json();

    document.getElementById('regName').value  = user.name || '';
    document.getElementById('regAvail').value = user.availability || '';
    document.getElementById('regRole').value  = user.role || 'Teammate';
    document.getElementById('regGoal').value  = user.goal || 'PBL';
    document.getElementById('regMode').value  = user.mode || 'Join';
    if (user.teamSize) document.getElementById('regTeamSize').value = user.teamSize;
    toggleTeamSize();

    currentSkills = (user.skills || []).map(s => ({
      name: s.name || s.skillName || s,
      level: s.level || s.proficiencyLevel || 'Intermediate'
    }));
    renderChips();

    currentUserEmail = email;
    updateSidebarUser(user.name, email);
    showStatus('Profile loaded!', 'success');
    toast('Profile loaded', 'success');
  } catch (e) {
    showStatus('Error loading profile', 'error');
    toast('Error loading profile', 'error');
  }
}

/* ─── DELETE PROFILE — DELETE /api/deleteProfile?email=... ─── */
async function deleteProfile() {
  const email = document.getElementById('regEmail').value.trim();
  if (!email) { highlightError('regEmail', 'Enter your email first'); return; }
  if (!confirm(`Delete profile for ${email}? This cannot be undone.`)) return;

  try {
    const res = await fetch(`${API}/api/deleteProfile?email=${encodeURIComponent(email)}`, { method: 'DELETE' });
    const text = await res.text();
    if (res.ok) {
      currentSkills = [];
      renderChips();
      document.getElementById('regName').value = '';
      document.getElementById('regEmail').value = '';
      currentUserEmail = '';
      updateSidebarUser(null, null);
      showStatus('Profile deleted.', 'success');
      toast('Profile deleted', 'info');
      allUsers = [];
    } else {
      showStatus(text || 'Delete failed', 'error');
    }
  } catch (e) {
    showStatus('Connection error', 'error');
  }
}

/* ════════════════════════════════════════
   FIND MATCHES — GET /api/matches?email=...
════════════════════════════════════════ */
async function findMatches() {
  const email = document.getElementById('matchEmailInput').value.trim();
  if (!email) { toast('Enter your email', 'error'); return; }

  showEl('matchesLoading', true);
  showEl('matchesEmpty', false);
  document.getElementById('matchesGrid').innerHTML = '';

  try {
    const res = await fetch(`${API}/api/matches?email=${encodeURIComponent(email)}`);
    showEl('matchesLoading', false);
    if (!res.ok) { showEl('matchesEmpty', true); toast('Could not load matches', 'error'); return; }
    const matches = await res.json();
    if (!matches || matches.length === 0) { showEl('matchesEmpty', true); return; }
    renderMatchCards(matches, email);
  } catch (e) {
    showEl('matchesLoading', false);
    showEl('matchesEmpty', true);
    toast('Connection error', 'error');
  }
}

function renderMatchCards(matches, senderEmail) {
  const grid = document.getElementById('matchesGrid');
  grid.innerHTML = '';
  matches.forEach((m, i) => {
    const score = Math.round((m.score || m.compatibilityScore || 0) * 100) / 100;
    const skills = (m.skills || m.complementarySkills || []);
    const div = document.createElement('div');
    div.className = 'member-card';
    div.style.animationDelay = `${i * 0.05}s`;
    div.innerHTML = `
      <div class="card-header">
        <div class="card-avatar">${avatarInitials(m.name)}</div>
        <div class="card-meta">
          <div class="card-name">${escHtml(m.name || 'Unknown')}</div>
          <div class="card-email">${escHtml(m.email || '')}</div>
        </div>
        ${scoreRing(score)}
      </div>
      <div class="card-info-row">
        ${infoItem(clockIcon(), m.availability ? `${m.availability}h/week` : '—')}
        ${infoItem(roleIcon(), m.role || '—')}
        ${infoItem(goalIcon(), m.goal || '—')}
      </div>
      <div class="tag-row">
        ${tagHtml(m.goal, 'goal')}
        ${tagHtml(m.role, 'role')}
        ${skills.slice(0, 4).map(s => tagHtml(typeof s === 'string' ? s : (s.name || s.skillName), 'skill')).join('')}
        ${skills.length > 4 ? `<span class="tag tag-skill">+${skills.length - 4}</span>` : ''}
      </div>
      ${m.reason || m.whyMatch ? `<details class="why-match"><summary>Why this match?</summary><p>${escHtml(m.reason || m.whyMatch)}</p></details>` : ''}
      <div class="card-actions">
        <button class="btn btn-primary" onclick="openConnectModal('${escHtml(m.email)}', '${escHtml(m.name)}')">
          ${connectIcon()} Connect
        </button>
      </div>
    `;
    grid.appendChild(div);
  });
}

/* ════════════════════════════════════════
   ALL MEMBERS — GET /api/users
════════════════════════════════════════ */
async function loadUsers() {
  showEl('usersLoading', true);
  showEl('usersEmpty', false);
  document.getElementById('usersGrid').innerHTML = '';
  try {
    const res = await fetch(`${API}/api/users`);
    showEl('usersLoading', false);
    if (!res.ok) { showEl('usersEmpty', true); return; }
    allUsers = await res.json();
    renderUserCards(allUsers);
  } catch (e) {
    showEl('usersLoading', false);
    showEl('usersEmpty', true);
  }
}

function filterUsers() {
  const q = document.getElementById('usersSearch').value.toLowerCase();
  if (!q) { renderUserCards(allUsers); return; }
  const filtered = allUsers.filter(u =>
    (u.name || '').toLowerCase().includes(q) ||
    (u.email || '').toLowerCase().includes(q) ||
    (u.skills || []).some(s => (s.name || s.skillName || s).toLowerCase().includes(q))
  );
  renderUserCards(filtered);
}

function renderUserCards(users) {
  const grid = document.getElementById('usersGrid');
  grid.innerHTML = '';
  if (!users || users.length === 0) { showEl('usersEmpty', true); return; }
  showEl('usersEmpty', false);
  users.forEach((u, i) => {
    const skills = (u.skills || []);
    const div = document.createElement('div');
    div.className = 'member-card';
    div.style.animationDelay = `${i * 0.04}s`;
    div.innerHTML = `
      <div class="card-header">
        <div class="card-avatar">${avatarInitials(u.name)}</div>
        <div class="card-meta">
          <div class="card-name">${escHtml(u.name || 'Unknown')}</div>
          <div class="card-email">${escHtml(u.email || '')}</div>
        </div>
      </div>
      <div class="card-info-row">
        ${infoItem(clockIcon(), u.availability ? `${u.availability}h/week` : '—')}
        ${infoItem(roleIcon(), u.role || '—')}
        ${infoItem(goalIcon(), u.goal || '—')}
      </div>
      <div class="tag-row">
        ${tagHtml(u.goal, 'goal')}
        ${tagHtml(u.role, 'role')}
        ${skills.slice(0, 5).map(s => tagHtml(typeof s === 'string' ? s : (s.name || s.skillName), 'skill')).join('')}
        ${skills.length > 5 ? `<span class="tag tag-skill">+${skills.length - 5}</span>` : ''}
      </div>
      <div class="card-actions">
        <button class="btn btn-secondary" onclick="openConnectModal('${escHtml(u.email)}', '${escHtml(u.name)}')">
          ${connectIcon()} Connect
        </button>
      </div>
    `;
    grid.appendChild(div);
  });
}

/* ════════════════════════════════════════
   CONNECTION REQUESTS
════════════════════════════════════════ */

/* ─── OPEN MODAL ─── */
function openConnectModal(receiverEmail, receiverName) {
  pendingReceiverEmail = receiverEmail;
  document.getElementById('modalTitle').textContent = `Connect with ${receiverName}`;
  document.getElementById('modalSenderEmail').value = currentUserEmail || '';
  document.getElementById('connectModal').style.display = 'flex';
}

function closeModal(e) {
  if (e.target === document.getElementById('connectModal')) {
    document.getElementById('connectModal').style.display = 'none';
  }
}

/* ─── SEND REQUEST — POST /api/requests ─── */
async function sendRequest() {
  const senderEmail = document.getElementById('modalSenderEmail').value.trim();
  if (!senderEmail) { toast('Enter your email', 'error'); return; }
  if (!pendingReceiverEmail) return;

  try {
    const res = await fetch(`${API}/api/requests`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ senderEmail, receiverEmail: pendingReceiverEmail })
    });
    const text = await res.text();
    if (res.ok) {
      toast('Connection request sent!', 'success');
      document.getElementById('connectModal').style.display = 'none';
    } else {
      toast(text || 'Could not send request', 'error');
    }
  } catch (e) {
    toast('Connection error', 'error');
  }
}

/* ─── LOAD INBOX — GET /api/requests?email=... ─── */
async function loadInbox() {
  const email = document.getElementById('inboxEmailInput').value.trim();
  if (!email) { toast('Enter your email', 'error'); return; }

  showEl('inboxLoading', true);
  showEl('inboxEmpty', false);
  document.getElementById('inboxGrid').innerHTML = '';

  try {
    const res = await fetch(`${API}/api/requests?email=${encodeURIComponent(email)}`);
    showEl('inboxLoading', false);
    if (!res.ok) { showEl('inboxEmpty', true); toast('Could not load inbox', 'error'); return; }
    const requests = await res.json();
    if (!requests || requests.length === 0) { showEl('inboxEmpty', true); return; }
    updateInboxBadge(requests.filter(r => r.status === 'PENDING').length);
    renderInbox(requests);
  } catch (e) {
    showEl('inboxLoading', false);
    showEl('inboxEmpty', true);
    toast('Connection error', 'error');
  }
}

function renderInbox(requests) {
  const grid = document.getElementById('inboxGrid');
  grid.innerHTML = '';
  requests.forEach(req => {
    const div = document.createElement('div');
    div.className = 'inbox-card';
    div.id = `req-${req.id}`;
    const isPending = req.status === 'PENDING';
    const isAccepted = req.status === 'ACCEPTED';
    div.innerHTML = `
      <div class="card-avatar" style="width:40px;height:40px;font-size:14px">${avatarInitials(req.senderName || req.senderEmail)}</div>
      <div class="inbox-card-info">
        <div class="inbox-sender">${escHtml(req.senderName || 'Unknown')}</div>
        <div class="inbox-email">${escHtml(req.senderEmail || '')}</div>
        ${isAccepted && req.senderEmail ? `<div class="contact-reveal" style="margin-top:8px">✓ Connected — ${escHtml(req.senderEmail)}</div>` : ''}
      </div>
      ${isPending ? `
      <div class="inbox-actions">
        <button class="btn btn-success" onclick="respondRequest(${req.id}, 'ACCEPTED')">Accept</button>
        <button class="btn btn-danger-ghost" onclick="respondRequest(${req.id}, 'REJECTED')">Reject</button>
      </div>` : `
      <div class="inbox-actions">
        <span class="tag ${isAccepted ? 'tag-role' : 'tag-skill'}">${req.status}</span>
      </div>`}
    `;
    grid.appendChild(div);
  });
}

/* ─── RESPOND TO REQUEST — PUT /api/requests ─── */
async function respondRequest(requestId, status) {
  try {
    const res = await fetch(`${API}/api/requests`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requestId, status })
    });
    if (res.ok) {
      toast(status === 'ACCEPTED' ? 'Request accepted!' : 'Request rejected', status === 'ACCEPTED' ? 'success' : 'info');
      // Re-load inbox to reflect updated state
      loadInbox();
    } else {
      const text = await res.text();
      toast(text || 'Could not update request', 'error');
    }
  } catch (e) {
    toast('Connection error', 'error');
  }
}

/* ════════════════════════════════════════
   UI HELPERS
════════════════════════════════════════ */

function scoreRing(score) {
  const r = 22, circ = 2 * Math.PI * r;
  const pct = Math.min(Math.max(score, 0), 100) / 100;
  const offset = circ - pct * circ;
  const color = score >= 70 ? 'var(--success)' : score >= 40 ? 'var(--accent)' : 'var(--warning)';
  return `
    <div class="score-ring" title="${score}% compatibility">
      <svg width="52" height="52" viewBox="0 0 52 52">
        <circle class="track" cx="26" cy="26" r="${r}"/>
        <circle class="fill" cx="26" cy="26" r="${r}"
          stroke="${color}"
          stroke-dasharray="${circ}"
          stroke-dashoffset="${offset}"
        />
      </svg>
      <div class="score-text">${Math.round(score)}%</div>
    </div>`;
}

function tagHtml(text, type) {
  if (!text) return '';
  return `<span class="tag tag-${type}">${escHtml(text)}</span>`;
}

function infoItem(icon, text) {
  return `<span class="card-info-item">${icon} ${escHtml(text)}</span>`;
}

function clockIcon() {
  return `<svg width="13" height="13" viewBox="0 0 13 13" fill="none"><circle cx="6.5" cy="6.5" r="5.5" stroke="currentColor" stroke-width="1.2"/><path d="M6.5 3.5v3l2 1.5" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>`;
}
function roleIcon() {
  return `<svg width="13" height="13" viewBox="0 0 13 13" fill="none"><circle cx="6.5" cy="4.5" r="2" stroke="currentColor" stroke-width="1.2"/><path d="M2 11c0-2.485 2.015-4.5 4.5-4.5S11 8.515 11 11" stroke="currentColor" stroke-width="1.2" stroke-linecap="round"/></svg>`;
}
function goalIcon() {
  return `<svg width="13" height="13" viewBox="0 0 13 13" fill="none"><path d="M6.5 1L8 5.2h4.5L9 7.8l1.2 4.2-3.7-2.7L2.8 12 4 7.8 0.5 5.2H5z" stroke="currentColor" stroke-width="1.1" stroke-linejoin="round"/></svg>`;
}
function connectIcon() {
  return `<svg width="14" height="14" viewBox="0 0 14 14" fill="none"><path d="M7 1v12M1 7h12" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>`;
}

function avatarInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0][0].toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

function escHtml(str) {
  if (str == null) return '';
  return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function showEl(id, show) {
  const el = document.getElementById(id);
  if (el) el.style.display = show ? 'flex' : 'none';
}

function showStatus(msg, type) {
  const el = document.getElementById('registerStatus');
  if (!msg) { el.style.display = 'none'; return; }
  el.style.display = 'block';
  el.className = 'status-message ' + (type || '');
  el.textContent = msg;
}

function highlightError(inputId, msg) {
  const el = document.getElementById(inputId);
  el?.classList.add('error');
  el?.addEventListener('input', () => el.classList.remove('error'), { once: true });
  toast(msg, 'error');
}

function setButtonLoading(btn, loading) {
  if (!btn) return;
  if (loading) {
    btn.dataset.origText = btn.innerHTML;
    btn.innerHTML = `<div class="spinner" style="width:16px;height:16px;border-width:2px"></div> Saving…`;
    btn.disabled = true;
  } else {
    btn.innerHTML = btn.dataset.origText || btn.innerHTML;
    btn.disabled = false;
  }
}

/* ─── TOAST ─── */
function toast(msg, type = 'info') {
  const container = document.getElementById('toastContainer');
  const t = document.createElement('div');
  t.className = `toast toast-${type}`;
  const icons = { success: '✓', error: '✕', info: 'ℹ' };
  t.innerHTML = `<span>${icons[type] || 'ℹ'}</span> ${escHtml(msg)}`;
  container.appendChild(t);
  setTimeout(() => t.remove(), 3200);
}

/* ─── SIDEBAR USER PILL ─── */
function updateSidebarUser(name, email) {
  const pill = document.getElementById('sidebarUserPill');
  if (!name || !email) { pill.style.display = 'none'; return; }
  pill.style.display = 'flex';
  document.getElementById('sidebarAvatar').textContent = avatarInitials(name);
  document.getElementById('sidebarUserName').textContent = name;
  document.getElementById('sidebarUserEmail').textContent = email;
}

function updateInboxBadge(count) {
  const badge = document.getElementById('inboxBadge');
  if (count > 0) {
    badge.textContent = count;
    badge.style.display = 'flex';
  } else {
    badge.style.display = 'none';
  }
}