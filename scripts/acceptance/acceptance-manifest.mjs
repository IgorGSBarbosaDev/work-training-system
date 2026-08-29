import { execFile } from 'node:child_process'
import { promisify } from 'node:util'
import { readdir } from 'node:fs/promises'
import { writeArtifact } from './http.mjs'

const execFileAsync = promisify(execFile)

async function version(command, args) {
  try {
    const { stdout, stderr } = await execFileAsync(command, args)
    return `${stdout || stderr}`.trim().split('\n')[0]
  } catch (error) {
    return `unavailable (${error.code ?? 'error'})`
  }
}

async function main() {
  const artifacts = await readdir('acceptance-artifacts').catch(() => [])
  const manifest = {
    generatedAt: new Date().toISOString(),
    commit: process.env.GITHUB_SHA ?? await version('git', ['rev-parse', 'HEAD']),
    runId: process.env.GITHUB_RUN_ID ?? 'local',
    runAttempt: process.env.GITHUB_RUN_ATTEMPT ?? 'local',
    tools: {
      java: await version('java', ['-version']), node: await version('node', ['--version']),
      npm: await version('npm', ['--version']), docker: await version('docker', ['--version']),
      dockerServer: await version('docker', ['info', '--format', '{{.ServerVersion}}']),
      ffmpeg: await version('ffmpeg', ['-version']),
    },
    checks: [
      'backend verify Java 21', 'backend verify Java 25', 'frontend tests', 'frontend lint',
      'frontend audit', 'frontend build', 'Compose validation', 'idempotent seed', 'API smoke',
      'SMTP FAILED -> PENDING -> SENT', 'Playwright Chromium desktop', 'Playwright mobile viewport',
    ],
    artifacts: [...artifacts, 'frontend/test-results', 'frontend/playwright-report'].sort(),
  }
  await writeArtifact('acceptance-manifest.json', manifest)
  console.log(JSON.stringify(manifest, null, 2))
}

main().catch((error) => {
  console.error(error.message)
  process.exitCode = 1
})
