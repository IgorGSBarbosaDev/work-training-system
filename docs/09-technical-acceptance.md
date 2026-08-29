# Aceite técnico e ambiente

**Status da implementação:** aceite local completo verde; a aprovação formal aguarda a execução do workflow oficial. Java 21 é a versão mínima e versões posteriores são aceitas pelo build.

**Execução integrada — 29/08/2026:** uma base e volumes isolados foram recriados do zero. O seed gerou um MP4
sintético válido com `ffmpeg` e o enviou pelo upload protegido; smoke, dashboards agregados, período inválido, retry
SMTP `FAILED -> PENDING -> SENT` e sete cenários Chromium passaram. O fluxo de navegador reproduziu dez segundos
reais, persistiu mais de 80%, aprovou o questionário e abriu o certificado. A viewport móvel validou abas e filtros.

**Atualização de implementação — 29/08/2026:** o smoke agora usa os resultados `FAILED` e `APPROVED` do contrato real de
avaliações. O QR Code usa a URL absoluta configurada por `QR_PUBLIC_BASE_URL` e a rota frontend canônica
`/verificar/{token}`. A execução local isolada passou com Compose, seed, smoke, Mailpit e Playwright; ainda falta a
execução oficial do workflow para publicar os artefatos com Java 21 e Node 22. A lista consolidada está em
[docs/10-mvp-pendencias.md](10-mvp-pendencias.md).

**Estabilização do CI — 29/08/2026:** os verificadores Bash e PowerShell consultam agora o campo suportado
`ServerVersion` do Docker. O job de aceite cria o diretório de evidências antes da inspeção do ambiente, coleta logs
mesmo quando nenhum container chegou a iniciar e retém as evidências formais por 30 dias. O frontend passou a ter
lint explícito como gate; relatórios rotineiros do backend são retidos por 14 dias. Essas mudanças foram validadas por
inspeção do workflow e execução local dos verificadores disponíveis; a execução oficial continua necessária para
preencher a matriz com o identificador do run.

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
node scripts/acceptance/smtp-retry.mjs
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
| Frontend | `npm ci`, `npm test`, `npm run lint`, `npm run build` | Node 22 | Suíte, lint e build verdes | Validado localmente: 10 testes, lint e build verdes; imagem Docker compilada com Node 22 | 29/08/2026 | working tree | Job frontend |
| Compose | `docker compose config --quiet` | Docker CLI | Configuração válida | Validado localmente | 29/08/2026 | working tree | Portas isoladas por variáveis locais |
| Serviços saudáveis | `docker compose up --build --detach` + readiness | Docker daemon | PostgreSQL, MinIO, Mailpit, backend e frontend saudáveis | Validado localmente com portas isoladas | 29/08/2026 | working tree | `docker compose ps` |
| Seed simulado | `node scripts/seed-simulated-demo.mjs` | Compose saudável + Mailpit | Massa completa pela API, com estados e evidências | Validado: namespace `SIM-20260831`, quatro papéis e 6 qualificações | 24/08/2026 | integration/organizational-relations-rebased | `simulated-demo-state.json` e `simulated-demo-result.json` |
| Seed idempotente | `node scripts/acceptance/seed-demo.mjs` duas vezes | Compose saudável | Mesmos IDs, sem duplicidade | Validado no ambiente isolado | 29/08/2026 | working tree | `demo-state.json`, `seed-result.json` |
| Fluxo API crítico | `node scripts/acceptance/smoke.mjs` | Seed + Compose | Fluxo treinamento → vídeo → avaliação → conclusão → certificado | Validado: 12 checks, incluindo quatro dashboards; resultados `FAILED` e `APPROVED` | 29/08/2026 | working tree | `smoke-result.json` |
| Mailpit | smoke + `mailpit-evidence.json` | SMTP apontando para `mailpit` | Mensagem real recebida e indexada | Validado no ambiente isolado | 29/08/2026 | working tree | Evidência sem corpo/token |
| Retry SMTP | `node scripts/acceptance/smtp-retry.mjs` | Seed + Compose | `FAILED -> PENDING -> SENT` com conteúdo original | Validado com Mailpit parado e reiniciado | 29/08/2026 | working tree | `smtp-retry-evidence.json`, sem corpo/token |
| Browser acceptance | `npm run e2e` | Frontend, seed e Chromium | Fluxo completo, escopos, desktop e mobile | Validado: 7 testes; MP4 real, 80%, questionário e certificado | 29/08/2026 | working tree | Screenshots/traces somente em falha |
| CI reprodutível | workflows `backend` e `acceptance` | Runner Ubuntu + Docker | Artefatos publicados mesmo em falha | Implementado; aguarda execução após a política Java 21+ | 24/08/2026 | integration/organizational-relations-rebased | Artefatos separados por versão Java |

## Evidências geradas

Os scripts de aceite nunca escrevem senhas, tokens de reset ou JWT nos arquivos. O diretório `acceptance-artifacts/` pode conter:

- `demo-state.json`: IDs, referências da fixture e o token QR fictício necessário ao teste autenticado do navegador; não contém senha, JWT ou token de reset;
- `seed-result.json` ou `seed-error.json`;
- `simulated-demo-state.json` e `simulated-demo-result.json` para a massa de demonstração;
- `smoke-result.json` ou `smoke-error.json`;
- `mailpit-evidence.json`: destinatário, assunto e ID técnico da mensagem;
- `smtp-retry-evidence.json`: IDs técnicos e estados do retry, sem corpo ou credenciais;
- `acceptance-manifest.json`: commit, run ID, versões, checks e nomes dos artefatos;
- `compose.log`, relatórios Surefire, screenshots e traces publicados pelo CI.

## Limitações registradas

- O smoke mantém o Mailpit saudável. O script separado `smtp-retry.mjs` para o serviço, cria uma notificação exclusiva,
  espera `FAILED`, reinicia o serviço, solicita retry e comprova `PENDING` e `SENT` pela API e pelo Mailpit.
- O endpoint público atual não possui descoberta completa dos vídeos e questionários de um módulo. Por isso, uma segunda execução do seed reutiliza `demo-state.json`; se o banco já tiver a fixture e esse estado tiver sido removido, o script falha com uma lacuna de API explícita em vez de usar SQL.
- A aprovação formal continua pendente até a execução verde do workflow em Java 21 + Node 22 + Docker, com todos os
  artefatos publicados; a matriz Java 25 também deve permanecer verde para confirmar compatibilidade posterior.
