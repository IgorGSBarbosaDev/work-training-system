# Handoff das pendências do MVP

**Atualizado em:** 29/08/2026  
**Motivo:** preservar a continuidade antes do encerramento da janela de trabalho. O ambiente do agente não informa o
percentual restante do limite de cinco horas, por isso o handoff foi criado preventivamente após o aceite local verde.

## Entregue nesta execução

- CI estabilizado: Docker `ServerVersion`, diretório antecipado de evidências, coleta tolerante de logs, lint e retenção
  de 14/30 dias.
- Dashboards agregados no PostgreSQL, com filtros, paginação, escopo, cache e respostas próprias por treinamento,
  atividade e colaborador.
- Dashboard pessoal ampliado e dashboard administrativo com abas, filtros compartilhados e apresentação responsiva.
- Telas operacionais específicas para usuários, certificados, notificações, e-mails e auditoria.
- Refresh de token concorrente seguro e retorno pós-login para rota interna segura.
- Auditoria com `organizationId`, eventos administrativos complementares e isolamento corrigido nos fluxos de usuários,
  certificados, notificações, e-mails, auditoria, reporting e QR.
- Migração V13 com corpo original do e-mail, deduplicação de notificações e índices de consulta.
- Retry SMTP com conteúdo original, erros sanitizados, timeouts configuráveis e prova `FAILED -> PENDING -> SENT`.
- Job diário de prazo próximo, configuração de produção com variáveis obrigatórias e demonstração desativada.
- Aceite com MP4 real gerado por `ffmpeg`, upload protegido, atribuição exclusiva do navegador, reprodução real até 80%,
  questionário, conclusão, certificado, desktop e viewport móvel.
- Manifesto de aceite sem senha, JWT, token de recuperação ou corpo de e-mail.

## Pendências prioritárias

1. Remover `DEFAULT_ORGANIZATION_ID` dos fluxos autenticados restantes: atividades, atribuições, avaliações,
   colaboradores, arquivos, cargos, organização, progresso, qualificações e catálogo de treinamentos. Os adaptadores
   internos também devem propagar o `organizationId` do comando ou evento.
2. Criar testes PostgreSQL com uma segunda organização cobrindo leitura e mutação cruzadas em todos esses módulos,
   inclusive arquivos, playback/progresso, avaliações, relatórios, retry, download, QR e auditoria.
3. Consolidar os sete tipos obrigatórios de notificação no smoke: nova atribuição, prazo próximo, vencendo, vencido,
   reprovação, conclusão e atividade bloqueada. A implementação existe; o aceite limpo comprovou atribuição,
   reprovação, conclusão e bloqueio, e o job de prazo precisa ser executado com cron acelerado no ambiente de aceite.
4. Consolidar num artefato único as contagens maiores que zero da massa simulada para não iniciado, em andamento,
   concluído, reprovado, vencido, vencendo e bloqueado.
5. Executar o workflow oficial no GitHub em Java 21 e 25/Node 22, confirmar todos os jobs verdes, registrar run ID e
   links dos artefatos em `docs/09-technical-acceptance.md` e somente então marcar a aprovação formal.

## Validações verdes locais

- `./mvnw -B -ntp verify`: 150 testes, zero falhas, Java 25 compilando com `release 21`.
- `npm test -- --run`: 10 testes, zero falhas.
- `npm run lint`, `npm audit --audit-level=high` e `npm run build`: verdes; zero vulnerabilidades altas.
- `docker compose config --quiet` e build das imagens backend/frontend: verdes.
- Base Compose limpa: seed, smoke com dashboards e filtros, SMTP `FAILED -> PENDING -> SENT` e manifesto verdes.
- Playwright Chromium: 7 cenários verdes em 13,3 segundos, incluindo MP4 real e viewport 390 x 844.
- `git diff --check`, sintaxe Bash e sintaxe dos scripts Node: verdes.

## Cuidados para a próxima execução

- Não marcar o MVP como formalmente aprovado antes de um run remoto integralmente verde.
- Não versionar `acceptance-artifacts/`, `frontend/test-results/` ou `frontend/playwright-report/`.
- Manter `.impeccable/design.json` sem regeneração; a atualização dessa sidecar segue fora do MVP sem solicitação
  específica.
- Preservar a organização padrão apenas no bootstrap de demonstração e em suporte explícito de testes.
