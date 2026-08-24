# Dados simulados para demonstração

O script `scripts/seed-simulated-demo.mjs` cria uma massa de demonstração usando a API pública do backend. Ele não acessa o PostgreSQL diretamente: uploads, usuários, vínculos, assignments, progresso, avaliações, conclusões, certificados, notificações e auditoria passam pelas mesmas regras usadas pela aplicação.

## Execução

Com o backend, PostgreSQL, MinIO e Mailpit em execução:

```powershell
node scripts/seed-simulated-demo.mjs
```

Variáveis opcionais:

```powershell
$env:SIMULATED_SEED = '20260824'
$env:SIMULATED_PASSWORD = 'ChangeMe-Simulated-2026!'
node scripts/seed-simulated-demo.mjs
```

O seed cria três unidades, seis setores, seis cargos, doze colaboradores, contas de `ADMIN`, `MANAGER`, `SUPERVISOR` e `EMPLOYEE`, três treinamentos publicados com vídeo/questionário, três atividades com requisitos e vínculos propagados aos colaboradores atuais, assignments em estados `NOT_STARTED`, `IN_PROGRESS`, `COMPLETED`, `FAILED`, `CANCELED` e `WAIVED`, qualificações `AVAILABLE`, `EXPIRING` e `BLOCKED`, além de conclusões válida, expirando e expirada.

As senhas não são gravadas nos artefatos. O script informa a variável usada ao final (`SIMULATED_PASSWORD`, ou o valor padrão da execução), e o Mailpit é necessário porque a criação da conta envia o fluxo real de ativação/reset de senha.

O arquivo `acceptance-artifacts/simulated-demo-state.json` permite consultar IDs e credenciais da fixture. Uma segunda execução com esse artefato apenas informa que a massa já foi criada; se o banco possuir a fixture sem o artefato, o script interrompe para evitar duplicação acidental. Use outro `SIMULATED_SEED` para gerar outro conjunto.

O vídeo enviado é uma fixture textual com `contentType` de vídeo, suficiente para validar upload, armazenamento protegido e progresso da API. A reprodução de um codec de vídeo real continua sendo uma verificação específica de browser/infraestrutura, não uma garantia do seed.
