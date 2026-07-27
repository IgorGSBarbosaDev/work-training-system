# Especificação de telas do MVP para Figma

Este documento lista as telas necessárias para transformar o frontend atual em uma aplicação utilizável. Ele pode ser usado como briefing para geração no Figma e como referência para implementação posterior via MCP.

## Diretrizes gerais para o Figma

- Criar uma aplicação web responsiva para desktop, tablet e mobile.
- Manter a linguagem visual atual do frontend: interface clara, operacional, calma e profissional; usar o branding atual apenas como referência visual, deixando cores e nomes fáceis de substituir.
- Criar componentes reutilizáveis para: sidebar, topbar, breadcrumb, tabs, cards, tabelas, badges de status, avatar, filtros, paginação, modal, drawer, toast, confirmação, upload, stepper, player de vídeo, questionário e empty states.
- Projetar sempre os estados `loading`, `empty`, `error`, `success`, `disabled`, `permission denied` e `offline` quando aplicável.
- Todas as tabelas devem prever busca, filtros, ordenação, paginação, seleção de linha e ações contextuais.
- Todas as ações destrutivas devem pedir confirmação e explicar o impacto: inativar, revogar, cancelar, arquivar, excluir rascunho ou remover vínculo.
- Usar textos em português no produto. Preservar nomes técnicos apenas em detalhes de API ou documentação.
- Não usar números ou pessoas hard-coded no design final. Os dados devem representar estados vindos da API e incluir exemplos fictícios realistas.
- Ocultar ações não permitidas pelo perfil. Quando uma informação não estiver disponível, usar estado vazio ou mascarado, nunca inventar permissão.

## Navegação e permissões

### Administrador

Acesso à visão geral, usuários, organização, colaboradores, cargos, atividades, treinamentos, atribuições, qualificações, certificados, QR Codes, notificações/e-mails, relatórios, auditoria e configurações.

### Gestor e supervisor

Acesso limitado ao escopo concedido: visão da equipe, colaboradores autorizados, atribuições, qualificações, certificados, QR Code autenticado, notificações e relatórios. Não podem administrar usuários, configurações globais ou auditoria completa.

### Colaborador

Acesso à sua dashboard, seus treinamentos/atribuições, execução, questionários, qualificações, certificados, QR Code próprio, notificações e perfil.

## Telas públicas e de acesso

### 1. Login

Elementos:

- e-mail;
- senha com mostrar/ocultar;
- botão “Entrar”;
- link “Esqueci minha senha”;
- opção de ambiente/demo apenas quando habilitada;
- identidade visual e mensagem de boas-vindas.

Estados e ações:

- validação de campos obrigatórios e formato de e-mail;
- loading durante autenticação;
- credenciais inválidas sem revelar se o e-mail existe;
- conta bloqueada/inativa;
- erro de rede;
- sucesso redirecionando conforme o perfil;
- renovar sessão quando o access token expirar.

API relacionada: `POST /auth/login`, `POST /auth/refresh`.

### 2. Recuperação de senha

Subtelas:

- solicitar recuperação com e-mail;
- confirmação neutra de envio;
- definir nova senha com token;
- sucesso e retorno ao login.

Elementos e regras:

- nova senha e confirmação;
- indicador da política de senha;
- expiração ou token inválido;
- não informar se a conta existe.

API relacionada: `POST /auth/password/forgot`, `POST /auth/password/reset`.

### 3. Verificação pública de certificado

Elementos:

- campo para código de validação;
- botão “Validar certificado”;
- identificação do treinamento, colaborador, data, validade, status e organização;
- selo visual de válido, revogado ou não encontrado;
- opção de imprimir/baixar quando permitido.

API relacionada: `GET /certificate-validations/{validationCode}`.

### 4. Tela pública de acesso negado/erro

Criar estados para `401`, `403`, `404`, sessão expirada, servidor indisponível e manutenção, com ação de voltar, tentar novamente ou entrar novamente.

## Shell autenticado

### 5. Layout principal responsivo

Elementos:

- sidebar com logo, organização atual e navegação por perfil;
- topbar com breadcrumb, busca opcional, notificações, avatar, nome/perfil e menu da conta;
- drawer da sidebar em mobile;
- seletor de organização/workspace se o usuário tiver mais de uma;
- breadcrumbs clicáveis;
- toast global de sucesso/erro;
- modal global de confirmação;
- estados de carregamento de página e sessão expirada.

Menu do perfil:

- meu perfil;
- configurações, quando permitido;
- trocar organização, quando aplicável;
- sair.

### 6. Central de notificações rápida

Abrir pelo ícone da topbar como popover/drawer com:

- contador de não lidas;
- lista resumida;
- tipo, título, data e status;
- marcar como lida;
- marcar todas como lidas;
- link para a central completa.

## Dashboard do colaborador

### 7. Dashboard pessoal

Objetivo: mostrar o que o colaborador precisa fazer agora.

Conteúdo:

- saudação e resumo do perfil;
- card “continuar treinamento” com próximo ponto de retomada;
- contadores de não iniciados, em andamento, concluídos, expirando e expirados;
- lista de atribuições prioritárias;
- prazos próximos;
- qualificações liberadas e bloqueadas;
- certificados recentes;
- alertas e notificações relevantes.

Ações:

- começar/retomar treinamento;
- abrir todas as atribuições;
- abrir qualificação;
- baixar certificado;
- abrir notificações.

API relacionada: `GET /me/dashboard`, `GET /me/training-assignments`, `GET /me/qualifications`, `GET /me/certificates`, `GET /me/notifications`.

### 8. Minhas atribuições

Elementos:

- tabs ou filtros: todas, não iniciadas, em andamento, aguardando avaliação, concluídas, expiradas e canceladas;
- cards ou tabela com treinamento, versão, prioridade, prazo, progresso, status e ação;
- busca por treinamento;
- ordenação por prazo, prioridade e status;
- empty state específico para cada filtro.

Ações:

- iniciar/retomar;
- ver detalhes;
- cancelar quando permitido;
- abrir histórico de tentativas e conclusão.

### 9. Detalhe da atribuição

Conteúdo:

- nome do treinamento e versão exata;
- origem da atribuição;
- prazo, prioridade, validade e status;
- progresso por módulo, vídeo e questionário;
- requisitos de conclusão;
- histórico de alterações;
- aviso de bloqueio, reprovação ou expiração.

Ações:

- iniciar/retomar;
- abrir conteúdo;
- iniciar questionário quando disponível;
- ver conclusão/certificado;
- voltar para minhas atribuições.

API relacionada: endpoints de atribuição, caminho de aprendizagem, progresso e tentativas.

### 10. Player de treinamento

Layout:

- vídeo principal com controles;
- título e descrição;
- módulos e itens em uma lateral ou drawer;
- progresso geral e progresso do vídeo;
- indicador obrigatório/opcional;
- duração e tempo assistido;
- bloqueio de itens futuros quando aplicável;
- botão anterior/próximo.

Comportamentos:

- registrar progresso automaticamente e por eventos;
- permitir retomada no último ponto válido;
- mostrar claramente o limite de 80% para vídeo obrigatório;
- não contar seek/abertura indevida como visualização;
- mostrar vídeo concluído somente ao atingir o limite;
- informar questionário liberado após os vídeos.

Estados:

- carregando URL protegida;
- vídeo indisponível;
- erro de reprodução;
- progresso sendo salvo;
- progresso salvo;
- conteúdo bloqueado;
- treinamento concluído.

### 11. Questionário

Elementos:

- nome e instruções;
- contador de questões;
- indicador de progresso;
- uma questão por tela ou lista navegável;
- alternativas de seleção única;
- botão anterior/próxima;
- resumo de questões respondidas;
- aviso de tentativa e limite de tentativas;
- botão “Enviar respostas”.

Estados e ações:

- questão sem resposta;
- tentativa incompleta;
- confirmação antes de enviar;
- enviando;
- aprovado com percentual, nota mínima e próxima etapa;
- reprovado com tentativas restantes e intervalo de nova tentativa;
- última tentativa sem aprovação;
- questionário indisponível.

Não exibir gabarito antes do envio. Após o envio, exibir apenas o resultado permitido pelo contrato.

### 12. Resultado de conclusão

Elementos:

- status concluído;
- nota e data;
- validade e data de vencimento;
- versão concluída;
- próximos treinamentos/requisitos;
- certificado disponível ou em geração.

Ações:

- baixar certificado;
- ver certificado;
- voltar para dashboard;
- iniciar reciclagem quando disponível.

### 13. Minhas qualificações

Exibir atividades com status `AVAILABLE`, `EXPIRING`, `BLOCKED` e `NOT_ASSIGNED`.

Cada item deve mostrar:

- atividade;
- status com cor e texto acessível;
- treinamentos exigidos;
- motivo estruturado do bloqueio;
- vencimento mais próximo;
- ação para abrir o treinamento responsável.

Filtros: status, atividade, unidade/setor/cargo quando o contrato permitir.

### 14. Meus certificados

Elementos:

- lista de certificados com treinamento, tipo, emissão, validade, status e código;
- filtro por status/tipo/período;
- detalhe do certificado;
- preview ou representação do PDF;
- botão baixar;
- código de validação copiável;
- estado revogado/expirado.

## Operação administrativa

### 15. Dashboard administrativo

Criar variações para administrador e visão de equipe para gestor/supervisor.

Indicadores:

- colaboradores ativos;
- treinamentos cadastrados/publicados;
- atribuições totais;
- não iniciadas, em andamento, concluídas e reprovadas;
- expirações próximas e vencidas;
- qualificações bloqueadas;
- progresso ao longo do período;
- distribuição por unidade, setor, cargo e treinamento.

Elementos:

- seletor de período;
- filtros por unidade, setor, cargo, treinamento e status;
- cards de KPI;
- gráficos;
- tabela de itens que exigem atenção;
- exportar/abrir relatório.

API relacionada: `/admin/dashboard/*`, `/team/dashboard`, `/reports/*`.

### 16. Relatório de status de treinamentos

Tabela com colaborador, treinamento, versão, atribuição, progresso, status, prazo, conclusão e vencimento.

Filtros:

- colaborador;
- treinamento;
- status;
- unidade/setor/cargo;
- período;
- prioridade.

Ações: abrir colaborador, abrir atribuição, registrar conclusão manual quando permitido e exportar.

### 17. Relatório de qualificações

Tabela e visão resumida por status. Mostrar atividade, colaborador, status, requisitos pendentes, motivo de bloqueio, data de cálculo e próximo vencimento.

### 18. Relatório de expirações e reciclagens

Tabs para vencendo, vencido, recalculado e reciclagens.

Ações:

- recalcular vencimentos;
- abrir conclusão;
- gerar reciclagem;
- acompanhar atribuição de reciclagem.

### 19. Colaboradores

Lista:

- busca por nome, matrícula e e-mail;
- filtros por status, unidade, setor e cargo;
- tabela com foto, nome, matrícula, cargo, setor, status e última atividade;
- paginação;
- botão novo colaborador;
- ações por linha.

Detalhe do colaborador:

- dados cadastrais e foto;
- status e cargo atual;
- unidade/setor;
- atribuições;
- qualificações;
- certificados;
- QR Code;
- histórico cadastral;
- histórico de treinamentos.

Formulário criar/editar:

- nome, matrícula, e-mail, telefone, unidade, setor, cargo e foto;
- validações de duplicidade;
- salvar, cancelar e inativar;
- confirmação de mudança de cargo com impacto em atividades e atribuições.

API relacionada: `/employees`, `/employees/{id}/history`, `/employees/{id}/job`, `/employees/{id}/status`.

### 20. Estrutura organizacional

Criar uma área com tabs ou submenus para:

- unidades;
- setores;
- cargos;
- configurações globais da organização.

Cada cadastro precisa de:

- lista com busca, status e paginação;
- criar/editar;
- ativar/inativar;
- confirmação de impacto;
- detalhes de relacionamentos.

Cargo deve incluir a tela de atividades padrão vinculadas. Setor deve mostrar a unidade pai. Configurações devem permitir nota mínima, percentual de vídeo, janelas de expiração e parâmetros definidos pela API.

### 21. Atividades operacionais

Lista:

- nome, código, status, quantidade de requisitos, cargos e colaboradores relacionados;
- filtros e busca;
- criar, editar, ativar/inativar.

Detalhe:

- descrição e dados regulatórios;
- treinamentos obrigatórios;
- versão/política do requisito;
- cargos com vínculo;
- colaboradores com atribuição específica;
- colaboradores qualificados, disponíveis e bloqueados.

Ações:

- adicionar/remover requisito;
- definir `FIXED_VERSION` ou `LATEST_PUBLISHED`;
- vincular ao cargo;
- atribuir diretamente a colaborador;
- consultar quem está qualificado.

### 22. Treinamentos

Lista:

- nome, código, status, versão publicada, validade, quantidade de módulos e última atualização;
- filtros por status e código;
- criar treinamento;
- abrir detalhe.

Detalhe do treinamento:

- informações gerais;
- lista de versões;
- versão publicada destacada;
- módulos, vídeos e questionários;
- requisitos que usam o treinamento;
- atribuições e conclusões relacionadas.

Formulário:

- nome, código, descrição, norma/regulamentação;
- criar versão inicial;
- status e validade.

### 23. Editor de versão de treinamento

Usar um layout com stepper ou abas:

1. informações e regras de validade;
2. módulos;
3. vídeos e arquivos;
4. questionários;
5. revisão e publicação.

Funcionalidades:

- criar/duplicar versão;
- editar versão em rascunho;
- ordenar módulos e conteúdos;
- configurar nota mínima e máximo de tentativas;
- definir validade em dias, meses ou indeterminada;
- salvar rascunho;
- visualizar resumo de conteúdo;
- validar pendências;
- publicar;
- arquivar versão;
- excluir conteúdo de rascunho.

### 24. Editor de módulo, vídeo e arquivo

Módulo:

- título, descrição, ordem, ativo/inativo;
- adicionar e reordenar vídeos/questionários.

Vídeo:

- título, descrição, duração, obrigatório/opcional;
- solicitar upload;
- acompanhar upload, checksum e verificação;
- visualizar arquivo concluído;
- substituir/remover em rascunho.

Estados de upload: solicitado, enviando, verificando, concluído, falho, expirado e cancelado.

### 25. Editor de questionário

Elementos:

- título, instruções, nota mínima, máximo de tentativas e intervalo;
- lista de questões com ordem;
- adicionar/editar/remover questão;
- alternativas;
- marcar resposta correta somente para o autor/admin;
- reordenar;
- visualizar como colaborador;
- validar que há uma resposta correta ativa;
- salvar e publicar junto com a versão.

### 26. Atribuições de treinamentos

Lista:

- colaborador, treinamento, versão, origem, prioridade, status, prazo, progresso e conclusão;
- filtros por status, período, colaborador, treinamento e origem;
- seleção múltipla;
- cancelamento e reciclagem quando permitido.

Nova atribuição:

- selecionar colaborador(es), treinamento e versão/política;
- definir prazo, prioridade e observação;
- revisar impacto;
- confirmar;
- mostrar resultado por item em atribuição em lote.

Detalhe:

- timeline de status;
- fontes da atribuição;
- progresso por conteúdo;
- tentativas;
- conclusão;
- histórico de alterações.

### 27. Conclusão manual

Modal/tela administrativa para registrar evidência externa:

- colaborador;
- treinamento e versão publicada/arquivada;
- data de conclusão;
- score/nota;
- observação;
- upload de certificado externo;
- revisão antes de confirmar;
- aviso de que a conclusão manual não altera atribuições antigas.

## Identidade, certificados e QR

### 28. Usuários e permissões

Lista e detalhe com:

- e-mail, nome, perfil, status, colaborador vinculado e último acesso;
- criar usuário;
- ativar/inativar;
- redefinir senha;
- alterar papel;
- permissões adicionais;
- grants por unidade, setor ou colaborador;
- histórico de ações.

### 29. QR Code do colaborador

Na tela do colaborador e em uma área administrativa específica:

- gerar QR Code;
- exibir imagem;
- baixar/imprimir;
- copiar token/link quando permitido;
- status ativo/revogado;
- data de geração;
- revogar com motivo;
- histórico de acessos para administrador.

### 30. Verificação por QR Code

Fluxo autenticado para gestor/supervisor/admin:

- câmera/leitor ou campo para token;
- carregamento e validação;
- identificação do colaborador;
- treinamentos concluídos e validade;
- normas/regulamentações;
- atividades liberadas/bloqueadas e motivos;
- data/hora da consulta;
- resultado inválido, revogado, sem acesso ou não encontrado.

### 31. Certificados administrativos

Além da lista do colaborador, incluir:

- lista geral com filtros;
- detalhe e histórico;
- geração/regeneração de PDF;
- estado de job de geração;
- download protegido;
- revogação com motivo;
- validação por código;
- certificado externo vinculado à conclusão.

### 32. Notificações e entregas de e-mail

Central do usuário:

- todas, não lidas e arquivadas;
- filtros por tipo e período;
- marcar como lida;
- arquivar;
- abrir entidade relacionada.

Administração de e-mail:

- lista de entregas;
- destinatário, assunto, status, tentativas, último erro e datas;
- filtros por status;
- retry manual;
- detalhe do erro;
- estados enviado, pendente, falho e cancelado.

### 33. Auditoria

Lista de eventos:

- data/hora;
- ator;
- ação;
- entidade;
- ID da entidade;
- request ID;
- IP quando disponível;
- detalhes estruturados.

Filtros: período, ator, ação, entidade, usuário e request ID. Criar detalhe expansível e exportação quando suportada pelo produto.

## Configurações e perfil

### 34. Meu perfil

- nome/e-mail do usuário;
- papel e organização;
- colaborador vinculado;
- alterar senha;
- preferências de notificação;
- sessões/dispositivos quando suportado;
- sair.

### 35. Configurações da organização

Somente administrador:

- nome e dados da organização;
- nota mínima padrão;
- percentual mínimo de vídeo;
- regras de validade;
- janelas de expiração;
- configurações de e-mail;
- storage e integrações somente se expostas pela aplicação.

## Componentes e estados que precisam ser desenhados

Criar variantes reutilizáveis no Figma para:

- botão primário, secundário, destrutivo, loading e disabled;
- input, select, combobox, date picker, textarea, upload e password field;
- tabela desktop e cards mobile;
- paginação;
- tabs;
- badge para cada status de atribuição, qualificação, certificado, QR e notificação;
- progress bar, progress ring e checklist;
- modal de confirmação;
- drawer de detalhes;
- toast de sucesso, alerta e erro;
- skeleton loading;
- empty state com ação;
- erro de API;
- acesso negado;
- tooltip e help text;
- player de vídeo;
- questionário;
- timeline;
- gráfico de KPI, linha, barra e distribuição;
- avatar/foto e fallback de iniciais.

## Fluxos prioritários para prototipar no Figma

### Fluxo A — colaborador concluindo treinamento

Login → Dashboard pessoal → Minhas atribuições → Detalhe da atribuição → Player → 80% de vídeo → Questionário → Resultado aprovado → Certificado.

### Fluxo B — administrador criando e atribuindo treinamento

Login admin → Treinamentos → Novo treinamento → Editor de versão → Upload de vídeo → Criar questionário → Revisar/publicar → Atribuições → Nova atribuição → Resultado do lote.

### Fluxo C — gestor verificando autorização

Login gestor → QR Code → Ler token → Perfil do colaborador → Treinamentos concluídos/validade → Atividades liberadas ou bloqueadas.

### Fluxo D — administrador acompanhando risco

Login admin → Dashboard → Relatório de expirações → Abrir colaborador → Qualificações bloqueadas → Gerar reciclagem → Notificação de atribuição.

### Fluxo E — recuperação e segurança

Login → Esqueci minha senha → E-mail enviado → Nova senha → Login → Sessão expirada → Renovar ou entrar novamente.

## Critério para considerar as telas prontas

Uma tela só deve ser considerada pronta para implementação quando possuir:

- versão desktop e mobile;
- acesso e ações por perfil;
- ligação explícita com endpoint ou fluxo de API;
- estados de carregamento, vazio, erro, sucesso e sem permissão;
- validações e mensagens de negócio;
- confirmação para ações irreversíveis;
- componentes nomeados e reutilizáveis;
- protótipo navegável nos fluxos prioritários;
- dados fictícios consistentes com o domínio do MVP.

