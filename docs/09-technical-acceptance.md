# Aceite técnico e ambiente

**Status da implementação:** preparado para execução em CI; Java 21 é a versão mínima e versões posteriores são aceitas pelo build. O aceite oficial usa Java 21 como baseline e a compatibilidade posterior é exercitada separadamente no CI.

## Contrato de execução

O ambiente oficial de aceite usa Java 21, Node.js 22 e Docker com engine Linux. Node.js não é dependência do backend Spring; ele é usado pelo frontend React/Vite, pelos scripts de aceite e pelo Playwright. O Maven Enforcer e os scripts aceitam Java 21 ou superior; a compilação mantém `--release 21` para preservar a compatibilidade mínima.

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
| Java 21 mínimo | `./scripts/check-environment.ps1`; `./mvnw.cmd validate` | JDK 21+ | JVM 21 ou superior aceita; bytecode permanece release 21 | Validado localmente com JDK 25; pacote compilado com release 21 | 24/08/2026 | integration/organizational-relations-rebased | CI testa Java 21 e 25 |
| Maven completo | `./mvnw.cmd -B -ntp test` | Java 21+ + Docker para Testcontainers | Build e testes verdes | Validado localmente: 150 testes, 0 falhas | 24/08/2026 | integration/organizational-relations-rebased | Testcontainers PostgreSQL |
| Frontend | `npm ci`, `npm test`, `npm run lint`, `npm run build` | Node 22 | Suíte, lint e build verdes | Build validado no Compose; testes/lint/audit ainda pendentes | 24/08/2026 | integration/organizational-relations-rebased | Job frontend |
| Compose | `docker compose config --quiet` | Docker CLI | Configuração válida | Validado localmente | 24/08/2026 | integration/organizational-relations-rebased | Portas isoladas por variáveis locais |
| Serviços saudáveis | `docker compose up --build --detach` + readiness | Docker daemon | PostgreSQL, MinIO, Mailpit, backend e frontend saudáveis | Validado localmente com portas isoladas | 24/08/2026 | integration/organizational-relations-rebased | `docker compose ps` |
| Seed simulado | `node scripts/seed-simulated-demo.mjs` | Compose saudável + Mailpit | Massa completa pela API, com estados e evidências | Validado: namespace `SIM-20260831`, quatro papéis e 6 qualificações | 24/08/2026 | integration/organizational-relations-rebased | `simulated-demo-state.json` e `simulated-demo-result.json` |
| Seed idempotente | `node scripts/acceptance/seed-demo.mjs` duas vezes | Compose saudável | Mesmos IDs, sem duplicidade | Pendente em runtime | 22/08/2026 | branch de aceite | `demo-state.json`, `seed-result.json` |
| Fluxo API crítico | `node scripts/acceptance/smoke.mjs` | Seed + Compose | Fluxo treinamento → vídeo → avaliação → conclusão → certificado | Pendente em runtime | 22/08/2026 | branch de aceite | `smoke-result.json` |
| Mailpit | smoke + `mailpit-evidence.json` | SMTP apontando para `mailpit` | Mensagem real recebida e indexada | Pendente em runtime | 22/08/2026 | branch de aceite | Evidência sem corpo/token |
| Browser acceptance | `npm run e2e` | Frontend, seed e Chromium | Login, escopos e telas integradas | Pendente em runtime | 22/08/2026 | branch de aceite | Screenshots/traces somente em falha |
| CI reprodutível | workflows `backend` e `acceptance` | Runner Ubuntu + Docker | Artefatos publicados mesmo em falha | Implementado; aguarda execução após a política Java 21+ | 24/08/2026 | integration/organizational-relations-rebased | Artefatos separados por versão Java |

## Evidências geradas

Os scripts de aceite nunca escrevem senhas, tokens de reset ou JWT nos arquivos. O diretório `acceptance-artifacts/` pode conter:

- `demo-state.json`: IDs, referências da fixture e o token QR fictício necessário ao teste autenticado do navegador; não contém senha, JWT ou token de reset;
- `seed-result.json` ou `seed-error.json`;
- `simulated-demo-state.json` e `simulated-demo-result.json` para a massa de demonstração;
- `smoke-result.json` ou `smoke-error.json`;
- `mailpit-evidence.json`: destinatário, assunto e ID técnico da mensagem;
- `compose.log`, relatórios Surefire, screenshots e traces publicados pelo CI.

## Limitações registradas

- O retry de e-mail é exercitado quando existe uma entrega `FAILED`. Em um Mailpit saudável, sem injeção de falha SMTP, o smoke registra explicitamente que não houve entrega falha para reenfileirar; isso não é tratado como sucesso falso.
- O endpoint público atual não possui descoberta completa dos vídeos e questionários de um módulo. Por isso, uma segunda execução do seed reutiliza `demo-state.json`; se o banco já tiver a fixture e esse estado tiver sido removido, o script falha com uma lacuna de API explícita em vez de usar SQL.
- A aprovação final continua pendente até a execução verde do aceite em Java 21 + Docker, com todos os artefatos publicados; a matriz Java 25 também deve permanecer verde para confirmar compatibilidade posterior.
