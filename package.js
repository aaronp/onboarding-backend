import { createRequire } from "module";
import path from 'path';
import { fileURLToPath } from 'url';

const require = createRequire(import.meta.url);
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const fs = require('fs');
const archiver = require('archiver');

// Output file
const output = fs.createWriteStream(__dirname + '/kind-app.zip');
const archive = archiver('zip', {
    zlib: { level: 9 }
});

output.on('close', function () {
    console.log(archive.pointer() + ' total bytes');
    console.log('Archive has been finalized and the output file descriptor has closed.');
});

archive.on('error', function (err) {
    throw err;
});

// Pipe archive data to the file
archive.pipe(output);

// Append the entire 'dist' folder
archive.directory('dist/', false);

archive.finalize();
