# Aceite técnico e ambiente

**Status da implementação:** preparado para execução em CI; a execução local deste commit ficou bloqueada pela ausência do daemon Docker e do JDK 21 nesta máquina.

## Contrato de execução

O ambiente oficial usa Java 21 estrito, Node.js 22 e Docker com engine Linux. A versão da JVM é validada pelo Maven Enforcer e pelos scripts de pré-requisito; Java 22 ou superior falha antes da compilação.

```text
./scripts/check-environment.ps1
./mvnw.cmd -B -ntp verify
cd frontend
npm ci
npm test
npm run lint
npm run build
```

No PowerShell, o ambiente integrado pode ser executado assim:

```text
Copy-Item .env.example .env
docker compose --project-name work-training-system-acceptance up --build --detach
node scripts/acceptance/seed-demo.mjs
node scripts/acceptance/smoke.mjs
cd frontend
npx playwright install chromium
npm run e2e
```

O encerramento seguro do aceite é feito com `docker compose --project-name work-training-system-acceptance down --volumes --remove-orphans`. O nome de projeto isolado evita remover volumes de outro ambiente.

## Matriz de evidências

| Critério | Comando/teste | Pré-requisito | Resultado esperado | Resultado obtido | Data | Commit | Artefato/observação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Java 21 estrito | `./scripts/check-environment.ps1`; `./mvnw.cmd validate` | JDK 21 | JVM 21.x aceita; demais versões falham | Pendente localmente: JVM detectada era 25 | 22/08/2026 | branch de aceite | Validar no CI com `setup-java@v4` |
| Maven completo | `./mvnw -B -ntp verify` | Java 21 + Docker para Testcontainers | Build e testes verdes | Pendente localmente: daemon Docker indisponível | 22/08/2026 | branch de aceite | Surefire será publicado pelo CI |
| Frontend | `npm ci`, `npm test`, `npm run lint`, `npm run build` | Node 22 | Suíte, lint e build verdes | A executar após atualizar lockfile | 22/08/2026 | branch de aceite | Relatórios do job frontend |
| Compose | `docker compose config --quiet` | Docker CLI | Configuração válida | Verificado na preparação; repetir no CI | 22/08/2026 | branch de aceite | Job `compose` |
| Serviços saudáveis | `docker compose up --build --detach` + readiness | Docker daemon | PostgreSQL, MinIO, Mailpit, backend e frontend saudáveis | Pendente localmente | 22/08/2026 | branch de aceite | `compose.log` e `docker compose ps` |
| Seed idempotente | `node scripts/acceptance/seed-demo.mjs` duas vezes | Compose saudável | Mesmos IDs, sem duplicidade | Pendente em runtime | 22/08/2026 | branch de aceite | `demo-state.json`, `seed-result.json` |
| Fluxo API crítico | `node scripts/acceptance/smoke.mjs` | Seed + Compose | Fluxo treinamento → vídeo → avaliação → conclusão → certificado | Pendente em runtime | 22/08/2026 | branch de aceite | `smoke-result.json` |
| Mailpit | smoke + `mailpit-evidence.json` | SMTP apontando para `mailpit` | Mensagem real recebida e indexada | Pendente em runtime | 22/08/2026 | branch de aceite | Evidência sem corpo/token |
| Browser acceptance | `npm run e2e` | Frontend, seed e Chromium | Login, escopos e telas integradas | Pendente em runtime | 22/08/2026 | branch de aceite | Screenshots/traces somente em falha |
| CI reprodutível | workflow `acceptance` | Runner Ubuntu + Docker | Artefatos publicados mesmo em falha | Implementado; aguarda primeira execução | 22/08/2026 | branch de aceite | Artefato `technical-acceptance-evidence` |

## Evidências geradas

Os scripts de aceite nunca escrevem senhas, tokens de reset ou JWT nos arquivos. O diretório `acceptance-artifacts/` pode conter:

- `demo-state.json`: IDs, referências da fixture e o token QR fictício necessário ao teste autenticado do navegador; não contém senha, JWT ou token de reset;
- `seed-result.json` ou `seed-error.json`;
- `smoke-result.json` ou `smoke-error.json`;
- `mailpit-evidence.json`: destinatário, assunto e ID técnico da mensagem;
- `compose.log`, relatórios Surefire, screenshots e traces publicados pelo CI.

## Limitações registradas

- O retry de e-mail é exercitado quando existe uma entrega `FAILED`. Em um Mailpit saudável, sem injeção de falha SMTP, o smoke registra explicitamente que não houve entrega falha para reenfileirar; isso não é tratado como sucesso falso.
- O endpoint público atual não possui descoberta completa dos vídeos e questionários de um módulo. Por isso, uma segunda execução do seed reutiliza `demo-state.json`; se o banco já tiver a fixture e esse estado tiver sido removido, o script falha com uma lacuna de API explícita em vez de usar SQL.
- A aprovação final continua pendente até a execução verde em Java 21 + Docker, com todos os artefatos publicados.
