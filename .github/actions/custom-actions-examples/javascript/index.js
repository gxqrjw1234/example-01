const fs = require('node:fs');

const outputFile = process.env.GITHUB_OUTPUT;
const input = process.env.INPUT_MESSAGE || 'Hello from JavaScript Action';
const result = `${input} (processed by Node.js)`;

console.log(result);

if (!outputFile) {
  throw new Error('GITHUB_OUTPUT is not available. Run this file inside GitHub Actions.');
}

fs.appendFileSync(outputFile, `result=${result}\n`, { encoding: 'utf8' });
