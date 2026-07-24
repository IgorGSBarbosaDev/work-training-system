# work-training-system — Fonte da Verdade

**Versão:** 1.0  
**Data:** 22/07/2026  
**Status:** Escopo oficial do MVP

## 1. Visão do produto

O work-training-system é uma plataforma corporativa para gestão, realização e validação de treinamentos de colaboradores.

O sistema deve permitir que a empresa:

- disponibilize treinamentos em vídeo;
- aplique questionários;
- acompanhe o progresso e a conclusão dos treinamentos;
- controle validade, vencimentos e reciclagens;
- relacione treinamentos a cargos e atividades operacionais;
- determine quais atividades cada colaborador está qualificado a executar;
- consulte as qualificações de um colaborador por meio de um QR Code individual.

O sistema deve ser simples, intuitivo, responsivo, rápido e otimizado para uso em computadores e celulares.

## 2. Problema que o sistema resolve

Empresas podem controlar treinamentos por planilhas, documentos separados ou sistemas que não permitem uma consulta rápida em campo.

O work-training-system centraliza:

- treinamentos disponíveis;
- progresso dos colaboradores;
- resultados de avaliações;
- treinamentos concluídos;
- validades e vencimentos;
- requisitos de cada atividade;
- atividades que cada colaborador pode executar;
- consulta rápida por QR Code.

## 3. Conceitos principais

### 3.1 Cargo

Representa a posição ocupada pelo colaborador dentro da empresa.

Exemplos:

- Operador Industrial;
- Eletricista de Manutenção;
- Técnico de Segurança;
- Mecânico de Manutenção.

O cargo define as atividades normalmente esperadas daquele colaborador.

### 3.2 Atividade operacional

Representa uma tarefa que o colaborador pode executar.

Exemplos:

- operar ponte rolante;
- operar empilhadeira;
- executar trabalho em altura;
- realizar manutenção elétrica;
- acessar espaço confinado.

Cada atividade possui uma lista de treinamentos obrigatórios.

### 3.3 Treinamento

Representa uma capacitação necessária para cumprir uma exigência interna, normativa ou operacional.

Exemplos:

- NR-10;
- NR-11;
- NR-35;
- Operação de Ponte Rolante;
- Segurança Operacional.

### 3.4 Relação entre cargo, atividade e treinamento

O funcionamento será:

```text
Cargo -> atividades padrão -> treinamentos obrigatórios
```

Exemplo:

```text
Cargo: Operador Industrial

Atividade: Operar ponte rolante

Treinamentos obrigatórios:
- NR-11
- Operação de Ponte Rolante
- Segurança Operacional
```

O cargo define quais atividades devem ser atribuídas inicialmente ao colaborador.

O administrador também poderá:

- adicionar uma atividade específica a um colaborador;
- remover uma atividade específica;
- manter atividades diferentes das atividades padrão do cargo.

A atividade somente será considerada liberada quando todos os treinamentos obrigatórios estiverem concluídos e válidos.

## 4. Perfis de acesso

### 4.1 Administrador

Pode:

- cadastrar e editar colaboradores;
- cadastrar cargos, setores e unidades;
- cadastrar atividades operacionais;
- cadastrar treinamentos;
- criar módulos e conteúdos;
- criar questionários;
- vincular treinamentos a atividades;
- vincular atividades a cargos;
- atribuir treinamentos;
- registrar conclusões manuais;
- acompanhar progresso e vencimentos;
- gerar e revogar QR Codes;
- consultar relatórios e indicadores;
- consultar o histórico de alterações.

### 4.2 Gestor ou supervisor

Pode:

- visualizar os colaboradores sob sua responsabilidade;
- acompanhar treinamentos pendentes, em andamento, concluídos e vencidos;
- consultar as atividades liberadas ou bloqueadas;
- escanear o QR Code de um colaborador;
- consultar indicadores da própria equipe ou setor;
- atribuir treinamentos quando tiver permissão.

### 4.3 Colaborador

Pode:

- visualizar seus treinamentos obrigatórios e opcionais;
- iniciar treinamentos;
- assistir aos vídeos;
- responder aos questionários;
- continuar um treinamento do ponto em que parou;
- acompanhar seu progresso;
- consultar treinamentos concluídos e vencimentos;
- consultar as atividades que pode executar;
- visualizar certificados;
- visualizar seu QR Code.

## 5. Gestão de colaboradores

Cada colaborador deve possuir:

- nome;
- matrícula única;
- e-mail;
- cargo;
- setor;
- unidade;
- foto opcional;
- status ativo ou inativo;
- QR Code individual;
- atividades atribuídas;
- histórico de treinamentos.

A matrícula será o identificador funcional único do colaborador.

Um colaborador inativo:

- não poderá receber novas atribuições;
- continuará disponível no histórico;
- terá suas conclusões preservadas;
- poderá ter o QR Code revogado.

## 6. Gestão de treinamentos

Cada treinamento deve possuir:

- nome;
- código;
- descrição;
- categoria;
- indicação se é uma NR;
- carga horária;
- validade em dias ou meses;
- nota mínima;
- status ativo ou inativo;
- versão;
- módulos;
- vídeos;
- questionários opcionais.

### 6.1 Conteúdos suportados no MVP

O MVP suportará somente:

- vídeos;
- questionários de múltipla escolha.

Não serão suportados no MVP:

- PDFs;
- textos como aulas independentes;
- aulas ao vivo;
- conteúdos SCORM;
- atividades interativas avançadas;
- provas dissertativas.

### 6.2 Estrutura do treinamento

A estrutura será:

```text
Treinamento
└── Módulos
    ├── Vídeos
    └── Questionário opcional
```

Cada módulo deve possuir:

- título;
- descrição;
- ordem;
- vídeos;
- questionário opcional.

Cada vídeo deve possuir:

- título;
- descrição;
- ordem;
- duração;
- URL ou referência do arquivo;
- percentual assistido pelo colaborador.

## 7. Conclusão de treinamentos

Um treinamento será considerado concluído quando todas as regras abaixo forem atendidas.

### 7.1 Regra dos vídeos

O colaborador deve assistir a pelo menos **80% de cada vídeo obrigatório**.

O sistema deve:

- registrar o progresso por vídeo;
- salvar o ponto em que o colaborador parou;
- permitir continuar posteriormente;
- impedir que apenas abrir o vídeo seja considerado conclusão;
- considerar concluído somente quando atingir 80% do vídeo.

### 7.2 Regra do questionário

Quando o treinamento possuir questionário, o colaborador deve acertar no mínimo **70% das questões**.

O sistema deve:

- calcular automaticamente a nota;
- registrar cada tentativa;
- armazenar data, nota e resultado;
- permitir configurar o número máximo de tentativas;
- permitir configurar o intervalo entre novas tentativas;
- considerar aprovado somente com nota igual ou superior a 70%.

### 7.3 Regra final de conclusão

O treinamento será concluído quando:

```text
Todos os vídeos obrigatórios >= 80%
E
Questionário >= 70%, quando existir
```

Quando não houver questionário, a conclusão dependerá apenas dos vídeos.

Também será possível registrar manualmente a conclusão de treinamentos presenciais ou realizados fora da plataforma.

## 8. Questionários

O MVP terá questionários com:

- questões de múltipla escolha;
- uma única resposta correta;
- nota mínima padrão de 70%;
- quantidade de tentativas configurável;
- histórico de tentativas;
- ordem aleatória opcional das questões;
- feedback de aprovação ou reprovação.

Cada questão deve possuir:

- enunciado;
- alternativas;
- resposta correta;
- ordem;
- status ativo ou inativo.

## 9. Atribuição de treinamentos

Os treinamentos poderão ser atribuídos por:

- colaborador;
- cargo;
- atividade;
- setor;
- unidade;
- grupo de colaboradores.

A atribuição deve registrar:

- colaborador;
- treinamento;
- origem da atribuição;
- data da atribuição;
- prazo para conclusão;
- status;
- prioridade;
- responsável pela atribuição.

### 9.1 Alteração de cargo

Quando o colaborador mudar de cargo:

- as atividades padrão do novo cargo serão adicionadas;
- os treinamentos obrigatórios dessas atividades serão atribuídos;
- treinamentos já realizados continuarão no histórico;
- atividades antigas poderão ser removidas pelo administrador;
- as qualificações serão recalculadas automaticamente.

## 10. Status dos treinamentos

Os principais status serão:

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

O status “vencendo em breve” deve considerar uma quantidade configurável de dias antes do vencimento. O padrão será 30 dias.

## 11. Validade e reciclagem

Cada treinamento poderá ter:

- validade em dias;
- validade em meses;
- validade indeterminada;
- necessidade de reciclagem.

A data de vencimento será calculada automaticamente:

```text
Data de conclusão + prazo de validade
```

Quando um treinamento vencer:

- ele deixa de atender aos requisitos das atividades;
- as atividades dependentes poderão ser bloqueadas;
- o colaborador deve receber uma nova atribuição de reciclagem;
- o histórico anterior deve ser preservado.

## 12. Versões dos treinamentos

Cada alteração relevante em um treinamento publicado deve gerar uma nova versão.

O sistema deve preservar:

- versão concluída pelo colaborador;
- conteúdo vigente no momento da conclusão;
- resultado do questionário;
- data de conclusão;
- validade.

Colaboradores que já iniciaram uma versão continuarão nela, salvo decisão administrativa de reiniciar o treinamento.

## 13. Qualificação para atividades

Uma atividade pode exigir um ou mais treinamentos.

Exemplo:

```text
Atividade: Operar ponte rolante

Requisitos:
- NR-11 válida
- Operação de Ponte Rolante válida
- Segurança Operacional válida
```

O sistema deve classificar a situação da atividade como:

- **Liberada:** todos os treinamentos obrigatórios estão concluídos e válidos;
- **Vencendo:** todos estão válidos, mas pelo menos um vencerá em breve;
- **Bloqueada:** falta treinamento, existe treinamento vencido ou existe reprovação pendente;
- **Não atribuída:** a atividade não faz parte das atribuições do colaborador.

O sistema informa uma qualificação baseada nos treinamentos registrados. Ele não substitui liberações médicas, operacionais ou legais externas.

## 14. QR Code do colaborador

Cada colaborador terá um QR Code individual.

O QR Code deve utilizar um token aleatório e não deve armazenar diretamente dados pessoais.

Exemplo:

```text
/verificar/{token-aleatorio}
```

Regras:

- um QR Code ativo por colaborador;
- possibilidade de revogação;
- possibilidade de geração de um novo código;
- token não sequencial e difícil de prever;
- registro de consultas para auditoria;
- consulta autenticada por administrador, gestor ou supervisor.

### 14.1 Dados exibidos após o escaneamento

A tela deve mostrar:

- nome do colaborador;
- matrícula;
- cargo;
- lista de todos os treinamentos realizados;
- lista específica das NRs realizadas;
- situação de validade de cada treinamento;
- data de conclusão;
- data de vencimento, quando existir;
- atividades que o colaborador pode executar;
- atividades bloqueadas e os treinamentos pendentes.

A tela não deve mostrar:

- CPF;
- endereço;
- telefone pessoal;
- informações médicas;
- dados desnecessários para validação.

## 15. Certificados

O sistema deve gerar certificado em PDF para treinamentos concluídos.

O certificado deve possuir:

- nome do colaborador;
- matrícula;
- nome do treinamento;
- versão;
- carga horária;
- data de conclusão;
- data de vencimento, quando existir;
- código de validação;
- identificação da empresa fictícia.

Também deve ser possível anexar certificados externos para treinamentos realizados fora da plataforma.

## 16. Dashboard do colaborador

A tela inicial do colaborador deve priorizar:

- continuar treinamento em andamento;
- treinamentos pendentes;
- treinamentos vencendo;
- treinamentos vencidos;
- treinamentos concluídos;
- atividades liberadas;
- atividades bloqueadas;
- certificados;
- QR Code pessoal.

## 17. Dashboard administrativo

O dashboard administrativo deve apresentar:

### 17.1 Indicadores gerais

- colaboradores ativos;
- treinamentos cadastrados;
- treinamentos atribuídos;
- treinamentos não iniciados;
- treinamentos em andamento;
- treinamentos concluídos;
- treinamentos reprovados;
- treinamentos vencidos;
- treinamentos vencendo em 30 dias;
- colaboradores com pendências;
- colaboradores com atividades bloqueadas.

### 17.2 Visão por treinamento

Para cada treinamento:

- total de colaboradores atribuídos;
- não iniciados;
- em andamento;
- aprovados;
- reprovados;
- concluídos;
- vencidos;
- taxa de conclusão;
- média das avaliações;
- tempo médio de conclusão.

### 17.3 Visão por atividade

Para cada atividade:

- cargos relacionados;
- treinamentos obrigatórios;
- colaboradores com atividade liberada;
- colaboradores com atividade vencendo;
- colaboradores com atividade bloqueada;
- principais treinamentos que causam bloqueio.

### 17.4 Visão por colaborador

Para cada colaborador:

- treinamentos obrigatórios;
- treinamentos opcionais;
- progresso atual;
- notas;
- conclusões;
- vencimentos;
- atividades liberadas;
- atividades bloqueadas;
- histórico completo.

### 17.5 Filtros

O dashboard deve permitir filtros por:

- unidade;
- setor;
- cargo;
- atividade;
- treinamento;
- situação;
- período.

## 18. Notificações

O MVP terá:

- notificações internas;
- envio de e-mails.

Eventos que geram notificações:

- novo treinamento atribuído;
- prazo de conclusão próximo;
- treinamento vencendo;
- treinamento vencido;
- reprovação em questionário;
- treinamento concluído;
- atividade bloqueada por falta de treinamento.

## 19. Auditoria

O sistema deve registrar ações relevantes:

- criação e alteração de treinamentos;
- alteração de versões;
- criação e alteração de atividades;
- vinculação de requisitos;
- atribuição de treinamentos;
- registro manual de conclusão;
- revogação de certificado;
- geração e revogação de QR Code;
- alteração de cargo;
- alteração de permissões;
- consulta por QR Code.

Cada registro deve possuir:

- usuário responsável;
- ação;
- entidade afetada;
- data e hora;
- dados essenciais da alteração.

## 20. Design e experiência

O sistema deve ter design:

- simples;
- limpo;
- intuitivo;
- responsivo;
- mobile-first;
- acessível;
- com poucos elementos por tela;
- com ações principais visíveis;
- com navegação consistente.

Regras de interface:

- utilizar texto e ícones para indicar status;
- não depender somente de cores;
- utilizar tabelas paginadas;
- utilizar filtros simples;
- priorizar ações frequentes;
- evitar animações desnecessárias;
- manter formulários curtos e organizados;
- garantir boa utilização em celular.

## 21. Performance e otimização

### 21.1 Frontend

- carregamento sob demanda de páginas;
- lazy loading de vídeos;
- lazy loading de imagens;
- paginação realizada pelo backend;
- evitar carregar listas completas;
- evitar bibliotecas desnecessárias;
- reduzir animações e efeitos pesados;
- cache de dados de consulta frequente;
- componentes reutilizáveis e simples.

### 21.2 Vídeos

Os vídeos não serão armazenados no banco de dados.

Devem ser armazenados em serviço de arquivos ou object storage.

O sistema deve:

- realizar streaming progressivo;
- carregar somente quando necessário;
- registrar o progresso sem atualizações excessivas;
- evitar download completo antes da reprodução;
- utilizar URLs protegidas quando necessário.

### 21.3 Backend

- consultas paginadas;
- índices adequados no banco;
- DTOs e projeções para consultas;
- evitar carregamento automático de relacionamentos desnecessários;
- cache para dados estáveis e indicadores;
- processamento assíncrono de e-mails e certificados;
- consultas específicas para dashboards;
- validação de entrada;
- logs estruturados.

### 21.4 Dashboard

Indicadores pesados não devem ser recalculados integralmente a cada acesso.

O sistema deve utilizar:

- consultas agregadas;
- cache temporário;
- atualização periódica quando necessário;
- filtros processados no backend.

## 22. Segurança

O sistema deve possuir:

- autenticação por e-mail e senha;
- controle de acesso por perfil;
- senhas armazenadas com hash seguro;
- recuperação de senha;
- validação de permissões no backend;
- proteção dos arquivos e vídeos;
- tokens de QR Code aleatórios;
- auditoria de ações administrativas;
- limitação de tentativas de login;
- validação e sanitização de entradas;
- proteção contra acesso indevido entre usuários.

## 23. Arquitetura do MVP

O sistema será desenvolvido como monólito modular.

Módulos principais:

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

O sistema será inicialmente utilizado por uma única organização, mas as entidades principais devem possuir estrutura que permita futura evolução para múltiplas empresas.

## 24. Tecnologias definidas

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
- Vite;
- interface responsiva e mobile-first.

### Infraestrutura

- Docker;
- Docker Compose;
- GitHub Actions;
- armazenamento de objetos para vídeos e certificados;
- MinIO no ambiente local.

## 25. Fora do escopo do MVP

Não fazem parte do MVP:

- aplicativo Android ou iOS;
- pagamentos;
- assinaturas;
- planos comerciais;
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

## 26. Critérios para conclusão do MVP

O MVP será considerado concluído quando for possível:

1. autenticar usuários com diferentes perfis;
2. cadastrar colaboradores, cargos, setores e unidades;
3. cadastrar atividades operacionais;
4. cadastrar treinamentos com vídeos e questionários;
5. vincular treinamentos obrigatórios às atividades;
6. vincular atividades padrão aos cargos;
7. atribuir atividades específicas a colaboradores;
8. atribuir treinamentos automaticamente e manualmente;
9. assistir aos vídeos e registrar o progresso;
10. exigir 80% de visualização de cada vídeo obrigatório;
11. responder questionários;
12. exigir no mínimo 70% de acertos;
13. concluir automaticamente um treinamento quando os critérios forem atendidos;
14. calcular validade e vencimento;
15. determinar atividades liberadas e bloqueadas;
16. gerar certificados em PDF;
17. gerar e revogar QR Codes;
18. consultar os dados do colaborador por QR Code autenticado;
19. visualizar treinamentos, NRs e atividades permitidas;
20. acompanhar indicadores no dashboard administrativo;
21. consultar o dashboard do colaborador;
22. enviar notificações internas e por e-mail;
23. registrar ações relevantes na auditoria;
24. executar o sistema com Docker Compose;
25. executar testes automatizados;
26. demonstrar o sistema com dados totalmente fictícios.

## 27. Regra de uso deste documento

Este arquivo é a fonte oficial de verdade do projeto work-training-system.

Toda implementação, issue, planejamento, decisão técnica ou solicitação para agentes de IA deve respeitar este documento.

Quando houver conflito entre uma implementação e este arquivo, este arquivo prevalece.

Qualquer mudança de escopo deve ser registrada primeiro neste documento antes de ser implementada.
