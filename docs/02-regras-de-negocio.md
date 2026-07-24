# Regras de Negócio — work-training-system

**Versão:** 1.0  
**Data:** 23/07/2026  
**Status:** Regras oficiais do MVP  
**Documento de referência:** `work-training-system-fonte-da-verdade.md`

## 1. Identidade e acesso

### RN-001 — Perfis de acesso

O sistema deve possuir os perfis Administrador, Gestor ou Supervisor e Colaborador.

### RN-002 — Autorização

Toda operação deve validar no backend se o usuário possui permissão para executar a ação solicitada.

### RN-003 — Consulta por QR Code

A consulta completa dos dados vinculados ao QR Code deve exigir autenticação de administrador, gestor ou supervisor.

### RN-004 — Segurança de senha

As senhas devem ser armazenadas utilizando hash seguro.

### RN-005 — Tentativas de login

O sistema deve limitar tentativas de login para reduzir ataques de força bruta.

## 2. Colaboradores

### RN-006 — Matrícula única

Cada colaborador deve possuir uma matrícula funcional única.

### RN-007 — Dados obrigatórios

Cada colaborador deve possuir nome, matrícula, e-mail, cargo, setor, unidade e status.

### RN-008 — Foto opcional

A foto do colaborador é opcional.

### RN-009 — Colaborador inativo

Um colaborador inativo não pode receber novas atribuições.

### RN-010 — Preservação do histórico

A inativação do colaborador não deve excluir seu histórico de treinamentos ou conclusões.

### RN-011 — QR Code de inativo

O QR Code de um colaborador inativo pode ser revogado.

## 3. Cargos, atividades e requisitos

### RN-012 — Atividades padrão do cargo

O cargo define as atividades normalmente esperadas do colaborador.

### RN-013 — Treinamentos obrigatórios da atividade

Cada atividade operacional pode possuir um ou mais treinamentos obrigatórios.

### RN-014 — Atividades específicas

O administrador pode adicionar ou remover atividades específicas de um colaborador, independentemente das atividades padrão do cargo.

### RN-015 — Alteração de cargo

Ao alterar o cargo de um colaborador:

1. as atividades padrão do novo cargo devem ser adicionadas;
2. os treinamentos obrigatórios dessas atividades devem ser atribuídos;
3. treinamentos já realizados devem permanecer no histórico;
4. atividades antigas podem ser removidas pelo administrador;
5. as qualificações devem ser recalculadas automaticamente.

## 4. Treinamentos e versões

### RN-016 — Identificação do treinamento

Cada treinamento deve possuir nome e código.

### RN-017 — Conteúdo permitido

O MVP aceita somente vídeos e questionários de múltipla escolha como conteúdo de treinamento.

### RN-018 — Conteúdo não permitido

PDFs, aulas independentes em texto, aulas ao vivo, SCORM, atividades interativas avançadas e provas dissertativas não fazem parte do MVP.

### RN-019 — Estrutura do treinamento

Um treinamento é composto por módulos. Cada módulo pode conter vídeos e um questionário opcional.

### RN-020 — Nova versão

Toda alteração relevante em um treinamento publicado deve gerar uma nova versão.

### RN-021 — Preservação da versão concluída

O sistema deve preservar a versão, o conteúdo vigente, o resultado do questionário, a data de conclusão e a validade associados à conclusão do colaborador.

### RN-022 — Treinamento iniciado

Colaboradores que já iniciaram uma versão devem continuar nela, salvo decisão administrativa de reiniciar o treinamento.

## 5. Vídeos e progresso

### RN-023 — Progresso por vídeo

O sistema deve registrar o percentual assistido e o ponto em que o colaborador parou em cada vídeo.

### RN-024 — Continuidade

O colaborador deve poder continuar posteriormente o vídeo a partir do ponto salvo.

### RN-025 — Conclusão do vídeo

Um vídeo obrigatório somente é considerado concluído quando o colaborador assistir a pelo menos 80% de sua duração.

### RN-026 — Abertura não conclui

A simples abertura do vídeo não pode ser considerada conclusão.

### RN-027 — Atualizações de progresso

O registro de progresso deve evitar atualizações excessivas.

## 6. Questionários e tentativas

### RN-028 — Tipo de questão

O MVP deve utilizar questões de múltipla escolha com uma única alternativa correta.

### RN-029 — Nota mínima padrão

A nota mínima padrão para aprovação é 70%.

### RN-030 — Cálculo automático

A nota do questionário deve ser calculada automaticamente.

### RN-031 — Histórico de tentativas

Cada tentativa deve registrar data, nota e resultado.

### RN-032 — Limite de tentativas

O número máximo de tentativas deve ser configurável.

### RN-033 — Intervalo entre tentativas

O intervalo para realizar uma nova tentativa deve ser configurável.

### RN-034 — Ordem aleatória

A ordem das questões pode ser aleatória quando essa configuração estiver ativa.

### RN-035 — Resultado

O colaborador somente é aprovado quando sua nota for igual ou superior à nota mínima configurada.

## 7. Conclusão do treinamento

### RN-036 — Conclusão com questionário

Um treinamento com questionário será concluído quando:

```text
Todos os vídeos obrigatórios tiverem progresso >= 80%
E
O questionário tiver resultado aprovado
```

### RN-037 — Conclusão sem questionário

Quando não houver questionário, a conclusão dependerá somente da conclusão de todos os vídeos obrigatórios.

### RN-038 — Conclusão automática

O sistema deve concluir automaticamente o treinamento quando todas as condições forem atendidas.

### RN-039 — Conclusão manual

O administrador pode registrar manualmente a conclusão de treinamentos presenciais ou realizados fora da plataforma.

## 8. Atribuições

### RN-040 — Formas de atribuição

Treinamentos podem ser atribuídos por colaborador, cargo, atividade, setor, unidade ou grupo de colaboradores.

### RN-041 — Dados da atribuição

Cada atribuição deve registrar:

- colaborador;
- treinamento;
- origem;
- data da atribuição;
- prazo para conclusão;
- status;
- prioridade;
- responsável.

### RN-042 — Inativos

Colaboradores inativos não podem receber atribuições novas.

### RN-043 — Atribuição automática

A inclusão de atividades obrigatórias deve gerar as atribuições necessárias para atender aos requisitos dessas atividades.

## 9. Status dos treinamentos

### RN-044 — Status disponíveis

Os principais status são:

- não iniciado;
- em andamento;
- aguardando avaliação;
- aprovado;
- reprovado;
- concluído;
- vencendo em breve;
- vencido;
- cancelado;
- dispensado.

### RN-045 — Vencendo em breve

O status vencendo em breve deve utilizar uma quantidade configurável de dias antes do vencimento.

### RN-046 — Padrão de vencimento próximo

O valor padrão para vencendo em breve é 30 dias.

## 10. Validade e reciclagem

### RN-047 — Tipos de validade

Um treinamento pode ter validade em dias, em meses ou indeterminada.

### RN-048 — Cálculo de vencimento

A data de vencimento deve ser calculada a partir da data de conclusão somada ao prazo de validade.

### RN-049 — Treinamento vencido

Quando um treinamento vencer, ele deixa de atender aos requisitos das atividades.

### RN-050 — Bloqueio por vencimento

Uma atividade dependente pode ser bloqueada quando um treinamento obrigatório estiver vencido.

### RN-051 — Reciclagem

O vencimento deve permitir a geração de uma nova atribuição de reciclagem.

### RN-052 — Histórico da reciclagem

A nova atribuição não deve excluir a conclusão anterior.

## 11. Qualificação para atividades

### RN-053 — Atividade liberada

Uma atividade é liberada quando todos os treinamentos obrigatórios estão concluídos e válidos.

### RN-054 — Atividade vencendo

Uma atividade está vencendo quando todos os requisitos ainda são válidos, mas pelo menos um treinamento vencerá em breve.

### RN-055 — Atividade bloqueada

Uma atividade está bloqueada quando:

- falta treinamento obrigatório;
- existe treinamento obrigatório vencido;
- existe reprovação pendente.

### RN-056 — Atividade não atribuída

Uma atividade é classificada como não atribuída quando não faz parte das atribuições do colaborador.

### RN-057 — Limite da qualificação

A qualificação calculada pelo sistema é baseada nos treinamentos registrados e não substitui liberações médicas, operacionais ou legais externas.

## 12. QR Code

### RN-058 — QR Code individual

Cada colaborador pode possuir um QR Code individual.

### RN-059 — Código ativo

Somente um QR Code pode estar ativo por colaborador.

### RN-060 — Token seguro

O QR Code deve utilizar token aleatório, não sequencial, difícil de prever e sem armazenar diretamente dados pessoais.

### RN-061 — Revogação e substituição

O QR Code pode ser revogado e substituído por um novo código.

### RN-062 — Auditoria de consulta

Cada consulta por QR Code deve ser registrada para auditoria.

### RN-063 — Dados exibidos

A consulta pode exibir:

- nome;
- matrícula;
- cargo;
- treinamentos realizados;
- NRs realizadas;
- validade;
- datas de conclusão e vencimento;
- atividades liberadas;
- atividades bloqueadas;
- treinamentos pendentes que causam bloqueio.

### RN-064 — Dados proibidos

A consulta não deve exibir CPF, endereço, telefone pessoal, informações médicas ou dados desnecessários para validação.

## 13. Certificados

### RN-065 — Geração

O sistema deve gerar certificado em PDF para treinamentos concluídos.

### RN-066 — Dados do certificado

O certificado deve possuir:

- nome e matrícula do colaborador;
- nome e versão do treinamento;
- carga horária;
- data de conclusão;
- data de vencimento, quando existir;
- código de validação;
- identificação da empresa fictícia.

### RN-067 — Certificado externo

O sistema deve permitir anexar certificados externos de treinamentos realizados fora da plataforma.

## 14. Dashboards e relatórios

### RN-068 — Dashboard do colaborador

O dashboard deve priorizar treinamentos em andamento, pendentes, vencendo, vencidos e concluídos, além de atividades, certificados e QR Code.

### RN-069 — Dashboard administrativo

O dashboard administrativo deve apresentar indicadores gerais e visões por treinamento, atividade e colaborador.

### RN-070 — Filtros

Os indicadores devem permitir filtros por unidade, setor, cargo, atividade, treinamento, situação e período.

### RN-071 — Processamento de indicadores

Indicadores pesados não devem ser recalculados integralmente a cada acesso.

## 15. Notificações

### RN-072 — Canais

O MVP deve possuir notificações internas e envio de e-mails.

### RN-073 — Eventos de notificação

Devem gerar notificações:

- novo treinamento atribuído;
- prazo de conclusão próximo;
- treinamento vencendo;
- treinamento vencido;
- reprovação;
- conclusão;
- atividade bloqueada por falta de treinamento.

## 16. Auditoria

### RN-074 — Ações auditáveis

O sistema deve auditar ações relevantes de cadastro, alteração, atribuição, conclusão manual, certificados, QR Codes, cargo, permissões e consultas por QR Code.

### RN-075 — Dados da auditoria

Cada registro deve possuir usuário responsável, ação, entidade afetada, data e hora e dados essenciais da alteração.

## 17. Performance, armazenamento e segurança

### RN-076 — Paginação

Listagens devem utilizar paginação processada pelo backend.

### RN-077 — Vídeos fora do banco

Vídeos não devem ser armazenados no banco de dados.

### RN-078 — Object storage

Vídeos e certificados devem utilizar serviço de arquivos ou object storage.

### RN-079 — Proteção de arquivos

Arquivos e vídeos devem ser protegidos contra acesso indevido.

### RN-080 — Validação de entrada

Toda entrada recebida pelo backend deve ser validada e sanitizada conforme o risco do campo.
