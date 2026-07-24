# Casos de Uso — work-training-system

**Versão:** 1.0  
**Data:** 23/07/2026  
**Status:** Casos de uso do MVP  
**Documento de referência:** `work-training-system-fonte-da-verdade.md`

## 1. Atores

### Administrador

Possui acesso completo às configurações, cadastros, atribuições, conclusões manuais, certificados, QR Codes, indicadores e auditoria.

### Gestor ou Supervisor

Acompanha colaboradores autorizados, pendências, qualificações e indicadores. Pode atribuir treinamentos quando possuir permissão.

### Colaborador

Realiza treinamentos, acompanha seu progresso, consulta qualificações, certificados e QR Code.

### Sistema

Executa cálculos, atribuições automáticas, atualizações de status, notificações, geração de certificados e recálculo de qualificações.

## 2. Casos de uso de acesso

### UC-001 — Autenticar usuário

**Ator principal:** Usuário  
**Pré-condição:** usuário ativo e cadastrado.

**Fluxo principal:**

1. O usuário informa e-mail e senha.
2. O sistema valida as credenciais.
3. O sistema identifica o perfil do usuário.
4. O sistema libera as funcionalidades autorizadas.

**Fluxos alternativos:**

- credenciais inválidas: negar acesso e registrar a tentativa;
- limite de tentativas atingido: aplicar a limitação configurada;
- usuário inativo: negar acesso.

**Pós-condição:** sessão autenticada e autorizada.

### UC-002 — Recuperar senha

**Ator principal:** Usuário

**Fluxo principal:**

1. O usuário solicita recuperação de senha.
2. O sistema valida a conta informada.
3. O sistema envia as instruções de recuperação.
4. O usuário define uma nova senha válida.
5. O sistema invalida o mecanismo de recuperação utilizado.

## 3. Casos de uso de colaboradores e organização

### UC-003 — Cadastrar colaborador

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador informa nome, matrícula, e-mail, cargo, setor, unidade e status.
2. O sistema valida os dados.
3. O sistema verifica a unicidade da matrícula.
4. O sistema cria o colaborador.
5. O sistema associa as atividades padrão do cargo.
6. O sistema gera as atribuições obrigatórias decorrentes dessas atividades.
7. O sistema registra a ação na auditoria.

**Fluxos alternativos:**

- matrícula duplicada: impedir o cadastro;
- cargo, setor ou unidade inválidos: impedir o cadastro.

### UC-004 — Editar colaborador

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona o colaborador.
2. O sistema apresenta os dados atuais.
3. O administrador altera os dados permitidos.
4. O sistema valida e salva as alterações.
5. Quando houver mudança de cargo, o sistema executa o UC-009.
6. O sistema registra a alteração na auditoria.

### UC-005 — Inativar colaborador

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador solicita a inativação.
2. O sistema altera o status para inativo.
3. O sistema preserva todo o histórico.
4. O sistema impede novas atribuições.
5. O administrador pode revogar o QR Code.
6. O sistema registra a ação na auditoria.

### UC-006 — Cadastrar estrutura organizacional

**Ator principal:** Administrador

**Abrange:** unidades, setores e cargos.

**Fluxo principal:**

1. O administrador informa os dados do cadastro.
2. O sistema valida as informações.
3. O sistema salva a entidade.
4. O sistema registra a ação na auditoria.

## 4. Casos de uso de atividades

### UC-007 — Cadastrar atividade operacional

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador informa nome, descrição e status.
2. O sistema valida e salva a atividade.
3. O sistema registra a criação na auditoria.

### UC-008 — Vincular atividade a cargo

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona um cargo.
2. O administrador seleciona uma atividade.
3. O sistema cria o vínculo como atividade padrão.
4. O sistema identifica colaboradores ativos no cargo.
5. O sistema atualiza as atividades atribuídas quando aplicável.
6. O sistema cria atribuições de treinamentos obrigatórios quando necessárias.
7. O sistema recalcula qualificações.
8. O sistema registra a ação na auditoria.

### UC-009 — Alterar cargo do colaborador

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona o novo cargo.
2. O sistema atualiza o cargo do colaborador.
3. O sistema adiciona as atividades padrão do novo cargo.
4. O sistema atribui os treinamentos obrigatórios dessas atividades.
5. O sistema preserva os treinamentos já realizados.
6. O sistema recalcula as qualificações.
7. O sistema registra a alteração na auditoria.

**Observação:** atividades antigas não são necessariamente removidas de forma automática; o administrador pode removê-las.

### UC-010 — Atribuir atividade específica ao colaborador

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona o colaborador.
2. O administrador seleciona a atividade.
3. O sistema cria a atribuição específica.
4. O sistema identifica os treinamentos obrigatórios.
5. O sistema cria as atribuições necessárias.
6. O sistema recalcula a qualificação.
7. O sistema registra a ação na auditoria.

### UC-011 — Remover atividade específica

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona a atividade atribuída.
2. O sistema remove ou inativa o vínculo específico.
3. O sistema preserva o histórico de treinamentos.
4. O sistema recalcula a situação da atividade.
5. O sistema registra a alteração na auditoria.

## 5. Casos de uso de treinamentos e conteúdo

### UC-012 — Cadastrar treinamento

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador informa nome, código, descrição, categoria, indicação de NR, carga horária, validade, nota mínima e status.
2. O sistema valida os dados.
3. O sistema cria o treinamento e sua versão inicial.
4. O sistema registra a criação na auditoria.

### UC-013 — Criar módulos e vídeos

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona uma versão de treinamento.
2. O administrador cria módulos ordenados.
3. O administrador adiciona vídeos aos módulos.
4. O administrador informa título, descrição, ordem, duração e referência do arquivo.
5. O sistema valida e salva o conteúdo.

### UC-014 — Criar questionário

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona um módulo.
2. O administrador cria um questionário opcional.
3. O administrador configura nota mínima, tentativas, intervalo e aleatoriedade.
4. O administrador adiciona questões e alternativas.
5. Para cada questão, define uma única resposta correta.
6. O sistema valida e salva o questionário.

### UC-015 — Publicar nova versão do treinamento

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador altera conteúdo ou regra relevante de um treinamento publicado.
2. O sistema exige a criação de uma nova versão.
3. O sistema preserva as versões anteriores.
4. O sistema publica a nova versão.
5. Colaboradores já iniciados permanecem na versão anterior, salvo decisão administrativa.
6. O sistema registra a publicação na auditoria.

### UC-016 — Vincular treinamento obrigatório à atividade

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona uma atividade.
2. O administrador seleciona um treinamento obrigatório.
3. O sistema cria o requisito.
4. O sistema identifica colaboradores com a atividade atribuída.
5. O sistema cria as atribuições necessárias.
6. O sistema recalcula qualificações.
7. O sistema registra a ação na auditoria.

## 6. Casos de uso de atribuição

### UC-017 — Atribuir treinamento manualmente

**Ator principal:** Administrador ou Gestor autorizado

**Fluxo principal:**

1. O ator seleciona o treinamento.
2. O ator seleciona a origem da atribuição: colaborador, cargo, atividade, setor, unidade ou grupo.
3. O ator define prazo, prioridade e demais configurações.
4. O sistema identifica os colaboradores elegíveis e ativos.
5. O sistema cria as atribuições.
6. O sistema envia as notificações correspondentes.
7. O sistema registra a ação na auditoria.

**Fluxo alternativo:**

- colaborador inativo: não criar atribuição.

### UC-018 — Atribuir treinamento automaticamente

**Ator principal:** Sistema

**Gatilhos:**

- cadastro de colaborador;
- alteração de cargo;
- atribuição de atividade;
- novo requisito de atividade;
- necessidade de reciclagem.

**Fluxo principal:**

1. O sistema identifica os treinamentos obrigatórios.
2. O sistema verifica o histórico e as atribuições existentes.
3. O sistema cria as atribuições necessárias.
4. O sistema atualiza os status relacionados.
5. O sistema notifica o colaborador.

## 7. Casos de uso de realização do treinamento

### UC-019 — Iniciar treinamento

**Ator principal:** Colaborador

**Pré-condições:**

- atribuição ativa;
- colaborador ativo;
- treinamento disponível.

**Fluxo principal:**

1. O colaborador acessa uma atribuição não iniciada.
2. O sistema apresenta a versão vinculada.
3. O sistema altera o status para em andamento.
4. O colaborador inicia o primeiro módulo.

### UC-020 — Assistir vídeo e registrar progresso

**Ator principal:** Colaborador

**Fluxo principal:**

1. O colaborador abre um vídeo.
2. O sistema carrega o vídeo sob demanda.
3. O sistema recupera o ponto salvo.
4. O colaborador assiste ao conteúdo.
5. O sistema registra posição e percentual periodicamente.
6. Ao atingir pelo menos 80%, o sistema considera o vídeo obrigatório concluído.
7. O sistema verifica se o treinamento pode avançar para avaliação ou conclusão.

### UC-021 — Continuar treinamento

**Ator principal:** Colaborador

**Fluxo principal:**

1. O colaborador acessa um treinamento em andamento.
2. O sistema identifica o último conteúdo acessado.
3. O sistema apresenta o ponto de continuidade.
4. O colaborador retoma o treinamento.

### UC-022 — Responder questionário

**Ator principal:** Colaborador

**Pré-condições:**

- questionário disponível;
- tentativa permitida;
- intervalo entre tentativas atendido.

**Fluxo principal:**

1. O sistema apresenta as questões.
2. O sistema pode aleatorizar a ordem quando configurado.
3. O colaborador seleciona uma resposta por questão.
4. O colaborador envia a tentativa.
5. O sistema calcula a nota.
6. O sistema registra respostas, data, nota e resultado.
7. O sistema informa aprovação ou reprovação.
8. O sistema verifica a conclusão do treinamento.

**Fluxos alternativos:**

- limite de tentativas atingido: impedir nova tentativa;
- intervalo não cumprido: informar quando uma nova tentativa estará disponível;
- nota abaixo da mínima: registrar reprovação.

### UC-023 — Concluir treinamento automaticamente

**Ator principal:** Sistema

**Fluxo principal:**

1. O sistema verifica todos os vídeos obrigatórios.
2. O sistema confirma progresso mínimo de 80% em cada vídeo.
3. Quando existir questionário, confirma nota mínima de 70% ou a nota configurada.
4. O sistema registra a conclusão.
5. O sistema calcula a data de vencimento.
6. O sistema gera ou disponibiliza o certificado.
7. O sistema recalcula as qualificações do colaborador.
8. O sistema envia notificações.
9. O sistema registra os eventos necessários.

### UC-024 — Registrar conclusão manual

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona colaborador e treinamento.
2. O administrador informa os dados da conclusão externa ou presencial.
3. O administrador pode anexar certificado externo.
4. O sistema registra a conclusão manual.
5. O sistema calcula a validade e o vencimento.
6. O sistema recalcula qualificações.
7. O sistema registra a ação na auditoria.

## 8. Casos de uso de validade e qualificação

### UC-025 — Atualizar status de vencimento

**Ator principal:** Sistema

**Fluxo principal:**

1. O sistema consulta conclusões com validade.
2. O sistema calcula a situação em relação à data atual.
3. Dentro da janela configurada, altera o status para vencendo em breve.
4. Após a data de vencimento, altera o status para vencido.
5. O sistema recalcula as atividades dependentes.
6. O sistema envia notificações.

### UC-026 — Gerar atribuição de reciclagem

**Ator principal:** Sistema ou Administrador

**Fluxo principal:**

1. Um treinamento vence ou se aproxima do vencimento conforme a regra configurada.
2. O sistema identifica a necessidade de reciclagem.
3. O sistema cria nova atribuição.
4. O sistema preserva a conclusão anterior.
5. O sistema notifica o colaborador.

### UC-027 — Calcular qualificação para atividade

**Ator principal:** Sistema

**Gatilhos:**

- conclusão ou reprovação;
- vencimento;
- nova atribuição;
- alteração de requisito;
- alteração de cargo;
- inclusão ou remoção de atividade.

**Fluxo principal:**

1. O sistema verifica se a atividade está atribuída.
2. O sistema lista os treinamentos obrigatórios.
3. O sistema consulta conclusões e validades.
4. O sistema classifica a atividade como liberada, vencendo, bloqueada ou não atribuída.
5. O sistema registra ou disponibiliza o motivo da situação.

## 9. Casos de uso de QR Code e certificados

### UC-028 — Gerar QR Code

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador solicita a geração do QR Code.
2. O sistema revoga o código ativo anterior, quando existir.
3. O sistema gera um token aleatório e não sequencial.
4. O sistema associa o token ao colaborador.
5. O sistema disponibiliza o QR Code.
6. O sistema registra a ação na auditoria.

### UC-029 — Revogar QR Code

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador seleciona o QR Code ativo.
2. O sistema altera o status para revogado.
3. O token deixa de permitir consulta.
4. O sistema registra a ação na auditoria.

### UC-030 — Consultar colaborador por QR Code

**Ator principal:** Administrador, Gestor ou Supervisor

**Pré-condições:**

- usuário autenticado;
- QR Code ativo.

**Fluxo principal:**

1. O ator escaneia o QR Code.
2. O sistema valida o token.
3. O sistema valida a permissão do usuário.
4. O sistema apresenta nome, matrícula, cargo, treinamentos, NRs, validades, conclusões, vencimentos e atividades.
5. O sistema informa atividades bloqueadas e treinamentos pendentes.
6. O sistema registra a consulta para auditoria.

**Fluxos alternativos:**

- token inválido ou revogado: negar consulta;
- usuário sem permissão: negar consulta.

### UC-031 — Gerar certificado

**Ator principal:** Sistema

**Pré-condição:** treinamento concluído.

**Fluxo principal:**

1. O sistema reúne dados da conclusão.
2. O sistema gera o PDF com código de validação.
3. O sistema armazena o arquivo no object storage.
4. O sistema associa o certificado à conclusão.
5. O certificado fica disponível ao colaborador e aos perfis autorizados.

## 10. Casos de uso de acompanhamento

### UC-032 — Consultar dashboard do colaborador

**Ator principal:** Colaborador

**Fluxo principal:**

1. O colaborador acessa a tela inicial.
2. O sistema apresenta treinamentos em andamento, pendentes, vencendo, vencidos e concluídos.
3. O sistema apresenta atividades liberadas e bloqueadas.
4. O sistema apresenta certificados e QR Code pessoal.

### UC-033 — Consultar dashboard administrativo

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador acessa o dashboard.
2. O sistema apresenta indicadores gerais.
3. O administrador aplica filtros.
4. O sistema apresenta visões por treinamento, atividade ou colaborador.
5. O sistema utiliza consultas agregadas e cache quando necessário.

### UC-034 — Acompanhar equipe

**Ator principal:** Gestor ou Supervisor

**Fluxo principal:**

1. O gestor acessa a visão de sua equipe ou setor.
2. O sistema restringe os dados ao escopo autorizado.
3. O sistema apresenta treinamentos, pendências, vencimentos e qualificações.
4. O gestor consulta detalhes dos colaboradores permitidos.

## 11. Casos de uso de notificações e auditoria

### UC-035 — Enviar notificação

**Ator principal:** Sistema

**Gatilhos:**

- treinamento atribuído;
- prazo próximo;
- vencimento próximo;
- treinamento vencido;
- reprovação;
- conclusão;
- atividade bloqueada.

**Fluxo principal:**

1. O sistema identifica o evento.
2. O sistema cria uma notificação interna.
3. Quando aplicável, agenda ou envia o e-mail.
4. O sistema registra o resultado do envio.

### UC-036 — Consultar auditoria

**Ator principal:** Administrador

**Fluxo principal:**

1. O administrador acessa o histórico de alterações.
2. O sistema apresenta usuário, ação, entidade, data, hora e dados essenciais.
3. O administrador aplica filtros para localizar eventos específicos.

## 12. Matriz resumida de acesso

| Caso de uso | Administrador | Gestor/Supervisor | Colaborador | Sistema |
|---|---:|---:|---:|---:|
| Autenticar e recuperar senha | Sim | Sim | Sim | Apoio |
| Manter colaboradores e organização | Sim | Não | Não | Não |
| Manter atividades e requisitos | Sim | Não | Não | Apoio |
| Manter treinamentos e conteúdo | Sim | Não | Não | Não |
| Atribuir treinamentos | Sim | Com permissão | Não | Automático |
| Realizar treinamentos | Não | Não | Sim | Apoio |
| Registrar conclusão manual | Sim | Não | Não | Não |
| Consultar equipe | Sim | Sim | Não | Não |
| Consultar dashboard pessoal | Não | Não | Sim | Não |
| Gerar e revogar QR Code | Sim | Não | Não | Geração técnica |
| Consultar por QR Code | Sim | Sim | Não | Validação |
| Calcular qualificações | Não | Não | Não | Sim |
| Atualizar vencimentos | Não | Não | Não | Sim |
| Consultar auditoria | Sim | Não | Não | Registro |
