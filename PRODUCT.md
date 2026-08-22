# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

Spring Boot com Java 21 e Maven no backend; React, TypeScript e Vite no frontend; PostgreSQL, Flyway, Spring Security/JWT e armazenamento de objetos fazem parte da arquitetura documentada e da implementação atual.

## Users

O produto atende igualmente três públicos, com experiências e permissões adequadas a cada contexto:

- **Administrador:** configura usuários, colaboradores, estrutura organizacional, atividades, treinamentos, atribuições, certificados, indicadores e auditoria.
- **Gestor ou supervisor:** acompanha sua equipe ou escopo autorizado, pendências, qualificações, certificados, consultas por QR Code e relatórios.
- **Colaborador:** realiza treinamentos atribuídos, acompanha progresso, resultados, vencimentos, qualificações, certificados e seu QR Code.

## Product Purpose

O **worksafe training system** é uma plataforma corporativa para administrar, realizar e validar treinamentos de colaboradores. Centraliza conteúdos, avaliações, progresso, conclusões, vencimentos, reciclagens, certificados e qualificações operacionais. O sucesso do produto é permitir que cada público encontre rapidamente o que precisa para configurar, acompanhar ou executar treinamentos e tomar decisões confiáveis sobre a liberação de atividades.

## Positioning

O produto combina gestão de treinamentos com cálculo automático da qualificação operacional, considerando requisitos obrigatórios, validade e situação de conclusão, e permite consultar essa situação por QR Code autenticado. Essa combinação conecta o aprendizado registrado à decisão operacional de liberar ou bloquear atividades.

## Operating Context

É utilizado em ambientes corporativos por administradores, gestores, supervisores e colaboradores, em computadores e celulares. Os fluxos incluem manutenção da estrutura organizacional, cadastro e versionamento de treinamentos, atribuição automática ou manual, realização de vídeos e questionários, acompanhamento de pendências e vencimentos, consulta de qualificações, emissão de certificados, notificações, relatórios e auditoria.

## Capabilities and Constraints

- Autenticação por e-mail e senha, recuperação de senha e autorização por perfil.
- Cadastro de colaboradores com matrícula única, cargo, setor, unidade, status e histórico preservado após inativação.
- Cadastro de cargos, atividades e requisitos de treinamentos obrigatórios.
- Treinamentos com versões, módulos, vídeos, questionários de múltipla escolha, nota mínima, tentativas e validade.
- Atribuições por colaborador, cargo, atividade, setor, unidade ou grupo, com atribuição automática quando regras do domínio exigirem.
- Registro de progresso, conclusão, vencimento, reciclagem e conclusões manuais.
- Qualificações classificadas como liberada, vencendo, bloqueada ou não atribuída.
- Certificados, QR Code individual autenticado, notificações, relatórios e trilha de auditoria.
- Interface responsiva e mobile-first, com textos em português.
- Ações e informações devem respeitar o perfil e o escopo autorizado; o frontend não substitui as regras de segurança do backend.
- Estados de loading, vazio, erro, sucesso, desabilitado, permissão negada e offline devem ser considerados quando aplicáveis.
- Status não podem depender apenas de cor; devem usar texto e/ou ícones.

## Brand Commitments

- Nome do produto: **worksafe training system**.
- A interface deve servir aos três públicos prioritários sem criar uma experiência secundária ou negligenciar nenhum deles.
- A linguagem do produto deve permanecer profissional, clara e operacional, conforme a documentação existente; decisões visuais detalhadas ficam para o registro de design e os briefings de superfície.

## Evidence on Hand

- `docs/01-prd-mvp.md`: visão, usuários, escopo funcional e requisitos não funcionais.
- `docs/02-regras-de-negocio.md`: regras de domínio e estados operacionais.
- `docs/03-modelo-de-dados.md`: entidades e relações do produto.
- `docs/04-casos-de-uso.md`: fluxos por ator.
- `docs/05-api-contract.md`: contratos de integração.
- `docs/08-screen-specification-for-figma.md`: inventário de telas, navegação, permissões e estados esperados.
- `frontend/src/`: aplicação React/Vite atual, incluindo navegação por perfil, autenticação, dashboards e telas operacionais.
- Não há depoimentos, métricas comerciais ou provas externas confirmadas; trabalhos futuros não devem fabricá-los.

## Product Principles

- **Decisão operacional confiável:** qualificações devem refletir requisitos, conclusão e validade reais.
- **Clareza por perfil:** cada pessoa deve ver ações, informações e pendências compatíveis com sua responsabilidade.
- **Rastreabilidade:** alterações, conclusões, versões, certificados e consultas relevantes devem preservar histórico verificável.
- **Continuidade do trabalho:** o colaborador deve conseguir retomar treinamentos e o gestor deve acompanhar a situação sem depender de planilhas paralelas.
- **Acesso responsável:** segurança, escopo autorizado, acessibilidade e uso em diferentes tamanhos de tela são requisitos do produto.

## Accessibility & Inclusion

O produto deve ser responsivo e utilizável em computadores e celulares, com acessibilidade como requisito funcional. Foco visível, navegação consistente, textos em português e representação de estados por texto e ícones devem ser preservados. Ações não permitidas devem ser ocultadas ou apresentadas com estado de permissão negada, sem inventar dados ou permissões.
