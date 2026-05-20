const fs = require('fs');
const jsdom = require('jsdom');
const { JSDOM } = jsdom;

const html = fs.readFileSync('src/main/resources/static/index.html', 'utf8');
const script = fs.readFileSync('src/main/resources/static/game.js', 'utf8');

const dom = new JSDOM(html, { runScripts: "dangerously", resources: "usable" });
const window = dom.window;
const document = window.document;

// Add fetch polyfill mock
window.fetch = async (url, options) => {
    console.log("Fetch called with url:", url, "options:", options);
    return {
        ok: true,
        json: async () => ({ status: 'otp_sent', role: 'student' })
    };
};

try {
    const scriptEl = document.createElement("script");
    scriptEl.textContent = script;
    document.body.appendChild(scriptEl);
    console.log("Script loaded successfully.");
} catch (e) {
    console.error("Script error:", e);
}

// Test register
document.getElementById('reg-name').value = 'Test User';
document.getElementById('reg-email').value = 'test@example.com';
document.getElementById('reg-password').value = 'password123';
document.getElementById('reg-role').value = 'STUDENT';
document.getElementById('reg-classroom').value = '1234';

window.registerUser().then(() => {
    console.log("registerUser completed. Toast:", document.getElementById('toast').textContent);
}).catch(e => console.error(e));

