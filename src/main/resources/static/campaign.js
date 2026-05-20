const campaignLevels = [
    // Easy Difficulty
    { level: 1, type: "phishing_swipe", difficulty: 1, title: "Phishing Swiper", description: "Swipe Left for Phish, Right for Safe URLs." },
    { level: 2, type: "rbac", difficulty: 1, title: "Role-Based Access", description: "Drag and drop the correct badges onto employee profiles." },
    { level: 3, type: "whack_a_mole", difficulty: 1, title: "Popup Defender I", description: "Close malicious popups before system health depletes." },
    { level: 4, type: "firewall", difficulty: 1, title: "Firewall Defender I", description: "Drop the red packets and allow the green ones." },
    { level: 5, type: "sqli", difficulty: 1, title: "SQLi Lockpicker I", description: "Use basic SQL injection to bypass the authentication gate." },
    { level: 6, type: "ransomware", difficulty: 1, title: "Ransomware Containment I", description: "Quarantine infected nodes before they encrypt the whole network." },
    { level: 7, type: "privesc", difficulty: 1, title: "PrivEsc Maze I", description: "Navigate the network and find exploits to reach the Domain Controller." },
    { level: 8, type: "crypto_decode", difficulty: 1, title: "Crypto Decoder I", description: "Decrypt the Caesar Cipher to find the password." },
    
    // Medium Difficulty
    { level: 9, type: "phishing_swipe", difficulty: 2, title: "Phishing Swiper II", description: "More tricky URLs. Swipe carefully." },
    { level: 10, type: "rbac", difficulty: 2, title: "Advanced RBAC", description: "Assign roles to a larger team. Don't violate Least Privilege." },
    { level: 11, type: "whack_a_mole", difficulty: 2, title: "Popup Defender II", description: "Popups appear faster. Do not close system processes!" },
    { level: 12, type: "firewall", difficulty: 2, title: "Firewall Defender II", description: "Faster traffic! Drop the malicious packets quickly." },
    { level: 13, type: "sqli", difficulty: 2, title: "SQLi Lockpicker II", description: "Use UNION-based SQLi to extract data from other tables." },
    { level: 14, type: "ransomware", difficulty: 2, title: "Ransomware Containment II", description: "Larger network grid. Contain the outbreak!" },
    { level: 15, type: "privesc", difficulty: 2, title: "PrivEsc Maze II", description: "More complex network routing. Find the SSH keys." },
    { level: 16, type: "crypto_decode", difficulty: 2, title: "Crypto Decoder II", description: "A slightly harder cipher to crack." },
    
    // Hard Difficulty
    { level: 17, type: "whack_a_mole", difficulty: 3, title: "Popup Defender III", description: "Malware storm! Close the popups instantly." },
    { level: 18, type: "phishing_swipe", difficulty: 3, title: "Phishing Swiper III", description: "Highly sophisticated spear-phishing attempts." },
    { level: 19, type: "ransomware", difficulty: 3, title: "Ransomware Containment III", description: "Massive network outbreak. You must be fast." },
    { level: 20, type: "firewall", difficulty: 3, title: "The Final Boss", description: "Defend against the ultimate network assault." }
];
