---
name: worksafe training system
description: Sistema corporativo de treinamentos, qualificações operacionais e segurança de execução.
colors:
  background: "#f5f7f7"
  foreground: "#172529"
  card: "#ffffff"
  muted: "#eef2f3"
  muted-foreground: "#607078"
  border: "#d9e1e3"
  primary: "#0f6973"
  primary-foreground: "#ffffff"
  primary-deep: "#0b5962"
  sidebar: "#1b2c31"
  sidebar-accent: "#294449"
  sidebar-border: "#334a50"
  destructive: "#a94236"
  destructive-deep: "#8f352c"
  success: "#2f7654"
  success-wash: "#edf7f0"
  success-line: "#b7d8c8"
  warning: "#9a6814"
  warning-wash: "#fff8e7"
  warning-line: "#e8d29f"
  skeleton: "#e6ecee"
typography:
  display:
    fontFamily: "Roboto Condensed, Arial Narrow, Inter, sans-serif"
    fontSize: "1.875rem"
    fontWeight: 700
    lineHeight: 1
    letterSpacing: "-0.025em"
  body:
    fontFamily: "Inter, Segoe UI, Arial, sans-serif"
    fontSize: "0.875rem"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "DM Mono, Consolas, monospace"
    fontSize: "0.625rem"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "0.14em"
rounded:
  sm: "0.25rem"
  md: "0.375rem"
spacing:
  2: "0.5rem"
  3: "0.75rem"
  4: "1rem"
  5: "1.25rem"
  6: "1.5rem"
  8: "2rem"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "{colors.primary-foreground}"
    typography: "600 0.875rem Inter, Segoe UI, Arial, sans-serif"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"
    height: "2.5rem"
  button-outline:
    backgroundColor: "{colors.card}"
    textColor: "{colors.foreground}"
    typography: "600 0.875rem Inter, Segoe UI, Arial, sans-serif"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"
    height: "2.5rem"
  button-danger:
    backgroundColor: "{colors.destructive}"
    textColor: "{colors.primary-foreground}"
    typography: "600 0.875rem Inter, Segoe UI, Arial, sans-serif"
    rounded: "{rounded.md}"
    padding: "0.5rem 1rem"
    height: "2.5rem"
  panel:
    backgroundColor: "{colors.card}"
    rounded: "{rounded.md}"
    padding: "1rem"
  input:
    backgroundColor: "{colors.card}"
    textColor: "{colors.foreground}"
    rounded: "{rounded.md}"
    padding: "0.75rem"
    height: "2.75rem"
  nav-item-active:
    backgroundColor: "{colors.sidebar-accent}"
    textColor: "{colors.primary-foreground}"
    rounded: "{rounded.md}"
    padding: "0.625rem 0.75rem"
---

# Design System: worksafe training system

## Overview

**Creative North Star: "Manual de campo digital"**

O sistema visual traduz a ideia de um manual de campo que precisa ser consultado com rapidez e confiança. A interface é precisa, calma e operacional: a hierarquia informa o próximo passo, os estados tornam a situação legível e os controles têm aparência confiável sem criar ruído decorativo.

O mundo atual combina superfícies claras e frias com uma navegação lateral em teal-azul profundo. Títulos condensados ajudam na orientação rápida; corpo neutro mantém a leitura confortável; labels monoespaçados sinalizam dados, status e linguagem operacional. A composição evita o dashboard SaaS genérico, excesso de ornamentação e dependência de cor isolada.

**Key Characteristics:**
- Scanabilidade operacional
- Navegação orientada por perfil
- Superfícies tonais e bordas discretas
- Status expressos por texto, ícone e cor
- Contraste calmo entre área de trabalho e trilho de navegação

## Colors

A paleta é industrial e contida: um teal profundo conduz ações, uma base de papel frio sustenta longas sessões e cores semânticas reservadas tornam pendências, bloqueios e conclusões fáceis de localizar.

### Primary
- **Teal industrial profundo** (`{colors.primary}`): ação principal, links, foco de navegação, títulos de seção e elementos que orientam a próxima decisão.
- **Teal industrial profundo escurecido** (`{colors.primary-deep}`): resposta de hover para ações primárias, mantendo continuidade sem trocar de linguagem.

### Neutral
- **Papel frio** (`{colors.background}`): canvas global da aplicação.
- **Carvão teal** (`{colors.foreground}`): texto principal e informação de alta prioridade.
- **Branco de superfície** (`{colors.card}`): cards, painéis, campos e menus sobre o canvas.
- **Cinza névoa** (`{colors.muted}`): áreas de apoio, estados neutros e hovers discretos.
- **Cinza de orientação** (`{colors.muted-foreground}`): texto auxiliar, descrições e metadados.
- **Linha fria** (`{colors.border}`): divisores, contornos e limites de componentes.
- **Trilho profundo** (`{colors.sidebar}`): navegação persistente e contraste estrutural.
- **Acento do trilho** (`{colors.sidebar-accent}`): item ativo e superfícies internas da navegação.
- **Linha do trilho** (`{colors.sidebar-border}`): separação entre áreas da navegação.

### Tertiary
- **Verde de aprovação** (`{colors.success}`) com lavagem suave (`{colors.success-wash}`) e linha clara (`{colors.success-line}`): concluído, válido, ativo e liberado.
- **Âmbar de atenção** (`{colors.warning}`) com lavagem suave (`{colors.warning-wash}`) e linha clara (`{colors.warning-line}`): vencendo, pendente, em andamento e processamento.
- **Vermelho tijolo de bloqueio** (`{colors.destructive}`) com resposta escurecida (`{colors.destructive-deep}`): erro, vencido, reprovado, revogado e ação destrutiva.

### Named Rules
**The Status-as-Signal Rule.** Toda cor semântica deve aparecer acompanhada de texto e/ou ícone; cor sozinha nunca é a única fonte de significado.

## Typography

**Display Font:** Roboto Condensed (com Arial Narrow, Inter e sans-serif como fallback)
**Body Font:** Inter (com Segoe UI e Arial como fallback)
**Label/Mono Font:** DM Mono (com Consolas e monospace como fallback)

**Character:** A combinação é compacta e funcional. O display condensado dá ao produto uma assinatura de manual técnico e comprime títulos sem torná-los agressivos; o corpo neutro reduz fadiga; a mono espaçada cria uma camada de leitura para códigos, estados e orientação operacional.

### Hierarchy
- **Display** (700, 1.875rem; 2.25rem em telas maiores, line-height 1): títulos de página, métricas e títulos de estados.
- **Title** (700, 1.25rem a 1.5rem, line-height ajustada ao conteúdo): títulos de cards, menus e blocos de decisão.
- **Body** (400, 0.875rem, line-height 1.5 a 1.7): instruções, descrições, tabelas e conteúdo de trabalho.
- **Label** (600, 0.625rem, line-height 1, tracking 0.14em, caixa alta quando usado como eyebrow): navegação auxiliar, categorias e marcadores de status.

### Named Rules
**The Field-Note Hierarchy Rule.** Use títulos condensados para localizar a seção, texto neutro para explicar a tarefa e mono espaçada apenas quando a informação tiver caráter de código, status ou índice.

## Layout

O shell de operação usa uma coluna lateral persistente de 16rem no desktop, com estado compacto de 4rem, e uma barra superior fixa de 4rem. O conteúdo fica em um container central de até 90rem, com respiro de 1rem no mobile, 1.5rem em telas médias e 2rem no desktop. Páginas de operação usam cabeçalho com eyebrow, título, descrição e ação alinhada; grades de métricas e listas crescem conforme a largura disponível.

Em telas menores, a lateral vira drawer e a navegação principal aparece como barra inferior fixa de 4rem, limitada aos primeiros destinos do perfil. O conteúdo reserva espaço inferior para não ficar oculto pela barra. A hierarquia permanece a mesma entre administrador, gestor/supervisor e colaborador; o que muda é o escopo e o conjunto de destinos autorizados.

## Elevation & Depth

O sistema usa camadas tonais com bordas discretas e sombra ambiente suave. Painéis são planos e legíveis em repouso; a sombra indica separação estrutural sem transformar a interface em uma pilha de cartões flutuantes. Menus, drawers e popovers podem receber elevação mais perceptível porque se sobrepõem ao fluxo principal.

### Shadow Vocabulary
- **Painel ambiente** (`0 14px 35px rgb(23 37 41 / 8%)`): separação suave de cards, estados e áreas de trabalho.
- **Sobreposição estrutural** (`shadow-xl` do sistema Tailwind): menus de notificações, perfil e drawers que precisam se destacar do conteúdo adjacente.

### Named Rules
**The Calm Layer Rule.** A profundidade deve explicar a relação entre superfícies; não deve competir com o conteúdo nem transformar cada componente em um objeto flutuante.

## Shapes

A forma é retangular com cantos moderadamente suaves: o padrão `rounded-md` equivale a 0.375rem e aparece em botões, campos, cards, badges e itens de navegação. Contornos de 1px são parte importante da linguagem e mantêm a interface organizada sem depender de sombras fortes. Ícones vivem em pequenos contêineres quadrados ou alinhados diretamente ao texto; não há ornamento geométrico gratuito.

Campos e ações mantêm silhuetas compactas, com altura mínima de 2.5rem para controles comuns e 2.75rem para campos de autenticação. O foco usa um anel teal visível de 3px com deslocamento de 2px, preservando navegação por teclado.

## Components

Os componentes são precisos e confiáveis: cada um comunica sua função, seu estado e seu próximo resultado.

### Buttons
- **Shape:** retângulo compacto com cantos moderados (`0.375rem`), borda de 1px e altura mínima de 2.5rem.
- **Primary:** teal industrial profundo, texto branco, padding de 0.5rem 1rem e peso 600; hover usa o teal escurecido.
- **Hover / Focus:** hover altera a tonalidade sem deslocar o layout; focus-visible usa o anel teal global de 3px.
- **Secondary / Ghost / Tertiary:** outline usa superfície branca e linha fria; ghost remove a borda e responde com cinza névoa; danger usa vermelho tijolo para ações destrutivas.

### Chips
- **Style:** badges de status usam fundo lavado, linha semântica, texto semântico e ícone correspondente.
- **State:** o texto é sempre visível; positive, warning, negative e neutro têm tratamentos distintos para leitura rápida.

### Cards / Containers
- **Corner Style:** cantos moderados (`0.375rem`).
- **Background:** branco de superfície sobre papel frio.
- **Shadow Strategy:** borda fria mais sombra ambiente de painel; elevação forte fica para sobreposições.
- **Border:** contorno de 1px; estados de erro podem acrescentar uma faixa lateral de 4px.
- **Internal Padding:** normalmente 1rem; estados e seções mais densos usam 1.5rem a 2rem.

### Inputs / Fields
- **Style:** superfície branca, contorno frio de 1px, cantos moderados e altura de 2.5rem a 2.75rem.
- **Focus:** a borda assume teal e o foco global adiciona um anel visível.
- **Error / Disabled:** erro usa vermelho tijolo com mensagem explícita; disabled reduz opacidade e bloqueia o cursor.

### Navigation
- **Style:** trilho lateral escuro com itens de 0.625rem vertical e 0.75rem horizontal, ícones Lucide nativos e labels semibold.
- **Default / Hover / Active:** estado padrão usa texto teal-claro; hover e ativo usam a superfície de acento do trilho; o ativo mantém texto branco.
- **Mobile:** drawer lateral para navegação completa e barra inferior para os cinco primeiros destinos do perfil.

### Operational States
Loading usa skeleton com shimmer discreto; erro usa faixa lateral e ação de retry; vazio usa ícone em contêiner neutro, título e orientação; sucesso e pendência usam o mesmo vocabulário semântico dos badges.

## Do's and Don'ts

### Do:
- **Do** priorizar leitura rápida de pendências, validade, qualificação e próxima ação.
- **Do** manter a combinação de títulos condensados, corpo neutro e labels monoespaçados.
- **Do** usar bordas frias e sombra ambiente para separar superfícies antes de adicionar decoração.
- **Do** mostrar texto e ícone junto de cores semânticas.
- **Do** preservar a navegação orientada por perfil e a adaptação para mobile.

### Don't:
- **Don't** usar cor como único indicador de estado.
- **Don't** transformar cada painel em um cartão flutuante com sombra forte.
- **Don't** substituir a precisão operacional por ornamentos, gradientes chamativos ou animações gratuitas.
- **Don't** criar uma estética de dashboard SaaS genérico que esconda o próximo passo.
- **Don't** expor ações ou informações fora do escopo autorizado do perfil.
