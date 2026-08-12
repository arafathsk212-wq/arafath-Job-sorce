const https = require('https');
const url = 'https://www.dice.com/jobs?q=java&location=remote';
const options = { headers: { 'User-Agent': 'Mozilla/5.0' } };
https.get(url, options, res => {
  let data = '';
  res.on('data', chunk => data += chunk);
  res.on('end', () => {
    const scriptRegex = /self\.__next_f\.push\(\[\d+,"((?:[^"\\]|\\.)*?)"/gs;
    let m;
    while ((m = scriptRegex.exec(data)) !== null) {
      const raw = m[1];
      const decoded = raw.replace(/\\n/g, '\n').replace(/\\"/g, '"').replace(/\\\//g, '/').replace(/\\t/g, '\t');
      const idx = decoded.indexOf('{"jobList":');
      if (idx !== -1) {
        const s = decoded.slice(idx, idx+2000);
        console.log('FOUND', s);
        break;
      }
    }
  });
}).on('error', e => console.error(e));
