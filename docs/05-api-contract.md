# Contrato de API — work-training-system

**Versão:** 1.0  
**Data:** 23/07/2026  
**Status:** Contrato inicial do MVP  
**Base funcional:** `work-training-system-fonte-da-verdade.md`, `01-prd-mvp.md`, `02-regras-de-negocio.md`, `03-modelo-de-dados.md` e `04-casos-de-uso.md`

## 1. Objetivo

Este documento define o contrato REST inicial do MVP do **work-training-system**:

- convenções da API;
- autenticação e autorização;
- paginação, filtros e ordenação;
- formato de erros;
- enums;
- recursos;
- comandos;
- endpoints;
- payloads principais;
- códigos HTTP;
- relação entre endpoints e casos de uso.

Este contrato orientará controllers, DTOs, validações, documentação OpenAPI/Swagger e integração com o frontend.

## 2. Decisões técnicas do contrato

As regras funcionais vêm da fonte da verdade. Os itens abaixo são decisões técnicas propostas para tornar o contrato implementável:

1. prefixo da API: `/api/v1`;
2. identificadores internos no formato UUID;
3. autenticação por access token e refresh token;
4. access token enviado como Bearer Token;
5. dados em JSON, exceto uploads e downloads;
6. datas e horários no padrão ISO 8601;
7. horários persistidos e retornados em UTC;
8. paginação iniciada na página `0`;
9. exclusão lógica ou inativação para dados com histórico;
10. erros retornados em formato único;
11. endpoints de ação usados somente quando a operação não representa CRUD simples;
12. validação de permissões sempre realizada no backend.

### 2.1 Lacunas funcionais identificadas

A fonte oficial exige atribuição por grupo e acompanhamento da equipe do gestor, mas o modelo de dados inicial ainda não define:

- entidade persistente de grupo de colaboradores;
- relacionamento entre gestor e colaboradores, setores ou unidades autorizadas.

Neste contrato:

- atribuição ad hoc por grupo utiliza uma lista explícita de `employeeIds`;
- endpoints de equipe retornam somente o escopo autorizado, mas a implementação desse escopo depende da definição posterior do relacionamento de gestão;
- grupos persistentes não são criados neste contrato.

## 3. Convenções gerais

### 3.1 URL base

```text
/api/v1
```

Exemplo:

```text
GET /api/v1/employees
```

### 3.2 Content types

| Operação | Content-Type |
|---|---|
| JSON | `application/json` |
| Upload direto | `multipart/form-data` |
| PDF | `application/pdf` |
| Imagem de QR Code | `image/png` |
| Download genérico | conforme o arquivo |

### 3.3 Identificadores

Todos os identificadores internos usam UUID:

```text
7df47d52-7272-42a7-b8ad-d615f9fe89f1
```

A matrícula continua sendo o identificador funcional único do colaborador, mas não substitui o UUID nas relações internas.

### 3.4 Datas

| Tipo | Formato |
|---|---|
| Data | `YYYY-MM-DD` |
| Data e hora | `YYYY-MM-DDTHH:mm:ssZ` |
| Duração de vídeo | segundos inteiros |
| Carga horária | minutos inteiros |
| Posição no vídeo | segundos inteiros |

### 3.5 Headers

| Header | Obrigatório | Uso |
|---|---:|---|
| `Authorization: Bearer <token>` | Sim, exceto login e recuperação | Autenticação |
| `Content-Type` | Sim em requisições com body | Tipo do conteúdo |
| `Accept-Language` | Não | Idioma das mensagens |
| `Idempotency-Key` | Recomendado em operações em lote | Evitar duplicações |
| `X-Request-Id` | Não | Correlação de logs |

### 3.6 Status HTTP

| Status | Uso |
|---:|---|
| `200 OK` | Consulta ou atualização concluída |
| `201 Created` | Recurso criado |
| `202 Accepted` | Processamento assíncrono aceito |
| `204 No Content` | Operação concluída sem body |
| `400 Bad Request` | Requisição inválida |
| `401 Unauthorized` | Não autenticado ou token inválido |
| `403 Forbidden` | Sem permissão |
| `404 Not Found` | Recurso inexistente |
| `409 Conflict` | Duplicidade ou conflito de estado |
| `422 Unprocessable Entity` | Regra de negócio violada |
| `429 Too Many Requests` | Limite de tentativas ou requisições |
| `500 Internal Server Error` | Falha inesperada |

## 4. Autenticação e autorização

### 4.1 Perfis

| Código | Descrição |
|---|---|
| `ADMIN` | Administrador |
| `MANAGER` | Gestor |
| `SUPERVISOR` | Supervisor |
| `EMPLOYEE` | Colaborador |

### 4.2 Abreviações usadas nas tabelas

| Abreviação | Perfis |
|---|---|
| Público | Sem autenticação |
| Todos | Todos os usuários autenticados |
| ADM | `ADMIN` |
| GES | `MANAGER` e `SUPERVISOR` |
| COL | `EMPLOYEE` |
| ADM/GES | Administrador, gestor ou supervisor |
| Próprio | Usuário acessando os próprios dados |
| Escopo | Dados limitados à equipe, setor ou unidade autorizada |

### 4.3 Regra de autorização

A presença de um perfil não elimina a validação de escopo. Um gestor ou supervisor somente pode acessar colaboradores e indicadores dentro de seu escopo autorizado.

## 5. Paginação, filtros e ordenação

### 5.1 Parâmetros comuns

| Parâmetro | Tipo | Padrão | Regra |
|---|---|---:|---|
| `page` | integer | `0` | mínimo `0` |
| `size` | integer | `20` | mínimo `1`, máximo `100` |
| `sort` | string | `createdAt,desc` | `campo,direção` |
| `search` | string | — | busca textual |
| `status` | enum | — | filtro por status |
| `createdFrom` | date | — | data inicial |
| `createdTo` | date | — | data final |

### 5.2 Resposta paginada

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true,
  "sort": [
    {
      "property": "createdAt",
      "direction": "DESC"
    }
  ]
}
```

## 6. Formato de erro

```json
{
  "timestamp": "2026-07-23T21:00:00Z",
  "status": 422,
  "error": "BUSINESS_RULE_VIOLATION",
  "code": "EMPLOYEE_INACTIVE",
  "message": "Colaborador inativo não pode receber novas atribuições.",
  "path": "/api/v1/training-assignments",
  "requestId": "0ef067da-104e-494e-a2d1-fe93ffc060e2",
  "fieldErrors": [
    {
      "field": "employeeId",
      "code": "inactive",
      "message": "O colaborador informado está inativo."
    }
  ]
}
```

### 6.1 Códigos de erro principais

| Código | Status | Situação |
|---|---:|---|
| `VALIDATION_ERROR` | 400 | Campo ausente ou formato inválido |
| `INVALID_CREDENTIALS` | 401 | E-mail ou senha inválidos |
| `ACCOUNT_LOCKED` | 429 | Limite de login atingido |
| `ACCESS_DENIED` | 403 | Sem permissão |
| `RESOURCE_NOT_FOUND` | 404 | Recurso inexistente |
| `REGISTRATION_ALREADY_EXISTS` | 409 | Matrícula duplicada |
| `EMAIL_ALREADY_EXISTS` | 409 | E-mail já utilizado |
| `TRAINING_CODE_ALREADY_EXISTS` | 409 | Código de treinamento duplicado |
| `ACTIVE_RELATION_ALREADY_EXISTS` | 409 | Vínculo ativo duplicado |
| `INVALID_STATE_TRANSITION` | 409 | Transição de estado inválida |
| `EMPLOYEE_INACTIVE` | 422 | Operação proibida para inativo |
| `TRAINING_INACTIVE` | 422 | Treinamento indisponível |
| `VERSION_NOT_PUBLISHED` | 422 | Versão ainda não publicada |
| `ASSESSMENT_NOT_AVAILABLE` | 422 | Questionário indisponível |
| `ATTEMPT_LIMIT_REACHED` | 422 | Tentativas esgotadas |
| `ATTEMPT_INTERVAL_NOT_MET` | 422 | Intervalo ainda não atendido |
| `VIDEO_PROGRESS_INVALID` | 422 | Progresso inconsistente |
| `ASSIGNMENT_NOT_AVAILABLE` | 422 | Atribuição indisponível |
| `QR_CODE_INVALID` | 404 | Token inválido |
| `QR_CODE_REVOKED` | 410 | QR Code revogado |
| `CERTIFICATE_REVOKED` | 410 | Certificado revogado |

## 7. Enums do contrato

### 7.1 Status cadastral

```text
ACTIVE
INACTIVE
```

### 7.2 Status do usuário

```text
ACTIVE
INACTIVE
LOCKED
```

### 7.3 Status da versão do treinamento

```text
DRAFT
PUBLISHED
ARCHIVED
```

### 7.4 Tipo de validade

```text
DAYS
MONTHS
INDEFINITE
```

### 7.5 Origem de atividade do colaborador

```text
JOB
MANUAL
```

### 7.6 Origem da atribuição

```text
EMPLOYEE
JOB
ACTIVITY
SECTOR
UNIT
GROUP
RECERTIFICATION
```

### 7.7 Status da atribuição

```text
NOT_STARTED
IN_PROGRESS
AWAITING_ASSESSMENT
APPROVED
FAILED
COMPLETED
EXPIRING_SOON
EXPIRED
CANCELLED
WAIVED
```

### 7.8 Prioridade da atribuição

Decisão proposta:

```text
NORMAL
HIGH
URGENT
```

### 7.9 Forma de conclusão

```text
AUTOMATIC
MANUAL
```

### 7.10 Situação da qualificação

```text
AVAILABLE
EXPIRING
BLOCKED
NOT_ASSIGNED
```

### 7.11 Tipo de certificado

```text
INTERNAL
EXTERNAL
```

### 7.12 Status do certificado

```text
ACTIVE
REVOKED
```

### 7.13 Status do QR Code

```text
ACTIVE
REVOKED
```

### 7.14 Status da notificação

```text
UNREAD
READ
ARCHIVED
```

## 8. Endpoints de autenticação

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/auth/login` | Público | Autenticar usuário | 200 |
| POST | `/auth/refresh` | Público com refresh token | Renovar tokens | 200 |
| POST | `/auth/logout` | Todos | Invalidar refresh token atual | 204 |
| GET | `/auth/me` | Todos | Consultar usuário autenticado | 200 |
| POST | `/auth/password/forgot` | Público | Solicitar recuperação de senha | 202 |
| POST | `/auth/password/reset` | Público | Redefinir senha com token | 204 |
| PATCH | `/auth/password` | Todos | Alterar a própria senha | 204 |

### 8.1 Login

```json
{
  "email": "colaborador@empresa.com",
  "password": "senha"
}
```

```json
{
  "accessToken": "jwt",
  "refreshToken": "opaque-or-jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {
    "id": "cfef5984-0724-47d7-9278-bb7f9aa716b4",
    "email": "colaborador@empresa.com",
    "role": "EMPLOYEE",
    "employeeId": "30f9bf30-c1b6-4404-b81d-133eaf56a44b"
  }
}
```

### 8.2 Recuperação de senha

Para evitar enumeração de usuários, o endpoint deve retornar `202` mesmo quando o e-mail não estiver cadastrado.

## 9. Endpoints de usuários e permissões

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/users` | ADM | Criar usuário | 201 |
| GET | `/users` | ADM | Listar usuários | 200 |
| GET | `/users/{userId}` | ADM | Consultar usuário | 200 |
| PATCH | `/users/{userId}` | ADM | Alterar e-mail, perfil ou vínculo | 200 |
| PATCH | `/users/{userId}/status` | ADM | Ativar, inativar ou bloquear | 200 |
| POST | `/users/{userId}/password-reset` | ADM | Forçar fluxo de redefinição | 202 |
| GET | `/users/{userId}/permissions` | ADM | Consultar permissões efetivas | 200 |
| PATCH | `/users/{userId}/permissions` | ADM | Configurar permissões adicionais | 200 |

### 9.1 Criar usuário

```json
{
  "email": "gestor@empresa.com",
  "role": "MANAGER",
  "employeeId": null,
  "sendActivationEmail": true
}
```

## 10. Organização

O MVP trabalha com uma única organização.

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/organization` | Todos | Consultar organização atual | 200 |
| PATCH | `/organization` | ADM | Atualizar nome e configurações | 200 |
| GET | `/organization/settings` | ADM | Consultar configurações globais | 200 |
| PATCH | `/organization/settings` | ADM | Alterar janela de vencimento e configurações | 200 |

### 10.1 Configurações globais

```json
{
  "expiringSoonDays": 30,
  "defaultPassingScore": 70,
  "defaultRequiredVideoPercentage": 80
}
```

Os padrões de 70%, 80% e 30 dias vêm das regras do MVP. Uma alteração global não deve modificar retroativamente versões já concluídas.

## 11. Unidades

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/units` | ADM | Criar unidade | 201 |
| GET | `/units` | ADM/GES | Listar unidades permitidas | 200 |
| GET | `/units/{unitId}` | ADM/GES | Consultar unidade | 200 |
| PATCH | `/units/{unitId}` | ADM | Atualizar unidade | 200 |
| PATCH | `/units/{unitId}/status` | ADM | Ativar ou inativar | 200 |
| GET | `/units/{unitId}/sectors` | ADM/GES | Listar setores da unidade | 200 |

### 11.1 Unidade

```json
{
  "name": "Unidade Ipatinga",
  "code": "IPA",
  "status": "ACTIVE"
}
```

## 12. Setores

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/sectors` | ADM | Criar setor | 201 |
| GET | `/sectors` | ADM/GES | Listar setores permitidos | 200 |
| GET | `/sectors/{sectorId}` | ADM/GES | Consultar setor | 200 |
| PATCH | `/sectors/{sectorId}` | ADM | Atualizar setor | 200 |
| PATCH | `/sectors/{sectorId}/status` | ADM | Ativar ou inativar | 200 |
| GET | `/sectors/{sectorId}/employees` | ADM/GES | Listar colaboradores do setor | 200 |

### 12.1 Setor

```json
{
  "unitId": "bd0df234-8fbc-430d-b7af-02c74af53bfc",
  "name": "Manutenção",
  "code": "MAN",
  "status": "ACTIVE"
}
```

## 13. Cargos

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/jobs` | ADM | Criar cargo | 201 |
| GET | `/jobs` | ADM/GES | Listar cargos | 200 |
| GET | `/jobs/{jobId}` | ADM/GES | Consultar cargo | 200 |
| PATCH | `/jobs/{jobId}` | ADM | Atualizar cargo | 200 |
| PATCH | `/jobs/{jobId}/status` | ADM | Ativar ou inativar | 200 |
| GET | `/jobs/{jobId}/activities` | ADM/GES | Listar atividades padrão | 200 |
| POST | `/jobs/{jobId}/activities` | ADM | Vincular atividade padrão | 201 |
| DELETE | `/jobs/{jobId}/activities/{activityId}` | ADM | Remover vínculo ativo | 204 |
| GET | `/jobs/{jobId}/employees` | ADM/GES | Listar colaboradores no cargo | 200 |

### 13.1 Cargo

```json
{
  "name": "Operador Industrial",
  "description": "Responsável por atividades operacionais industriais.",
  "status": "ACTIVE"
}
```

### 13.2 Vincular atividade ao cargo

```json
{
  "activityId": "f8b2f229-5c13-476d-825d-caf80e45c001",
  "applyToCurrentEmployees": true
}
```

A aplicação aos colaboradores atuais pode criar atividades e atribuições obrigatórias, recalcular qualificações e gerar notificações.

## 14. Colaboradores

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/employees` | ADM | Cadastrar colaborador | 201 |
| GET | `/employees` | ADM/GES | Listar colaboradores permitidos | 200 |
| GET | `/employees/{employeeId}` | ADM/GES ou Próprio | Consultar colaborador | 200 |
| GET | `/employees/by-registration/{registration}` | ADM/GES | Consultar por matrícula | 200 |
| PATCH | `/employees/{employeeId}` | ADM | Atualizar cadastro | 200 |
| PATCH | `/employees/{employeeId}/status` | ADM | Ativar ou inativar | 200 |
| PATCH | `/employees/{employeeId}/job` | ADM | Alterar cargo | 200 |
| PUT | `/employees/{employeeId}/photo` | ADM ou Próprio | Atualizar foto | 200 |
| DELETE | `/employees/{employeeId}/photo` | ADM ou Próprio | Remover foto | 204 |
| GET | `/employees/{employeeId}/training-history` | ADM/GES ou Próprio | Consultar histórico | 200 |
| GET | `/employees/{employeeId}/assignments` | ADM/GES ou Próprio | Listar atribuições | 200 |
| GET | `/employees/{employeeId}/completions` | ADM/GES ou Próprio | Listar conclusões | 200 |
| GET | `/employees/{employeeId}/qualifications` | ADM/GES ou Próprio | Listar qualificações | 200 |
| GET | `/employees/{employeeId}/certificates` | ADM/GES ou Próprio | Listar certificados | 200 |

### 14.1 Filtros da listagem

```text
search
registration
email
unitId
sectorId
jobId
activityId
status
qualificationStatus
hasExpiredTraining
hasPendingTraining
page
size
sort
```

### 14.2 Criar colaborador

```json
{
  "name": "Ana Souza",
  "registration": "100245",
  "email": "ana.souza@empresa.com",
  "jobId": "0f7a31ef-3212-4d36-ad08-c5c3bf708ab0",
  "sectorId": "6c5b64c0-8835-47e3-9e33-d25ca78b67ef",
  "unitId": "bd0df234-8fbc-430d-b7af-02c74af53bfc",
  "status": "ACTIVE"
}
```

### 14.3 Resposta do colaborador

```json
{
  "id": "30f9bf30-c1b6-4404-b81d-133eaf56a44b",
  "name": "Ana Souza",
  "registration": "100245",
  "email": "ana.souza@empresa.com",
  "status": "ACTIVE",
  "photoUrl": null,
  "job": {
    "id": "0f7a31ef-3212-4d36-ad08-c5c3bf708ab0",
    "name": "Operador Industrial"
  },
  "sector": {
    "id": "6c5b64c0-8835-47e3-9e33-d25ca78b67ef",
    "name": "Operação"
  },
  "unit": {
    "id": "bd0df234-8fbc-430d-b7af-02c74af53bfc",
    "name": "Unidade Ipatinga"
  },
  "createdAt": "2026-07-23T21:00:00Z",
  "updatedAt": "2026-07-23T21:00:00Z"
}
```

### 14.4 Alterar cargo

```json
{
  "jobId": "16aa2f8c-2fe8-49d4-b312-08ca680630ad",
  "removePreviousJobActivities": false
}
```

A resposta deve informar os efeitos:

```json
{
  "employeeId": "30f9bf30-c1b6-4404-b81d-133eaf56a44b",
  "previousJobId": "0f7a31ef-3212-4d36-ad08-c5c3bf708ab0",
  "currentJobId": "16aa2f8c-2fe8-49d4-b312-08ca680630ad",
  "activitiesAdded": 3,
  "activitiesRemoved": 0,
  "assignmentsCreated": 5,
  "qualificationsRecalculated": 3
}
```

## 15. Atividades do colaborador

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/employees/{employeeId}/activities` | ADM/GES ou Próprio | Listar atividades atribuídas | 200 |
| POST | `/employees/{employeeId}/activities` | ADM | Atribuir atividade específica | 201 |
| DELETE | `/employees/{employeeId}/activities/{activityId}` | ADM | Remover atividade específica | 204 |
| GET | `/employees/{employeeId}/activities/{activityId}` | ADM/GES ou Próprio | Consultar atividade e requisitos | 200 |

### 15.1 Atribuição específica

```json
{
  "activityId": "f8b2f229-5c13-476d-825d-caf80e45c001",
  "reason": "Atividade adicional autorizada pelo gestor operacional."
}
```

A remoção manual não apaga históricos de treinamento.

## 16. Atividades operacionais

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/activities` | ADM | Criar atividade | 201 |
| GET | `/activities` | ADM/GES | Listar atividades | 200 |
| GET | `/activities/{activityId}` | ADM/GES | Consultar atividade | 200 |
| PATCH | `/activities/{activityId}` | ADM | Atualizar atividade | 200 |
| PATCH | `/activities/{activityId}/status` | ADM | Ativar ou inativar | 200 |
| GET | `/activities/{activityId}/jobs` | ADM/GES | Listar cargos relacionados | 200 |
| GET | `/activities/{activityId}/requirements` | ADM/GES | Listar treinamentos obrigatórios | 200 |
| POST | `/activities/{activityId}/requirements` | ADM | Vincular treinamento obrigatório | 201 |
| PATCH | `/activities/{activityId}/requirements/{requirementId}` | ADM | Atualizar requisito | 200 |
| DELETE | `/activities/{activityId}/requirements/{requirementId}` | ADM | Remover requisito ativo | 204 |
| GET | `/activities/{activityId}/qualified-employees` | ADM/GES | Listar situação dos colaboradores | 200 |

### 16.1 Atividade

```json
{
  "name": "Operar ponte rolante",
  "description": "Operação de ponte rolante industrial.",
  "status": "ACTIVE"
}
```

### 16.2 Requisito de treinamento

```json
{
  "trainingId": "f2bb0243-ee35-4103-bb4e-6fd887aa2498",
  "versionPolicy": "LATEST_PUBLISHED",
  "required": true,
  "applyToCurrentEmployees": true
}
```

`versionPolicy` é uma decisão técnica do contrato:

```text
LATEST_PUBLISHED
FIXED_VERSION
```

Quando `FIXED_VERSION` for usada, `trainingVersionId` torna-se obrigatório.

## 17. Treinamentos

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/trainings` | ADM | Criar treinamento e versão inicial | 201 |
| GET | `/trainings` | ADM/GES | Listar treinamentos | 200 |
| GET | `/trainings/{trainingId}` | ADM/GES | Consultar treinamento | 200 |
| PATCH | `/trainings/{trainingId}` | ADM | Atualizar dados cadastrais | 200 |
| PATCH | `/trainings/{trainingId}/status` | ADM | Ativar ou inativar | 200 |
| GET | `/trainings/{trainingId}/versions` | ADM | Listar versões | 200 |
| POST | `/trainings/{trainingId}/versions` | ADM | Criar rascunho de nova versão | 201 |
| GET | `/trainings/{trainingId}/requirements` | ADM | Listar atividades que exigem o treinamento | 200 |
| GET | `/trainings/{trainingId}/assignments` | ADM/GES | Listar atribuições | 200 |
| GET | `/trainings/{trainingId}/completions` | ADM/GES | Listar conclusões | 200 |
| GET | `/trainings/{trainingId}/statistics` | ADM/GES | Consultar indicadores | 200 |

### 17.1 Criar treinamento

```json
{
  "name": "NR-11",
  "code": "NR11",
  "description": "Treinamento de transporte, movimentação e armazenagem.",
  "category": "Normas Regulamentadoras",
  "isRegulatoryStandard": true,
  "status": "ACTIVE",
  "initialVersion": {
    "workloadMinutes": 480,
    "validityType": "MONTHS",
    "validityValue": 24,
    "passingScore": 70,
    "maxAttempts": 3,
    "retryIntervalMinutes": 1440
  }
}
```

## 18. Versões de treinamento

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/training-versions/{versionId}` | ADM | Consultar versão completa | 200 |
| PATCH | `/training-versions/{versionId}` | ADM | Atualizar rascunho | 200 |
| POST | `/training-versions/{versionId}/publish` | ADM | Publicar versão | 200 |
| POST | `/training-versions/{versionId}/archive` | ADM | Arquivar versão | 200 |
| POST | `/training-versions/{versionId}/duplicate` | ADM | Criar novo rascunho baseado na versão | 201 |
| GET | `/training-versions/{versionId}/modules` | ADM ou COL atribuído | Listar módulos | 200 |
| GET | `/training-versions/{versionId}/content-summary` | ADM | Validar estrutura antes da publicação | 200 |

### 18.1 Atualizar versão

```json
{
  "workloadMinutes": 480,
  "validityType": "MONTHS",
  "validityValue": 24,
  "passingScore": 70,
  "maxAttempts": 3,
  "retryIntervalMinutes": 1440
}
```

### 18.2 Publicação

Uma versão somente pode ser publicada quando:

- possuir ao menos um módulo ativo;
- todos os vídeos possuírem referência de arquivo válida;
- cada questionário possuir questões;
- cada questão possuir exatamente uma alternativa correta;
- ordens internas não estiverem duplicadas;
- parâmetros obrigatórios estiverem válidos.

Conteúdo publicado não deve ser alterado diretamente. Alteração relevante exige nova versão.

## 19. Módulos

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/training-versions/{versionId}/modules` | ADM | Criar módulo | 201 |
| GET | `/modules/{moduleId}` | ADM ou COL atribuído | Consultar módulo | 200 |
| PATCH | `/modules/{moduleId}` | ADM | Atualizar módulo de rascunho | 200 |
| PATCH | `/modules/{moduleId}/status` | ADM | Ativar ou inativar | 200 |
| PATCH | `/training-versions/{versionId}/modules/order` | ADM | Reordenar módulos | 200 |
| DELETE | `/modules/{moduleId}` | ADM | Remover módulo do rascunho | 204 |
| GET | `/modules/{moduleId}/videos` | ADM | Listar vídeos do módulo para edição | 200 |

### 19.1 Criar módulo

```json
{
  "title": "Fundamentos",
  "description": "Conceitos iniciais e requisitos de segurança.",
  "order": 1,
  "status": "ACTIVE"
}
```

### 19.2 Reordenar módulos

```json
{
  "items": [
    {
      "moduleId": "8cbfca4a-a626-4913-b122-eb3cbd1013af",
      "order": 1
    },
    {
      "moduleId": "09a4cf7b-dbee-4df4-adbf-e5809ab6f780",
      "order": 2
    }
  ]
}
```

## 20. Vídeos

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/modules/{moduleId}/videos` | ADM | Criar metadados do vídeo | 201 |
| GET | `/videos/{videoId}` | ADM ou COL atribuído | Consultar metadados | 200 |
| PATCH | `/videos/{videoId}` | ADM | Atualizar vídeo de rascunho | 200 |
| PATCH | `/videos/{videoId}/status` | ADM | Ativar ou inativar | 200 |
| PATCH | `/modules/{moduleId}/videos/order` | ADM | Reordenar vídeos | 200 |
| DELETE | `/videos/{videoId}` | ADM | Remover vídeo do rascunho | 204 |
| POST | `/videos/{videoId}/playback-url` | COL atribuído ou ADM | Gerar URL temporária de reprodução | 200 |

### 20.1 Criar vídeo

```json
{
  "title": "Introdução à NR-11",
  "description": "Visão geral da norma.",
  "order": 1,
  "durationSeconds": 900,
  "storageObjectKey": "videos/nr11/v1/introducao.mp4",
  "required": true,
  "status": "ACTIVE"
}
```

### 20.2 URL de reprodução

```json
{
  "url": "https://storage.example/signed-url",
  "expiresAt": "2026-07-23T21:15:00Z",
  "resumeAtSeconds": 315
}
```

A API não deve retornar permanentemente uma URL pública do arquivo protegido.

## 21. Questionários

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/modules/{moduleId}/questionnaire` | ADM | Criar questionário | 201 |
| GET | `/questionnaires/{questionnaireId}` | ADM | Consultar questionário administrativo | 200 |
| PATCH | `/questionnaires/{questionnaireId}` | ADM | Atualizar questionário de rascunho | 200 |
| PATCH | `/questionnaires/{questionnaireId}/status` | ADM | Ativar ou inativar | 200 |
| DELETE | `/modules/{moduleId}/questionnaire` | ADM | Remover questionário do rascunho | 204 |
| GET | `/modules/{moduleId}/questionnaire` | ADM | Consultar questionário opcional do módulo | 200/404 |

### 21.1 Criar questionário

```json
{
  "title": "Avaliação do módulo",
  "passingScore": 70,
  "maxAttempts": 3,
  "retryIntervalMinutes": 1440,
  "shuffleQuestions": true,
  "status": "ACTIVE"
}
```

## 22. Questões e alternativas

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/questionnaires/{questionnaireId}/questions` | ADM | Criar questão | 201 |
| GET | `/questions/{questionId}` | ADM | Consultar questão com gabarito | 200 |
| PATCH | `/questions/{questionId}` | ADM | Atualizar questão | 200 |
| PATCH | `/questions/{questionId}/status` | ADM | Ativar ou inativar | 200 |
| DELETE | `/questions/{questionId}` | ADM | Remover questão do rascunho | 204 |
| PATCH | `/questionnaires/{questionnaireId}/questions/order` | ADM | Reordenar questões | 200 |
| POST | `/questions/{questionId}/options` | ADM | Criar alternativa | 201 |
| PATCH | `/answer-options/{optionId}` | ADM | Atualizar alternativa | 200 |
| PATCH | `/answer-options/{optionId}/status` | ADM | Ativar ou inativar | 200 |
| DELETE | `/answer-options/{optionId}` | ADM | Remover alternativa do rascunho | 204 |
| PATCH | `/questions/{questionId}/options/order` | ADM | Reordenar alternativas | 200 |

### 22.1 Criar questão

```json
{
  "statement": "Qual é o percentual mínimo de visualização de cada vídeo obrigatório?",
  "order": 1,
  "status": "ACTIVE",
  "options": [
    {
      "text": "50%",
      "order": 1,
      "correct": false
    },
    {
      "text": "70%",
      "order": 2,
      "correct": false
    },
    {
      "text": "80%",
      "order": 3,
      "correct": true
    }
  ]
}
```

O campo `correct` nunca deve ser exposto ao colaborador durante a realização da avaliação.

## 23. Upload de arquivos

O fluxo preferencial usa URL pré-assinada para evitar tráfego pesado pelo backend.

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/uploads` | ADM ou Próprio conforme finalidade | Solicitar upload | 201 |
| POST | `/uploads/{uploadId}/complete` | Solicitante | Confirmar upload | 200 |
| DELETE | `/uploads/{uploadId}` | Solicitante ou ADM | Cancelar upload pendente | 204 |
| GET | `/files/{fileId}/download-url` | Autorizado | Gerar URL temporária de download | 200 |

### 23.1 Solicitar upload

```json
{
  "purpose": "TRAINING_VIDEO",
  "fileName": "introducao.mp4",
  "contentType": "video/mp4",
  "sizeBytes": 104857600
}
```

Propósitos previstos:

```text
TRAINING_VIDEO
EMPLOYEE_PHOTO
EXTERNAL_CERTIFICATE
GENERATED_CERTIFICATE
```

Resposta:

```json
{
  "uploadId": "8c81e3ba-f006-4994-9910-c3fd70c1f7ef",
  "method": "PUT",
  "uploadUrl": "https://storage.example/signed-upload-url",
  "objectKey": "videos/temp/8c81e3ba-f006-4994-9910-c3fd70c1f7ef.mp4",
  "expiresAt": "2026-07-23T21:15:00Z",
  "requiredHeaders": {
    "Content-Type": "video/mp4"
  }
}
```

## 24. Atribuições de treinamento

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/training-assignments` | ADM ou GES com permissão | Criar atribuição individual | 201 |
| POST | `/training-assignments/batch` | ADM ou GES com permissão | Criar atribuições em lote | 202 |
| GET | `/training-assignments` | ADM/GES | Listar atribuições permitidas | 200 |
| GET | `/training-assignments/{assignmentId}` | ADM/GES ou Próprio | Consultar atribuição | 200 |
| PATCH | `/training-assignments/{assignmentId}` | ADM ou GES com permissão | Alterar prazo ou prioridade | 200 |
| POST | `/training-assignments/{assignmentId}/start` | Próprio | Iniciar treinamento | 200 |
| POST | `/training-assignments/{assignmentId}/cancel` | ADM ou GES com permissão | Cancelar atribuição | 200 |
| POST | `/training-assignments/{assignmentId}/waive` | ADM | Dispensar colaborador | 200 |
| POST | `/training-assignments/{assignmentId}/recycle` | ADM | Criar atribuição de reciclagem | 201 |
| GET | `/training-assignments/{assignmentId}/learning-path` | ADM/GES ou Próprio | Consultar módulos e progresso | 200 |
| GET | `/training-assignment-batches/{batchId}` | Solicitante | Consultar resultado do lote | 200 |

### 24.1 Atribuição individual

```json
{
  "employeeId": "30f9bf30-c1b6-4404-b81d-133eaf56a44b",
  "trainingId": "f2bb0243-ee35-4103-bb4e-6fd887aa2498",
  "trainingVersionId": null,
  "origin": "EMPLOYEE",
  "dueDate": "2026-08-31",
  "priority": "HIGH"
}
```

Quando `trainingVersionId` for omitido, a API seleciona a versão publicada vigente.

### 24.2 Atribuição em lote

```json
{
  "trainingId": "f2bb0243-ee35-4103-bb4e-6fd887aa2498",
  "trainingVersionId": null,
  "target": {
    "type": "SECTOR",
    "sectorId": "6c5b64c0-8835-47e3-9e33-d25ca78b67ef",
    "employeeIds": []
  },
  "dueDate": "2026-08-31",
  "priority": "NORMAL",
  "skipEmployeesWithValidCompletion": true,
  "skipExistingActiveAssignments": true
}
```

Tipos de destino:

```text
EMPLOYEE
JOB
ACTIVITY
SECTOR
UNIT
GROUP
```

Para `GROUP`, `employeeIds` contém a seleção ad hoc. Não existe grupo persistente nesta versão do contrato.

### 24.3 Resultado do lote

```json
{
  "batchId": "955c89d9-7abe-42ff-a3dc-6e6d31e5024c",
  "status": "COMPLETED",
  "requested": 120,
  "created": 112,
  "skipped": 7,
  "failed": 1,
  "errors": [
    {
      "employeeId": "a3be2ba7-54fe-4d22-a23f-9975547edca5",
      "code": "EMPLOYEE_INACTIVE",
      "message": "Colaborador inativo."
    }
  ]
}
```

## 25. Realização e progresso

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/me/training-assignments` | COL | Listar treinamentos do colaborador autenticado | 200 |
| GET | `/me/training-assignments/{assignmentId}` | COL | Consultar treinamento atribuído | 200 |
| PUT | `/training-assignments/{assignmentId}/videos/{videoId}/progress` | Próprio | Registrar progresso | 200 |
| GET | `/training-assignments/{assignmentId}/videos/{videoId}/progress` | ADM/GES ou Próprio | Consultar progresso | 200 |
| GET | `/training-assignments/{assignmentId}/resume-point` | Próprio | Consultar ponto de continuidade | 200 |

### 25.1 Registrar progresso

```json
{
  "positionSeconds": 420,
  "watchedSeconds": 720,
  "percentageWatched": 80.0,
  "eventAt": "2026-07-23T21:00:00Z"
}
```

Regras:

- atualização é um `upsert`;
- valores menores não devem apagar progresso maior já validado, salvo correção administrativa;
- `percentageWatched` não deve ser confiado isoladamente;
- o backend deve validar duração e progresso acumulado;
- atingir 80% conclui o vídeo obrigatório;
- a resposta deve informar se o treinamento mudou de estado.

```json
{
  "assignmentId": "d23b7338-b740-4dd8-b9ed-1395a56def42",
  "videoId": "ecad440e-ad29-442e-a2d5-8fd1d7e7a5a2",
  "positionSeconds": 420,
  "percentageWatched": 80.0,
  "completed": true,
  "assignmentStatus": "AWAITING_ASSESSMENT",
  "updatedAt": "2026-07-23T21:00:00Z"
}
```

## 26. Avaliações

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/training-assignments/{assignmentId}/questionnaires/{questionnaireId}` | Próprio | Obter avaliação sem gabarito | 200 |
| GET | `/training-assignments/{assignmentId}/questionnaires/{questionnaireId}/availability` | Próprio | Consultar tentativas e próxima liberação | 200 |
| POST | `/training-assignments/{assignmentId}/questionnaires/{questionnaireId}/attempts` | Próprio | Enviar tentativa | 201 |
| GET | `/training-assignments/{assignmentId}/assessment-attempts` | ADM/GES ou Próprio | Listar tentativas | 200 |
| GET | `/assessment-attempts/{attemptId}` | ADM/GES ou Próprio | Consultar resultado da tentativa | 200 |

### 26.1 Questionário entregue ao colaborador

```json
{
  "id": "96c79d8e-12f8-486d-9218-c401b2f3644a",
  "title": "Avaliação do módulo",
  "shuffleQuestions": true,
  "questions": [
    {
      "id": "74ffd191-d422-4cb7-98af-3b22ef370cb1",
      "statement": "Qual opção está correta?",
      "options": [
        {
          "id": "cbd2d642-2aa0-4fb7-a039-13e8719c3402",
          "text": "Alternativa A"
        },
        {
          "id": "1186123c-ea63-478f-a11f-95d6760952ea",
          "text": "Alternativa B"
        }
      ]
    }
  ]
}
```

### 26.2 Enviar tentativa

```json
{
  "answers": [
    {
      "questionId": "74ffd191-d422-4cb7-98af-3b22ef370cb1",
      "answerOptionId": "1186123c-ea63-478f-a11f-95d6760952ea"
    }
  ]
}
```

Resposta:

```json
{
  "attemptId": "acb74fba-baa2-4d39-a1e1-8ea33ada6714",
  "attemptNumber": 2,
  "score": 80.0,
  "passingScore": 70.0,
  "result": "APPROVED",
  "assignmentStatus": "COMPLETED",
  "completedAt": "2026-07-23T21:00:00Z",
  "nextAttemptAt": null
}
```

## 27. Conclusões

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| POST | `/training-completions/manual` | ADM | Registrar conclusão manual | 201 |
| GET | `/training-completions` | ADM/GES | Listar conclusões | 200 |
| GET | `/training-completions/{completionId}` | ADM/GES ou Próprio | Consultar conclusão | 200 |
| GET | `/training-completions/{completionId}/certificate` | ADM/GES ou Próprio | Consultar certificado | 200 |
| POST | `/training-completions/{completionId}/certificate` | ADM | Anexar certificado externo | 201 |
| POST | `/training-completions/{completionId}/recalculate-expiration` | ADM | Recalcular vencimento por correção | 200 |

### 27.1 Conclusão manual

```json
{
  "employeeId": "30f9bf30-c1b6-4404-b81d-133eaf56a44b",
  "trainingId": "f2bb0243-ee35-4103-bb4e-6fd887aa2498",
  "trainingVersionId": "9eb46b7e-4717-4d18-9467-968baed1275a",
  "completedAt": "2026-07-20T13:00:00Z",
  "score": 85.0,
  "validityType": "MONTHS",
  "validityValue": 24,
  "notes": "Treinamento presencial realizado externamente.",
  "externalCertificateFileId": "2f056162-422c-4fdc-a093-49812a6ad0de"
}
```

A conclusão automática não possui endpoint público de criação. Ela é executada pelo domínio quando vídeos e avaliação atendem às regras.

## 28. Validade, vencimentos e reciclagem

As atualizações automáticas são tarefas internas da aplicação, não endpoints públicos obrigatórios.

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/expirations` | ADM/GES | Listar treinamentos vencendo ou vencidos | 200 |
| POST | `/expirations/recalculate` | ADM | Solicitar recálculo administrativo | 202 |
| POST | `/recertifications` | ADM | Criar reciclagem manual | 201 |
| GET | `/recertifications` | ADM/GES | Listar reciclagens | 200 |

Filtros principais:

```text
employeeId
trainingId
unitId
sectorId
jobId
status=EXPIRING_SOON|EXPIRED
expiresFrom
expiresTo
```

## 29. Qualificações

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/qualifications` | ADM/GES | Listar qualificações permitidas | 200 |
| GET | `/qualifications/{qualificationId}` | ADM/GES ou Próprio | Consultar qualificação | 200 |
| GET | `/employees/{employeeId}/activities/{activityId}/qualification` | ADM/GES ou Próprio | Consultar situação e requisitos | 200 |
| POST | `/employees/{employeeId}/qualifications/recalculate` | ADM | Recalcular qualificações do colaborador | 202 |
| POST | `/activities/{activityId}/qualifications/recalculate` | ADM | Recalcular qualificações da atividade | 202 |

### 29.1 Resposta da qualificação

```json
{
  "employee": {
    "id": "30f9bf30-c1b6-4404-b81d-133eaf56a44b",
    "name": "Ana Souza",
    "registration": "100245"
  },
  "activity": {
    "id": "f8b2f229-5c13-476d-825d-caf80e45c001",
    "name": "Operar ponte rolante"
  },
  "status": "BLOCKED",
  "calculatedAt": "2026-07-23T21:00:00Z",
  "nextExpirationDate": null,
  "blockingReasons": [
    {
      "type": "MISSING_TRAINING",
      "trainingId": "f2bb0243-ee35-4103-bb4e-6fd887aa2498",
      "trainingName": "NR-11",
      "assignmentStatus": "NOT_STARTED"
    }
  ],
  "disclaimer": "A qualificação é baseada nos treinamentos registrados e não substitui liberações médicas, operacionais ou legais externas."
}
```

## 30. QR Code

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/employees/{employeeId}/qr-code` | ADM ou Próprio | Consultar QR Code atual | 200 |
| POST | `/employees/{employeeId}/qr-code` | ADM | Gerar ou substituir QR Code | 201 |
| POST | `/employees/{employeeId}/qr-code/revoke` | ADM | Revogar código ativo | 200 |
| GET | `/employees/{employeeId}/qr-code/image` | ADM ou Próprio | Obter imagem do QR Code | 200 |
| GET | `/qr-verifications/{token}` | ADM/GES | Verificar colaborador pelo token | 200 |
| GET | `/qr-verifications/{token}/access-log` | ADM | Consultar acessos do código | 200 |

### 30.1 Verificação por token

A URL codificada no QR Code pode apontar para:

```text
/verificar/{token}
```

O frontend autenticado usa o token para chamar:

```text
GET /api/v1/qr-verifications/{token}
```

Resposta:

```json
{
  "employee": {
    "name": "Ana Souza",
    "registration": "100245",
    "job": "Operador Industrial"
  },
  "trainings": [
    {
      "name": "NR-11",
      "code": "NR11",
      "isRegulatoryStandard": true,
      "completedAt": "2026-07-20T13:00:00Z",
      "expiresAt": "2028-07-20T13:00:00Z",
      "status": "COMPLETED"
    }
  ],
  "regulatoryStandards": [
    {
      "name": "NR-11",
      "completedAt": "2026-07-20T13:00:00Z",
      "expiresAt": "2028-07-20T13:00:00Z"
    }
  ],
  "activities": [
    {
      "name": "Operar ponte rolante",
      "status": "AVAILABLE",
      "pendingTrainings": []
    }
  ]
}
```

O endpoint não retorna CPF, endereço, telefone, dados médicos ou informações fora da validação de treinamento.

## 31. Certificados

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/certificates` | ADM/GES | Listar certificados permitidos | 200 |
| GET | `/certificates/{certificateId}` | ADM/GES ou Próprio | Consultar metadados | 200 |
| GET | `/certificates/{certificateId}/download` | ADM/GES ou Próprio | Baixar PDF | 200 |
| POST | `/certificates/{certificateId}/revoke` | ADM | Revogar certificado | 200 |
| POST | `/certificates/{certificateId}/regenerate` | ADM | Gerar novamente o PDF | 202 |
| GET | `/certificate-validations/{validationCode}` | Público | Validar autenticidade sem expor dados excessivos | 200 |

### 31.1 Validação pública

```json
{
  "valid": true,
  "status": "ACTIVE",
  "trainingName": "NR-11",
  "employeeName": "Ana Souza",
  "completedAt": "2026-07-20",
  "expiresAt": "2028-07-20",
  "issuedAt": "2026-07-20"
}
```

A exibição pública do nome completo deve ser revisada antes da implementação conforme a política de privacidade do projeto. Alternativa recomendada: nome parcialmente mascarado.

## 32. Notificações

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/me/notifications` | Todos | Listar notificações próprias | 200 |
| GET | `/me/notifications/unread-count` | Todos | Contar não lidas | 200 |
| PATCH | `/me/notifications/{notificationId}/read` | Próprio | Marcar como lida | 200 |
| PATCH | `/me/notifications/read-all` | Próprio | Marcar todas como lidas | 200 |
| PATCH | `/me/notifications/{notificationId}/archive` | Próprio | Arquivar | 200 |
| GET | `/admin/email-deliveries` | ADM | Consultar fila e falhas de e-mail | 200 |
| POST | `/admin/email-deliveries/{deliveryId}/retry` | ADM | Tentar envio novamente | 202 |

A criação de notificações é orientada por eventos internos, não por endpoint público genérico.

## 33. Dashboard do colaborador

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/me/dashboard` | COL | Consultar resumo pessoal | 200 |
| GET | `/me/activities` | COL | Consultar atividades e qualificações | 200 |
| GET | `/me/certificates` | COL | Consultar certificados | 200 |
| GET | `/me/qr-code` | COL | Consultar QR Code próprio | 200 |

### 33.1 Resumo pessoal

```json
{
  "continueTraining": {
    "assignmentId": "d23b7338-b740-4dd8-b9ed-1395a56def42",
    "trainingName": "NR-11",
    "progressPercentage": 45.0,
    "resumeAt": {
      "videoId": "ecad440e-ad29-442e-a2d5-8fd1d7e7a5a2",
      "positionSeconds": 315
    }
  },
  "counts": {
    "pending": 2,
    "inProgress": 1,
    "expiringSoon": 1,
    "expired": 0,
    "completed": 8,
    "availableActivities": 3,
    "blockedActivities": 1
  },
  "pendingTrainings": [],
  "expiringTrainings": [],
  "blockedActivities": []
}
```

## 34. Dashboard administrativo e relatórios

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/admin/dashboard/overview` | ADM | Indicadores gerais | 200 |
| GET | `/admin/dashboard/trainings` | ADM | Visão por treinamento | 200 |
| GET | `/admin/dashboard/activities` | ADM | Visão por atividade | 200 |
| GET | `/admin/dashboard/employees` | ADM | Visão por colaborador | 200 |
| GET | `/team/dashboard` | GES | Indicadores da equipe autorizada | 200 |
| GET | `/team/employees` | GES | Acompanhar colaboradores autorizados | 200 |
| GET | `/reports/training-status` | ADM/GES | Relatório paginado de situação | 200 |
| GET | `/reports/qualifications` | ADM/GES | Relatório paginado de qualificações | 200 |
| GET | `/reports/expirations` | ADM/GES | Relatório de vencimentos | 200 |

### 34.1 Filtros comuns dos dashboards

```text
unitId
sectorId
jobId
activityId
trainingId
status
periodFrom
periodTo
```

### 34.2 Indicadores gerais

```json
{
  "activeEmployees": 500,
  "registeredTrainings": 42,
  "assignedTrainings": 1800,
  "notStarted": 240,
  "inProgress": 130,
  "completed": 1250,
  "failed": 15,
  "expired": 80,
  "expiringIn30Days": 45,
  "employeesWithPendingItems": 160,
  "employeesWithBlockedActivities": 70,
  "generatedAt": "2026-07-23T21:00:00Z"
}
```

Consultas pesadas devem utilizar agregação, cache e processamento no backend.

## 35. Auditoria

| Método | Endpoint | Acesso | Descrição | Sucesso |
|---|---|---|---|---:|
| GET | `/audit-logs` | ADM | Listar eventos de auditoria | 200 |
| GET | `/audit-logs/{auditLogId}` | ADM | Consultar detalhes | 200 |

### 35.1 Filtros

```text
userId
action
entityType
entityId
occurredFrom
occurredTo
requestId
page
size
sort
```

### 35.2 Resposta

```json
{
  "id": "29a66a25-062d-497b-9c59-13254a036f05",
  "user": {
    "id": "cfef5984-0724-47d7-9278-bb7f9aa716b4",
    "email": "admin@empresa.com"
  },
  "action": "EMPLOYEE_JOB_CHANGED",
  "entityType": "EMPLOYEE",
  "entityId": "30f9bf30-c1b6-4404-b81d-133eaf56a44b",
  "occurredAt": "2026-07-23T21:00:00Z",
  "requestId": "0ef067da-104e-494e-a2d1-fe93ffc060e2",
  "changes": {
    "jobId": {
      "from": "0f7a31ef-3212-4d36-ad08-c5c3bf708ab0",
      "to": "16aa2f8c-2fe8-49d4-b312-08ca680630ad"
    }
  }
}
```

## 36. Endpoints internos e tarefas agendadas

Os processos abaixo devem ser serviços internos ou tarefas agendadas, não APIs públicas:

- atualizar treinamentos vencendo;
- marcar treinamentos vencidos;
- criar reciclagens automáticas;
- recalcular qualificações afetadas;
- gerar certificados após conclusão;
- enviar notificações;
- enviar e-mails;
- atualizar caches de dashboard;
- limpar uploads temporários;
- revogar tokens expirados.

Endpoints administrativos de recálculo existem apenas para correção operacional e devem ser protegidos, auditados e executados de forma assíncrona.

## 37. Transições de estado

### 37.1 Atribuição

```text
NOT_STARTED
  -> IN_PROGRESS
  -> AWAITING_ASSESSMENT
  -> APPROVED
  -> COMPLETED
```

Fluxo de reprovação:

```text
AWAITING_ASSESSMENT
  -> FAILED
  -> IN_PROGRESS ou AWAITING_ASSESSMENT
  -> APPROVED
  -> COMPLETED
```

Fluxos administrativos:

```text
NOT_STARTED | IN_PROGRESS | AWAITING_ASSESSMENT
  -> CANCELLED

NOT_STARTED | IN_PROGRESS | AWAITING_ASSESSMENT
  -> WAIVED
```

Após a conclusão:

```text
COMPLETED
  -> EXPIRING_SOON
  -> EXPIRED
```

`EXPIRING_SOON` e `EXPIRED` representam a situação da validade e não apagam a conclusão histórica.

### 37.2 QR Code

```text
ACTIVE -> REVOKED
```

Gerar novo código revoga o anterior.

### 37.3 Certificado

```text
ACTIVE -> REVOKED
```

## 38. Validações principais

### 38.1 Colaborador

- nome obrigatório;
- matrícula obrigatória e única;
- e-mail válido;
- cargo, setor e unidade existentes e ativos;
- setor deve pertencer à unidade informada;
- colaborador inativo não recebe novas atribuições.

### 38.2 Treinamento

- nome e código obrigatórios;
- código único por organização;
- carga horária positiva;
- validade positiva quando o tipo não for `INDEFINITE`;
- nota mínima entre `0` e `100`;
- máximo de tentativas maior que zero, quando informado;
- intervalo entre tentativas não negativo.

### 38.3 Conteúdo

- ordens únicas dentro do agrupamento;
- duração de vídeo positiva;
- referência de arquivo válida;
- questionário com pelo menos uma questão;
- questão com pelo menos duas alternativas;
- exatamente uma alternativa correta;
- conteúdo publicado imutável.

### 38.4 Progresso

- posição não negativa;
- posição não superior à duração;
- percentual entre `0` e `100`;
- atribuição deve pertencer ao colaborador autenticado;
- vídeo deve pertencer à versão atribuída.

### 38.5 Avaliação

- uma resposta por questão;
- questões e alternativas devem pertencer ao questionário;
- tentativa somente quando disponível;
- nota calculada exclusivamente no backend.

### 38.6 Qualificação

- atividade somente `AVAILABLE` quando todos os requisitos obrigatórios estiverem concluídos e válidos;
- `EXPIRING` quando todos estiverem válidos e ao menos um estiver na janela de vencimento;
- `BLOCKED` quando faltar treinamento, existir vencimento ou reprovação pendente;
- `NOT_ASSIGNED` quando não houver vínculo com o colaborador.

## 39. Mapeamento com casos de uso

| Caso de uso | Endpoints principais |
|---|---|
| UC-001 Autenticar usuário | `POST /auth/login`, `POST /auth/refresh` |
| UC-002 Recuperar senha | `POST /auth/password/forgot`, `POST /auth/password/reset` |
| UC-003 Cadastrar colaborador | `POST /employees` |
| UC-004 Editar colaborador | `PATCH /employees/{employeeId}` |
| UC-005 Inativar colaborador | `PATCH /employees/{employeeId}/status` |
| UC-006 Estrutura organizacional | `/units`, `/sectors`, `/jobs` |
| UC-007 Cadastrar atividade | `POST /activities` |
| UC-008 Vincular atividade ao cargo | `POST /jobs/{jobId}/activities` |
| UC-009 Alterar cargo | `PATCH /employees/{employeeId}/job` |
| UC-010 Atribuir atividade específica | `POST /employees/{employeeId}/activities` |
| UC-011 Remover atividade específica | `DELETE /employees/{employeeId}/activities/{activityId}` |
| UC-012 Cadastrar treinamento | `POST /trainings` |
| UC-013 Criar módulos e vídeos | `/training-versions/{versionId}/modules`, `/modules/{moduleId}/videos` |
| UC-014 Criar questionário | `/modules/{moduleId}/questionnaire`, `/questionnaires/{id}/questions` |
| UC-015 Publicar versão | `POST /training-versions/{versionId}/publish` |
| UC-016 Vincular requisito | `POST /activities/{activityId}/requirements` |
| UC-017 Atribuir manualmente | `POST /training-assignments`, `POST /training-assignments/batch` |
| UC-018 Atribuição automática | Serviço interno orientado a eventos |
| UC-019 Iniciar treinamento | `POST /training-assignments/{assignmentId}/start` |
| UC-020 Registrar progresso | `PUT /training-assignments/{assignmentId}/videos/{videoId}/progress` |
| UC-021 Continuar treinamento | `GET /training-assignments/{assignmentId}/resume-point` |
| UC-022 Responder questionário | `POST /training-assignments/{assignmentId}/questionnaires/{id}/attempts` |
| UC-023 Concluir automaticamente | Serviço de domínio após progresso ou tentativa |
| UC-024 Conclusão manual | `POST /training-completions/manual` |
| UC-025 Atualizar vencimento | Tarefa interna; recálculo administrativo em `/expirations/recalculate` |
| UC-026 Gerar reciclagem | `POST /recertifications` ou serviço interno |
| UC-027 Calcular qualificação | Endpoints `/qualifications` e serviços internos |
| UC-028 Gerar QR Code | `POST /employees/{employeeId}/qr-code` |
| UC-029 Revogar QR Code | `POST /employees/{employeeId}/qr-code/revoke` |
| UC-030 Consultar QR Code | `GET /qr-verifications/{token}` |
| UC-031 Gerar certificado | Serviço interno; consulta em `/certificates` |
| UC-032 Dashboard do colaborador | `GET /me/dashboard` |
| UC-033 Dashboard administrativo | `/admin/dashboard/*` |
| UC-034 Acompanhar equipe | `/team/dashboard`, `/team/employees` |
| UC-035 Enviar notificação | Serviço interno; consulta em `/me/notifications` |
| UC-036 Consultar auditoria | `GET /audit-logs` |

## 40. Ordem recomendada de implementação

### Fase 1 — Fundação

1. autenticação;
2. usuários;
3. organização;
4. unidades;
5. setores;
6. cargos;
7. colaboradores;
8. tratamento global de erros;
9. paginação e auditoria básica.

### Fase 2 — Domínio operacional

1. atividades;
2. atividades padrão do cargo;
3. atividades específicas do colaborador;
4. treinamentos;
5. versões;
6. requisitos de atividade;
7. atribuições;
8. qualificações.

### Fase 3 — Realização do treinamento

1. módulos;
2. vídeos;
3. upload;
4. progresso;
5. questionários;
6. tentativas;
7. conclusão automática;
8. conclusão manual.

### Fase 4 — Validação e acompanhamento

1. validade;
2. reciclagem;
3. certificados;
4. QR Code;
5. notificações;
6. dashboards;
7. relatórios;
8. auditoria completa.

## 41. Critério de aceite do contrato

O contrato estará implementado quando:

- todos os endpoints aplicáveis estiverem documentados no Swagger;
- DTOs não expuserem entidades JPA diretamente;
- validações retornarem o formato de erro padronizado;
- autorização e escopo forem aplicados no backend;
- listagens relevantes forem paginadas;
- operações críticas forem auditadas;
- arquivos usarem object storage;
- regras de 80%, 70%, validade e qualificação forem aplicadas pelo domínio;
- testes de integração validarem fluxos principais e violações de regra;
- endpoints fora do perfil ou do escopo retornarem `403`;
- histórico de versões, tentativas e conclusões for preservado.
