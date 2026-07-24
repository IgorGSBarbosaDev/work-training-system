# PRD do MVP — work-training-system

**Versão:** 1.0  
**Data:** 23/07/2026  
**Status:** Planejamento oficial do MVP  
**Documento de referência:** `work-training-system-fonte-da-verdade.md`

## 1. Visão do produto

O **work-training-system** é uma plataforma corporativa para gestão, realização e validação de treinamentos de colaboradores.

O sistema centraliza treinamentos, conteúdos, avaliações, progresso, conclusões, vencimentos, reciclagens, certificados e qualificações operacionais. Também permite verificar, por QR Code individual, os treinamentos e as atividades que um colaborador está qualificado a executar.

A aplicação será responsiva, mobile-first e utilizável em computadores e celulares.

## 2. Problema

Empresas podem controlar treinamentos por planilhas, documentos separados ou sistemas que dificultam a consulta rápida e confiável das qualificações de um colaborador.

Isso gera problemas como:

- informações dispersas;
- dificuldade para acompanhar pendências e vencimentos;
- ausência de rastreabilidade das conclusões;
- risco de liberar atividades sem todos os treinamentos válidos;
- dificuldade de consulta em campo;
- retrabalho para gestores e administradores.

## 3. Objetivo do MVP

Entregar uma plataforma funcional que permita:

1. administrar usuários, colaboradores e estrutura organizacional;
2. cadastrar treinamentos com vídeos e questionários;
3. relacionar cargos, atividades e treinamentos obrigatórios;
4. atribuir treinamentos de forma automática e manual;
5. acompanhar progresso, avaliações, conclusões e vencimentos;
6. calcular automaticamente qualificações para atividades;
7. emitir certificados;
8. consultar qualificações por QR Code autenticado;
9. acompanhar indicadores em dashboards;
10. registrar ações relevantes para auditoria.

## 4. Usuários

### 4.1 Administrador

Responsável pela configuração e operação completa do sistema.

Principais necessidades:

- manter colaboradores, cargos, setores e unidades;
- cadastrar atividades e treinamentos;
- criar módulos, vídeos e questionários;
- configurar requisitos e atribuições;
- registrar conclusões manuais;
- acompanhar progresso, vencimentos e indicadores;
- gerar certificados e QR Codes;
- consultar o histórico de alterações.

### 4.2 Gestor ou supervisor

Responsável pelo acompanhamento de uma equipe, setor ou grupo autorizado.

Principais necessidades:

- visualizar colaboradores sob sua responsabilidade;
- acompanhar treinamentos e pendências;
- consultar atividades liberadas ou bloqueadas;
- consultar dados por QR Code;
- visualizar indicadores da própria equipe;
- atribuir treinamentos quando autorizado.

### 4.3 Colaborador

Responsável por realizar os treinamentos atribuídos.

Principais necessidades:

- visualizar treinamentos obrigatórios e opcionais;
- iniciar e continuar treinamentos;
- assistir aos vídeos;
- responder aos questionários;
- acompanhar progresso, resultados e vencimentos;
- consultar atividades liberadas e bloqueadas;
- acessar certificados e QR Code pessoal.

## 5. Escopo funcional

### 5.1 Identidade e acesso

- autenticação por e-mail e senha;
- recuperação de senha;
- controle de acesso por perfil;
- limitação de tentativas de login;
- validação de permissões no backend.

### 5.2 Colaboradores e estrutura organizacional

- cadastro e edição de colaboradores;
- matrícula funcional única;
- associação com cargo, setor e unidade;
- status ativo ou inativo;
- foto opcional;
- atividades atribuídas;
- histórico de treinamentos;
- preservação do histórico após inativação.

### 5.3 Cargos e atividades

- cadastro de cargos;
- cadastro de atividades operacionais;
- associação de atividades padrão aos cargos;
- associação de treinamentos obrigatórios às atividades;
- inclusão ou remoção de atividades específicas por colaborador;
- recálculo de qualificações após alteração de cargo ou requisitos.

### 5.4 Treinamentos

Cada treinamento poderá conter:

- nome e código;
- descrição e categoria;
- indicação se é uma NR;
- carga horária;
- validade em dias, meses ou indeterminada;
- nota mínima;
- status ativo ou inativo;
- versão;
- módulos;
- vídeos;
- questionários opcionais.

### 5.5 Conteúdo

O MVP suportará:

- vídeos;
- questionários de múltipla escolha.

Estrutura:

```text
Treinamento
└── Módulos
    ├── Vídeos
    └── Questionário opcional
```

### 5.6 Progresso e conclusão

- registro de progresso por vídeo;
- salvamento do ponto em que o colaborador parou;
- continuidade posterior;
- conclusão de vídeo obrigatório com no mínimo 80% assistido;
- conclusão automática do treinamento quando todos os critérios forem atendidos;
- registro manual de treinamentos presenciais ou externos.

### 5.7 Questionários

- questões de múltipla escolha;
- uma única alternativa correta;
- nota mínima padrão de 70%;
- quantidade máxima de tentativas configurável;
- intervalo entre tentativas configurável;
- histórico de tentativas;
- ordem aleatória opcional;
- feedback de aprovação ou reprovação.

### 5.8 Atribuições

Os treinamentos poderão ser atribuídos por:

- colaborador;
- cargo;
- atividade;
- setor;
- unidade;
- grupo de colaboradores.

Cada atribuição registrará origem, data, prazo, status, prioridade e responsável.

### 5.9 Validade e reciclagem

- cálculo automático da data de vencimento;
- status de vencendo em breve, com padrão de 30 dias;
- bloqueio de atividades dependentes quando necessário;
- nova atribuição para reciclagem;
- preservação do histórico anterior.

### 5.10 Qualificações

As atividades serão classificadas como:

- **Liberada:** todos os treinamentos obrigatórios estão concluídos e válidos;
- **Vencendo:** todos estão válidos, mas pelo menos um vencerá em breve;
- **Bloqueada:** falta treinamento, existe treinamento vencido ou reprovação pendente;
- **Não atribuída:** a atividade não está associada ao colaborador.

### 5.11 QR Code

- um QR Code ativo por colaborador;
- token aleatório, não sequencial e sem dados pessoais;
- geração, revogação e substituição do código;
- consulta autenticada;
- registro das consultas para auditoria;
- exibição de treinamentos, NRs, validades, atividades liberadas e bloqueadas.

### 5.12 Certificados

- geração de certificado em PDF;
- código de validação;
- versão, carga horária, conclusão e vencimento;
- anexação de certificados externos.

### 5.13 Dashboards e relatórios

- dashboard do colaborador;
- dashboard administrativo;
- indicadores gerais;
- visões por treinamento, atividade e colaborador;
- filtros por unidade, setor, cargo, atividade, treinamento, situação e período.

### 5.14 Notificações

- notificações internas;
- envio de e-mails;
- eventos de atribuição, prazo próximo, vencimento, reprovação, conclusão e bloqueio de atividade.

### 5.15 Auditoria

Registro de ações administrativas e consultas relevantes, incluindo:

- usuário responsável;
- ação executada;
- entidade afetada;
- data e hora;
- dados essenciais da alteração.

## 6. Requisitos não funcionais

### 6.1 Experiência

- interface simples, limpa e intuitiva;
- design responsivo e mobile-first;
- poucos elementos por tela;
- ações principais visíveis;
- navegação consistente;
- acessibilidade;
- status representados por texto e ícones, sem depender somente de cores.

### 6.2 Performance

- paginação no backend;
- lazy loading de páginas, vídeos e imagens;
- índices adequados no banco;
- DTOs e projeções para consultas;
- relacionamentos sem carregamento automático desnecessário;
- cache para dados estáveis e indicadores;
- consultas agregadas para dashboards;
- processamento assíncrono de e-mails e certificados;
- streaming progressivo de vídeos.

### 6.3 Segurança

- senhas com hash seguro;
- autorização por perfil;
- validação e sanitização de entradas;
- proteção de vídeos e arquivos;
- QR Codes com tokens aleatórios;
- auditoria de ações administrativas;
- proteção contra acesso indevido entre usuários.

### 6.4 Arquitetura

O MVP será implementado como **monólito modular**, inicialmente para uma única organização, com estrutura que permita futura evolução para múltiplas empresas.

Módulos previstos:

```text
identity
employees
organizations
jobs
activities
trainings
content
assessments
assignments
progress
qualifications
certificates
qr-verification
notifications
audit
reporting
```

## 7. Tecnologias definidas

### Backend

- Java 21;
- Spring Boot;
- Spring Security;
- Spring Data JPA;
- PostgreSQL;
- Flyway;
- Bean Validation;
- OpenAPI/Swagger;
- JUnit;
- Mockito;
- Testcontainers.

### Frontend

- React;
- TypeScript;
- Vite.

### Infraestrutura

- Docker;
- Docker Compose;
- GitHub Actions;
- object storage para vídeos e certificados;
- MinIO no ambiente local.

## 8. Fora do escopo

Não fazem parte do MVP:

- aplicativos Android ou iOS;
- pagamentos, assinaturas ou planos comerciais;
- multiempresa completo;
- cadastro público de empresas;
- SCORM;
- aulas ao vivo;
- provas dissertativas;
- editor interativo avançado;
- chat;
- inteligência artificial;
- reconhecimento facial;
- assinatura eletrônica;
- WhatsApp;
- Microsoft Teams;
- microserviços;
- integração com sistemas reais de RH;
- prontuário médico;
- validação legal automática de aptidão.

## 9. Critérios de conclusão

O MVP será considerado concluído quando todos os 26 critérios definidos na fonte da verdade estiverem atendidos, incluindo autenticação, cadastros, conteúdos, progresso, avaliações, qualificações, certificados, QR Code, dashboards, notificações, auditoria, Docker Compose, testes automatizados e demonstração com dados fictícios.

## 10. Restrições e premissas

- o sistema será inicialmente utilizado por uma única organização;
- os dados utilizados na demonstração serão fictícios;
- a qualificação calculada pelo sistema não substitui liberações médicas, operacionais ou legais externas;
- vídeos não serão armazenados no banco de dados;
- mudanças de escopo devem ser registradas primeiro na fonte da verdade.
