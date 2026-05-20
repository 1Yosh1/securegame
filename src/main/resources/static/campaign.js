const campaignLevels = [
    { level: 1, type: "phishing", difficulty: 1, title: "Phishing Basics", description: "Identify the safe and malicious URLs." },
    { level: 2, type: "password", difficulty: 1, title: "Password Fortress", description: "Create a strong password to defend the server." },
    { level: 3, type: "mfa", difficulty: 1, title: "MFA Shield", description: "Defend against an MFA fatigue attack." },
    { level: 4, type: "matching", difficulty: 1, title: "Security Concepts", description: "Match the security concepts with their definitions." },
    { level: 5, type: "firewall", difficulty: 1, title: "Firewall Defender I", description: "Drop the red packets (Port 23) and allow the green ones (Port 443)." },
    { level: 6, type: "sqli", difficulty: 1, title: "SQLi Lockpicker I", description: "Use basic SQL injection to bypass the authentication gate." },
    { level: 7, type: "ransomware", difficulty: 1, title: "Ransomware Containment I", description: "Quarantine infected nodes before they encrypt the whole network." },
    { level: 8, type: "privesc", difficulty: 1, title: "PrivEsc Maze I", description: "Navigate the network and find exploits to reach the Domain Controller." },
    { level: 9, type: "crypto_decode", difficulty: 1, title: "Crypto Decoder I", description: "Decrypt the Caesar Cipher to find the password." },
    
    // Medium Difficulty
    { level: 10, type: "firewall", difficulty: 2, title: "Firewall Defender II", description: "Faster traffic! Drop the malicious packets quickly." },
    { level: 11, type: "sqli", difficulty: 2, title: "SQLi Lockpicker II", description: "Use UNION-based SQLi to extract data from other tables." },
    { level: 12, type: "ransomware", difficulty: 2, title: "Ransomware Containment II", description: "Larger network grid. Contain the outbreak!" },
    { level: 13, type: "privesc", difficulty: 2, title: "PrivEsc Maze II", description: "More complex network routing. Find the SSH keys." },
    { level: 14, type: "crypto_decode", difficulty: 2, title: "Crypto Decoder II", description: "A slightly harder cipher to crack." },
    
    // Hard Difficulty
    { level: 15, type: "firewall", difficulty: 3, title: "Firewall Defender III", description: "DDoS incoming! Maximum packet speed." },
    { level: 16, type: "sqli", difficulty: 3, title: "SQLi Lockpicker III", description: "Advanced SQLi. Use comments to bypass logic." },
    { level: 17, type: "ransomware", difficulty: 3, title: "Ransomware Containment III", description: "Massive network outbreak. You must be fast." },
    { level: 18, type: "privesc", difficulty: 3, title: "PrivEsc Maze III", description: "The ultimate network maze. Get Domain Admin." },
    { level: 19, type: "crypto_decode", difficulty: 3, title: "Crypto Decoder III", description: "Crack the final cipher to proceed." },
    
    { level: 20, type: "firewall", difficulty: 3, title: "The Final Boss", description: "Defend against the ultimate network assault." }
];
