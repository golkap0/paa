const { chromium } = require('playwright');
const path = require('path');

(async () => {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36'
  });
  const page = await context.newPage();

  const url = 'https://www.apkmirror.com/apk/google-inc/youtube/youtube-20-05-46-release/youtube-20-05-46-2-android-apk-download/';
  
  console.log('Membuka halaman...');
  try {
    await page.goto(url, { waitUntil: 'networkidle', timeout: 60000 });

    console.log('Mencari tombol download...');
    
    // Strategi 1: Cari link yang mengandung teks "Download" secara eksplisit
    // Strategi 2: Cari tombol dengan class yang umum digunakan di APKMirror
    const downloadButton = page.locator('a:has-text("Download"), a:has-text("Download APK")').first();

    if (await downloadButton.count() === 0) {
      console.log('Strategi 1 gagal, mencoba mencari via selector CSS...');
      // Strategi 3: Mencari link yang mengarah ke varian download
      const fallbackButton = page.locator('a.download-button, a[href*="download/variant"]').first();
      if (await fallbackButton.count() > 0) {
        await fallbackButton.click();
      } else {
        throw new Error('Tombol download tetap tidak ditemukan.');
      }
    } else {
      await downloadButton.click();
    }

    console.log('Menunggu proses download dimulai...');
    const [ download ] = await Promise.all([
      page.waitForEvent('download', { timeout: 60000 }),
      // Jika klik pertama hanya pindah halaman, kita harus cari tombol download kedua di halaman baru
      page.waitForTimeout(3000) 
    ]);

    const filePath = path.join(__dirname, 'youtube-20-05-46.apk');
    await download.saveAs(filePath);
    console.log(`Berhasil! File disimpan di: ${filePath}`);

  } catch (error) {
    console.error('Terjadi kesalahan:', error.message);
    // Ambil screenshot untuk debugging jika gagal
    await page.screenshot({ path: 'error_screenshot.png' });
    console.log('Screenshot error disimpan sebagai error_screenshot.png');
    process.exit(1);
  } finally {
    await browser.close();
  }
})();
