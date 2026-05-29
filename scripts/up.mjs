import { spawn } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), '..');
const adminPanel = path.join(root, 'admin-panel');

function run(command, args, { cwd = root, check = true } = {}) {
  return new Promise((resolve, reject) => {
    const child = spawn(command, args, {
      cwd,
      stdio: 'inherit',
      shell: process.platform === 'win32'
    });

    child.on('error', reject);
    child.on('exit', code => {
      if (check && code !== 0) {
        reject(new Error(`${command} ${args.join(' ')} exited with code ${code}`));
        return;
      }
      resolve(code);
    });
  });
}

async function main() {
  console.log('\nSIEP — arranque de desarrollo\n');

  console.log('[1/4] PostgreSQL (Docker)...');
  await run('docker', ['compose', 'up', '-d', 'db']);

  console.log('[2/4] Backend Spring Boot (Docker)...');
  await run('docker', ['compose', 'up', '-d', '--build', 'backend']);

  console.log('[3/4] Esperando API en http://localhost:8090 ...');
  await run('npx', ['wait-on', 'http-get://127.0.0.1:8090/swagger-ui.html', '-t', '180000', '-i', '2000']);

  console.log('[4/4] Frontend Angular (dev server)...');
  console.log('\n  Portal:  http://localhost:4200');
  console.log('  API:     http://localhost:8090');
  console.log('  Swagger: http://localhost:8090/swagger-ui.html\n');

  await run('npm', ['start'], { cwd: adminPanel });
}

main().catch(error => {
  console.error(`\nError al arrancar SIEP: ${error.message}\n`);
  process.exit(1);
});
