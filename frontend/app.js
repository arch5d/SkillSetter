const apiFromQuery = new URLSearchParams(window.location.search).get('api');
const API_BASE = window.localStorage.getItem('skillsetter_api_base') || apiFromQuery || 'http://localhost:8080';
const API_URL = `${API_BASE}/api`;
const SESSION_KEY = 'skillsetter_current_user';
const OPTIMISTIC_SENT_KEY = 'skillsetter_optimistic_sent';
const ACTIVE_TAB_KEY = 'skillsetter_active_tab';
const DEBUG = true;

let currentUser = null;
let currentSkills = [];
let currentIncomingRequests = [];
let currentTab = window.localStorage.getItem(ACTIVE_TAB_KEY) || 'matches';

function debugLog(message, details) {
    if (!DEBUG) {
        return;
    }

    const timestamp = new Date().toISOString();
    if (details !== undefined) {
        console.log(`[SkillSetter][${timestamp}] ${message}`, details);
    } else {
        console.log(`[SkillSetter][${timestamp}] ${message}`);
    }
}

function getOptimisticSentRequests() {
    try {
        const raw = window.localStorage.getItem(OPTIMISTIC_SENT_KEY);
        if (!raw) {
            return [];
        }
        const parsed = JSON.parse(raw);
        return Array.isArray(parsed) ? parsed : [];
    } catch (error) {
        debugLog('Failed parsing optimistic sent cache', { error: error.message });
        return [];
    }
}

function setOptimisticSentRequests(requests) {
    window.localStorage.setItem(OPTIMISTIC_SENT_KEY, JSON.stringify(requests));
}

function upsertOptimisticSentRequest(entry) {
    const existing = getOptimisticSentRequests();
    const withoutSame = existing.filter(r =>
        !(r.senderEmail === entry.senderEmail && r.receiverEmail === entry.receiverEmail)
    );
    withoutSame.push(entry);
    setOptimisticSentRequests(withoutSame);
}

function pruneOptimisticSentRequests(serverSentRequests) {
    const existing = getOptimisticSentRequests();
    if (!existing.length) {
        return;
    }

    const keep = existing.filter(localReq => {
        return !serverSentRequests.some(serverReq =>
            serverReq.receiverEmail === localReq.receiverEmail &&
            serverReq.status === localReq.status
        );
    });

    setOptimisticSentRequests(keep);
}

// UI State Management
function showLogin(isLogin) {
    document.getElementById('login-form').classList.toggle('hidden', !isLogin);
    document.getElementById('register-form').classList.toggle('hidden', isLogin);
    
    const tabs = document.querySelectorAll('.tab');
    tabs[0].classList.toggle('active', isLogin);
    tabs[1].classList.toggle('active', !isLogin);
}

function toggleTeamSize() {
    const role = document.getElementById('reg-role').value;
    document.getElementById('team-size-group').classList.toggle('hidden', role !== 'LEADER');
}

// Skills Management
function addSkill() {
    const nameInput = document.getElementById('skill-name');
    const levelInput = document.getElementById('skill-level');
    const name = nameInput.value.trim();
    
    if (name) {
        currentSkills.push({ name, level: levelInput.value });
        renderSkills();
        nameInput.value = '';
    }
}

function removeSkill(index) {
    currentSkills.splice(index, 1);
    renderSkills();
}

function renderSkills() {
    const container = document.getElementById('skill-list');
    container.innerHTML = currentSkills.map((s, i) => `
        <li class="skill-tag">
            ${s.name} (${s.level})
            <button onclick="removeSkill(${i})">&times;</button>
        </li>
    `).join('');
}

// Authentication
async function login() {
    const email = document.getElementById('login-email').value.trim();
    if (!email) {
        document.getElementById('login-error').innerText = 'Email is required';
        return;
    }

    try {
        debugLog('Login attempt', { email });
        const response = await fetch(`${API_URL}/user?email=${encodeURIComponent(email)}`);
        if (!response.ok) {
            document.getElementById('login-error').innerText = 'User not found. Try registering.';
            return;
        }

        const user = await response.json();
        if (user) {
            currentUser = user;
            window.localStorage.setItem(SESSION_KEY, JSON.stringify(currentUser));
            debugLog('Login success', { email: currentUser.email });
            showDashboard();
        } else {
            document.getElementById('login-error').innerText = 'User not found. Try registering.';
        }
    } catch (e) {
        debugLog('Login failed', { error: e.message });
        document.getElementById('login-error').innerText = 'Error connecting to server. Is it running?';
    }
}

async function register() {
    const name = document.getElementById('reg-name').value;
    const email = document.getElementById('reg-email').value;
    const availability = document.getElementById('reg-availability').value;
    const role = document.getElementById('reg-role').value;
    const goal = document.getElementById('reg-goal').value;
    const mode = document.getElementById('reg-mode').value;
    const teamSize = document.getElementById('reg-teamsize').value;

    if (!name || !email || !availability) {
        document.getElementById('reg-error').innerText = 'Please fill out all core fields.';
        return;
    }

    const payload = {
        name,
        email,
        availability: parseInt(availability),
        role,
        goal,
        mode,
        teamSize: role === 'LEADER' && teamSize ? parseInt(teamSize) : null,
        skills: currentSkills
    };

    try {
        const response = await fetch(`${API_URL}/register`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (response.ok) {
            const me = await fetch(`${API_URL}/user?email=${encodeURIComponent(email)}`);
            currentUser = await me.json();
            window.localStorage.setItem(SESSION_KEY, JSON.stringify(currentUser));
            debugLog('Registration success', { email: currentUser.email });
            showDashboard();
        } else {
            const err = await response.json().catch(() => ({}));
            document.getElementById('reg-error').innerText = err.error || 'Registration failed.';
        }
    } catch (e) {
        debugLog('Registration failed', { error: e.message });
        document.getElementById('reg-error').innerText = 'Error connecting to server.';
    }
}

function logout() {
    debugLog('Logout called', { currentUser: currentUser ? currentUser.email : null });
    console.trace('[SkillSetter] Logout trace');
    currentUser = null;
    currentSkills = [];
    currentIncomingRequests = [];
    currentTab = 'matches';
    window.localStorage.removeItem(SESSION_KEY);
    window.localStorage.removeItem(ACTIVE_TAB_KEY);
    document.getElementById('nav-actions').classList.add('hidden');
    document.getElementById('auth-section').classList.remove('hidden');
    document.getElementById('dashboard-section').classList.add('hidden');
    document.getElementById('login-email').value = '';
}

// Dashboard & Matching
function showDashboard() {
    document.getElementById('auth-section').classList.add('hidden');
    document.getElementById('dashboard-section').classList.remove('hidden');
    document.getElementById('nav-actions').classList.remove('hidden');
    
    document.getElementById('user-info').innerText = `${currentUser.name} | ${currentUser.role} | Mode: ${currentUser.mode}`;
    
    showTab(currentTab === 'requests' ? 'requests' : 'matches');
}

function showTab(tab) {
    currentTab = tab === 'requests' ? 'requests' : 'matches';
    window.localStorage.setItem(ACTIVE_TAB_KEY, currentTab);

    if (tab === 'matches') {
        document.getElementById('matches-container').classList.remove('hidden');
        document.getElementById('requests-container').classList.add('hidden');
        document.getElementById('btn-matches').className = 'btn-primary';
        document.getElementById('btn-requests').className = 'btn-secondary';
        
        const subtitle = currentUser.mode === 'JOIN' 
            ? 'Here are the best teams/leaders looking for your skills.'
            : 'Here are the best candidates to join your team.';
        document.getElementById('dashboard-subtitle').innerText = subtitle;
        loadMatches();
    } else {
        document.getElementById('matches-container').classList.add('hidden');
        document.getElementById('requests-container').classList.remove('hidden');
        document.getElementById('btn-matches').className = 'btn-secondary';
        document.getElementById('btn-requests').className = 'btn-primary';
        
        document.getElementById('dashboard-subtitle').innerText = 'Incoming connection requests from other users.';
        loadRequests();
    }
}

async function loadMatches() {
    const container = document.getElementById('matches-container');
    container.innerHTML = '<p>Loading matches...</p>';

    try {
        debugLog('Loading matches', { user: currentUser ? currentUser.email : null });
        const response = await fetch(`${API_URL}/matches?email=${encodeURIComponent(currentUser.email)}`);
        const matches = await response.json();
        
        if (matches.length === 0) {
            container.innerHTML = '<p>No matches found yet. Check back later!</p>';
            return;
        }

        container.innerHTML = matches.map(match => `
            <div class="match-card">
                <div class="match-header">
                    <div>
                        <div class="match-name">${match.user.name}</div>
                        <div class="match-role">${match.user.role} | ${match.user.goal}</div>
                    </div>
                    <div style="text-align: right">
                        <div class="match-score">${match.score}%</div>
                        <div class="match-score-label">Compatibility</div>
                    </div>
                </div>
                
                <div class="match-info">
                    Availability: ${match.user.availability} hrs/week
                    <br/><br/>
                    <strong>Complementary Skills (What they bring to YOU):</strong><br/>
                    <span style="color: var(--primary)">${match.complementarySkills && match.complementarySkills.length > 0 ? match.complementarySkills.join(', ') : 'None'}</span>
                    <br/><br/>
                    <strong>All their Skills:</strong><br/>
                    ${match.user.skills.map(s => s.name).join(', ')}
                </div>

                <div class="match-reasons">
                    <strong>Why it's a match:</strong>
                    ${match.reason}
                </div>

                <div class="match-actions">
                    <button type="button" class="btn-secondary" onclick="ignoreMatch(this)">Hide</button>
                    <button type="button" class="btn-primary" onclick='return sendRequest(event, ${JSON.stringify(match.user.email)}, ${JSON.stringify(match.user.name)}, this)'>Connect</button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        debugLog('Loading matches failed', { error: e.message });
        container.innerHTML = '<p class="error-msg">Error loading matches.</p>';
    }
}

async function sendRequest(evt, receiverEmail, receiverName, btn) {
    if (evt) {
        evt.preventDefault();
        evt.stopPropagation();
    }

    btn.innerText = 'Sending...';
    btn.disabled = true;
    try {
        debugLog('Send request start', { sender: currentUser ? currentUser.email : null, receiverEmail, receiverName });
        await fetch(`${API_URL}/requests`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ sender: currentUser.email, receiver: receiverEmail })
        }).then(async (res) => {
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.error || 'Unable to send request');
            }
        });

        if (currentUser && currentUser.email) {
            upsertOptimisticSentRequest({
                id: `local-${Date.now()}`,
                senderEmail: currentUser.email,
                receiverEmail,
                receiverName,
                status: 'PENDING'
            });
        }

        btn.innerText = 'Request Sent';
        btn.classList.replace('btn-primary', 'btn-secondary');
        debugLog('Send request success', { receiverEmail });
    } catch (e) {
        debugLog('Send request failed', { error: e.message, receiverEmail });
        btn.innerText = e.message;
        btn.disabled = false;
    }

    return false;
}

async function loadRequests() {
    const container = document.getElementById('requests-container');
    container.innerHTML = '<p>Loading requests...</p>';

    try {
        debugLog('Loading requests', { user: currentUser ? currentUser.email : null });
        const response = await fetch(`${API_URL}/requests?email=${encodeURIComponent(currentUser.email)}`);
        const data = await response.json();
        const incoming = Array.isArray(data) ? data : (data.incoming || []);
        const sent = Array.isArray(data) ? [] : (data.sent || []);
        const optimisticSent = getOptimisticSentRequests().filter(r => currentUser && r.senderEmail === currentUser.email);
        const mergedSent = [...sent];
        optimisticSent.forEach(localReq => {
            const existsOnServer = mergedSent.some(serverReq => serverReq.receiverEmail === localReq.receiverEmail && serverReq.status === localReq.status);
            if (!existsOnServer) {
                mergedSent.push(localReq);
            }
        });

        pruneOptimisticSentRequests(sent);

        currentIncomingRequests = incoming;
        debugLog('Requests fetched', { incoming: incoming.length, sent: sent.length, mergedSent: mergedSent.length });

        if (incoming.length === 0 && mergedSent.length === 0) {
            container.innerHTML = '<p>No requests yet.</p>';
            return;
        }

        const incomingHtml = incoming.map(req => `
            <div class="match-card">
                <div class="match-header">
                    <div>
                        <div class="match-name">${req.senderName || req.senderEmail}</div>
                        <div class="match-role">Incoming | Status: ${req.status}</div>
                    </div>
                </div>
                ${req.status === 'ACCEPTED' ? `
                <div class="match-info">
                    <strong>Contact Details:</strong><br/>
                    Name: ${req.senderName || req.senderEmail}<br/>
                    Email: <a href="mailto:${req.senderEmail}">${req.senderEmail}</a>
                </div>
                ` : ''}
                ${req.status === 'PENDING' ? `
                <div class="match-actions">
                    <button type="button" class="btn-secondary" onclick="return updateRequest(event, ${req.id}, 'REJECTED')">Decline</button>
                    <button type="button" class="btn-primary" onclick="return updateRequest(event, ${req.id}, 'ACCEPTED')">Accept</button>
                </div>
                ` : ''}
            </div>
        `).join('');

        const sentHtml = mergedSent.map(req => `
            <div class="match-card">
                <div class="match-header">
                    <div>
                        <div class="match-name">${req.receiverName || req.receiverEmail}</div>
                        <div class="match-role">Sent | Status: ${req.status}</div>
                    </div>
                </div>
                <div class="match-info">
                    <strong>Recipient:</strong><br/>
                    Name: ${req.receiverName || req.receiverEmail}<br/>
                    Email: <a href="mailto:${req.receiverEmail}">${req.receiverEmail}</a>
                </div>
                ${req.status === 'ACCEPTED' ? `
                <div class="match-info">
                    <strong>Result:</strong><br/>
                    Your request was accepted. You can now contact them.
                </div>
                ` : ''}
                ${req.status === 'REJECTED' ? `
                <div class="match-info">
                    <strong>Result:</strong><br/>
                    Your request was declined.
                </div>
                ` : ''}
            </div>
        `).join('');

        container.innerHTML = `
            <h3 style="margin-bottom: 0.8rem;">Incoming Requests</h3>
            ${incoming.length ? incomingHtml : '<p>No incoming requests.</p>'}
            <h3 style="margin: 1.4rem 0 0.8rem;">Sent Requests</h3>
            ${mergedSent.length ? sentHtml : '<p>No sent requests.</p>'}
        `;
    } catch (e) {
        debugLog('Loading requests failed', { error: e.message });
        container.innerHTML = '<p class="error-msg">Error loading requests.</p>';
    }
}

async function updateRequest(evt, id, status) {
    if (evt) {
        evt.preventDefault();
        evt.stopPropagation();
    }

    try {
        debugLog('Update request', { id, status, actor: currentUser ? currentUser.email : null });
        const response = await fetch(`${API_URL}/requests`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ id, status, actor: currentUser.email })
        });

        if (!response.ok) {
            const err = await response.json().catch(() => ({}));
            throw new Error(err.error || 'Unable to update request');
        }

        const selectedRequest = currentIncomingRequests.find(r => Number(r.id) === Number(id));

        await loadRequests();
        showTab('requests');

        if (status === 'ACCEPTED') {
            const subtitle = document.getElementById('dashboard-subtitle');
            if (selectedRequest && subtitle) {
                subtitle.innerText = `Accepted ${selectedRequest.senderName || selectedRequest.senderEmail}. Contact details are now visible in Incoming Requests.`;
            }
        } else if (status === 'REJECTED') {
            const subtitle = document.getElementById('dashboard-subtitle');
            if (subtitle) {
                subtitle.innerText = 'Request declined successfully.';
            }
        }
    } catch (e) {
        alert(e.message || 'Error updating request.');
    }

    return false;
}

async function deleteProfile() {
    if(!confirm("Are you sure you want to delete your profile? This cannot be undone.")) return;
    
    try {
        const response = await fetch(`${API_URL}/deleteProfile?email=${encodeURIComponent(currentUser.email)}`, {
            method: 'DELETE'
        });
        if (!response.ok) {
            throw new Error('Failed to delete profile');
        }
        alert('Profile deleted.');
        logout();
    } catch (e) {
        alert(e.message || 'Error deleting profile.');
    }
}

function showContact(name, email) {
    document.getElementById('modal-details').innerHTML = `
        <strong>Name:</strong> ${name}<br/>
        <strong>Email:</strong> <a href="mailto:${email}">${email}</a>
    `;
    document.getElementById('contact-modal').classList.remove('hidden');
}

function closeModal() {
    document.getElementById('contact-modal').classList.add('hidden');
}

// Initial state setup
document.getElementById('reg-role').value = 'TEAMMATE';
toggleTeamSize();

// Restore logged-in user after accidental refresh.
try {
    const savedUser = window.localStorage.getItem(SESSION_KEY);
    if (savedUser) {
        const parsed = JSON.parse(savedUser);
        if (parsed && parsed.email) {
            currentUser = parsed;
            showDashboard();
        }
    }
} catch (error) {
    debugLog('Session restore failed', { error: error.message });
    window.localStorage.removeItem(SESSION_KEY);
}

window.addEventListener('beforeunload', () => {
    debugLog('beforeunload fired', { user: currentUser ? currentUser.email : null });
});
