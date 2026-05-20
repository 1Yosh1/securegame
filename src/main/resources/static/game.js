let player = { email: '', role: '', roomCode: '', xp: 0, level: 1, currentTopic: '', combo: 1 };
let pollInterval;

// -- GAME DATA --
const phishingData = [
    { url: "http://192.168.1.100/admin", safe: false, reason: "IP address instead of domain name." },
    { url: "https://accounts.google.com/signin", safe: true, reason: "Valid domain and HTTPS." },
    { url: "http://paypa1-secure.tk/login", safe: false, reason: "Brand spoofing (paypa1) and cheap TLD (.tk)." },
    { url: "https://www.amazon-security-update.com", safe: false, reason: "Suspiciously long, hyphenated domain." },
    { url: "https://github.com/login", safe: true, reason: "Valid well-known domain." },
    { url: "https://xn--pypal-4ve.com", safe: false, reason: "Punycode homograph attack. Looks like paypal but isn't." },
    { url: "http://netflix-billing-update.info", safe: false, reason: "HTTP instead of HTTPS and suspicious generic TLD (.info)." },
    { url: "https://apple.com.billing-update.com/login", safe: false, reason: "Subdomain spoofing. The actual domain is billing-update.com." },
    { url: "https://portal.azure.com", safe: true, reason: "Valid enterprise portal." },
    { url: "https://secure.bankofamerica.com.login.userid.verify.com", safe: false, reason: "Excessive subdomains masking the real domain verify.com." }
];
let phishingIndex = 0;

const cryptoData = [
    { q: "Which of the following is irreversible and used for storing passwords?", opts: ["AES-256", "RSA", "SHA-256 Hash", "Base64 Encoding"], ans: 2, expl: "A hash is a mathematical one-way function. Encryption like AES or RSA is two-way (can be decrypted). Base64 is just an encoding, not security." },
    { q: "Alice wants to send a secret message to Bob. Whose key should she use to encrypt it?", opts: ["Alice's Public Key", "Alice's Private Key", "Bob's Public Key", "Bob's Private Key"], ans: 2, expl: "To ensure ONLY Bob can read the message, it must be encrypted with Bob's Public Key. Bob will then use his own Private Key to decrypt it." },
    { q: "What is the primary purpose of a Digital Signature?", opts: ["To encrypt the data", "To prove authenticity and non-repudiation", "To compress the data", "To hide the sender's IP address"], ans: 1, expl: "Digital signatures don't encrypt the message. They use the sender's private key to sign a hash of the message, proving who sent it and that it wasn't tampered with." },
    { q: "What does a Salt do in password hashing?", opts: ["It encrypts the password twice.", "It defends against Rainbow Table attacks by adding random data.", "It compresses the password to save space.", "It tells the server which hash algorithm was used."], ans: 1, expl: "A salt is random data added to a password before hashing. It prevents attackers from using precomputed dictionary 'Rainbow Tables' to crack hashes." },
    { q: "What is Symmetric Encryption?", opts: ["The same key is used for both encryption and decryption.", "One key encrypts, a different key decrypts.", "It is only used for hashing passwords.", "It does not use keys at all."], ans: 0, expl: "Symmetric encryption uses a single shared key for both locking and unlocking data (e.g. AES). Asymmetric uses a pair (Public/Private)." }
];
let cryptoIndex = 0;

const matchingData = {
    terms: ["Least Privilege", "Zero Trust", "MFA", "Salting", "Phishing", "DDoS", "SQL Injection"],
    defs: [
        "Give users only the access they need.",
        "Never trust, always verify.",
        "Requiring two or more verification methods.",
        "Adding random data before hashing.",
        "Tricking users into revealing sensitive data.",
        "Overwhelming a server with traffic.",
        "Exploiting unsanitized database queries."
    ]
};
let selectedTerm = null;
let matchesFound = 0;

// -- CORE UI --
function showScreen(id) {
    document.querySelectorAll('.screen').forEach(s => s.classList.remove('active'));
    document.getElementById(id).classList.add('active');
}
function showToast(msg) {
    const t = document.getElementById('toast');
    t.innerText = msg; t.classList.add('show');
    setTimeout(() => t.classList.remove('show'), 3000);
}

// -- SECURITY AUDIT: XSS Mitigation --
function escapeHtml(unsafe) {
    return (unsafe||'').toString()
         .replace(/&/g, "&amp;")
         .replace(/</g, "&lt;")
         .replace(/>/g, "&gt;")
         .replace(/"/g, "&quot;")
         .replace(/'/g, "&#039;");
}

// -- UI/UX POLISH: Audio & Particles --
const audioCtx = new (window.AudioContext || window.webkitAudioContext)();
function playSound(type) {
    if(audioCtx.state === 'suspended') audioCtx.resume();
    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain); gain.connect(audioCtx.destination);
    
    if(type === 'success') {
        osc.type = 'square';
        osc.frequency.setValueAtTime(440, audioCtx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(880, audioCtx.currentTime + 0.1);
        gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.1);
        osc.start(); osc.stop(audioCtx.currentTime + 0.1);
    } else if(type === 'error') {
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(300, audioCtx.currentTime);
        osc.frequency.exponentialRampToValueAtTime(100, audioCtx.currentTime + 0.2);
        gain.gain.setValueAtTime(0.1, audioCtx.currentTime);
        gain.gain.exponentialRampToValueAtTime(0.01, audioCtx.currentTime + 0.2);
        osc.start(); osc.stop(audioCtx.currentTime + 0.2);
    }
}
function spawnParticles(x, y) {
    for(let i=0; i<10; i++) {
        let p = document.createElement('div');
        p.className = 'particle';
        p.style.left = (x + (Math.random()*40 - 20)) + 'px';
        p.style.top = (y + (Math.random()*40 - 20)) + 'px';
        document.body.appendChild(p);
        setTimeout(() => p.remove(), 1000);
    }
}
document.addEventListener('click', (e) => {
    if(e.target.tagName === 'BUTTON') {
        playSound('success');
        spawnParticles(e.clientX, e.clientY);
    }
});

// -- AUTH & LOBBY --
function switchAuthTab(tab) {
    if(tab === 'login') {
        document.getElementById('form-login').style.display = 'block';
        document.getElementById('form-register').style.display = 'none';
        document.getElementById('tab-login').style.background = 'var(--grass)';
        document.getElementById('tab-register').style.background = '#333';
    } else {
        document.getElementById('form-login').style.display = 'none';
        document.getElementById('form-register').style.display = 'block';
        document.getElementById('tab-register').style.background = 'var(--grass)';
        document.getElementById('tab-login').style.background = '#333';
    }
}

function validateEmail(email) {
    const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return re.test(String(email).toLowerCase());
}

async function registerUser() {
    const name = document.getElementById('reg-name').value.trim();
    const email = document.getElementById('reg-email').value.trim();
    const pass = document.getElementById('reg-password').value;

    if(!name || !email || !pass) return showToast("Please fill all required fields");
    if(!validateEmail(email)) return showToast("Please enter a valid email address");

    const formData = new URLSearchParams();
    formData.append("email", email);
    formData.append("password", pass);
    formData.append("name", name);
    formData.append("role", "PENDING");

    showToast("Registering account...");
    document.getElementById('form-register').style.opacity = '0.5';

    try {
        const res = await fetch('/api/auth/register', { method: 'POST', body: formData, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });
        document.getElementById('form-register').style.opacity = '1';
        if(res.ok) {
            showToast("Registration successful! Please login.");
            switchAuthTab('login');
        } else {
            const data = await res.json();
            showToast("Registration failed: " + data.message);
        }
    } catch(e) { 
        document.getElementById('form-register').style.opacity = '1';
        showToast("Backend not responding."); 
    }
}

async function requestLogin() {
    const email = document.getElementById('login-email').value.trim();
    const pass = document.getElementById('login-password').value;
    if(!email || !pass) return showToast("Enter email and password");
    if(!validateEmail(email)) return showToast("Please enter a valid email address");
    player.email = email;
    
    const formData = new URLSearchParams();
    formData.append("email", email);
    formData.append("password", pass);

    showToast("Connecting to server & Sending OTP...");
    document.getElementById('form-login').style.opacity = '0.5';

    try {
        const res = await fetch('/api/auth/login', { method: 'POST', body: formData, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });
        document.getElementById('form-login').style.opacity = '1';
        if(res.ok) {
            const data = await res.json();
            player.role = data.role.toLowerCase();
            document.getElementById('otp-section').style.display = 'block';
            if (data.otp) {
                document.getElementById('login-otp').value = data.otp;
            }
            showToast("OTP generated and sent to email! Check terminal console for local testing.");
        } else {
            const data = await res.json();
            showToast("Login failed: " + data.message);
        }
    } catch (e) { 
        document.getElementById('form-login').style.opacity = '1';
        showToast("Backend not responding."); 
    }
}
async function verifyOTP() {
    const otp = document.getElementById('login-otp').value;
    try {
        const formData = new URLSearchParams();
        formData.append("email", player.email);
        formData.append("otp", otp);
        const res = await fetch(`/api/auth/verify`, { method: 'POST', body: formData, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });
        
        if(res.ok) {
            const data = await res.json();
            player.role = data.role.toLowerCase();
            player.xp = data.xp || 0;
            player.level = data.level || 1;

            document.getElementById('hud').style.display = 'flex';
            document.getElementById('player-info').innerText = player.email;
            document.getElementById('level-label').innerText = "Lv." + player.level;
            document.getElementById('xp-fill').style.width = (player.xp/1000)*100 + "%";
            
            showScreen('screen-role');
            showToast(`Authentication successful. Select your role.`);
        } else { showToast("Invalid OTP."); }
    } catch(e) { showToast("Error verifying OTP."); }
}

function logout() {
    player = { email: '', role: '', roomCode: '', xp: 0, level: 1, currentTopic: '', combo: 1 };
    document.getElementById('hud').style.display = 'none';
    if (pollInterval) clearInterval(pollInterval);
    document.getElementById('login-email').value = '';
    document.getElementById('login-password').value = '';
    document.getElementById('login-otp').value = '';
    document.getElementById('otp-section').style.display = 'none';
    showScreen('screen-login');
    showToast("Logged out successfully");
}

async function selectRole(role) {
    showToast(`Setting role to ${role}...`);
    try {
        const formData = new URLSearchParams();
        formData.append("email", player.email);
        formData.append("role", role.toUpperCase());
        const res = await fetch('/api/auth/update-role', { method: 'POST', body: formData, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });
        if(res.ok) {
            const data = await res.json();
            player.role = data.role.toLowerCase();
            if(player.role === 'teacher') {
                showScreen('screen-teacher');
            } else if(player.role === 'student') {
                showScreen('screen-student');
            } else {
                showScreen('screen-freeplay-modes');
            }
        } else {
            showToast("Failed to update role on server.");
        }
    } catch(e) {
        showToast("Error updating role.");
    }
}

function selectFreePlayMode(mode) {
    if(mode === 'arcade') {
        showScreen('screen-hub');
    } else if(mode === 'campaign') {
        showCampaignLevels();
    }
}

async function createRoom() {
    const res = await fetch(`/api/multiplayer/create?teacher=${player.email}`, {method:'POST'});
    const room = await res.json();
    player.roomCode = room.code;
    document.getElementById('teacher-room-info').style.display = 'block';
    document.getElementById('teacher-code').innerText = room.code;
    pollRoomStatus();
}
async function setTopic(topic) {
    await fetch(`/api/multiplayer/setTopic?code=${player.roomCode}&topic=${topic}`, {method:'POST'});
    showToast(`Pushed "${topic}" to all connected students!`);
}
async function joinRoom() {
    const code = document.getElementById('join-code').value;
    try {
        const res = await fetch(`/api/multiplayer/join?code=${code}&student=${player.email}`, {method:'POST'});
        if(!res.ok) throw new Error();
        player.roomCode = code;
        document.getElementById('student-waiting').style.display = 'block';
        pollRoomStatus();
    } catch(e) { showToast("Room not found"); }
}
function pollRoomStatus() {
    pollInterval = setInterval(async () => {
        try {
            const res = await fetch(`/api/multiplayer/status?code=${player.roomCode}`);
            const room = await res.json();
            if(player.role === 'teacher') {
                document.getElementById('student-count').innerText = room.students.length;
            } else if (player.role === 'student') {
                if(room.topic !== 'WAITING' && room.topic !== player.currentTopic) {
                    player.currentTopic = room.topic;
                    launchModule(room.topic);
                }
            }
        } catch(e) {}
    }, 2000);
}

// -- GAME MODULES --
function launchModule(topic) {
    showScreen('screen-module');
    document.getElementById('module-title').innerText = "Topic: " + topic.toUpperCase();
    document.getElementById('module-back-btn').style.display = (player.role === 'solo') ? 'block' : 'none';
    player.combo = 1; updateComboUI();
    
    if (topic === 'mfa') renderMfa();
    else if (topic === 'password') renderPassword();
    else if (topic === 'phishing') { phishingIndex = 0; renderPhishing(); }
    else if (topic === 'crypto') { cryptoIndex = 0; renderCrypto(); }
    else if (topic === 'matching') { matchesFound = 0; renderMatching(); }
}

// 1. MFA Shield
function renderMfa() {
    const content = document.getElementById('module-content');
    content.innerHTML = `
        <div style="background:#222; padding:20px; border:4px solid var(--wood); margin-bottom:20px;">
            <p style="font-size:10px; line-height:1.8;">An attacker has stolen your password and is trying to log in.<br>Your phone buzzes with an SMS code: <strong style="color:var(--sky)">492811</strong>.<br>You are currently away from your computer.</p>
        </div>
        <div style="display:flex; flex-direction:column; gap:15px;">
            <button style="background:var(--red);" onclick="actionMfa(false)">Deny Login (It's not me!)</button>
            <div style="display:flex; gap:10px;">
                <input type="text" id="mfa-input" placeholder="Enter MFA Code" style="margin:0;">
                <button style="background:var(--grass); color:black;" onclick="actionMfa(true)">Submit Code</button>
            </div>
        </div>
        <div id="mfa-result" style="margin-top:20px; text-align:center;"></div>`;
}
function actionMfa(submitted) {
    if(submitted) {
        document.getElementById('mfa-result').innerHTML = "<span style='color:var(--red)'>HACKED! You gave the attacker the code!</span>";
        player.combo = 1; updateComboUI();
        showAiFeedback("Critical Failure", "MFA Fatigue and Push Bombing rely on users blindly accepting prompts. By entering that code, you bypassed your own security and handed the attacker the keys. Never approve MFA if you didn't initiate the login!", false);
    } else {
        document.getElementById('mfa-result').innerHTML = "<span style='color:var(--grass)'>ATTACK PREVENTED!</span>";
        addXp(200);
        showAiFeedback("Threat Neutralized", "Excellent. Denying unrequested MFA prompts stops attackers in their tracks. This is the correct response to an MFA bombing attack.", true);
    }
}

// 2. Password Fortress
function renderPassword() {
    document.getElementById('module-content').innerHTML = `
        <div style="background:#222; padding:20px; border:4px solid var(--wood); margin-bottom:20px;">
            <p style="font-size:10px;">Craft a password with > 70 bits of entropy.</p>
        </div>
        <input type="text" id="pass-input" placeholder="Type password..." onkeyup="checkEntropy()">
        <div style="background:#000; border:2px solid #fff; height:20px; width:100%; margin-top:10px;">
            <div id="ent-fill" style="background:var(--red); height:100%; width:0%;"></div>
        </div>
        <p id="ent-text" style="text-align:center; margin-top:10px;">0 Bits</p>`;
}
function checkEntropy() {
    const pass = document.getElementById('pass-input').value;
    let pool = 0;
    if (/[a-z]/.test(pass)) pool += 26;
    if (/[A-Z]/.test(pass)) pool += 26;
    if (/[0-9]/.test(pass)) pool += 10;
    if (/[^a-zA-Z0-9]/.test(pass)) pool += 32;
    const score = pool === 0 ? 0 : pass.length * Math.log2(pool);
    document.getElementById('ent-text').innerText = score.toFixed(1) + " Bits";
    document.getElementById('ent-fill').style.width = Math.min(100, (score/100)*100) + "%";
    document.getElementById('ent-fill').style.background = score > 70 ? 'var(--grass)' : (score > 40 ? 'var(--accent)' : 'var(--red)');
    
    if(score > 70 && !window.passCleared) {
        window.passCleared = true;
        addXp(200); 
        showAiFeedback("Fortress Secured", `Impressive. A password with ${score.toFixed(1)} bits of entropy is cryptographically secure. Mixing character sets exponentially increases the computational time required for a brute-force attack.`, true);
    }
}

// 3. Phishing Rapid Fire
function renderPhishing() {
    if (phishingIndex >= phishingData.length) {
        document.getElementById('module-content').innerHTML = `<h3 style="text-align:center; color:var(--grass)">All Threats Cleared!</h3>`;
        return;
    }
    const target = phishingData[phishingIndex];
    document.getElementById('module-content').innerHTML = `
        <div style="text-align:center; margin-bottom:10px;">Threat ${phishingIndex+1} of ${phishingData.length}</div>
        <div style="background:#000; padding:30px 10px; text-align:center; font-size:14px; margin-bottom:20px; border:4px dashed #666; color:var(--sky)">
            ${escapeHtml(target.url)}
        </div>
        <div style="display:flex; gap:20px;">
            <button style="flex:1; background:var(--grass); color:black;" onclick="guessPhishing(true)">SAFE</button>
            <button style="flex:1; background:var(--red);" onclick="guessPhishing(false)">PHISHING</button>
        </div>
        <div id="phish-res" style="margin-top:20px; text-align:center; height:40px;"></div>
    `;
}
function guessPhishing(guessSafe) {
    const target = phishingData[phishingIndex];
    const correct = (guessSafe === target.safe);
    const resDiv = document.getElementById('phish-res');
    
    if(correct) {
        playSound('success');
        resDiv.innerHTML = `<span style="color:var(--grass)">CORRECT! ${escapeHtml(target.reason)}</span>`;
        addXp(50 * player.combo);
        player.combo++; updateComboUI();
        phishingIndex++;
        setTimeout(renderPhishing, 1500);
    } else {
        playSound('error');
        resDiv.innerHTML = `<span style="color:var(--red)">WRONG! ${escapeHtml(target.reason)}</span>`;
        player.combo = 1; updateComboUI();
        showAiFeedback("Phishing Assessment Failed", `You misidentified that URL. Reason: ${target.reason}. Attackers use these techniques to bypass human intuition. Stay alert!`, false);
        phishingIndex++;
        setTimeout(renderPhishing, 1500);
    }
}

// 4. Crypto Quiz
function renderCrypto() {
    if (cryptoIndex >= cryptoData.length) {
        document.getElementById('module-content').innerHTML = `<h3 style="text-align:center; color:var(--grass)">Quiz Complete!</h3>`;
        return;
    }
    const q = cryptoData[cryptoIndex];
    let html = `
        <div style="text-align:center; margin-bottom:10px;">Question ${cryptoIndex+1} of ${cryptoData.length}</div>
        <div style="background:#222; padding:20px; border:4px solid var(--wood); margin-bottom:20px;">
            <p>${escapeHtml(q.q)}</p>
        </div>
        <div style="display:flex; flex-direction:column; gap:10px;">`;
    
    q.opts.forEach((opt, idx) => {
        html += `<button onclick="answerCrypto(${idx})" style="text-align:left; text-transform:none;">${idx+1}. ${escapeHtml(opt)}</button>`;
    });
    html += `</div><div id="crypto-res" style="margin-top:20px; text-align:center; height:40px;"></div>`;
    document.getElementById('module-content').innerHTML = html;
}
function answerCrypto(idx) {
    const q = cryptoData[cryptoIndex];
    const correct = (idx === q.ans);
    const resDiv = document.getElementById('crypto-res');
    if(correct) {
        playSound('success');
        resDiv.innerHTML = `<span style="color:var(--grass)">Correct!</span>`;
        addXp(100 * player.combo); player.combo++; updateComboUI();
        cryptoIndex++;
        setTimeout(renderCrypto, 1500);
    } else {
        playSound('error');
        resDiv.innerHTML = `<span style="color:var(--red)">Incorrect!</span>`;
        player.combo = 1; updateComboUI();
        showAiFeedback("Cryptography Error", q.expl, false);
        cryptoIndex++;
        setTimeout(renderCrypto, 1500);
    }
}

// 5. Access Control Match
function renderMatching() {
    if(matchesFound === matchingData.terms.length) {
        document.getElementById('module-content').innerHTML = `<h3 style="text-align:center; color:var(--grass)">All Concepts Matched!</h3>`;
        return;
    }
    
    // Shuffle arrays for display
    let dispTerms = [...matchingData.terms].map(t => ({id: t, text: t, matched: false}));
    let dispDefs = [...matchingData.defs].map((d, i) => ({id: matchingData.terms[i], text: d, matched: false}));
    // Quick shuffle
    dispDefs.sort(() => Math.random() - 0.5);

    let html = `
        <p style="text-align:center; font-size:10px; margin-bottom:15px;">Click a term, then click its definition.</p>
        <div style="display:flex; gap:20px;">
            <div style="flex:1; display:flex; flex-direction:column; gap:10px;" id="match-terms">
                ${dispTerms.map(t => `<div class="match-card" onclick="selectTerm('${escapeHtml(t.id)}', this)">${escapeHtml(t.text)}</div>`).join('')}
            </div>
            <div style="flex:1; display:flex; flex-direction:column; gap:10px;" id="match-defs">
                ${dispDefs.map(d => `<div class="match-card" onclick="selectDef('${escapeHtml(d.id)}', this)">${escapeHtml(d.text)}</div>`).join('')}
            </div>
        </div>
    `;
    document.getElementById('module-content').innerHTML = html;
}
let currentTermCard = null;
function selectTerm(id, el) {
    document.querySelectorAll('#match-terms .match-card').forEach(c => c.style.borderColor = '#000');
    if(el.style.visibility !== 'hidden') {
        el.style.borderColor = 'var(--accent)';
        selectedTerm = id;
        currentTermCard = el;
    }
}
function selectDef(id, el) {
    if(!selectedTerm) return;
    if(selectedTerm === id) {
        el.style.background = 'var(--grass)';
        currentTermCard.style.background = 'var(--grass)';
        setTimeout(() => {
            el.style.visibility = 'hidden';
            currentTermCard.style.visibility = 'hidden';
            matchesFound++;
            addXp(50 * player.combo); player.combo++; updateComboUI();
            if(matchesFound === matchingData.terms.length) setTimeout(renderMatching, 500);
        }, 500);
    } else {
        el.classList.add('shake');
        currentTermCard.classList.add('shake');
        player.combo = 1; updateComboUI();
        showAiFeedback("Mapping Mismatch", `You tried to match "${selectedTerm}" with an incorrect definition. Access control principles must be strictly understood. Review your concepts.`, false);
        setTimeout(() => {
            el.classList.remove('shake');
            currentTermCard.classList.remove('shake');
            currentTermCard.style.borderColor = '#000';
            selectedTerm = null;
        }, 500);
    }
}

// -- SYSTEMS --
async function addXp(amt) {
    player.xp += amt;
    if(player.xp >= 1000) { player.level++; player.xp -= 1000; showToast("LEVEL UP!"); }
    document.getElementById('level-label').innerText = "Lv." + player.level;
    document.getElementById('xp-fill').style.width = (player.xp/1000)*100 + "%";

    try {
        const formData = new URLSearchParams();
        formData.append("email", player.email);
        formData.append("xp", player.xp);
        formData.append("level", player.level);
        await fetch('/api/auth/progress', { method: 'POST', body: formData, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });
    } catch(e) { console.error("Error syncing progress:", e); }
}
function updateComboUI() {
    const lbl = document.getElementById('streak-label');
    if(player.combo > 1) {
        lbl.style.display = 'block';
        lbl.innerText = `Combo x${player.combo}`;
        lbl.style.fontSize = Math.min(14 + (player.combo*2), 24) + 'px';
    } else {
        lbl.style.display = 'none';
    }
}

// -- AI MODAL SYSTEM --
function showAiFeedback(title, message, isSuccess) {
    const modal = document.getElementById('ai-modal');
    modal.style.display = 'flex';
    document.getElementById('ai-modal-title').innerText = title;
    document.getElementById('ai-modal-title').style.color = isSuccess ? 'var(--grass)' : 'var(--red)';
    document.getElementById('ai-modal-text').innerText = message;
}
function closeAiModal() {
    document.getElementById('ai-modal').style.display = 'none';
}

// -- AI SCENARIO GENERATOR --
async function generateScenario(type) {
    const topic = document.getElementById('ai-topic').value;
    if(!topic) return showToast("Please enter a topic.");
    showToast("Generating " + type + " scenarios via LLM...");
    
    try {
        const res = await fetch(`/api/scenario/generate?topic=${encodeURIComponent(topic)}&type=${type}`);
        const data = await res.json();
        
        if (type === 'crypto') {
            cryptoData.push(...data);
            showToast(`Added ${data.length} new crypto questions!`);
        } else if (type === 'phishing') {
            phishingData.push(...data);
            showToast(`Added ${data.length} new phishing URLs!`);
        } else if (type === 'matching') {
            matchingData.terms = data.terms;
            matchingData.defs = data.defs;
            showToast("Matching game seeded!");
        }
    } catch(e) {
        showToast("Error generating scenarios.");
        console.error(e);
    }
}

// -- PASSWORD RECOVERY UI --
function showRecoveryForm() {
    document.getElementById('form-login').style.display = 'none';
    document.getElementById('form-register').style.display = 'none';
    document.getElementById('form-recovery').style.display = 'block';
    document.getElementById('recovery-reset-section').style.display = 'none';
}

function cancelRecovery() {
    document.getElementById('form-recovery').style.display = 'none';
    document.getElementById('form-login').style.display = 'block';
}

async function requestRecoveryOtp() {
    const email = document.getElementById('recovery-email').value.trim();
    if(!email) return showToast("Please enter your email");
    if(!validateEmail(email)) return showToast("Please enter a valid email address");

    showToast("Sending verification code...");
    try {
        const formData = new URLSearchParams();
        formData.append("email", email);
        const res = await fetch('/api/auth/forgot-password', { method: 'POST', body: formData, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });
        if(res.ok) {
            const data = await res.json();
            showToast("Recovery OTP sent! Check your inbox.");
            document.getElementById('recovery-reset-section').style.display = 'block';
            if (data.otp) {
                document.getElementById('recovery-otp').value = data.otp;
            }
        } else {
            const data = await res.json();
            showToast("Failed: " + data.message);
        }
    } catch(e) {
        showToast("Error requesting recovery code.");
    }
}

async function submitPasswordReset() {
    const email = document.getElementById('recovery-email').value.trim();
    const otp = document.getElementById('recovery-otp').value.trim();
    const pass = document.getElementById('recovery-password').value;

    if(!otp || !pass) return showToast("Fill all fields");

    showToast("Resetting password...");
    try {
        const formData = new URLSearchParams();
        formData.append("email", email);
        formData.append("otp", otp);
        formData.append("newPassword", pass);
        const res = await fetch('/api/auth/reset-password', { method: 'POST', body: formData, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });
        if(res.ok) {
            showToast("Password reset success! Please login.");
            cancelRecovery();
        } else {
            const data = await res.json();
            showToast("Reset failed: " + data.message);
        }
    } catch(e) {
        showToast("Error resetting password.");
    }
}

// -- CAMPAIGN MODE --
let currentCampaignLevel = null;
let currentCampaignQIdx = 0;

function showCampaignLevels() {
    showScreen('screen-campaign-levels');
    const container = document.getElementById('campaign-levels-grid');
    container.removeAttribute('style');
    container.className = 'map-container';
    container.innerHTML = '';
    
    const mapContent = document.createElement('div');
    mapContent.className = 'map-content';
    
    const svgNS = "http://www.w3.org/2000/svg";
    const svg = document.createElementNS(svgNS, "svg");
    svg.setAttribute("class", "path-svg");
    svg.setAttribute("viewBox", "0 0 100 2000");
    svg.setAttribute("preserveAspectRatio", "none");
    
    const path = document.createElementNS(svgNS, "path");
    path.setAttribute("stroke", "#444");
    path.setAttribute("stroke-width", "4");
    path.setAttribute("stroke-dasharray", "10,10");
    path.setAttribute("fill", "none");
    path.setAttribute("vector-effect", "non-scaling-stroke");
    
    let pathD = "";
    const startY = 1900; 
    let highestUnlockedY = 1900;

    campaignLevels.forEach((lvl, index) => {
        const isLocked = lvl.level > player.level;
        const xVal = 50 + Math.sin(index * 1.2) * 35; // zig zag between 15% and 85%
        const yPx = startY - (index * 90);
        
        if (!isLocked) highestUnlockedY = yPx;

        const btn = document.createElement('div');
        btn.className = 'level-node' + (isLocked ? ' locked' : '');
        btn.style.left = `${xVal}%`;
        btn.style.top = `${yPx}px`;
        
        const titleToast = document.createElement('div');
        titleToast.className = 'level-title-toast';
        titleToast.innerText = `Lvl ${lvl.level}: ${lvl.title}`;
        btn.appendChild(titleToast);
        
        btn.appendChild(document.createTextNode(isLocked ? "🔒" : lvl.level));

        if(!isLocked) {
            btn.onclick = () => startCampaignLevel(lvl.level);
        }

        mapContent.appendChild(btn);
        
        if (index === 0) {
            pathD += `M ${xVal} ${yPx} `;
        } else {
            pathD += `L ${xVal} ${yPx} `;
        }
    });

    path.setAttribute("d", pathD);
    svg.appendChild(path);
    mapContent.appendChild(svg);
    container.appendChild(mapContent);

    setTimeout(() => {
        container.scrollTop = highestUnlockedY - (container.clientHeight / 2);
    }, 100);
}

function startCampaignLevel(levelNum) {
    const lvlObj = campaignLevels.find(l => l.level === levelNum);
    if(!lvlObj) return;
    
    currentCampaignLevel = lvlObj;
    currentCampaignQIdx = 0;
    
    showScreen('screen-campaign-game');
    document.getElementById('campaign-level-title').innerText = `Level ${lvlObj.level}: ${lvlObj.title}`;
    document.getElementById('campaign-level-desc').innerText = lvlObj.description;
    
    renderCampaignQuestion();
}

function renderCampaignQuestion() {
    const content = document.getElementById('campaign-level-content');
    if(currentCampaignQIdx >= currentCampaignLevel.questions.length) {
        showToast("Level Cleared!");
        content.innerHTML = `
            <div style="text-align:center;">
                <h3 style="color:var(--grass); font-size:18px;">CONGRATULATIONS!</h3>
                <p style="margin: 15px 0;">You cleared all threats in Level ${currentCampaignLevel.level}!</p>
                <p style="color:var(--sky);">+1000 XP Granted</p>
                <button onclick="clearCampaignLevelReward()" style="width:100%; background:var(--grass); color:black; margin-top:20px;">Claim Reward & Back</button>
            </div>
        `;
        return;
    }
    
    const q = currentCampaignLevel.questions[currentCampaignQIdx];
    let html = `
        <div style="background:#222; padding:20px; border:2px solid var(--wood); margin-bottom:20px;">
            <p style="font-size:12px; line-height:1.6;">${escapeHtml(q.q)}</p>
        </div>
        <div style="display:flex; flex-direction:column; gap:10px;">`;
    
    q.opts.forEach((opt, idx) => {
        html += `<button onclick="submitCampaignAnswer(${idx})" style="text-align:left; text-transform:none;">${idx+1}. ${escapeHtml(opt)}</button>`;
    });
    html += `</div>`;
    content.innerHTML = html;
}

function submitCampaignAnswer(idx) {
    const q = currentCampaignLevel.questions[currentCampaignQIdx];
    const correct = (idx === q.ans);
    
    if(correct) {
        playSound('success');
        showToast("Correct response!");
        currentCampaignQIdx++;
        renderCampaignQuestion();
    } else {
        playSound('error');
        showToast("Incorrect system response! Try again.");
        const panel = document.querySelector('#screen-campaign-game .panel');
        panel.classList.add('shake');
        setTimeout(() => panel.classList.remove('shake'), 500);
    }
}

async function clearCampaignLevelReward() {
    if(currentCampaignLevel.level === player.level) {
        await addXp(1000);
    } else {
        await addXp(200);
    }
    showCampaignLevels();
}

function quitCampaign() {
    showCampaignLevels();
}

