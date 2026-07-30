# Diagnóstico do caminho até o MVP

**Data do diagnóstico:** 29/07/2026
**Base analisada:** árvore atual do repositório, `pom.xml`, `src/`, `frontend/`, Docker Compose, contratos e documentos em `docs`.
**Referências:** [PRD do MVP](01-prd-mvp.md), [fonte da verdade](../work-training-system-fonte-da-verdade.md), [auditoria de implementação](06-implementation-audit.md) e [contrato da API](05-api-contract.md).

## Resumo executivo

A prioridade 1 do MVP — editor administrativo de treinamentos — está implementada nesta branch. O backend preserva a imutabilidade de versões publicadas, restringe mutações administrativas a rascunhos, valida consistência antes da publicação e usa uploads privados no MinIO com URLs presignadas. O frontend agora oferece o fluxo operacional de edição de treinamento, versão, módulos, vídeos, questionários, questões e alternativas.

O restante do MVP ainda não deve ser considerado concluído: as telas de execução do colaborador, dashboards completos, certificados, notificações, expirações, QR Code e auditoria ainda possuem lacunas de produto na interface. O Compose também não inclui um servidor de e-mail local, portanto o health geral pode permanecer `DOWN` por causa do SMTP ausente mesmo com a aplicação pronta para os fluxos administrativos.

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

- O Compose não inclui MailHog/SMTP local; e-mail, retries e falhas de entrega ainda precisam de ambiente dedicado para smoke.
- O smoke de MinIO validou metadados, privacidade e acesso assinado com um payload mínimo; a validação de decodificação/streaming de um vídeo real permanece dependente de um arquivo de mídia válido e de browser com suporte ao codec.
- O aceite completo do MVP exige executar os 26 critérios da fonte da verdade, incluindo os fluxos de execução e operação que não fazem parte desta prioridade.

## Validação da prioridade 1

Validação executada nesta branch:

- `frontend`: `npm run lint`, `npm run type-check`, `npm test` — 4 testes aprovados — e `npm run build`.
- backend: `./mvnw test` — 146 testes aprovados — e `./mvnw -DskipTests package`.
- containers: `docker compose config --quiet` e `docker compose build`.
- runtime: suíte Testcontainers executada com Docker e migrations `V1`–`V12` validadas; imagens de backend/frontend construídas com Java 21/Node 22.
- smoke manual: browser autenticado, criação do catálogo e conteúdo, upload/conclusão MinIO, objeto privado `403`, playback presignado `200`, publicação, duplicação e bloqueio de alteração publicada `422`.

A suíte local reportou apenas avisos de depreciação do Java 25/auto-attach do Mockito; o `pom.xml` e as imagens Docker continuam configurados para Java 21. Browser/CORS, MinIO e SMTP são evidências separadas: o primeiro smoke passou para o editor e MinIO, enquanto SMTP continua pendente por não haver MailHog no Compose.

## Conclusão

O estado atual é **prioridade 1 do editor administrativo implementada e validada; MVP completo ainda não aceito**. A versão publicada permanece protegida contra alterações, o histórico fica preservado e os gaps restantes são de execução do colaborador, operação administrativa além do editor e infraestrutura de demonstração, não lacunas ocultas do editor de treinamentos.
