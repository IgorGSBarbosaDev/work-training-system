# Diagnóstico do caminho até o MVP

**Data do diagnóstico:** 27/07/2026  
**Base analisada:** árvore atual do repositório, `pom.xml`, `src/`, `frontend/`, Docker Compose, CI e documentos em `docs/`.  
**Referências:** [PRD do MVP](01-prd-mvp.md), [fonte da verdade](../work-training-system-fonte-da-verdade.md) e [auditoria de implementação](06-implementation-audit.md).

## Resumo executivo

O projeto já possui uma fundação de backend bastante avançada. Há um monólito modular Spring Boot com Java 21 configurado no Maven, PostgreSQL/Flyway com migrações até `V12`, autenticação JWT, estrutura organizacional, colaboradores, catálogo de treinamentos, atividades, atribuições, progresso de vídeo, avaliações, conclusões, qualificações, expiração/recertificação, certificados, QR Code, notificações e auditoria persistente.

O MVP ainda não está pronto para aceite porque o fluxo completo não está demonstrável pela interface. A prioridade 2
agora possui telas administrativas funcionais para estrutura organizacional, atividades e relações com colaboradores,
mas o caminho de aprendizagem e várias áreas de operação ainda são placeholders. A validação local usa Docker e o
runtime disponível; o aceite final ainda exige execução oficial com Java 21 e smoke test autenticado no navegador.

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

### Frontend — prioridade 2 funcional, demais áreas ainda protótipo

O frontend em `frontend/src/App.tsx` já permite login real contra a API, uma sessão demo e navegação por perfil. Porém:

- o overview usa números e eventos hard-coded;
- as telas de `Assignments`, `Qualifications`, `Certificates`, `QR verification`, `Notifications`, `Reports`, `Team insights` e `Audit trail` ainda são placeholders que apenas fazem um `GET` e mostram “live data loaded”;
- a prioridade 2 adicionou formulários reais para unidades, setores, cargos, atividades, vínculos de cargo, requisitos obrigatórios e atividades específicas de colaboradores; ainda faltam formulários equivalentes para treinamentos, conteúdo, atribuições em lote e usuários;
- não há player de vídeo, retomada de progresso, fluxo de questionário, submissão de avaliação ou tela de conclusão;
- não há telas funcionais para certificados, QR Code, notificações, expiração, qualificações ou auditoria;
- as telas da prioridade 2 processam paginação e filtros no backend e tratam carregamento, vazio, erro e permissões básicas; as demais telas ainda não oferecem essa experiência completa;
- os dados visuais de demonstração (`Northstar`, `Atlas Manufacturing`, Marina e indicadores fictícios) não estão conectados ao backend.

### Entrega verificada da prioridade 2 — 29/07/2026

- Backend: relações organizacionais, atividades, requisitos, atribuições, qualificações e mudança de cargo foram
  exercitados contra PostgreSQL real em Testcontainers, sem apagar históricos.
- Frontend: `/admin/organizacao`, `/admin/atividades`, detalhe de atividade e painéis de colaborador usam os contratos
  existentes; o editor de treinamentos, upload, questionários e publicação não foram alterados.
- Testes locais verdes: lint, type-check, 5 testes frontend, build frontend e teste de endpoint da prioridade 2.
- Pendências reais: suíte completa com Java 21, smoke test de navegador autenticado/CORS e implementação das demais
  áreas frontend do MVP.

## O que falta para alcançar o MVP

### 1. Corrigir a base de execução e tornar a suíte confiável — bloqueador de aceite

- Executar o projeto com Java 21, conforme `pom.xml` e `AGENTS.md`; Java 21 não está instalado neste ambiente (a execução local usa Java 25 com `release 21`).
- [x] Reexecutar `./mvnw test` com Docker disponível: 148 testes passaram e as migrações `V1`–`V12` foram validadas no PostgreSQL 17 do Testcontainers.
- [ ] Repetir a suíte com Java 21 e avaliar a configuração do agente do Mockito; em Java 25 há somente os avisos de auto-attach, sem falhas.
- [x] Registrar execuções verdes de `./mvnw test`, `./mvnw -DskipTests package`, `npm test`, `npm run build` e `docker compose config --quiet`.
- Fazer um smoke test real no Compose: login, cadastro, atribuição, execução, conclusão, certificado, QR, notificação e auditoria.

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
- Configurar e testar SMTP/MailHog no Compose para demonstrar e-mail, retries e falhas de entrega.
- Criar uma carga de demonstração idempotente com dados totalmente fictícios para os três perfis e para o fluxo completo. O login demo atual é apenas uma sessão local do frontend e não substitui um usuário real no backend.
- Revisar escopo multi-organização e valores default usados nos módulos recentes antes de considerar produção/demonstração final.

### 4. Testes e aceite ponta a ponta

- Adicionar testes de contrato para certificados, expiração, QR, notificações, auditoria e relatórios.
- Adicionar testes frontend de fluxo, não apenas o teste unitário existente.
- Validar permissões de cada perfil pela UI e pela API.
- Validar idempotência, concorrência e reprocessamento de e-mail/certificado em ambiente com PostgreSQL e MinIO.
- Executar os 26 critérios de conclusão do MVP da fonte da verdade como checklist de aceite, com evidência para cada item.

## Ordem recomendada

1. Fixar Java 21 e deixar testes/CI verdes.
2. Criar dados demo reais e executar o smoke test do backend.
3. Implementar o fluxo colaborador: atribuição → vídeo → questionário → conclusão → certificado.
4. Implementar o fluxo administrativo de cadastros e atribuições.
5. Implementar dashboards, qualificações, expirações, QR, notificações e auditoria na UI.
6. Corrigir relatórios específicos e completar testes de contrato/e2e.
7. Rodar o checklist de aceite e só então declarar o MVP concluído.

## Conclusão

Em termos de domínio e persistência, o backend está próximo do escopo funcional. Em termos de produto utilizável, o trabalho restante é significativo: falta transformar os endpoints em uma aplicação frontend operacional, criar uma demonstração real e obter validação verde em Java 21 + Docker. Portanto, o estado atual deve ser considerado **backend avançado / MVP ainda não aceito**, e não MVP concluído.

