# Modelo de Dados — work-training-system

**Versão:** 1.0  
**Data:** 23/07/2026  
**Status:** Modelo conceitual inicial do MVP  
**Documento de referência:** `work-training-system-fonte-da-verdade.md`

## 1. Objetivo

Este documento organiza as entidades e relacionamentos necessários para atender ao MVP. Ele representa o modelo conceitual e lógico inicial, sem definir ainda contratos de API.

## 2. Visão geral

```text
Organization
├── Unit
├── Sector
├── Job
│   └── JobActivity
├── Employee
│   ├── EmployeeActivity
│   ├── TrainingAssignment
│   ├── VideoProgress
│   ├── AssessmentAttempt
│   ├── TrainingCompletion
│   ├── Certificate
│   └── EmployeeQrCode
└── Training
    ├── TrainingVersion
    │   ├── Module
    │   │   ├── Video
    │   │   └── Questionnaire
    │   │       ├── Question
    │   │       └── AnswerOption
    │   └── ActivityTrainingRequirement
    └── TrainingAssignment
```

## 3. Entidades principais

### 3.1 Organization

Representa a organização proprietária dos dados.

Campos de negócio:

- nome;
- status.

Observação: o MVP utilizará uma única organização, mas as entidades principais devem permitir evolução futura para múltiplas empresas.

### 3.2 User

Representa a identidade autenticável.

Campos de negócio:

- e-mail;
- senha armazenada com hash;
- perfil de acesso;
- status;
- colaborador associado, quando aplicável.

Perfis:

- ADMIN;
- MANAGER ou SUPERVISOR;
- EMPLOYEE.

### 3.3 Unit

Representa uma unidade organizacional.

Campos de negócio:

- organização;
- nome;
- código opcional;
- status.

### 3.4 Sector

Representa um setor da organização.

Campos de negócio:

- organização;
- unidade;
- nome;
- código opcional;
- status.

### 3.5 Job

Representa o cargo ocupado pelo colaborador.

Campos de negócio:

- organização;
- nome;
- descrição opcional;
- status.

### 3.6 Employee

Representa o colaborador.

Campos de negócio:

- organização;
- nome;
- matrícula única;
- e-mail;
- cargo;
- setor;
- unidade;
- foto opcional;
- status ativo ou inativo.

Relacionamentos:

- pertence a uma organização;
- possui um cargo;
- pertence a um setor e uma unidade;
- pode possuir um usuário de acesso;
- possui atividades atribuídas;
- possui atribuições e histórico de treinamentos;
- pode possuir um QR Code ativo.

## 4. Atividades e requisitos

### 4.1 Activity

Representa uma atividade operacional.

Campos de negócio:

- organização;
- nome;
- descrição;
- status.

### 4.2 JobActivity

Relaciona um cargo às suas atividades padrão.

Campos de negócio:

- cargo;
- atividade;
- data de vinculação;
- status.

Restrição:

- o mesmo cargo não deve possuir o mesmo vínculo ativo duplicado com uma atividade.

### 4.3 EmployeeActivity

Relaciona diretamente uma atividade a um colaborador.

Campos de negócio:

- colaborador;
- atividade;
- origem da atribuição;
- data de atribuição;
- status;
- responsável pela alteração.

Origens esperadas:

- cargo;
- atribuição manual;
- outra origem controlada pelo domínio.

### 4.4 ActivityTrainingRequirement

Relaciona uma atividade aos treinamentos obrigatórios.

Campos de negócio:

- atividade;
- treinamento;
- versão ou regra de versão aplicável;
- obrigatoriedade;
- status;
- data de vinculação.

## 5. Treinamentos e conteúdo

### 5.1 Training

Representa o cadastro principal do treinamento.

Campos de negócio:

- organização;
- nome;
- código;
- descrição;
- categoria;
- indicador de NR;
- status ativo ou inativo.

Restrições:

- o código deve identificar o treinamento dentro da organização;
- treinamentos inativos não devem ser usados em novas atribuições, salvo regra administrativa específica futura.

### 5.2 TrainingVersion

Representa uma versão publicada do treinamento.

Campos de negócio:

- treinamento;
- número da versão;
- carga horária;
- validade;
- unidade da validade: dias, meses ou indeterminada;
- nota mínima;
- máximo de tentativas;
- intervalo entre tentativas;
- status da versão;
- data de publicação.

Finalidade:

- preservar o conteúdo e as regras vigentes no momento da realização;
- permitir que colaboradores já iniciados permaneçam na versão atribuída.

### 5.3 Module

Representa uma seção ordenada de uma versão do treinamento.

Campos de negócio:

- versão do treinamento;
- título;
- descrição;
- ordem;
- status.

### 5.4 Video

Representa um vídeo de treinamento.

Campos de negócio:

- módulo;
- título;
- descrição;
- ordem;
- duração;
- URL ou referência no object storage;
- obrigatório;
- status.

Observação:

- o arquivo do vídeo não deve ser armazenado no banco de dados.

### 5.5 Questionnaire

Representa o questionário opcional de um módulo.

Campos de negócio:

- módulo;
- título;
- nota mínima;
- máximo de tentativas;
- intervalo entre tentativas;
- ordem aleatória das questões;
- status.

### 5.6 Question

Representa uma questão de múltipla escolha.

Campos de negócio:

- questionário;
- enunciado;
- ordem;
- status.

### 5.7 AnswerOption

Representa uma alternativa de resposta.

Campos de negócio:

- questão;
- texto;
- indicador de resposta correta;
- ordem;
- status.

Restrição:

- cada questão deve possuir uma única alternativa correta.

## 6. Atribuições, progresso e avaliação

### 6.1 TrainingAssignment

Representa a atribuição de um treinamento a um colaborador.

Campos de negócio:

- colaborador;
- treinamento;
- versão atribuída;
- origem da atribuição;
- data da atribuição;
- prazo para conclusão;
- status;
- prioridade;
- responsável pela atribuição;
- indicador de reciclagem, quando aplicável.

Origens:

- colaborador;
- cargo;
- atividade;
- setor;
- unidade;
- grupo de colaboradores.

Status possíveis:

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

### 6.2 VideoProgress

Representa o progresso de um colaborador em um vídeo.

Campos de negócio:

- atribuição;
- vídeo;
- percentual assistido;
- posição atual;
- data da última atualização;
- indicador de conclusão.

Regra:

- o vídeo obrigatório é concluído com percentual igual ou superior a 80%.

### 6.3 AssessmentAttempt

Representa uma tentativa de questionário.

Campos de negócio:

- atribuição;
- questionário;
- número da tentativa;
- data e hora;
- nota;
- resultado aprovado ou reprovado.

### 6.4 AttemptAnswer

Representa a resposta registrada em uma tentativa.

Campos de negócio:

- tentativa;
- questão;
- alternativa selecionada;
- indicador de acerto.

### 6.5 TrainingCompletion

Representa uma conclusão preservada no histórico.

Campos de negócio:

- colaborador;
- treinamento;
- versão concluída;
- atribuição de origem;
- data de conclusão;
- forma de conclusão: automática ou manual;
- nota final, quando existir;
- validade aplicada;
- data de vencimento;
- responsável pelo registro manual, quando aplicável;
- evidência ou observação de treinamento externo, quando aplicável.

## 7. Qualificações

### 7.1 ActivityQualification

Representa o resultado calculado da qualificação de um colaborador para uma atividade.

Campos de negócio:

- colaborador;
- atividade;
- situação;
- data do cálculo;
- próximo vencimento, quando existir;
- motivo do bloqueio, quando existir.

Situações:

- LIBERADA;
- VENCENDO;
- BLOQUEADA;
- NAO_ATRIBUIDA.

Observação:

- esta entidade pode ser persistida ou calculada sob demanda com cache. A decisão deve considerar desempenho, auditoria e atualização automática.

## 8. QR Code e certificados

### 8.1 EmployeeQrCode

Representa o QR Code individual do colaborador.

Campos de negócio:

- colaborador;
- token aleatório;
- status ativo ou revogado;
- data de geração;
- data de revogação;
- responsável pela geração ou revogação.

Restrições:

- somente um código ativo por colaborador;
- o token não deve conter matrícula nem outro dado pessoal direto.

### 8.2 QrCodeAccessLog

Representa uma consulta realizada por QR Code.

Campos de negócio:

- QR Code consultado;
- usuário responsável pela consulta;
- data e hora;
- resultado da consulta;
- informações técnicas mínimas necessárias para auditoria.

### 8.3 Certificate

Representa um certificado gerado ou anexado.

Campos de negócio:

- conclusão;
- tipo: interno ou externo;
- código de validação;
- URL ou referência do arquivo;
- data de emissão;
- status;
- responsável pelo anexo ou emissão.

Observação:

- o PDF deve ser armazenado em object storage, não no banco de dados.

## 9. Notificações e auditoria

### 9.1 Notification

Representa uma notificação interna.

Campos de negócio:

- destinatário;
- tipo;
- título;
- mensagem;
- entidade relacionada;
- data de criação;
- data de leitura;
- status.

### 9.2 EmailDelivery

Representa o controle de um e-mail enviado ou pendente.

Campos de negócio:

- destinatário;
- evento;
- conteúdo ou referência do template;
- status;
- data de criação;
- data de envio;
- erro, quando existir.

### 9.3 AuditLog

Representa uma ação auditável.

Campos de negócio:

- usuário responsável;
- ação;
- entidade afetada;
- identificador da entidade;
- data e hora;
- dados essenciais da alteração.

## 10. Relacionamentos principais

```text
Organization 1 --- N Unit
Organization 1 --- N Sector
Organization 1 --- N Job
Organization 1 --- N Employee
Organization 1 --- N Activity
Organization 1 --- N Training

Unit 1 --- N Sector
Unit 1 --- N Employee
Sector 1 --- N Employee
Job 1 --- N Employee

Job N --- N Activity              por JobActivity
Employee N --- N Activity         por EmployeeActivity
Activity N --- N Training         por ActivityTrainingRequirement

Training 1 --- N TrainingVersion
TrainingVersion 1 --- N Module
Module 1 --- N Video
Module 1 --- 0..1 Questionnaire
Questionnaire 1 --- N Question
Question 1 --- N AnswerOption

Employee 1 --- N TrainingAssignment
TrainingAssignment 1 --- N VideoProgress
TrainingAssignment 1 --- N AssessmentAttempt
AssessmentAttempt 1 --- N AttemptAnswer
Employee 1 --- N TrainingCompletion
TrainingCompletion 1 --- N Certificate

Employee 1 --- N ActivityQualification
Employee 1 --- N EmployeeQrCode
EmployeeQrCode 1 --- N QrCodeAccessLog
```

## 11. Restrições de integridade

- matrícula do colaborador deve ser única;
- apenas um QR Code pode estar ativo por colaborador;
- cada questão deve possuir uma única resposta correta;
- ordem de módulos, vídeos e questões deve ser controlada dentro do respectivo agrupamento;
- histórico de conclusões e tentativas não deve ser apagado por alterações cadastrais;
- uma conclusão deve permanecer vinculada à versão realizada;
- colaborador inativo não pode receber nova atribuição;
- vídeos e certificados devem ser referenciados por URL ou identificador de object storage;
- relacionamentos importantes devem possuir índices adequados para consultas e dashboards.

## 12. Campos técnicos recomendados

Os campos abaixo são uma recomendação técnica para implementação e não alteram as regras de negócio:

- identificador interno;
- data de criação;
- data da última atualização;
- usuário responsável pela criação ou alteração, quando aplicável;
- controle de versão para concorrência otimista;
- exclusão lógica apenas quando necessária para preservação histórica.
