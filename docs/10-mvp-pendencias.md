# Pendências do MVP

**Data:** 29/08/2026
**Status:** aceite local completo verde; workflow oficial e endurecimento organizacional amplo ainda pendentes.

Este documento registra o que ainda precisa ser executado ou aprofundado antes de declarar o MVP oficialmente concluído.

## Pendências de aceite e operação

Correções preparatórias concluídas neste ciclo: leitura correta da versão do Docker, criação antecipada do diretório
de evidências, coleta tolerante de logs, lint como gate e retenção de 14/30 dias. Falta comprovar o resultado no
workflow oficial.

- [ ] Executar o workflow oficial de CI com Java 21, Node.js 22 e Docker.
- [ ] Confirmar a matriz de compatibilidade com Java 25 e publicar os artefatos do aceite.
- [ ] Executar `verify`, `npm ci`, testes, lint, build, seed, smoke e Playwright no ambiente oficial do CI.
- [x] Validar no navegador o upload e a reprodução de um arquivo de vídeo real.
- [x] Exercitar o retry de e-mail com uma falha SMTP controlada (`FAILED -> PENDING -> SENT`).
- [ ] Atualizar a matriz de aceite com os links ou identificadores dos artefatos gerados pelo workflow e registrar a aprovação formal.

## Pendências de produto dentro do MVP

- [x] Implementar consultas específicas e paginadas para dashboards por treinamento, atividade e colaborador.
- [x] Completar filtros e ações próprias nas telas de usuários, certificados, notificações, e-mails e auditoria; proteger o refresh concorrente.
- [x] Confirmar no navegador o fluxo completo do colaborador: atribuição, vídeo real, questionário, conclusão e certificado.
- [ ] Conferir que os eventos relevantes geram auditoria, notificações internas e e-mail conforme a fonte da verdade.
- [ ] Consolidar em um único artefato as contagens da massa para pendente, em andamento, concluído, reprovado, vencido, vencendo e bloqueado.

## Revisões antes de produção

- [ ] Remover o uso da organização padrão dos fluxos autenticados restantes (atividades, atribuições, avaliações,
  colaboradores, arquivos, cargos, organização, progresso, qualificações e treinamentos) e executar a matriz completa
  de isolamento com uma segunda organização. Reporting, usuários, certificados, notificações, auditoria e QR já usam
  a organização do ator ou evento.
- [ ] Reavaliar permissões, escopos e políticas de segurança com dados reais de implantação.
- [ ] Definir a política operacional para armazenamento, retenção e rotação dos artefatos de aceite.

## Referências

- [Diagnóstico das lacunas do MVP](07-mvp-gap-analysis.md)
- [Aceite técnico](09-technical-acceptance.md)
- [Contrato da API](05-api-contract.md)
- [Fonte da verdade do MVP](../work-training-system-fonte-da-verdade.md)
