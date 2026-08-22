# Diagnóstico do caminho até o MVP

**Data do diagnóstico:** 27/07/2026  
**Base analisada:** árvore atual do repositório, `pom.xml`, `src/`, `frontend/`, Docker Compose, CI e documentos em `docs/`.  
**Referências:** [PRD do MVP](01-prd-mvp.md), [fonte da verdade](../work-training-system-fonte-da-verdade.md) e [auditoria de implementação](06-implementation-audit.md).

## Resumo executivo

O projeto já possui uma fundação de backend bastante avançada. Há um monólito modular Spring Boot com Java 21 configurado no Maven, PostgreSQL/Flyway com migrações até `V12`, autenticação JWT, estrutura organizacional, colaboradores, catálogo de treinamentos, atividades, atribuições, progresso de vídeo, avaliações, conclusões, qualificações, expiração/recertificação, certificados, QR Code, notificações e auditoria persistente.

O MVP ainda não está pronto para aceite porque o fluxo completo precisa de execução integrada comprovada em Java 21 + Docker. O frontend atual já possui autenticação real, roteamento por perfil, dashboards API-backed, atribuições, player, questionários, certificados, QR Code, notificações e páginas administrativas, mas a profundidade de alguns fluxos e o aceite ponta a ponta ainda precisam ser confirmados pelo CI. A suíte backend observada anteriormente ficou limitada pelo runtime local (Java 25 e Docker indisponível); esse fato não deve ser confundido com um resultado verde.

## Estado atual observado

### Backend — implementado ou muito próximo de implementado

- Identidade, login, refresh, logout, recuperação de senha, usuários, papéis, permissões e escopos.
- Unidades, setores, cargos, colaboradores, status, histórico e foto protegida em object storage.
- Atividades, requisitos de treinamento e vínculos com cargos/colaboradores.
- Treinamentos versionados, módulos, vídeos, questionários, questões, alternativas, publicação e imutabilidade de versões publicadas.
- Atribuição individual e em lote, atribuição automática, reciclagem e qualificações.
- Upload privado, progresso de vídeo com regra de 80%, tentativas, correção no servidor, nota mínima de 70% e conclusão automática.
- Validade, vencimento, recálculo, recertificação e bloqueio/liberação de atividades.
- Certificados internos/externos, geração de PDF, download, validação e revogação.
- QR Code com geração, revogação, verificação autenticada e registro de acessos.
- Notificações internas, entregas de e-mail/reenvio e auditoria persistida.
- Dashboards e relatórios básicos no backend.
- Docker Compose com PostgreSQL, MinIO, backend e frontend; workflow de CI para backend, frontend e validação do Compose.

### Frontend — integrado, com profundidade ainda a comprovar

O frontend em `frontend/src/App.tsx` permite login real contra a API, sessão persistida e redirecionamento por perfil. Há telas e integrações para dashboard administrativo e do colaborador, atribuições, player com progresso, questionário, resultado, certificados, QR Code, notificações, relatórios, gestão de colaboradores, treinamentos e auditoria. O Playwright adicionado ao aceite valida os caminhos de login, escopo e superfícies críticas sem depender de dados visuais hard-coded.

Ainda devem ser comprovados no aceite integrado, e aprofundados quando houver requisito funcional específico:

- cobertura visual e comportamental de todos os estados vazios, erros de domínio, paginação, filtros e expiração de sessão;
- formulários e operações administrativas menos exercitadas pelo smoke, incluindo atividades, usuários, versões e edição de conteúdo;
- reprodução real do arquivo de vídeo em navegador, além da validação de URL protegida e do registro de progresso pela API;
- execução de falha SMTP controlada para obter evidência real do caminho de retry;
- confirmação dos relatórios e dashboards específicos contra dados de produção de demonstração, não apenas a resposta HTTP.

## O que falta para alcançar o MVP

### 1. Corrigir a base de execução e tornar a suíte confiável — aceite pendente

- A exigência estrita de Java 21 foi implementada no Maven, Dockerfile, CI e scripts de pré-requisito.
- Ainda é necessário executar `./mvnw -B -ntp verify` com Java 21 e Docker disponível para validar Testcontainers e as migrações `V1`–`V12`.
- A execução local comprovada de frontend e `docker compose config --quiet` está verde; a execução integrada e os artefatos de CI ainda precisam ser registrados.
- O smoke test real agora está versionado em `scripts/acceptance/smoke.mjs`.

### 2. Substituir o protótipo frontend por fluxos funcionais — maior lacuna

Implementar, priorizando o caminho crítico do usuário:

1. login, sessão, expiração/renovação do token e logout;
2. dashboard do colaborador com atribuições e retomada do treinamento;
3. player de vídeo com progresso e regra de 80%;
4. questionário com tentativas, nota, reprovação e conclusão;
5. dashboard administrativo com indicadores reais;
6. gestão de colaboradores, unidades, setores e cargos;
7. gestão de atividades e requisitos;
8. gestão de treinamentos, versões, módulos, vídeos, uploads e questionários;
9. atribuição manual/em lote e acompanhamento de status;
10. qualificações, expirações e recertificações;
11. certificados: visualizar, baixar, validar e revogar;
12. QR Code: gerar, revogar, exibir e consultar dados autenticados;
13. notificações internas e acompanhamento de entregas de e-mail;
14. consulta de auditoria com filtros e paginação.

### 3. Fechar as lacunas de produto no backend

- Separar os endpoints de dashboard administrativo por treinamento, atividade e colaborador; atualmente os endpoints correspondentes retornam o mesmo overview geral em `ReportingController`.
- Confirmar que todos os eventos relevantes geram auditoria persistente, não somente operações de certificado, QR e recertificação.
- Confirmar o disparo de notificações internas e e-mail para atribuição, prazo próximo, vencimento, reprovação, conclusão e mudança de qualificação.
- Mailpit foi incluído no Compose e o seed/smoke validam a entrega SMTP local; falta apenas uma execução com falha SMTP controlada para evidenciar o retry sem condição de corrida.
- O seed idempotente via API cria dados fictícios, usuários reais para os três perfis, fixture de vídeo no MinIO, atribuição individual e regra automática por atividade/cargo.
- Revisar escopo multi-organização e valores default usados nos módulos recentes antes de considerar produção/demonstração final.

### 4. Testes e aceite ponta a ponta

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

Em termos de domínio e persistência, o backend está próximo do escopo funcional. Em termos de produto utilizável, o trabalho restante é significativo: falta transformar os endpoints em uma aplicação frontend operacional, criar uma demonstração real e obter validação verde em Java 21 + Docker. Portanto, o estado atual deve ser considerado **backend avançado / MVP ainda não aceito**, e não MVP concluído.

