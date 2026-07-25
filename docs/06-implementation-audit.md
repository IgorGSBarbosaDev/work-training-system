# Auditoria de implementação

**Data:** 24/07/2026
**Escopo:** fundação técnica da Fase 1, sem alteração da fonte oficial de verdade

## Auditoria inicial

- O repositório continha um único módulo Spring Boot com Java 21, PostgreSQL, Flyway e migrações `V1` a `V4`.
- A dependência de Authorization Server não correspondia ao papel definido de Resource Server JWT.
- OpenAPI declarava o esquema bearer, mas não o aplicava globalmente.
- Erros refletiam `X-Request-Id` sem sanitização e não havia correlação via MDC.
- O Compose tinha PostgreSQL e MinIO, mas não inicializava bucket privado, não executava o backend e usava imagem MinIO flutuante.
- Não havia interface de object storage, Dockerfile de produção ou workflow de CI.

## Requisito para artefato

| Requisito de fundação | Artefato |
|---|---|
| Resource Server JWT e bibliotecas futuras necessárias | `pom.xml` |
| Bearer global no contrato OpenAPI | `OpenApiConfiguration` |
| Correlação sanitizada e logging estruturado | `RequestCorrelationFilter`, `application.yaml` |
| Porta de object storage e adaptador MinIO | `shared/storage/application`, `shared/storage/minio` |
| Configuração por ambiente para JWT, storage, mail, cache e management | `application.yaml`, propriedades tipadas |
| Imagem de produção | `Dockerfile`, `.dockerignore` |
| PostgreSQL, MinIO privado, inicializador e backend | `docker-compose.yml` |
| Teste, pacote, Compose e frontend futuro condicional | `.github/workflows/ci.yml` |
| Artefatos locais e futuros do frontend ignorados | `.gitignore` |
| Testes sem Docker para correlação e configuração | `RequestCorrelationFilterTest`, `FoundationPropertiesTest` |

As migrações `V1` a `V4`, regras de domínio e requisitos oficiais não foram alterados.

## Fase 2 - identidade e autenticação

- [x] Migração append-only `V5` para usuários, tokens opacos com hash, tentativas, permissões e escopos explícitos.
- [x] Login, refresh com rotação e detecção de reuso, logout, recuperação e alteração de senha.
- [x] Administração de usuários, status, redefinição, permissões adicionais e grants de escopo.
- [x] Resource Server JWT stateless com issuer, audience, expiração, HMAC SHA-256 e chave mínima de 32 bytes.
- [x] Erros padronizados `401`/`403`, CORS tipado, BCrypt configurável (padrão 12) e política de senha.
- [x] Interface `CurrentUser` e avaliador que não concede acesso organizacional implícito a gestores/supervisores.
- [x] Bootstrap de administrador demo opcional, idempotente e sem credenciais em migrações ou logs.
- [x] Porta de auditoria reutilizável com adaptador no-op; persistência completa permanece para a Fase 5.
- [x] E-mail de recuperação enviado após commit por porta substituível, sem registrar o token em logs.

## Fase 2 - organização e colaboradores

- [x] Migração append-only `V6` para configurações da organização, unicidade de e-mail, metadados de foto e histórico imutável de colaborador.
- [x] Consulta e manutenção da organização e dos padrões globais, preservando nota mínima de 70% e percentual fixo de vídeo de 80%.
- [x] CRUD/status de unidades, setores e cargos, incluindo setores por unidade e colaboradores por setor/cargo.
- [x] CRUD/status/cargo de colaboradores com matrícula e e-mail únicos na organização e validação de referências ativas.
- [x] Histórico cadastral paginado em `/employees/{employeeId}/history`; histórico de treinamentos permanece para as fases de progresso.
- [x] Foto protegida em object storage com chave gerada, validação JPEG/PNG/WEBP de até 5 MB e URL temporária; limpeza do objeto anterior após commit.
- [x] Listagens e detalhes organizacionais filtrados no banco pelos grants ativos UNIT/SECTOR/EMPLOYEE de gestores e supervisores.
- [x] Validação dos alvos de grants por portas públicas de aplicação, sem acesso cruzado a repositórios.
- [x] Limitação de login para contas conhecidas e desconhecidas usando hash do e-mail e janela configurável em `login_attempt_states`.
- [ ] Efeitos de atividades, atribuições e qualificações após mudança de cargo pertencem à Fase 3 e são sinalizados como pendentes.
- [ ] Auditoria administrativa persistente de identidade pertence à Fase 5; `EmployeeHistory` já é persistido nesta fase.

## Fase 3 - slice A: atividades e integridade do catálogo

- [x] Migração append-only `V7` para atividades, vínculos históricos de cargo/colaborador e requisitos obrigatórios.
- [x] Isolamento organizacional por chaves compostas e unicidade parcial para impedir vínculos ativos duplicados.
- [x] CRUD paginado de atividades, requisitos e relações de cargo/colaborador com escopo de gestor aplicado nas consultas.
- [x] Origem efetiva sem duplicação (`JOB`/`MANUAL`) e remoção lógica que preserva vínculos e treinamentos históricos.
- [x] Propagação para colaboradores ativos e eventos after-commit para atribuições e recálculo; consumidores desta fase são no-op explícito.
- [x] Porta pública `TrainingCatalog` para validação de treinamento ativo, versão publicada e resolução da publicação vigente.
- [x] Nota mínima de 70%, resposta correta ativa única, vídeo obrigatório, publicação segura e snapshots imutáveis de metadados/conteúdo.
- [x] Duplicação de versão, resumo de conteúdo, reordenação em lote e endpoints dedicados de status/exclusão de conteúdo em rascunho.
- [ ] Persistência de atribuições, qualificações e `/activities/{id}/qualified-employees` pertence ao slice B; nenhum resultado fictício é retornado.

## Fase 3 - slice B: atribuições e qualificações operacionais

- [x] Migração append-only `V8` para atribuições com versão exata, proveniência múltipla, lotes/resultados e qualificações persistidas; `V1` a `V7` permanecem inalteradas.
- [x] Unicidade parcial impede atribuições executáveis concorrentes por colaborador/treinamento/versão e permite reciclagem posterior após estados terminais.
- [x] APIs individuais, em lote, pessoais, de cancelamento, dispensa e reciclagem aplicam versão publicada, colaborador ativo, idempotência, permissão e escopo no banco.
- [x] Lotes limitam grupos ad hoc a 500 IDs e percorrem cargo, atividade, setor e unidade em páginas de 100, registrando resultados por colaborador.
- [x] Eventos after-commit do slice A agora geram atribuições idempotentes e preservam fontes automáticas adicionais sem duplicar a atribuição efetiva.
- [x] Criação e mudança de cargo de colaborador orquestram atividades padrão, atribuições e qualificações por portas públicas; remoção opcional atinge apenas origem `JOB`.
- [x] Qualificações persistem `AVAILABLE`, `EXPIRING`, `BLOCKED` e `NOT_ASSIGNED`, motivos estruturados, próximo vencimento e o disclaimer obrigatório.
- [x] Política `FIXED_VERSION` exige a versão exata; `LATEST_PUBLISHED` exige conclusão válida da versão publicada atualmente requerida.
- [x] A porta pública `TrainingCompliancePort` representa evidência exata de conclusão/versão/vencimento e reprovação pendente. O adaptador provisório retorna ausência conservadora até a Fase 4, sem inventar conclusão.
- [x] Dispensa encerra a atribuição, mas não produz evidência de conformidade e não satisfaz requisito obrigatório.
- [x] Testes unitários cobrem origens, inatividade, escopo, idempotência/concorrência, lotes, transições e os quatro estados de qualificação; teste de endpoints com Testcontainers valida o contrato e a migração quando Docker está disponível.
- [x] Progresso real passa ao slice A da Fase 4; tentativas e conclusões permanecem para o slice B e substituirão o
  adaptador conservador de conformidade.

## Fase 4 - slice A: arquivos, execução e prontidão de conteúdo

- [x] Migração append-only `V9` para uploads/arquivos privados, vínculo verificado de vídeo, progresso por vídeo,
  eventos idempotentes imutáveis e histórico de transições; `V1` a `V8` permanecem inalteradas.
- [x] A expectativa de migração é exatamente `V9__phase_4_slice_a_files_and_video_progress.sql`; nenhuma alteração
  retroativa em migrações aplicadas é permitida.
- [x] Upload pré-assinado usa chave imprevisível gerada pelo backend, allowlists por finalidade, limites de tamanho,
  autorização por papel/próprio, expiração e verificação `HEAD` de tipo, tamanho e checksum opcional antes de `UPLOADED`.
- [x] `ObjectStorage` expõe metadados e URLs privadas; reprodução por URL curta é restrita ao administrador ou ao
  colaborador com atribuição ativa da versão exata e preserva suporte a `Range` do MinIO.
- [x] Autoria de novos vídeos exige `fileId` de `TRAINING_VIDEO` concluído. Chaves internas permanecem apenas para
  histórico de versões antigas e nunca aparecem no caminho de aprendizagem ou nas respostas do colaborador.
- [x] A fronteira pública `AssignmentExecutionPort` concentra propriedade, início e transições; progresso não acessa
  repositórios de atribuições e somente o próprio colaborador inicia ou altera execução.
- [x] Caminho de aprendizagem e retomada usam módulos, vídeos e questionários ativos da versão imutável atribuída,
  sem gabarito ou chaves privadas, incluindo duração, obrigatoriedade, progresso e disponibilidade da avaliação.
- [x] Progresso trata `watchedSeconds` como delta validado, aplica tolerâncias configuráveis, rejeita futuro/antigo/
  impossível, não regride, limita à duração, é idempotente por evento e conclui vídeo somente no limite exato de 80%.
- [x] Somente vídeos ativos obrigatórios bloqueiam prontidão. Com questionário, a atribuição avança para
  `AWAITING_ASSESSMENT`; sem questionário, `TrainingReadinessPort` recebe a prontidão e o adaptador conservador não
  fabrica conclusão até o slice B.
- [x] Testes unitários Docker-independent cobrem limite 79,99/80, abertura/seek, regressão, plausibilidade temporal,
  duplicidade, limite acumulado, versão, vídeo opcional, upload verificado e reprodução protegida.
- [ ] Tentativas, conclusão, validade e evidência de conformidade real pertencem ao slice B.

## Fase 4 - slice B: avaliações, conclusões, validade e conformidade

- [x] Migração append-only `V10__phase_4_slice_b_assessments_and_completions.sql` cria tentativas, respostas,
  conclusões e histórico explícito de recálculo de vencimento; `V1` a `V9` permanecem inalteradas.
- [x] Chaves compostas vinculam tentativa e conclusão à organização, ao colaborador, ao treinamento, à versão exata
  e à atribuição. Unicidade serializa a numeração, protege idempotência e limita a uma conclusão automática por
  atribuição, sem impedir novas conclusões históricas de reciclagens.
- [x] Tentativas, respostas, conclusões e recálculos de vencimento são append-only no PostgreSQL; respostas guardam
  enunciado, alternativa selecionada e acerto como snapshots, sem depender de futuras alterações de catálogo.
- [x] Entrega e disponibilidade do questionário exigem proprietário, versão atribuída, conteúdo obrigatório pronto
  e questionário/questões/alternativas ativos. O DTO do colaborador não possui campo de correção ou gabarito e a
  ordem aleatória é determinística por atribuição, questionário e número da tentativa.
- [x] Submissão exige conjunto completo e sem duplicidades, valida pertencimento e atividade da alternativa, calcula
  no servidor com duas casas decimais, aplica o maior limite entre 70%, versão e questionário, máximo de tentativas,
  intervalo e `Idempotency-Key`, usando lock da atribuição para numeração concorrente.
- [x] Reprovações e respostas permanecem no histórico. Aprovação parcial mantém `AWAITING_ASSESSMENT`; somente todos
  os questionários ativos aprovados e todos os vídeos obrigatórios em 80% geram conclusão automática e `COMPLETED`.
  Versões sem questionário usam a mesma conclusão idempotente quando o progresso informa prontidão.
- [x] APIs paginadas de tentativas e conclusões aplicam escopo de administrador, gestor/supervisor ou próprio no
  predicado de consulta. O detalhe de tentativa apresenta apenas seleções submetidas e seu acerto, nunca gabarito
  de uma avaliação não enviada.
- [x] Conclusão manual administrativa aceita versão publicada ou arquivada, preserva score/notas e pode vincular um
  upload concluído `EXTERNAL_CERTIFICATE`; não altera atribuições antigas e não é criada por dispensa.
- [x] Validade aplicada é snapshot `DAYS`, `MONTHS` ou `INDEFINITE`; vencimento usa a data UTC de conclusão e semântica
  de `LocalDate.plusMonths` para fim de mês e ano bissexto. Recálculo administrativo adiciona histórico imutável em
  vez de alterar silenciosamente a conclusão.
- [x] `TrainingCompliancePort` agora consulta conclusões persistidas e a última reprovação não resolvida.
  `TrainingReadinessPort` agora cria conclusão real. Conclusão e reprovação publicam evento after-commit para
  recálculo de qualificações; certificado e notificação permanecem em portas condicionais no-op da Fase 5.
- [x] Testes unitários sem Docker cobrem ocultação do gabarito, shuffle, validação das respostas, limites 69,99/70,
  tentativas e intervalo, idempotência, múltiplos questionários, conclusão automática/manual/externa, versão exata,
  validade em dias/meses/indeterminada, imutabilidade, conformidade e transições. O teste de endpoints com
  Testcontainers valida o carregamento da V10 quando um daemon estiver disponível.
- [ ] Certificados internos/externos como entidade, scheduler de vencimentos, notificações e auditoria persistente
  permanecem para a Fase 5; este slice expõe somente eventos e portas explícitas para esses consumidores.
