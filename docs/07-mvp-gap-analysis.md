# Diagnóstico do caminho até o MVP

**Data do diagnóstico:** 29/07/2026
**Base analisada:** árvore atual do repositório, `pom.xml`, `src/`, `frontend/`, Docker Compose, contratos e documentos em `docs`.
**Referências:** [PRD do MVP](01-prd-mvp.md), [fonte da verdade](../work-training-system-fonte-da-verdade.md), [auditoria de implementação](06-implementation-audit.md) e [contrato da API](05-api-contract.md).

## Resumo executivo

A prioridade 1 do MVP — editor administrativo de treinamentos — está implementada nesta branch. O backend preserva a imutabilidade de versões publicadas, restringe mutações administrativas a rascunhos, valida consistência antes da publicação e usa uploads privados no MinIO com URLs presignadas. O frontend agora oferece o fluxo operacional de edição de treinamento, versão, módulos, vídeos, questionários, questões e alternativas.

O MVP ainda não está pronto para aceite porque o fluxo completo precisa de execução integrada comprovada em Java 21 + Docker. O frontend atual já possui autenticação real, roteamento por perfil, dashboards API-backed, atribuições, player, questionários, certificados, QR Code, notificações e páginas administrativas, mas a profundidade de alguns fluxos e o aceite ponta a ponta ainda precisam ser confirmados pelo CI. A suíte backend observada anteriormente ficou limitada pelo runtime local (Java 25 e Docker indisponível); esse fato não deve ser confundido com um resultado verde.
O restante do MVP ainda não deve ser considerado concluído: as telas de execução do colaborador, dashboards completos, certificados, notificações, expirações, QR Code e auditoria ainda possuem lacunas de produto na interface. O Compose agora inclui Mailpit para o ambiente local, mas o aceite completo e a cobertura de falhas SMTP ainda dependem de execução integrada.

## Estado atual observado

### Backend — base existente e prioridade 1 concluída

- Identidade, login, refresh, logout, recuperação de senha, usuários, papéis, permissões e escopos.
- Unidades, setores, cargos, colaboradores, status, histórico e foto protegida em object storage.
- Atividades, requisitos de treinamento e vínculos com cargos/colaboradores.
- Treinamentos versionados com edição dos dados principais, criação/duplicação de versões, publicação e arquivamento compatível com o modelo atual.
- Módulos com criação, edição, remoção, status e ordenação; vídeos com criação, edição, remoção, status, ordenação e listagem por módulo.
- Questionários opcionais com criação, edição, remoção, status e parâmetros de nota mínima, tentativas e intervalo.
- Questões e alternativas com criação, edição, remoção, status e ordenação; a regra de uma única alternativa correta ativa é validada no serviço.
- Resumo de publicação com validação de módulo ativo, vídeo obrigatório, questionário não vazio, alternativas suficientes, única resposta correta, ordens únicas e parâmetros válidos.
- Upload privado de vídeos para MinIO via URL presignada, confirmação server-side por `HEAD` de tipo/tamanho/checksum e URL curta protegida para playback.
- Imutabilidade de conteúdo publicado e preservação da versão exata usada por atribuições/conclusões.
- Atribuição, progresso de vídeo, avaliações, conclusões, qualificações, expiração/recertificação, certificados, QR Code, notificações, auditoria e reporting continuam disponíveis conforme a base existente.

### Frontend — integrado, com profundidade ainda a comprovar

O frontend em `frontend/src/App.tsx` permite login real contra a API, sessão persistida e redirecionamento por perfil. Há telas e integrações para dashboard administrativo e do colaborador, atribuições, player com progresso, questionário, resultado, certificados, QR Code, notificações, relatórios, gestão de colaboradores, treinamentos e auditoria. O Playwright adicionado ao aceite valida os caminhos de login, escopo e superfícies críticas sem depender de dados visuais hard-coded.

Ainda devem ser comprovados no aceite integrado, e aprofundados quando houver requisito funcional específico:

- cobertura visual e comportamental de todos os estados vazios, erros de domínio, paginação, filtros e expiração de sessão;
- formulários e operações administrativas menos exercitadas pelo smoke, incluindo atividades, usuários, versões e edição de conteúdo;
- reprodução real do arquivo de vídeo em navegador, além da validação de URL protegida e do registro de progresso pela API;
- execução de falha SMTP controlada para obter evidência real do caminho de retry;
- confirmação dos relatórios e dashboards específicos contra dados de produção de demonstração, não apenas a resposta HTTP.
### Frontend — prioridade 1 funcional

- Catálogo administrativo com busca, criação de treinamento e edição dos dados principais.
- Histórico de versões com duplicação de versão publicada/arquivada para novo rascunho, publicação e arquivamento.
- Editor de versão com parâmetros, resumo de publicação, erros de consistência, estados de carregamento/erro/vazio e bloqueio visual de versões publicadas.
- Editor de módulos com edição, remoção, status e reordenação.
- Editor de vídeos com upload direto para MinIO sem carregar o arquivo completo na API, edição, remoção, status, reordenação e teste de URL de playback protegida.
- Editor de questionários, questões e alternativas com edição, remoção, ativação/inativação, reordenação e definição exclusiva da resposta correta.
- Validações de campos no frontend complementadas pelas regras e autorização administrativa no backend.
- Interface responsiva reutilizando os componentes e tokens do design system atual.

### Infraestrutura e evidência de smoke

- `docker-compose.yml` mantém PostgreSQL, MinIO, backend e frontend; CORS do backend e da API do MinIO inclui as portas locais do frontend (`5173` e `3000`).
- O bootstrap opcional de administrador demo é configurável por variáveis do Compose e permanece desativado por padrão.
- O MinIO comunitário é configurado por `MINIO_API_CORS_ALLOW_ORIGIN`; o init não tenta usar `mc cors set`, operação não disponível nessa distribuição.
- Smoke executado com o Compose: login administrativo pelo navegador, criação do treinamento, módulo, questionário, questão, duas alternativas, escolha de uma resposta correta, upload/conclusão de vídeo, criação de vídeo, publicação e criação de nova versão.
- O objeto sem URL presignada respondeu `403`; a URL protegida de playback respondeu `200`; alteração da versão publicada respondeu `422` por imutabilidade.
- Nenhuma migration nova foi necessária para a prioridade 1; os contratos existentes e as tabelas atuais suportam o editor.

## Gaps reais restantes

### Produto fora da prioridade 1

- Dashboard administrativo ainda precisa de indicadores e filtros completos por treinamento, atividade e colaborador.
- Ainda faltam na interface os fluxos completos do colaborador: atribuição, player com progresso, retomada, questionário, conclusão e certificado.
- Ainda faltam telas operacionais completas para colaboradores, estrutura, atividades, atribuições, usuários, qualificações, expirações, certificados, QR Code, notificações, e-mails e auditoria.
- O frontend ainda precisa substituir placeholders desses módulos por fluxos reais e adicionar testes de interação mais amplos.

### Ambiente e aceite de produto

- A exigência estrita de Java 21 foi implementada no Maven, Dockerfile, CI e scripts de pré-requisito.
- Ainda é necessário executar `./mvnw -B -ntp verify` com Java 21 e Docker disponível para validar Testcontainers e as migrações `V1`–`V12`.
- A execução local comprovada de frontend e `docker compose config --quiet` está verde; a execução integrada e os artefatos de CI ainda precisam ser registrados.
- O smoke test real agora está versionado em `scripts/acceptance/smoke.mjs`.
- O Compose inclui Mailpit; e-mail, retries e falhas de entrega ainda precisam de evidência completa em ambiente integrado.
- O smoke de MinIO validou metadados, privacidade e acesso assinado com um payload mínimo; a validação de decodificação/streaming de um vídeo real permanece dependente de um arquivo de mídia válido e de browser com suporte ao codec.
- O aceite completo do MVP exige executar os 26 critérios da fonte da verdade, incluindo os fluxos de execução e operação que não fazem parte desta prioridade.

### Lacunas de backend e operação

- Separar os endpoints de dashboard administrativo por treinamento, atividade e colaborador; atualmente os endpoints correspondentes retornam o mesmo overview geral em `ReportingController`.
- Confirmar que todos os eventos relevantes geram auditoria persistente, não somente operações de certificado, QR e recertificação.
- Confirmar o disparo de notificações internas e e-mail para atribuição, prazo próximo, vencimento, reprovação, conclusão e mudança de qualificação.
- Mailpit foi incluído no Compose e o seed/smoke validam a entrega SMTP local; falta apenas uma execução com falha SMTP controlada para evidenciar o retry sem condição de corrida.
- O seed idempotente via API cria dados fictícios, usuários reais para os três perfis, fixture de vídeo no MinIO, atribuição individual e regra automática por atividade/cargo.
- Revisar escopo multi-organização e valores default usados nos módulos recentes antes de considerar produção/demonstração final.

## Validação da prioridade 1

Validação executada nesta branch:

- `frontend`: `npm run lint`, `npm run type-check`, `npm test` — 4 testes aprovados — e `npm run build`.
- backend: `./mvnw test` — 146 testes aprovados — e `./mvnw -DskipTests package`.
- containers: `docker compose config --quiet` e `docker compose build`.
- runtime: suíte Testcontainers executada com Docker e migrations `V1`–`V12` validadas; imagens de backend/frontend construídas com Java 21/Node 22.
- smoke manual: browser autenticado, criação do catálogo e conteúdo, upload/conclusão MinIO, objeto privado `403`, playback presignado `200`, publicação, duplicação e bloqueio de alteração publicada `422`.

- Playwright foi adicionado para login, dashboards, escopo, player, questionário, certificado e QR; a execução depende do Compose saudável e Chromium.
- O smoke valida permissões, idempotência, PostgreSQL, MinIO, certificado, QR, notificação, Mailpit e auditoria pela API.
- Permanecem como comprovação de aceite a execução em máquina/runner com Java 21 + Docker e a atualização dos resultados da matriz em `docs/09-technical-acceptance.md`.
- Os critérios de conclusão do MVP devem ser marcados com os artefatos do CI, não com aprovação manual não registrada.

## Ordem recomendada

1. Executar o aceite completo em Java 21, Node 22 e Docker e publicar os artefatos.
2. Corrigir apenas falhas reais encontradas no smoke, no navegador ou no CI.
3. Aprofundar os fluxos de produto que ainda exigirem formulários, filtros, estados vazios ou regras não cobertas.
4. Completar cobertura de falha SMTP/retry e contratos de relatórios específicos.
5. Atualizar a matriz de aceite e só então declarar o MVP concluído.

## Atualização de 22/08/2026 — fechamento do aceite técnico e ambiente

Foi implementada a base reproduzível de aceite descrita em [09-technical-acceptance.md](09-technical-acceptance.md): Java 21 estrito no Maven, imagens fixadas, Compose com PostgreSQL, MinIO, backend, frontend e Mailpit, `.env.example`, healthchecks, scripts multiplataforma de pré-requisito, seed fictício via API, smoke test, Playwright e job de aceite no CI com coleta de artefatos.

O diagnóstico funcional de frontend acima permanece válido como retrato histórico do diagnóstico de 27/07/2026 e não deve ser usado para declarar o estado atual sem conferir o código. A aceitação técnica ainda não foi aprovada neste worktree porque a máquina local está em Java 25 e não possui daemon Docker disponível; os critérios de runtime precisam ser executados no CI ou em uma máquina com Java 21, Node 22 e Docker.

## Conclusão

O estado atual é **prioridade 1 do editor administrativo implementada e validada; MVP completo ainda não aceito**. A versão publicada permanece protegida contra alterações, o histórico fica preservado e os gaps restantes são de execução do colaborador, operação administrativa além do editor e infraestrutura de demonstração, não lacunas ocultas do editor de treinamentos.
