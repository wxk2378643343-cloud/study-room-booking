import { chromium } from "playwright";

const wait = (ms) => new Promise((r) => setTimeout(r, ms));

async function captureMobile(browser) {
  const page = await browser.newPage({ viewport: { width: 390, height: 844 } });
  await page.goto("http://localhost:5173/login", { waitUntil: "networkidle" });
  await page.screenshot({ path: "../docs/images/fig6-1-mobile-login.png", fullPage: true });

  await page.fill('input[placeholder="手机号"]', '13800000002');
  await page.fill('input[placeholder="密码"]', '123456');
  await page.getByRole("button", { name: "登录" }).click();
  await page.waitForURL("**/", { timeout: 12000 });
  await wait(1500);
  await page.screenshot({ path: "../docs/images/fig6-2-mobile-home.png", fullPage: true });

  await page.goto("http://localhost:5173/rooms", { waitUntil: "networkidle" });
  await wait(1000);
  const roomCard = page.locator('.room-card').first();
  if (await roomCard.count()) {
    await roomCard.click();
    await page.waitForURL("**/rooms/*", { timeout: 12000 });
  }
  await wait(1200);
  await page.screenshot({ path: "../docs/images/fig6-3-mobile-room-detail.png", fullPage: true });

  await page.goto("http://localhost:5173/booking", { waitUntil: "networkidle" });
  await wait(1200);
  await page.screenshot({ path: "../docs/images/fig6-4-mobile-booking.png", fullPage: true });

  await page.goto("http://localhost:5173/user", { waitUntil: "networkidle" });
  await wait(1000);
  const pointsBtn = page.getByRole('button', { name: /积分流水/ });
  if (await pointsBtn.count()) {
    await pointsBtn.click();
    await wait(900);
  }
  await page.screenshot({ path: "../docs/images/fig6-5-mobile-user-records.png", fullPage: true });
  await page.close();
}

async function captureAdmin(browser) {
  const page = await browser.newPage({ viewport: { width: 1440, height: 900 } });
  await page.goto("http://localhost:5174/login", { waitUntil: "networkidle" });
  await page.screenshot({ path: "../docs/images/fig6-6-admin-login.png", fullPage: true });

  await page.fill('input[placeholder="手机号"]', '13800000001');
  await page.fill('input[placeholder="密码"]', '123456');
  await page.getByRole('button', { name: '进入管理台' }).click();
  await page.waitForURL('**/admin', { timeout: 12000 });
  await wait(1500);
  await page.screenshot({ path: "../docs/images/fig6-7-admin-dashboard.png", fullPage: true });

  await page.getByRole('button', { name: '举报处理' }).click();
  await wait(1000);
  await page.screenshot({ path: "../docs/images/fig6-8-admin-risk-report.png", fullPage: true });

  await page.getByRole('button', { name: '自习室管理' }).click();
  await wait(1000);
  await page.screenshot({ path: "../docs/images/fig6-9-admin-room-manage.png", fullPage: true });

  const userBtn = page.getByRole('button', { name: '用户管理' });
  if (await userBtn.count()) {
    await userBtn.click();
    await wait(1000);
    await page.screenshot({ path: "../docs/images/fig6-10-admin-user-manage.png", fullPage: true });
  }

  await page.close();
}

const browser = await chromium.launch({ headless: true });
try {
  await captureMobile(browser);
  await captureAdmin(browser);
} finally {
  await browser.close();
}
