import { spawnSync } from 'node:child_process';
import { createHash, randomUUID } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';

const DB_NAME = process.env.CYNA_DB_NAME ?? 'cyna';
const DB_USER = process.env.CYNA_DB_USER ?? 'cyna';
const DB_PASSWORD = process.env.CYNA_DB_PASSWORD ?? 'cyna_dev_password';
const DB_HOST = process.env.CYNA_DB_HOST ?? process.env.PGHOST;
const DB_PORT = process.env.CYNA_DB_PORT ?? process.env.PGPORT ?? '5432';
const POSTGRES_CONTAINER = process.env.CYNA_POSTGRES_CONTAINER ?? 'cyna-postgres';

function formatCommandError(context: string, stdout: string | null, stderr: string | null): string {
  return [
    context,
    stdout?.trim() ? `stdout:\n${stdout.trim()}` : '',
    stderr?.trim() ? `stderr:\n${stderr.trim()}` : '',
  ]
    .filter(Boolean)
    .join('\n\n');
}

function escapeSqlLiteral(value: string): string {
  return value.replace(/'/g, "''");
}

function shouldUseDirectPsql(): boolean {
  return process.env.CYNA_USE_DIRECT_PSQL === 'true' || !!DB_HOST;
}

function resolveDockerContainer(): string {
  if (process.env.CYNA_POSTGRES_CONTAINER) {
    return process.env.CYNA_POSTGRES_CONTAINER;
  }

  const result = spawnSync('docker', ['ps', '--format', '{{.Names}}'], {
    encoding: 'utf-8',
  });

  if (result.status !== 0) {
    throw new Error(formatCommandError('Unable to list running Docker containers for PostgreSQL access', result.stdout, result.stderr));
  }

  const names = result.stdout
    .split(/\r?\n/)
    .map((name) => name.trim())
    .filter(Boolean);

  return names.find((name) => name === POSTGRES_CONTAINER)
    ?? names.find((name) => name.includes('postgres'))
    ?? POSTGRES_CONTAINER;
}

function runDirectPsql(sql: string, options: { tuplesOnly?: boolean }): string {
  if (!DB_HOST) {
    throw new Error('Direct psql mode requires CYNA_DB_HOST or PGHOST');
  }

  const args = [
    '-h',
    DB_HOST,
    '-p',
    DB_PORT,
    '-U',
    DB_USER,
    '-d',
    DB_NAME,
    '-v',
    'ON_ERROR_STOP=1',
  ];

  if (options.tuplesOnly ?? true) {
    args.push('-At');
  }

  const result = spawnSync('psql', args, {
    encoding: 'utf-8',
    input: sql,
    env: {
      ...process.env,
      PGPASSWORD: DB_PASSWORD,
    },
  });

  if (result.status !== 0) {
    throw new Error(formatCommandError(`psql command failed against ${DB_HOST}:${DB_PORT}/${DB_NAME}`, result.stdout, result.stderr));
  }

  return result.stdout.trim();
}

function runDockerPsql(sql: string, options: { tuplesOnly?: boolean }): string {
  const container = resolveDockerContainer();
  const args = [
    'exec',
    '-i',
    container,
    'psql',
    '-U',
    DB_USER,
    '-d',
    DB_NAME,
    '-v',
    'ON_ERROR_STOP=1',
  ];

  if (options.tuplesOnly ?? true) {
    args.push('-At');
  }

  const result = spawnSync('docker', args, {
    encoding: 'utf-8',
    input: sql,
  });

  if (result.status !== 0) {
    throw new Error(formatCommandError(`psql command failed against container "${container}"`, result.stdout, result.stderr));
  }

  return result.stdout.trim();
}

function runPsql(sql: string, options: { tuplesOnly?: boolean } = {}): string {
  if (shouldUseDirectPsql()) {
    return runDirectPsql(sql, options);
  }

  return runDockerPsql(sql, options);
}

export async function promoteUserToAdmin(email: string): Promise<void> {
  runPsql(
    `UPDATE user_schema.users SET role = 'ADMIN' WHERE email = '${escapeSqlLiteral(email)}';`,
  );
}

export async function activateUserAndIssueRefreshToken(email: string): Promise<string> {
  const escapedEmail = escapeSqlLiteral(email);
  const userId = runPsql(
    `SELECT id FROM user_schema.users WHERE email = '${escapedEmail}' ORDER BY created_at DESC LIMIT 1;`,
  );

  if (!userId) {
    throw new Error(`Unable to issue refresh token: user not found for email ${email}`);
  }

  const rawRefreshToken = randomUUID();
  const tokenHash = createHash('sha256')
    .update(rawRefreshToken, 'utf8')
    .digest('hex');

  runPsql(
    `
      UPDATE user_schema.users SET status = 'ACTIVE' WHERE id = '${escapeSqlLiteral(userId)}';
      INSERT INTO user_schema.refresh_tokens (id, user_id, token_hash, expires_at, revoked, created_at)
      VALUES (gen_random_uuid(), '${escapeSqlLiteral(userId)}', '${escapeSqlLiteral(tokenHash)}', NOW() + INTERVAL '24 hours', FALSE, NOW());
    `,
    { tuplesOnly: false },
  );

  return rawRefreshToken;
}

export async function ensureDashboardSeedData(): Promise<void> {
  const seededUsers = Number(
    runPsql(
      "SELECT COUNT(*) FROM user_schema.users WHERE email LIKE 'seed-dash-2026-user-%@cyna.test';",
    ),
  );

  if (seededUsers > 0) {
    return;
  }

  const seedFile = resolve(__dirname, '../../cyna-backend/scripts/seed-demo-data.sql');
  const sql = readFileSync(seedFile, 'utf-8');
  runPsql(sql, { tuplesOnly: false });
}
