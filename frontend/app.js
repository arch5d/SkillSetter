const apiFromQuery = new URLSearchParams(window.location.search).get('api');
const API_BASE = window.localStorage.getItem('skillsetter_api_base') || apiFromQuery || 'http://localhost:8080';
const API_URL = `${API_BASE}/api`;

let currentUser = null;
let currentSkills = [];

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
        const response = await fetch(`${API_URL}/user?email=${encodeURIComponent(email)}`);
        if (!response.ok) {
            document.getElementById('login-error').innerText = 'User not found. Try registering.';
            return;
        }

        const user = await response.json();
        if (user) {
            currentUser = user;
            showDashboard();
        } else {
            document.getElementById('login-error').innerText = 'User not found. Try registering.';
        }
    } catch (e) {
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
            showDashboard();
        } else {
            const err = await response.json().catch(() => ({}));
            document.getElementById('reg-error').innerText = err.error || 'Registration failed.';
        }
    } catch (e) {
        document.getElementById('reg-error').innerText = 'Error connecting to server.';
    }
}

function logout() {
    currentUser = null;
    currentSkills = [];
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
    
    showTab('matches');
}

function showTab(tab) {
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
                    <button class="btn-secondary" onclick="ignoreMatch(this)">Hide</button>
                    <button class="btn-primary" onclick="sendRequest('${match.user.email}', this)">Connect</button>
                </div>
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<p class="error-msg">Error loading matches.</p>';
    }
}

async function sendRequest(receiverEmail, btn) {
    btn.innerText = 'Sending...';
    btn.disabled = true;
    try {
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
        btn.innerText = 'Request Sent';
        btn.classList.replace('btn-primary', 'btn-secondary');
    } catch (e) {
        btn.innerText = e.message;
        btn.disabled = false;
    }
}

async function loadRequests() {
    const container = document.getElementById('requests-container');
    container.innerHTML = '<p>Loading requests...</p>';

    try {
        const response = await fetch(`${API_URL}/requests?email=${encodeURIComponent(currentUser.email)}`);
        const requests = await response.json();
        
        if (requests.length === 0) {
            container.innerHTML = '<p>No pending requests.</p>';
            return;
        }

        container.innerHTML = requests.map(req => `
            <div class="match-card">
                <div class="match-header">
                    <div>
                        <div class="match-name">${req.senderName || req.senderEmail}</div>
                        <div class="match-role">Status: ${req.status}</div>
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
                    <button class="btn-secondary" onclick='updateRequest(${req.id}, "REJECTED", ${JSON.stringify(req.senderName || req.senderEmail)}, ${JSON.stringify(req.senderEmail)})'>Decline</button>
                    <button class="btn-primary" onclick='updateRequest(${req.id}, "ACCEPTED", ${JSON.stringify(req.senderName || req.senderEmail)}, ${JSON.stringify(req.senderEmail)})'>Accept</button>
                </div>
                ` : ''}
            </div>
        `).join('');
    } catch (e) {
        container.innerHTML = '<p class="error-msg">Error loading requests.</p>';
    }
}

async function updateRequest(id, status, senderName, senderEmail) {
    try {
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

        if (status === 'ACCEPTED') {
            showContact(senderName, senderEmail);
        }
        loadRequests(); // refresh
    } catch (e) {
        alert(e.message || 'Error updating request.');
    }
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
