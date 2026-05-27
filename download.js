const { chromium } = require('playwright');
const fs = require('fs');
const path = require('path');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext();
  const page = await context.newPage();

  // URL halaman yang Anda berikan
  const url = 'https://www.apkmirror.com/apk/google-inc/youtube/youtube-20-05-46-release/youtube-20-05-46-2-android-apk-download/';
  
  console.log('Membuka halaman...');
  await page.goto(url, { waitUntil: 'networkidle' });

  // Mencari tombol download (berdasarkan teks atau selector)
  // Catatan: Selector ini mungkin perlu disesuaikan jika struktur web berubah
  console.log('Mencari tombol download...');
  const downloadButton = page.locator('a[href*="download"]'); 
  
  if (await downloadButton.count() > 0) {
    const [ download ] = await Promise.all([
      page.waitForEvent('download'), // Menunggu event download dimulai
      downloadButton.click(),        // Klik tombol
    ]);

    const filePath = path.join(__dirname, 'youtube-20-05-46.apk');
    await download.saveAs(filePath);
    console.log(`Berhasil! File disimpan di: ${filePath}`);
  } else {
    console.error('Tombol download tidak ditemukan.');
    process.exit(1);
  }

  await browser.close();
})();
