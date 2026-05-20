// minigames.js - Logic for Interactive Arcade Games

const MiniGames = {
    activeInterval: null,

    cleanup: function() {
        if(this.activeInterval) clearInterval(this.activeInterval);
        this.activeInterval = null;
        const container = document.getElementById(this.targetContainer || 'module-content');
        if(container) container.innerHTML = '';
    },

    // 1. FIREWALL DEFENDER
    startFirewall: function(difficulty, onWin, onLose) {
        this.cleanup();
        const container = document.getElementById(this.targetContainer || 'module-content');
        container.innerHTML = `
            <div style="text-align:center; margin-bottom:10px;">
                <h3 style="color:var(--sky); margin:0;">Firewall Defender</h3>
                <p style="font-size:10px; color:#aaa;">Click the <span style="color:var(--red);">RED (Malicious)</span> packets to drop them. Let <span style="color:var(--grass);">GREEN (Safe)</span> packets through.</p>
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
                // Wait for existing to finish
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
            packet.dataset.malicious = isMalicious;
            
            // Port label
            packet.innerHTML = `<span style="font-size:8px; display:block; text-align:center; margin-top:8px; color:#000;">${isMalicious ? '23' : '443'}</span>`;

            packet.onclick = function() {
                if(isMalicious) {
                    playSound('success');
                    score++;
                    document.getElementById('fw-score').innerText = score;
                    packet.remove();
                } else {
                    playSound('error');
                    health -= 20; // Dropped safe traffic!
                    document.getElementById('fw-health').innerText = health;
                    document.getElementById('fw-health').style.color = 'var(--red)';
                    packet.remove();
                    if(health <= 0) { clearInterval(MiniGames.activeInterval); onLose("Dropped too much legitimate traffic! Business halted."); }
                }
            };

            area.appendChild(packet);

            // Animate
            let y = -30;
            const fallInt = setInterval(() => {
                if(!packet.parentNode) { clearInterval(fallInt); return; }
                y += speed;
                packet.style.top = y + 'px';
                if(y > area.clientHeight) {
                    clearInterval(fallInt);
                    if(isMalicious && packet.parentNode) {
                        health -= 20; // Malicious traffic got through
                        document.getElementById('fw-health').innerText = health;
                        document.getElementById('fw-health').style.color = 'var(--red)';
                        if(health <= 0) { clearInterval(MiniGames.activeInterval); onLose("Server overloaded by malicious traffic."); }
                    } else if (!isMalicious && packet.parentNode) {
                        score++; // Safe traffic got through
                        document.getElementById('fw-score').innerText = score;
                    }
                    packet.remove();
                }
            }, 30);

        }, spawnRate);
    },

    // 2. SQLi LOCKPICKER
    startSQLi: function(difficulty, onWin, onLose) {
        this.cleanup();
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
            correctAnswer = "admin' --"; // MySQL comment to ignore password check
        }

        // Shuffle blocks
        blocks.sort(() => Math.random() - 0.5);

        container.innerHTML = `
            <div style="text-align:center; margin-bottom:20px;">
                <h3 style="color:var(--sky); margin:0;">SQLi Lockpicker</h3>
                <p style="font-size:10px; color:#aaa;">Select the correct payload to bypass the query logic.</p>
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
                        playSound('success');
                        onWin();
                    } else {
                        playSound('error');
                        onLose("Syntax Error or logic failed to bypass the authentication gate.");
                    }
                }, 500);
            };
        });
    },

    // 3. RANSOMWARE QUARANTINE
    startRansomware: function(difficulty, onWin, onLose) {
        this.cleanup();
        const container = document.getElementById(this.targetContainer || 'module-content');
        const gridSize = difficulty === 3 ? 6 : (difficulty === 2 ? 5 : 4);
        const totalNodes = gridSize * gridSize;
        
        container.innerHTML = `
            <div style="text-align:center; margin-bottom:10px;">
                <h3 style="color:var(--sky); margin:0;">Ransomware Quarantine</h3>
                <p style="font-size:10px; color:#aaa;">Click computers to disconnect them before the RED infection spreads!</p>
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
            el.style.background = 'var(--grass)'; // Clean
            el.style.border = '2px solid #000';
            el.style.cursor = 'pointer';
            el.dataset.state = 'clean'; // clean, infected, offline
            
            el.onclick = () => {
                if(!active) return;
                if(el.dataset.state === 'clean') {
                    el.dataset.state = 'offline';
                    el.style.background = '#555'; // Disconnected
                    playSound('success');
                }
            };
            grid.appendChild(el);
            nodes.push(el);
        }

        // Start infection in center-ish
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
                    // Check neighbors
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
                                if(Math.random() > 0.5) newInfections.push(idx); // 50% chance to spread per tick
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
                // Outbreak contained
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
    },

    // 4. PRIVILEGE ESCALATION MAZE
    startPrivEsc: function(difficulty, onWin, onLose) {
        this.cleanup();
        const container = document.getElementById(this.targetContainer || 'module-content');
        
        let inventory = [];
        let currentNode = "Guest-PC";
        
        const nodes = {
            "Guest-PC": { unlock: null, text: "Your starting point. Low privileges.", contains: "SSH-Key" },
            "Dev-Server": { unlock: "SSH-Key", text: "Development server. Has some outdated scripts.", contains: "Root-Exploit" },
            "Database": { unlock: "DB-Password", text: "Customer database. You shouldn't be here.", contains: "Admin-Hash" },
            "Domain-Controller": { unlock: "Root-Exploit", text: "The heart of the network.", contains: "Win" }
        };

        if(difficulty > 1) {
            nodes["Guest-PC"].contains = "DB-Password";
            nodes["Database"].contains = "SSH-Key"; // Force detour
        }

        const renderMap = () => {
            let html = `
                <div style="text-align:center; margin-bottom:20px;">
                    <h3 style="color:var(--sky); margin:0;">PrivEsc Maze</h3>
                    <p style="font-size:10px; color:#aaa;">Find exploits and pivot through the network to compromise the Domain Controller.</p>
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
                        // Search current node
                        const item = nodes[nodeName].contains;
                        if(item === "Win") {
                            playSound('success');
                            onWin();
                        } else if(item && !inventory.includes(item)) {
                            playSound('success');
                            showToast("Found item: " + item);
                            inventory.push(item);
                            renderMap();
                        } else {
                            showToast("Nothing else of value here.");
                        }
                    } else if (access) {
                        playSound('success');
                        currentNode = nodeName;
                        renderMap();
                    } else {
                        playSound('error');
                        const panel = document.querySelector('#screen-campaign-game .panel') || document.querySelector('#screen-module .panel');
                        panel.classList.add('shake');
                        setTimeout(() => panel.classList.remove('shake'), 500);
                        showToast("Access Denied. Missing requirement.");
                    }
                }
            });
        };

        renderMap();
    },

    // 5. CRYPTO DECODER
    startCryptoDecoder: function(difficulty, onWin, onLose) {
        this.cleanup();
        const container = document.getElementById(this.targetContainer || 'module-content');
        
        let targetWord = "";
        let shift = 0;
        
        if (difficulty === 1) {
            targetWord = "ADMIN";
            shift = 3;
        } else if (difficulty === 2) {
            targetWord = "SECURE";
            shift = 5;
        } else {
            targetWord = "ENCRYPTED";
            shift = 7;
        }

        const encrypt = (text, s) => {
            return text.split('').map(c => String.fromCharCode((c.charCodeAt(0) - 65 + s) % 26 + 65)).join('');
        };
        
        const encryptedWord = encrypt(targetWord, shift);
        
        container.innerHTML = `
            <div style="text-align:center; margin-bottom:20px;">
                <h3 style="color:var(--sky); margin:0;">Crypto Decoder</h3>
                <p style="font-size:10px; color:#aaa;">Decrypt the Caesar Cipher to find the password.</p>
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
            // Negative shift means decoding
            const decoded = encryptedWord.split('').map(c => {
                let code = c.charCodeAt(0) - 65 + currentShift;
                while(code < 0) code += 26;
                return String.fromCharCode((code % 26) + 65);
            }).join('');
            document.getElementById('crypto-preview').innerText = decoded;
        };

        document.getElementById('crypto-down').onclick = () => {
            currentShift--;
            updatePreview();
        };

        document.getElementById('crypto-up').onclick = () => {
            currentShift++;
            updatePreview();
        };

        document.getElementById('crypto-submit').onclick = () => {
            const preview = document.getElementById('crypto-preview').innerText;
            if (preview === targetWord) {
                playSound('success');
                onWin();
            } else {
                playSound('error');
                onLose("Incorrect decryption attempt.");
            }
        };
    }
};
