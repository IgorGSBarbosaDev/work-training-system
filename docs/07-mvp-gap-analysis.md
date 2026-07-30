# Diagnóstico do caminho até o MVP

**Data do diagnóstico:** 24/08/2026  
**Base analisada:** árvore atual do repositório, `pom.xml`, `src/`, `frontend/`, Docker Compose, CI, contratos e documentos em `docs/`.  
**Referências:** [PRD do MVP](01-prd-mvp.md), [fonte da verdade](../work-training-system-fonte-da-verdade.md), [auditoria de implementação](06-implementation-audit.md), [contrato da API](05-api-contract.md) e [aceite técnico](09-technical-acceptance.md).

## Resumo executivo

O projeto possui uma fundação de backend avançada em um monólito modular Spring Boot. A prioridade 1 — editor administrativo de treinamentos — e a prioridade 2 — relações organizacionais — estão integradas nesta linha de trabalho. O backend preserva versões publicadas, aplica regras de escopo e autorização, e mantém histórico de atribuições, conclusões, qualificações e auditoria.

O MVP ainda não deve ser declarado aceito. O código para o fluxo integrado está versionado, mas a matriz de aceite ainda precisa ser executada com o toolchain oficial e os fluxos de produto precisam ser conferidos com dados de demonstração. O maior risco restante é profundidade operacional e evidência ponta a ponta, não a ausência da fundação de domínio.

## Estado atual observado

### Backend

- Identidade, login, refresh, logout, recuperação de senha, usuários, papéis, permissões e escopos.
- Unidades, setores, cargos, colaboradores, status, histórico e fotos protegidas em object storage.
- Relação `cargo -> atividades padrão -> treinamentos obrigatórios`, além de atividades manuais por colaborador.
- Propagação de mudança de cargo, atribuições automáticas, qualificações e distinção de origens `JOB` e `MANUAL`.
- Treinamentos versionados, módulos, vídeos, questionários, questões, alternativas, publicação e imutabilidade de versões publicadas.
- Upload privado de vídeos via MinIO, confirmação server-side e playback protegido por URL presignada.
- Atribuições individuais e em lote, progresso de vídeo, avaliações, conclusões, validade, vencimento, recertificação e bloqueio/liberação de atividades.
- Certificados internos/externos, PDF, download, validação e revogação.
- QR Code com geração, revogação, verificação autenticada e registro de acesso.
- Notificações internas, entregas de e-mail, reenvio e auditoria persistente.
- Migrações Flyway `V1` a `V12`, Compose com PostgreSQL/MinIO/Mailpit e workflow de CI.

### Frontend

O frontend em `frontend/src/App.tsx` usa a API real, sessão persistida e navegação por perfil. Existem fluxos para login, dashboard pessoal e operacional, atribuições, player, questionários, resultados, certificados, QR Code, notificações, colaboradores, atividades, treinamentos, editor de conteúdo, relações organizacionais e auditoria.

A entrega de relações organizacionais acrescenta telas funcionais para unidades, setores, cargos, vínculos de cargo, requisitos de treinamento e atividades específicas de colaboradores. O editor administrativo mantém as operações de versões, módulos, vídeos, questionários, questões e alternativas.

Ainda precisam de comprovação integrada ou aprofundamento conforme o aceite:

- estados de erro, vazio, paginação, filtros e expiração de sessão em todos os fluxos;
- operações administrativas menos exercitadas pelo smoke;
- reprodução de um arquivo de vídeo real no navegador;
- dashboards e relatórios específicos contra dados de demonstração;
- telas operacionais de usuários, certificados, notificações, e-mails e auditoria com ações e filtros completos.

## O que falta para alcançar o MVP

### Produto

- Implementar DTOs e consultas específicas para os dashboards por treinamento, atividade e colaborador. Atualmente esses endpoints ainda delegam para o mesmo overview geral.
- Aprofundar filtros, paginação, ações, estados vazios e mensagens de domínio nas áreas administrativas.
- Confirmar no navegador o caminho completo do colaborador: atribuição, retomada, vídeo, questionário, conclusão e certificado.
- Confirmar que todos os eventos relevantes geram auditoria e notificações internas/e-mail conforme a fonte da verdade.
- Usar dados fictícios consistentes para demonstrar os estados pendente, em andamento, concluído, reprovado, vencido, vencendo e bloqueado.

### Ambiente e aceite

- Executar `./mvnw -B -ntp verify` com Java 21 e versões posteriores suportadas.
- Executar `npm ci`, testes, lint, build e Playwright com Node 22 no CI.
- Subir Compose, executar seed idempotente, smoke API, Mailpit e browser acceptance.
- Validar upload e reprodução de um vídeo real, além de certificado, QR Code, notificações, e-mail e auditoria.
- Atualizar a matriz de [docs/09-technical-acceptance.md](09-technical-acceptance.md) com artefatos reais antes de declarar o MVP concluído.

### Operação e segurança

- Revisar o uso de organização padrão nos módulos recentes antes de uma demonstração multi-organização ou produção.
- Confirmar retry de e-mail em falha SMTP controlada, sem considerar a ausência de falha em Mailpit saudável como evidência de retry.

## Validação registrada

- A prioridade 2 foi desenvolvida com testes de relações organizacionais, propagação de efeitos e persistência de auditoria.
- A prioridade 1 registrou validação de frontend, backend, migrations, Compose, MinIO, publicação e imutabilidade.
- A validação final continua dependente de uma execução oficial com Java 21, Node 22 e Docker, seguida da atualização da matriz de aceite.

## Ordem recomendada

1. Finalizar a compatibilidade Java 21+ e executar o CI completo.
2. Gerar dados fictícios reproduzíveis e executar seed/smoke/browser com esses dados.
3. Corrigir somente falhas reais encontradas na execução integrada.
4. Implementar dashboards específicos e aprofundar as telas operacionais restantes.
5. Atualizar a matriz de aceite e declarar o MVP concluído somente com evidência completa.
