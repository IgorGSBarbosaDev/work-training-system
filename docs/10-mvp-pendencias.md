# Pendências do MVP

**Data:** 29/08/2026
**Status:** implementação principal deste ciclo concluída; aceite formal ainda pendente.

Este documento registra o que ainda precisa ser executado ou aprofundado antes de declarar o MVP oficialmente concluído.

## Pendências de aceite e operação

- [ ] Executar o workflow oficial de CI com Java 21, Node.js 22 e Docker.
- [ ] Confirmar a matriz de compatibilidade com Java 25 e publicar os artefatos do aceite.
- [ ] Executar `verify`, `npm ci`, testes, lint, build, seed, smoke e Playwright no ambiente oficial do CI.
- [ ] Validar no navegador o upload e a reprodução de um arquivo de vídeo real.
- [ ] Exercitar o retry de e-mail com uma falha SMTP controlada. O Mailpit saudável foi validado, mas não produz uma entrega `FAILED` espontaneamente.
- [ ] Atualizar a matriz de aceite com os links ou identificadores dos artefatos gerados pelo workflow e registrar a aprovação formal.

## Pendências de produto dentro do MVP

- [ ] Implementar consultas específicas para os dashboards por treinamento, atividade e colaborador; hoje alguns endpoints ainda reutilizam o overview geral.
- [ ] Completar filtros, paginação, estados vazios, expiração de sessão e mensagens de domínio nas telas administrativas menos exercitadas.
- [ ] Confirmar no navegador o fluxo completo do colaborador: atribuição, retomada, vídeo, questionário, conclusão e certificado.
- [ ] Conferir que os eventos relevantes geram auditoria, notificações internas e e-mail conforme a fonte da verdade.
- [ ] Ampliar a massa de demonstração para cobrir pendente, em andamento, concluído, reprovado, vencido, vencendo e bloqueado.

## Revisões antes de produção

- [ ] Revisar o uso da organização padrão nos módulos recentes antes de uma operação multi-organização.
- [ ] Reavaliar permissões, escopos e políticas de segurança com dados reais de implantação.
- [ ] Definir a política operacional para armazenamento, retenção e rotação dos artefatos de aceite.

## Referências

- [Diagnóstico das lacunas do MVP](07-mvp-gap-analysis.md)
- [Aceite técnico](09-technical-acceptance.md)
- [Contrato da API](05-api-contract.md)
- [Fonte da verdade do MVP](../work-training-system-fonte-da-verdade.md)
