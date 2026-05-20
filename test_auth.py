import asyncio
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page()
        page.on("console", lambda msg: print(f"Browser console: {msg.text}"))
        page.on("pageerror", lambda err: print(f"Browser error: {err}"))
        
        await page.goto('http://localhost:8080')
        print("Page loaded")
        
        # Test clicking Login without inputs
        await page.click('button:has-text("Login")')
        await asyncio.sleep(1)
        
        # Fill inputs and test Login
        await page.fill('#login-email', 'test@example.com')
        await page.fill('#login-password', 'password123')
        await page.click('#form-login button:has-text("Login")')
        await asyncio.sleep(2)
        
        # Switch to Register
        await page.click('#tab-register')
        await asyncio.sleep(1)
        
        # Fill inputs and test Register
        await page.fill('#reg-name', 'Test User')
        await page.fill('#reg-email', 'test2@example.com')
        await page.fill('#reg-password', 'password123')
        await page.click('#form-register button:has-text("Create Account")')
        await asyncio.sleep(2)
        
        await browser.close()

asyncio.run(main())
