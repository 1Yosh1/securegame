// minigames.js - Logic for Interactive Arcade Games

const MiniGames = {
    activeInterval: null,

    cleanup: function() {
        if(this.activeInterval) clearInterval(this.activeInterval);
        this.activeInterval = null;
        const container = document.getElementById(this.targetContainer || 'module-content');
        if(container) container.innerHTML = '';
    },

    showBriefing: function(title, text, onStart) {
        const overlay = document.createElement('div');
        overlay.id = 'mission-briefing-overlay';
        overlay.style.position = 'fixed';
        overlay.style.top = '0';
        overlay.style.left = '0';
        overlay.style.width = '100%';
        overlay.style.height = '100%';
        overlay.style.background = 'rgba(0,0,0,0.93)';
        overlay.style.display = 'flex';
        overlay.style.flexDirection = 'column';
        overlay.style.justifyContent = 'center';
        overlay.style.alignItems = 'center';
        overlay.style.zIndex = '9999';
        overlay.style.padding = '20px';
        overlay.style.textAlign = 'center';
        overlay.style.boxSizing = 'border-box';
        
        overlay.innerHTML = `
            <div style="background:#1a1a1a; border:4px solid var(--accent); padding:30px; max-width:400px; width:90%; box-shadow:0 0 40px var(--accent);">
                <h2 style="color:var(--accent); text-shadow:2px 2px #000; font-size:18px; margin-bottom:8px;">MISSION BRIEFING</h2>
                <h3 style="color:var(--sky); margin-bottom:20px; font-size:13px;">${title}</h3>
                <p style="color:#ddd; font-size:11px; line-height:1.8; margin-bottom:25px;">${text}</p>
                <button id="btn-start-mission" class="btn" style="background:var(--grass); color:#000; font-size:13px; padding:12px 30px; border:4px solid #fff; width:100%;">▶ START MISSION</button>
            </div>
        `;
        document.body.appendChild(overlay);
        
        document.getElementById('btn-start-mission').onclick = () => {
            playSound && playSound('success');
            overlay.remove();
            onStart();
        };
    },

    // 1. FIREWALL DEFENDER
    startFirewall: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("Firewall Defender", "Click the <span style='color:var(--red);'>RED (Malicious)</span> packets to drop them.<br><br>Let <span style='color:var(--grass);'>GREEN (Safe)</span> packets through.", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            container.innerHTML = `
                <div style="text-align:center; margin-bottom:10px;">
                    <h3 style="color:var(--sky); margin:0;">Firewall Defender</h3>
                    <div style="margin-top:10px;">Health: <span id="fw-health" style="color:var(--grass)">100</span>% | Score: <span id="fw-score">0</span>/10</div>
                </div>
                <div id="fw-game-area" style="position:relative; width:100%; height:300px; background:#111; border:4px solid var(--wood); overflow:hidden;"></div>
            `;

            let health = 100;
            let score = 0;
            const speed = difficulty === 3 ? 5 : (difficulty === 2 ? 3 : 1.5);
            const spawnRate = difficulty === 3 ? 600 : (difficulty === 2 ? 800 : 1200);
            let packetsSpawned = 0;
            const totalToSpawn = 15;
            const area = document.getElementById('fw-game-area');

            this.activeInterval = setInterval(() => {
                if (packetsSpawned >= totalToSpawn) {
                    if (area.children.length === 0) {
                        clearInterval(this.activeInterval);
                        if (health > 0) onWin();
                        else onLose("Server was compromised by malicious traffic.");
                    }
                    return;
                }

                packetsSpawned++;
                const packet = document.createElement('div');
                const isMalicious = Math.random() > 0.4;
                
                packet.style.position = 'absolute';
                packet.style.width = '30px';
                packet.style.height = '30px';
                packet.style.top = '-30px';
                packet.style.left = Math.random() * (area.clientWidth - 30) + 'px';
                packet.style.background = isMalicious ? 'var(--red)' : 'var(--grass)';
                packet.style.border = '2px solid #fff';
                packet.style.cursor = 'pointer';
                packet.innerHTML = `<span style="font-size:8px; display:block; text-align:center; margin-top:8px; color:#000;">${isMalicious ? '23' : '443'}</span>`;

                packet.onclick = function() {
                    if(isMalicious) {
                        playSound && playSound('success');
                        score++;
                        document.getElementById('fw-score').innerText = score;
                        packet.remove();
                    } else {
                        playSound && playSound('error');
                        health -= 20;
                        document.getElementById('fw-health').innerText = health;
                        document.getElementById('fw-health').style.color = 'var(--red)';
                        packet.remove();
                        if(health <= 0) { clearInterval(MiniGames.activeInterval); onLose("Dropped too much legitimate traffic!"); }
                    }
                };
                area.appendChild(packet);

                let y = -30;
                const fallInt = setInterval(() => {
                    if(!packet.parentNode) { clearInterval(fallInt); return; }
                    y += speed;
                    packet.style.top = y + 'px';
                    if(y > area.clientHeight) {
                        clearInterval(fallInt);
                        if(isMalicious && packet.parentNode) {
                            health -= 20;
                            document.getElementById('fw-health').innerText = health;
                            document.getElementById('fw-health').style.color = 'var(--red)';
                            if(health <= 0) { clearInterval(MiniGames.activeInterval); onLose("Server overloaded by malicious traffic."); }
                        } else if (!isMalicious && packet.parentNode) {
                            score++;
                            document.getElementById('fw-score').innerText = score;
                        }
                        packet.remove();
                    }
                }, 30);
            }, spawnRate);
        });
    },

    // 2. SQLi LOCKPICKER
    startSQLi: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("SQLi Lockpicker", "Select the correct payload to bypass the query logic.<br><br>Find the right SQL injection string to log in.", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            
            let queryTemplate = "";
            let blocks = [];
            let correctAnswer = "";

            if(difficulty === 1) {
                queryTemplate = `SELECT * FROM users WHERE username = 'admin' AND password = '<span id="sqli-drop" style="display:inline-block; width:150px; height:20px; border-bottom:2px dashed var(--accent);"></span>'`;
                blocks = ["password123", "' OR '1'='1", "admin", "'); DROP TABLE users;--"];
                correctAnswer = "' OR '1'='1";
            } else if (difficulty === 2) {
                queryTemplate = `SELECT id, name FROM products WHERE id = <span id="sqli-drop" style="display:inline-block; width:150px; height:20px; border-bottom:2px dashed var(--accent);"></span>`;
                blocks = ["1", "1 UNION SELECT password FROM users", "' OR 1=1", "DROP TABLE products"];
                correctAnswer = "1 UNION SELECT password FROM users";
            } else {
                queryTemplate = `SELECT * FROM accounts WHERE email = '<span id="sqli-drop" style="display:inline-block; width:150px; height:20px; border-bottom:2px dashed var(--accent);"></span>' LIMIT 1`;
                blocks = ["admin@site.com", "admin' --", "' OR 1=1;", "admin' #"];
                correctAnswer = "admin' --"; 
            }

            blocks.sort(() => Math.random() - 0.5);

            container.innerHTML = `
                <div style="text-align:center; margin-bottom:20px;">
                    <h3 style="color:var(--sky); margin:0;">SQLi Lockpicker</h3>
                </div>
                <div style="background:#000; color:var(--grass); padding:20px; font-family:monospace; font-size:12px; border:2px solid #333; margin-bottom:20px;">
                    ${queryTemplate}
                </div>
                <div id="sqli-options" style="display:flex; flex-direction:column; gap:10px;">
                    ${blocks.map(b => `<button class="sqli-btn" style="background:#222; text-transform:none; border:2px solid var(--wood);">${escapeHtml(b)}</button>`).join('')}
                </div>
            `;

            document.querySelectorAll('.sqli-btn').forEach(btn => {
                btn.onclick = function() {
                    const choice = this.innerText;
                    document.getElementById('sqli-drop').innerText = choice;
                    document.getElementById('sqli-drop').style.color = 'var(--accent)';
                    
                    setTimeout(() => {
                        if(choice === correctAnswer) {
                            playSound && playSound('success');
                            onWin();
                        } else {
                            playSound && playSound('error');
                            onLose("Syntax Error or logic failed to bypass the authentication gate.");
                        }
                    }, 500);
                };
            });
        });
    },

    // 3. RANSOMWARE QUARANTINE
    startRansomware: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("Ransomware Containment", "Click computers to disconnect them before the RED infection spreads!<br><br>Contain the outbreak before the grid is lost.", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            const gridSize = difficulty === 3 ? 6 : (difficulty === 2 ? 5 : 4);
            const totalNodes = gridSize * gridSize;
            
            container.innerHTML = `
                <div style="text-align:center; margin-bottom:10px;">
                    <h3 style="color:var(--sky); margin:0;">Ransomware Quarantine</h3>
                    <div style="margin-top:10px;">Infected: <span id="rw-infected" style="color:var(--red)">1</span> / ${totalNodes}</div>
                </div>
                <div id="rw-grid" style="display:grid; grid-template-columns:repeat(${gridSize}, 1fr); gap:5px; background:#111; padding:10px; border:4px solid var(--wood); margin:0 auto; width:100%; max-width:400px;"></div>
            `;

            const grid = document.getElementById('rw-grid');
            let nodes = [];
            let infectedCount = 1;
            let active = true;

            for(let i=0; i<totalNodes; i++) {
                const el = document.createElement('div');
                el.style.height = '40px';
                el.style.background = 'var(--grass)';
                el.style.border = '2px solid #000';
                el.style.cursor = 'pointer';
                el.dataset.state = 'clean';
                
                el.onclick = () => {
                    if(!active) return;
                    if(el.dataset.state === 'clean') {
                        el.dataset.state = 'offline';
                        el.style.background = '#555';
                        playSound && playSound('success');
                    }
                };
                grid.appendChild(el);
                nodes.push(el);
            }

            const startIdx = Math.floor(totalNodes / 2);
            nodes[startIdx].dataset.state = 'infected';
            nodes[startIdx].style.background = 'var(--red)';

            const speed = difficulty === 3 ? 600 : (difficulty === 2 ? 800 : 1000);

            this.activeInterval = setInterval(() => {
                if(!active) return;
                let newInfections = [];
                let canSpread = false;

                nodes.forEach((node, i) => {
                    if(node.dataset.state === 'infected') {
                        const row = Math.floor(i / gridSize);
                        const col = i % gridSize;
                        const neighbors = [
                            {r: row-1, c: col}, {r: row+1, c: col},
                            {r: row, c: col-1}, {r: row, c: col+1}
                        ];

                        neighbors.forEach(n => {
                            if(n.r >= 0 && n.r < gridSize && n.c >= 0 && n.c < gridSize) {
                                const idx = n.r * gridSize + n.c;
                                if(nodes[idx].dataset.state === 'clean') {
                                    canSpread = true;
                                    if(Math.random() > 0.5) newInfections.push(idx);
                                }
                            }
                        });
                    }
                });

                newInfections.forEach(idx => {
                    if(nodes[idx].dataset.state === 'clean') {
                        nodes[idx].dataset.state = 'infected';
                        nodes[idx].style.background = 'var(--red)';
                        infectedCount++;
                    }
                });

                document.getElementById('rw-infected').innerText = infectedCount;

                if(!canSpread && newInfections.length === 0) {
                    active = false;
                    clearInterval(this.activeInterval);
                    if(infectedCount === totalNodes) onLose("Entire network encrypted!");
                    else onWin();
                } else if (infectedCount === totalNodes) {
                    active = false;
                    clearInterval(this.activeInterval);
                    onLose("Entire network encrypted!");
                }
            }, speed);
        });
    },

    // 4. PRIVILEGE ESCALATION MAZE
    startPrivEsc: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("PrivEsc Maze", "Find exploits and pivot through the network to compromise the Domain Controller.<br><br>Find the necessary keys to unlock paths.", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            
            let inventory = [];
            let currentNode = "Guest-PC";
            
            const nodes = {
                "Guest-PC": { unlock: null, contains: "SSH-Key" },
                "Dev-Server": { unlock: "SSH-Key", contains: "Root-Exploit" },
                "Database": { unlock: "DB-Password", contains: "Admin-Hash" },
                "Domain-Controller": { unlock: "Root-Exploit", contains: "Win" }
            };

            if(difficulty > 1) {
                nodes["Guest-PC"].contains = "DB-Password";
                nodes["Database"].contains = "SSH-Key"; 
            }

            const renderMap = () => {
                let html = `
                    <div style="text-align:center; margin-bottom:20px;">
                        <h3 style="color:var(--sky); margin:0;">PrivEsc Maze</h3>
                        <div style="margin-top:10px; font-size:10px;">Inventory: <span style="color:var(--accent)">${inventory.join(', ') || 'Empty'}</span></div>
                    </div>
                    <div style="display:flex; flex-direction:column; gap:10px;">
                `;

                for(const [name, data] of Object.entries(nodes)) {
                    const isCurrent = name === currentNode;
                    const canAccess = !data.unlock || inventory.includes(data.unlock) || isCurrent;
                    
                    let btnStyle = "background:#222; border:2px solid #555; color:#555;";
                    if(isCurrent) btnStyle = "background:var(--grass); border:2px solid var(--grass); color:#000;";
                    else if(canAccess) btnStyle = "background:var(--wood); border:2px solid var(--accent); color:#fff;";

                    html += `<button class="pe-node-btn" data-node="${name}" data-access="${canAccess}" style="${btnStyle} text-transform:none;">
                        <strong>${name}</strong><br>
                        <span style="font-size:8px;">${isCurrent ? '📍 YOU ARE HERE' : (canAccess ? 'Access Granted' : '🔒 Requires: ' + data.unlock)}</span>
                    </button>`;
                }
                html += `</div>`;
                container.innerHTML = html;

                document.querySelectorAll('.pe-node-btn').forEach(btn => {
                    btn.onclick = () => {
                        const nodeName = btn.dataset.node;
                        const access = btn.dataset.access === 'true';
                        
                        if(nodeName === currentNode) {
                            const item = nodes[nodeName].contains;
                            if(item === "Win") {
                                playSound && playSound('success');
                                onWin();
                            } else if(item && !inventory.includes(item)) {
                                playSound && playSound('success');
                                window.showToast && window.showToast("Found item: " + item);
                                inventory.push(item);
                                renderMap();
                            } else {
                                window.showToast && window.showToast("Nothing else of value here.");
                            }
                        } else if (access) {
                            playSound && playSound('success');
                            currentNode = nodeName;
                            renderMap();
                        } else {
                            playSound && playSound('error');
                            const panel = document.querySelector('#screen-campaign-game .panel') || document.querySelector('#screen-module .panel');
                            if (panel) { panel.classList.add('shake'); setTimeout(() => panel.classList.remove('shake'), 500); }
                            window.showToast && window.showToast("Access Denied. Missing requirement.");
                        }
                    }
                });
            };
            renderMap();
        });
    },

    // 5. CRYPTO DECODER
    startCryptoDecoder: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("Crypto Decoder", "Decrypt the Caesar Cipher to find the password.<br><br>Use the + and - buttons to shift characters.", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            let targetWord = difficulty === 1 ? "ADMIN" : (difficulty === 2 ? "SECURE" : "ENCRYPTED");
            let shift = difficulty === 1 ? 3 : (difficulty === 2 ? 5 : 7);

            const encrypt = (text, s) => text.split('').map(c => String.fromCharCode((c.charCodeAt(0) - 65 + s) % 26 + 65)).join('');
            const encryptedWord = encrypt(targetWord, shift);
            
            container.innerHTML = `
                <div style="text-align:center; margin-bottom:20px;">
                    <h3 style="color:var(--sky); margin:0;">Crypto Decoder</h3>
                    <div style="margin-top:10px; font-size:14px; color:var(--red); letter-spacing: 5px;">${encryptedWord}</div>
                    <div style="margin-top:10px; font-size:10px; color:#aaa;">Shift: <span id="crypto-shift-val">0</span></div>
                </div>
                <div style="display:flex; justify-content:center; gap:20px; margin-bottom:20px;">
                    <button id="crypto-down" class="btn" style="width:40px; background:#222; color:#fff;">-</button>
                    <div id="crypto-preview" style="font-family:monospace; font-size:18px; line-height:30px; letter-spacing: 5px; color:var(--grass);">${encryptedWord}</div>
                    <button id="crypto-up" class="btn" style="width:40px; background:#222; color:#fff;">+</button>
                </div>
                <button id="crypto-submit" class="btn" style="width:100%;">Attempt Decryption</button>
            `;

            let currentShift = 0;
            const updatePreview = () => {
                document.getElementById('crypto-shift-val').innerText = currentShift;
                const decoded = encryptedWord.split('').map(c => {
                    let code = c.charCodeAt(0) - 65 + currentShift;
                    while(code < 0) code += 26;
                    return String.fromCharCode((code % 26) + 65);
                }).join('');
                document.getElementById('crypto-preview').innerText = decoded;
            };

            document.getElementById('crypto-down').onclick = () => { currentShift--; updatePreview(); };
            document.getElementById('crypto-up').onclick = () => { currentShift++; updatePreview(); };
            document.getElementById('crypto-submit').onclick = () => {
                const preview = document.getElementById('crypto-preview').innerText;
                if (preview === targetWord) {
                    playSound && playSound('success');
                    onWin();
                } else {
                    playSound && playSound('error');
                    onLose("Incorrect decryption attempt.");
                }
            };
        });
    },

    // 6. RBAC DRAG & DROP
    startRBAC: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("Role-Based Access Control", "Assign the correct security badge to each employee. Drag the badge to the employee profile.<br><br>Warning: Violating the Principle of Least Privilege will cause a security breach!", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            
            const roles = [
                { id: "r1", name: "Intern", desc: "Read public policies.", badge: "Guest" },
                { id: "r2", name: "Developer", desc: "Push code.", badge: "User" },
                { id: "r3", name: "SysAdmin", desc: "Config firewalls.", badge: "Root" }
            ];
            if(difficulty > 1) {
                roles.push({ id: "r4", name: "DB Admin", desc: "Manage schema.", badge: "DBA" });
            }
            
            const badges = roles.map(r => r.badge);
            badges.sort(() => Math.random() - 0.5); // Shuffle
            
            let matched = 0;

            let html = `
                <div style="text-align:center; margin-bottom:10px;">
                    <h3 style="color:var(--sky); margin:0;">Access Control</h3>
                    <p style="font-size:8px; color:#aaa;">Drag badges to the correct employee.</p>
                </div>
                <div style="display:flex; justify-content:space-around; margin-bottom:20px; flex-wrap:wrap; gap:5px;" id="rbac-badges">
                    ${badges.map(b => `<div draggable="true" data-badge="${b}" class="rbac-badge" style="background:var(--accent); color:#000; padding:10px; border:2px solid #fff; cursor:grab; font-size:10px; font-weight:bold;">🎫 ${b}</div>`).join('')}
                </div>
                <div style="display:flex; flex-direction:column; gap:10px;" id="rbac-roles">
                    ${roles.map(r => `
                        <div data-role="${r.badge}" class="rbac-role-drop" style="background:#222; border:2px dashed #555; padding:10px; display:flex; justify-content:space-between; align-items:center;">
                            <div>
                                <div style="color:var(--grass); font-size:12px; margin-bottom:5px;">${r.name}</div>
                                <div style="color:#aaa; font-size:8px;">${r.desc}</div>
                            </div>
                            <div class="rbac-slot" style="width:70px; height:35px; background:#111; border:2px solid #444; display:flex; justify-content:center; align-items:center;"></div>
                        </div>
                    `).join('')}
                </div>
            `;
            container.innerHTML = html;

            let draggedBadge = null;

            document.querySelectorAll('.rbac-badge').forEach(b => {
                b.addEventListener('dragstart', (e) => {
                    draggedBadge = b;
                    e.dataTransfer.setData('text/plain', b.dataset.badge);
                    b.style.opacity = '0.5';
                });
                b.addEventListener('dragend', (e) => {
                    b.style.opacity = '1';
                });
            });

            document.querySelectorAll('.rbac-role-drop').forEach(r => {
                r.addEventListener('dragover', (e) => {
                    e.preventDefault();
                    r.style.borderColor = 'var(--accent)';
                });
                r.addEventListener('dragleave', (e) => {
                    r.style.borderColor = '#555';
                });
                r.addEventListener('drop', (e) => {
                    e.preventDefault();
                    r.style.borderColor = '#555';
                    if(!draggedBadge) return;
                    
                    const requiredBadge = r.dataset.role;
                    const droppedBadge = draggedBadge.dataset.badge;

                    if (requiredBadge === droppedBadge) {
                        playSound && playSound('success');
                        r.querySelector('.rbac-slot').appendChild(draggedBadge);
                        r.querySelector('.rbac-slot').style.borderColor = 'var(--grass)';
                        draggedBadge.setAttribute('draggable', 'false');
                        draggedBadge.style.cursor = 'default';
                        draggedBadge = null;
                        matched++;
                        if(matched === roles.length) {
                            setTimeout(onWin, 800);
                        }
                    } else {
                        playSound && playSound('error');
                        window.showToast && window.showToast("SECURITY BREACH! Wrong privilege level.");
                        r.classList.add('shake');
                        setTimeout(() => r.classList.remove('shake'), 500);
                        onLose("You gave '" + droppedBadge + "' access to a '" + r.querySelector('div>div').innerText + "'. Principle of Least Privilege violated!");
                    }
                });
            });
        });
    },

    // 7. PHISHING SWIPE
    startPhishingSwipe: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("Phishing Swiper", "Analyze the URLs.<br><br>Drag LEFT for Phishing.<br>Drag RIGHT for Safe.<br><br>Don't be fooled by fake domains!", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            
            const urls = [
                { url: "http://192.168.1.100/admin", safe: false, reason: "IP address instead of domain name." },
                { url: "https://accounts.google.com/signin", safe: true, reason: "Valid domain and HTTPS." },
                { url: "http://paypa1-secure.tk/login", safe: false, reason: "Brand spoofing (paypa1)." },
                { url: "https://github.com/login", safe: true, reason: "Valid well-known domain." },
                { url: "https://xn--pypal-4ve.com", safe: false, reason: "Punycode homograph attack." },
                { url: "http://netflix-billing-update.info", safe: false, reason: "HTTP instead of HTTPS and generic TLD." }
            ];
            urls.sort(() => Math.random() - 0.5);
            let currentIdx = 0;
            const total = difficulty === 3 ? 5 : (difficulty === 2 ? 4 : 3);
            
            const renderCard = () => {
                if (currentIdx >= total) {
                    onWin();
                    return;
                }
                const item = urls[currentIdx];
                container.innerHTML = `
                    <div style="text-align:center; margin-bottom:10px;">
                        <h3 style="color:var(--sky); margin:0;">Phishing Swiper</h3>
                        <p style="font-size:10px; color:#aaa;">Email ${currentIdx+1} of ${total}</p>
                    </div>
                    <div style="position:relative; width:100%; height:200px; display:flex; justify-content:center; align-items:center; overflow:hidden;">
                        <div style="position:absolute; left:5px; color:var(--red); font-size:12px; font-weight:bold; opacity:0.8;">&lt; PHISH</div>
                        <div style="position:absolute; right:5px; color:var(--grass); font-size:12px; font-weight:bold; opacity:0.8;">SAFE &gt;</div>
                        
                        <div id="swipe-card" style="position:absolute; width:220px; height:120px; background:#222; border:4px solid #fff; display:flex; justify-content:center; align-items:center; text-align:center; padding:10px; font-size:10px; color:var(--sky); cursor:grab; user-select:none; transition: transform 0.2s; z-index:10; box-shadow:0 10px 20px rgba(0,0,0,0.5); word-wrap: break-word;">
                            ${escapeHtml(item.url)}
                        </div>
                    </div>
                    <div style="text-align:center; margin-top:20px;">
                        <button id="btn-swipe-phish" class="btn" style="background:var(--red); margin-right:10px;">&lt; Phish</button>
                        <button id="btn-swipe-safe" class="btn" style="background:var(--grass); color:#000;">Safe &gt;</button>
                    </div>
                `;

                const card = document.getElementById('swipe-card');
                let isDragging = false;
                let startX = 0;
                let currentX = 0;

                const checkAnswer = (guessedSafe) => {
                    if (guessedSafe === item.safe) {
                        playSound && playSound('success');
                        window.showToast && window.showToast("Correct!");
                        currentIdx++;
                        setTimeout(renderCard, 400);
                    } else {
                        playSound && playSound('error');
                        onLose(`You swiped wrong! ${item.reason}`);
                    }
                };

                const onDown = (e) => {
                    isDragging = true;
                    startX = e.clientX || (e.touches && e.touches[0] ? e.touches[0].clientX : 0);
                    card.style.transition = 'none';
                    card.style.cursor = 'grabbing';
                };
                const onMove = (e) => {
                    if(!isDragging) return;
                    const clientX = e.clientX || (e.touches && e.touches[0] ? e.touches[0].clientX : 0);
                    currentX = clientX - startX;
                    const rotate = currentX * 0.1;
                    card.style.transform = `translateX(${currentX}px) rotate(${rotate}deg)`;
                };
                const onUp = (e) => {
                    if(!isDragging) return;
                    isDragging = false;
                    card.style.transition = 'transform 0.3s';
                    card.style.cursor = 'grab';
                    
                    if (currentX > 80) {
                        card.style.transform = `translateX(300px) rotate(30deg)`;
                        checkAnswer(true);
                    } else if (currentX < -80) {
                        card.style.transform = `translateX(-300px) rotate(-30deg)`;
                        checkAnswer(false);
                    } else {
                        card.style.transform = `translateX(0px) rotate(0deg)`;
                    }
                };

                card.addEventListener('mousedown', onDown);
                card.addEventListener('touchstart', onDown, {passive:true});
                document.addEventListener('mousemove', onMove);
                document.addEventListener('touchmove', onMove, {passive:true});
                document.addEventListener('mouseup', onUp);
                document.addEventListener('touchend', onUp);

                // Button fallbacks
                document.getElementById('btn-swipe-phish').onclick = () => {
                    card.style.transform = `translateX(-300px) rotate(-30deg)`;
                    checkAnswer(false);
                };
                document.getElementById('btn-swipe-safe').onclick = () => {
                    card.style.transform = `translateX(300px) rotate(30deg)`;
                    checkAnswer(true);
                };
            };
            
            renderCard();
        });
    },

    // 8. WHACK-A-MOLE (Popup Defender)
    startWhackAMole: function(difficulty, onWin, onLose) {
        this.cleanup();
        this.showBriefing("Popup Defender", "Malicious popups are infecting your system!<br><br>Click the RED popups to close them before they corrupt the system.<br>Do not click the GREEN legitimate notifications!", () => {
            const container = document.getElementById(this.targetContainer || 'module-content');
            let health = 100;
            let score = 0;
            const targetScore = difficulty === 3 ? 15 : (difficulty === 2 ? 10 : 6);
            
            container.innerHTML = `
                <div style="text-align:center; margin-bottom:10px;">
                    <h3 style="color:var(--sky); margin:0;">Popup Defender</h3>
                    <div style="margin-top:5px; font-size:10px;">Health: <span id="wam-health" style="color:var(--grass)">${health}%</span> | Closed: <span id="wam-score">0</span>/${targetScore}</div>
                </div>
                <div id="wam-area" style="position:relative; width:100%; height:250px; background:#1a1a1a; border:4px solid #444; overflow:hidden;"></div>
            `;

            const area = document.getElementById('wam-area');
            let active = true;
            const spawnRate = difficulty === 3 ? 600 : (difficulty === 2 ? 800 : 1000);
            
            this.activeInterval = setInterval(() => {
                if(!active) return;
                
                const isMalicious = Math.random() > 0.3;
                const popup = document.createElement('div');
                popup.style.position = 'absolute';
                popup.style.width = '70px';
                popup.style.height = '40px';
                popup.style.left = Math.random() * (area.clientWidth - 70) + 'px';
                popup.style.top = Math.random() * (area.clientHeight - 40) + 'px';
                popup.style.border = '2px solid #fff';
                popup.style.display = 'flex';
                popup.style.justifyContent = 'center';
                popup.style.alignItems = 'center';
                popup.style.fontSize = '8px';
                popup.style.cursor = 'pointer';
                popup.style.userSelect = 'none';
                popup.style.boxShadow = '2px 2px 5px rgba(0,0,0,0.5)';

                if (isMalicious) {
                    popup.style.background = 'var(--red)';
                    popup.style.color = '#fff';
                    popup.innerHTML = "<strong>VIRUS.exe</strong><br>X";
                } else {
                    popup.style.background = 'var(--grass)';
                    popup.style.color = '#000';
                    popup.innerHTML = "<strong>System OK</strong><br>X";
                }

                popup.onmousedown = () => {
                    if(!active) return;
                    if(isMalicious) {
                        playSound && playSound('success');
                        score++;
                        document.getElementById('wam-score').innerText = score;
                        popup.remove();
                        if(score >= targetScore) {
                            active = false;
                            clearInterval(this.activeInterval);
                            onWin();
                        }
                    } else {
                        playSound && playSound('error');
                        health -= 20;
                        document.getElementById('wam-health').innerText = health + "%";
                        document.getElementById('wam-health').style.color = 'var(--red)';
                        popup.remove();
                        if(health <= 0) {
                            active = false;
                            clearInterval(this.activeInterval);
                            onLose("You closed critical system processes!");
                        }
                    }
                };

                area.appendChild(popup);

                setTimeout(() => {
                    if(popup.parentNode) {
                        popup.remove();
                        if(isMalicious && active) {
                            health -= 15;
                            document.getElementById('wam-health').innerText = health + "%";
                            document.getElementById('wam-health').style.color = 'var(--red)';
                            playSound && playSound('error');
                            if(health <= 0) {
                                active = false;
                                clearInterval(this.activeInterval);
                                onLose("Malware overtook the system!");
                            }
                        }
                    }
                }, difficulty === 3 ? 1200 : 1800);

            }, spawnRate);
        });
    }
};
