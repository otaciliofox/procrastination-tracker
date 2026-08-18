# Spec 002 — Modo Tracker de Atividades ("Pizza")

Status: **Fase 1 implementada** (código em `core`, `trackerdata`, `app`, `wear`; não compilado neste ambiente — ver §9)
Depende de: Spec 001 (app base — timer 52/17 / Pomodoro / celular + relógio), já implementado.

## 1. Motivação

O modo Timer atual (52/17, Pomodoro, futuro 45/15) é binário: foco vs. pausa. Isso não representa
o dia real do usuário, que alterna entre várias categorias de tempo (trabalho, estudo, treino,
hobby, procrastinação) em blocos irregulares, e precisa de flexibilidade — especialmente por lidar
com TDAH, onde um plano rígido (ex: 52 min fixos de foco) pode atrapalhar mais do que ajudar.

O Modo Tracker resolve isso: um cronômetro por categoria ("fatia"), troca livre entre categorias,
sem alarmes forçando um ritmo. O objetivo nesta fase é **apenas rastrear e reportar**, sem
sugestões/dicas automáticas (isso fica para uma fase futura, fora de escopo aqui).

## 2. Decisões já fechadas com o usuário

| Decisão | Resolução |
|---|---|
| Limite de fatias | 2 (mínimo) a 6 (máximo), mesmo limite no celular e no relógio |
| Timer (intervalos) vs. Tracker (pizza) | Duas seções separadas por enquanto. Combiná-las (pomodoro rodando dentro de uma fatia) é desejado, mas fica pra Fase 3 |
| Virada do dia | Automática à meia-noite (hora local do aparelho). "Concluir dia" é um atalho manual opcional, não obrigatório |
| Gesto de pausar/trocar/concluir fatia | Em aberto — usuário sugeriu um botão central que muda de forma (vira uma "gota" apontando pra fatia ativa). Entra refinado na Fase 2; Fase 1 usa a interação mais simples possível (tocar na fatia) |
| Backup em nuvem | Não. Apenas SQLite local em cada aparelho. Perda de dados ao limpar o app é um risco aceito |
| Sincronização | Celular e relógio funcionam 100% independentes offline. Quando os dois estão por perto, sincronizam nos dois sentidos (não só relógio→celular como no Spec 001) |
| Nomes de exibição | "Modo Timer" e "Modo Tracker" na tela inicial |
| Perfis de layout (Duo/Tri/Custom) | **Duo** e **Tri** são 2 templates fixos, prontos, somente-leitura — nunca são sobrescritos, quantidade de fatias travada (2 e 3). Você pode usá-los direto como estão. Se editar algo neles (ex: renomear uma fatia) e salvar, o app cria um **novo perfil Custom** (uma cópia com sua edição) — Duo/Tri continuam intactos. Custom também pode nascer do zero (não precisa partir de Duo/Tri), com 2 a 6 fatias livres. Pode haver **vários perfis Custom** ao mesmo tempo, cada um com nome próprio, editável e removível. Só um perfil fica ativo por vez (Duo, Tri, ou qualquer Custom). Ver Fase 1 → "Perfis de layout" |
| App em segundo plano vs. app fechado | Ir para outro app (Home, trocar pro Instagram etc.) não afeta o tracking — sessão continua rodando com notificação persistente. Remover o app da tela de recentes é tratado como "Parar": salva o tempo até ali e encerra a sessão. Ver §6 |
| Intervalo do heartbeat de resiliência | 30 segundos. App é leve, tracking não é uma operação pesada, então o custo de bateria/CPU de um heartbeat a cada 30s é desprezível |
| Nome sugerido ao criar Custom a partir de Duo/Tri | Vem pré-preenchido com o mesmo nome do template + "(cópia)", mas o campo é editável — o usuário pode digitar por cima ou só confirmar. Se salvar mais de um a partir do mesmo template, o app numera automaticamente (ex: "Tri (cópia)", depois "Tri (cópia_01)", "Tri (cópia_02)"...) pra evitar nomes duplicados |
| Limite de perfis Custom | Até 10 ao mesmo tempo. Dificilmente alguém cria tantos assim na prática — quem chega perto desse número passa a editar os que já existem em vez de criar novos. O seletor de perfil vira uma lista rolável, funciona bem mesmo no relógio |

## 3. Fora de escopo (por enquanto)

- Dicas/sugestões automáticas baseadas nos dados.
- Alarme/som/vibração dentro do modo Tracker (fica só no modo Timer).
- Backup em nuvem / multi-dispositivo além de 1 celular + 1 relógio.
- Edição de sessões passadas (corrigir um tempo lançado errado) — avaliar em fase futura.

## 4. Fases

A ideia é entregar em fatias pequenas (trocadilho intencional), testar, e só então avançar.

### Fase 1 — Pizza básica, sem subtarefas

**O que entra:**

- Tela inicial com dois cartões: "Modo Timer" e "Modo Tracker".

#### Perfis de layout: Duo / Tri / Custom

- Ao entrar no Modo Tracker pela primeira vez, **Duo** e **Tri** já existem, pré-criados e
  **somente-leitura**:
  - **Duo** — Foco / Procrastinando (igual ao app original), sempre 2 fatias.
  - **Tri** — Trabalho / Estudo / Procrastinando, sempre 3 fatias.
  Dá pra ativar e usar os dois direto como estão, mas **nenhum dos dois pode ser editado nem
  sobrescrito** — nome das fatias e quantidade (2/3) são fixos pra sempre.
- Se o usuário tocar em editar algo num Duo ou Tri ativo (ex: renomear "Foco" pra "Estudo
  Profundo"), o app não altera o original — ao salvar, pede um nome pro novo perfil e **cria um
  Custom novo** (uma cópia com a edição aplicada), com a mesma quantidade de fatias que o
  template de origem tinha. Duo e Tri continuam intactos e disponíveis pra sempre.
- Também dá pra criar um Custom do zero (sem partir de Duo/Tri), com 2 a 6 fatias livres — já que
  é Custom, a quantidade não é travada.
- Pode existir **mais de um perfil Custom** ao mesmo tempo (ex: um "Custom (de Tri)" e outro
  "Rotina de fim de semana" criado do zero) — cada um com nome próprio dado pelo usuário,
  editável e removível a qualquer momento.
- Um seletor de perfil (ex: lista/menu no topo da tela do Tracker) mostra Duo, Tri e todos os
  Custom existentes, e troca qual está ativo. Só um fica ativo por vez — é ele que aparece na
  pizza e recebe os toques.
- Trocar de perfil com uma fatia ativa primeiro encerra a sessão em andamento (mesmo fluxo do
  "Parar" em §6), depois troca a pizza exibida.
- Cada perfil (Duo, Tri, ou qualquer Custom) guarda seu próprio conjunto de fatias e seu próprio
  histórico de sessões — trocar de perfil não apaga nem mistura os dados de nenhum outro.
- Dentro de um Custom: edição livre — nome do perfil, nome/cor de cada fatia, adicionar e
  remover fatias (respeitando 2 a 6).
- Configuração de cada fatia: nome livre, cor/ícone simples. Guardado localmente.
- Tocar numa fatia inativa → ela vira a fatia ativa, começa a contar. Se havia outra fatia ativa,
  o tempo acumulado dela é salvo e ela para de contar.
- Tocar na fatia **já ativa** → pausa (nenhuma fatia ativa, cronômetro geral do dia continua
  existindo mas nada está sendo contado até o usuário tocar em alguma fatia de novo).
- Cronômetro central mostra: fatia ativa (ou "pausado") + tempo da sessão atual continua desde que
  a fatia foi ativada pela última vez.
- Totais do dia por fatia, visíveis na própria tela do Tracker (não precisa ir pra outra tela pra
  ver quanto tempo já foi pra cada categoria hoje).
- Virada automática de dia à meia-noite: totais zeram, dia anterior vai para o histórico.
- Tela "Resumo" simples: hoje (por fatia, em horas/minutos) e semana (soma dos últimos 7 dias por
  fatia). Ainda sem gráfico, só números.
- Tela ativa durante o uso; botão manual de "bloquear tela agora" (usa a API de screen lock do
  Android, não desliga o tracking).
- Serviço em primeiro plano (foreground service) dedicado ao Tracker, com notificação persistente
  enquanto uma fatia está ativa — nome da fatia + tempo da sessão atual, com ações rápidas
  "Pausar" e "Parar" direto na notificação (mesma ideia da barra de acompanhamento de pedido do
  iFood: você vê o status sem precisar abrir o app). Ver §6 para o detalhamento técnico.
- Dentro do app, com uma fatia ativa, o botão voltar (ou um menu dedicado) abre 4 opções: Pausar,
  Parar, Reiniciar (zera a sessão atual sem salvar) e Sair do app (para tudo e fecha).
- Sem subtarefas ainda. Sem combinação com Timer ainda. Sem gesto criativo de botão central ainda
  — interação é só "tocar na fatia" (ativar/pausar/trocar), como descrito acima.
- Persistência SQLite local nos dois apps (celular e relógio), cada um funcionando sozinho.
- Sincronização básica: ao detectar o outro aparelho por perto (Wearable Data Layer, mesmo canal
  já usado no Spec 001), troca as sessões que um tem e o outro não, dos últimos N dias. Sessão é
  imutável depois de criada (tem UUID + timestamps), então merge é só união de conjuntos — não
  tem conflito de edição para resolver nesta fase.

**Critério de pronto:** Duo e Tri já existem prontos e não dá pra sobrescrevê-los; eu consigo criar
um Custom do zero (Trabalho, Estudo, Treino, Hobby, Procrastinando) e também criar um Custom
editando um Duo/Tri (o original continua intacto); tenho vários perfis Custom se eu quiser, cada
um com seu nome; alterno de perfil sem perder o que já foi registrado em nenhum deles; alterno
entre fatias o dia todo no celular e no relógio; vejo o total de hoje e da semana em cada perfil;
e os dois aparelhos concordam sobre os números quando sincronizam. Trocar de app pra ver o
Instagram não interrompe o tracking; deslizar o app pra fora dos recentes para e salva; e as 4
opções (Pausar/Parar/Reiniciar/Sair) funcionam como esperado.

### Fase 2 — Subtarefas + interação refinada

**O que entra:**

- Dentro de uma fatia: lista de subtarefas (título + tempo acumulado). Botão "+" para adicionar.
- Subtarefa pode ser marcada como "diária" — reaparece todo dia (não precisa recriar), mesmo
  depois de concluída no dia anterior. Subtarefa não-diária some da lista ativa ao ser concluída,
  mas fica no histórico.
- Tocar numa subtarefa dentro da fatia ativa: passa a contar o tempo dela junto com o tempo global
  da fatia. Concluir a subtarefa para o cronômetro dela mas a fatia continua contando (útil pra
  quando você termina "Inglês" mas continua fazendo outra coisa dentro de "Estudo" sem subtarefa
  específica).
- Segundo toque na fatia já ativa (em vez de pausar) passa a abrir a tela de subtarefas dela —
  primeiro toque ativa, segundo toque (na já ativa) acessa o conteúdo. Pausar vira uma ação
  separada (botão dedicado dentro dessa tela, ou o gesto abaixo).
- Interação do botão central: refinar para o conceito de "gota" sugerido — botão redondo no
  centro que, quando uma fatia está ativa, se estica/aponta visualmente na direção dela; tocar
  numa fatia diferente move o indicador pra lá; tocar no botão central pausa; toque longo abre
  a fatia ativa (atalho pra subtarefas). Este é um ponto de design visual — vamos prototipar e
  ajustar depois de ver funcionando, não é uma regra fechada.
- Resumo diário/semanal passa a detalhar por subtarefa também, não só por fatia.

**Critério de pronto:** consigo cadastrar "Inglês", "Português", "Harness/IA", "Design Patterns"
dentro da fatia "Estudo", marcar as que são diárias, alternar entre elas ao longo do dia, pausar
tudo pra sair e voltar depois sem perder o progresso, e ver quanto tempo foi pra cada uma no
resumo.

### Fase 3 — Combinar Tracker com Timer (pomodoro por fatia)

**O que entra:**

- Cada fatia pode, opcionalmente, ter um modo de intervalo associado (52/17, Pomodoro, 45/15 ou
  customizado — reaproveitando o Spec 001). Se a fatia tem um modo associado, ativar essa fatia
  também inicia o ciclo de foco/pausa com alarme, junto com o cronômetro geral dela.
- Fatias sem modo associado continuam funcionando como hoje (cronômetro livre, sem alarme).
- Alarme/som/vibração passam a poder ocorrer dentro do Tracker, mas só nas fatias configuradas
  para isso — mantendo o "sem alerta" como padrão pras fatias que não pediram.

**Critério de pronto:** consigo configurar "Trabalho" pra tocar em ciclos de 45/15 automaticamente
enquanto rastreio o tempo total de trabalho, e deixar "Treino" e "Hobby" como cronômetro livre sem
alarme nenhum.

### Fase 4 — Fora de escopo por enquanto (mencionar, não detalhar)

Dicas automáticas com base no histórico (ex: "você procrastina mais depois das 15h"), edição de
sessões passadas, exportar relatório, gráficos. Revisitar depois que as fases 1–3 estiverem em uso
real por um tempo.

## 5. Modelo de dados (rascunho, SQLite)

```sql
-- Duo e Tri: exatamente 2 linhas fixas, seedadas uma vez, somente-leitura pro app (nunca
-- editadas nem deletadas via UI). Custom: 0 ou mais linhas, criadas/editadas/removidas livremente.
CREATE TABLE layout_profile (
    id                    TEXT PRIMARY KEY,   -- 'duo' | 'tri' (fixos) | UUID pra cada Custom
    type                  TEXT NOT NULL,      -- 'DUO' | 'TRI' | 'CUSTOM'
    title                 TEXT NOT NULL,      -- "Duo" / "Tri" fixos; nome livre escolhido pelo usuário nos Custom
    is_active             INTEGER NOT NULL DEFAULT 0,  -- só 1 linha com 1 por vez
    forked_from_profile_id TEXT REFERENCES layout_profile(id), -- opcional: de qual perfil este Custom foi copiado (NULL se criado do zero)
    created_at            INTEGER NOT NULL,
    updated_at            INTEGER NOT NULL
);

-- Fatias configuradas pelo usuário, sempre dentro de um perfil
CREATE TABLE activity_slice (
    id              TEXT PRIMARY KEY,      -- UUID gerado no aparelho
    profile_id      TEXT NOT NULL REFERENCES layout_profile(id),
    title           TEXT NOT NULL,
    color           TEXT,                  -- hex, opcional
    position        INTEGER NOT NULL,      -- ordem de exibição na pizza, dentro do perfil
    timer_mode_id   TEXT,                  -- NULL = cronômetro livre; senão referencia timer_mode (Fase 3)
    archived        INTEGER NOT NULL DEFAULT 0,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);

-- Sessões de tempo (imutáveis após fechadas -> merge de sync é só união)
CREATE TABLE activity_session (
    id              TEXT PRIMARY KEY,      -- UUID
    slice_id        TEXT NOT NULL REFERENCES activity_slice(id),
    subtask_id      TEXT REFERENCES subtask(id), -- NULL = tempo "global" da fatia, sem subtarefa
    start_time      INTEGER NOT NULL,
    end_time        INTEGER,               -- NULL enquanto está rodando
    source_device   TEXT NOT NULL,         -- "phone" | "watch"
    created_at      INTEGER NOT NULL
);

-- Subtarefas dentro de uma fatia (Fase 2)
CREATE TABLE subtask (
    id              TEXT PRIMARY KEY,
    slice_id        TEXT NOT NULL REFERENCES activity_slice(id),
    title           TEXT NOT NULL,
    is_daily        INTEGER NOT NULL DEFAULT 0,  -- reaparece todo dia se 1
    archived        INTEGER NOT NULL DEFAULT 0,
    created_at      INTEGER NOT NULL,
    updated_at      INTEGER NOT NULL
);

-- Conclusão de subtarefa por dia (permite subtarefa diária resetar sem apagar histórico)
CREATE TABLE subtask_completion (
    id              TEXT PRIMARY KEY,
    subtask_id      TEXT NOT NULL REFERENCES subtask(id),
    date            TEXT NOT NULL,          -- "YYYY-MM-DD" local
    completed_at    INTEGER NOT NULL
);
```

Totais diários/semanais são **calculados** a partir de `activity_session` (soma de
`end_time - start_time` agrupado por `slice_id`/`subtask_id`/dia), não guardados numa tabela à
parte — evita inconsistência entre o valor cru e o resumo.

## 6. Execução em segundo plano: distinguir "app em background" de "app fechado"

Essa distinção é possível no Android, e é o mesmo mecanismo que apps como o iFood usam pra manter
a barra de acompanhamento de pedido viva. Como funciona aqui:

- **Ir para outro app ou pra tela Home** (usuário quer ver o Instagram, responder uma mensagem,
  etc.) — a Activity vai para segundo plano, mas o `TrackerForegroundService` continua rodando
  normalmente (é um foreground service, o Android não mata isso sob pressão de memória exceto em
  situações extremas). A notificação persistente continua mostrando fatia + tempo, com os botões
  Pausar/Parar. Nada é interrompido. Isso já é essencialmente o que o Spec 001 faz hoje pro modo
  Timer — vamos reaproveitar a mesma base de serviço.
- **Remover o app da tela de recentes** (deslizar o card do app pra fora) — o Android chama
  `onTaskRemoved()` no serviço quando isso acontece. Vamos sobrescrever esse callback pra tratar
  como um toque em "Parar": fecha a sessão ativa (grava `end_time = agora`), salva no SQLite, para
  o serviço e derruba a notificação. Diferente do comportamento padrão de `START_STICKY`, que
  tentaria recriar o serviço — aqui queremos exatamente o oposto quando é remoção deliberada.
- **App crasha ou o processo morre sem aviso** (bem mais raro com foreground service, mas
  possível em situações extremas de memória) — pra não perder o dia inteiro de dados, o serviço
  grava um "heartbeat" da sessão em andamento a cada ~30s (atualiza um `end_time` provisório no
  SQLite). Se o app for reaberto e encontrar uma sessão sem fechamento limpo, ela é encerrada
  automaticamente usando o horário do último heartbeat — na pior hipótese perde-se ~30s de tempo
  rastreado, nunca o dia todo. Isso também cobre o caso de esquecer o relógio sem bateria, ou o
  celular desligar (pergunta que estava em aberto na versão anterior deste documento).
- **Menu de saída explícita dentro do app** (botão voltar com fatia ativa, ou um menu dedicado):
  Pausar / Parar / Reiniciar / Sair do app — "Sair do app" força o mesmo caminho de "Parar" antes
  de fechar, garantindo que nunca fique uma sessão "pendurada".

## 7. Sincronização entre celular e relógio

- Cada sessão (`activity_session`) tem UUID + `source_device`. Como só é criada quando termina
  (start+end conhecidos) ou tem heartbeat periódico enquanto ativa (§6), o merge nunca precisa
  resolver conflito de edição — é só "o outro aparelho tem uma sessão que eu não tenho, eu
  adiciono à minha base local".
- Igual acontece para `activity_slice` e `subtask`: `updated_at` resolve o raro caso de editar o
  nome de uma fatia nos dois aparelhos ao mesmo tempo (last-write-wins por timestamp).
- Transporte: mesma API já usada no Spec 001 (Wearable Data Layer / `DataClient`), trocando o
  `path` para `/procrastination-tracker/activity-sync`.
- Sessão "em andamento" (fatia ativa agora) não precisa sincronizar em tempo real — só quando
  fecha (usuário troca de fatia, pausa, parar, ou o heartbeat de §6 a encerra). Isso simplifica
  bastante a Fase 1.

## 8. Onde está o código

- `core/` — sem mudanças nesta fase (é só a lógica do Modo Timer).
- `trackerdata/` (módulo Android library novo) — entidades Room (`layout_profile`,
  `activity_slice`, `activity_session`), DAOs, `TrackerDatabase`, e `TrackerRepository` com toda a
  lógica de negócio (seed de Duo/Tri, fork-to-custom com nome automático, ativar/pausar/parar/
  reiniciar fatia, heartbeat, recuperação de sessão travada, totais hoje/semana, snapshot e merge
  de sincronização). Compartilhado por `app` e `wear`, igual o `core`.
- `app/` — tela inicial com os 2 cartões, `TrackerScreen` (pizza desenhada em Canvas + seletor de
  perfis + resumo + diálogos de criar/editar/fork), `TrackerForegroundService`, `ActivitySyncSender`/
  `ActivitySyncListenerService`, `ScreenLockAccessibilityService`.
- `wear/` — mesma pizza em Canvas adaptada pro mostrador redondo, `TrackerForegroundService`,
  `ActivitySyncSender`/`ActivitySyncListenerService`, telas de perfil/resumo/menu via
  `SwipeDismissableNavHost`.

## 9. Decisões tomadas durante a implementação (não estavam 100% fechadas no spec)

- **Heartbeat**: em vez de sobrescrever `end_time` a cada 30s (como o texto original do §6
  sugeria), a sessão ganhou uma coluna própria `lastHeartbeatAt`. `end_time` só é gravado quando a
  sessão realmente fecha. Efeito prático é o mesmo (recupera sessão travada usando o último
  heartbeat conhecido), mas fica mais fácil de raciocinar sobre o estado "ainda rodando".
- **Bloquear tela agora**: implementado via `AccessibilityService` (`GLOBAL_ACTION_LOCK_SCREEN`),
  não Device Admin — o usuário ativa uma vez em Configurações > Acessibilidade. Só existe no
  celular; no relógio ficou de fora (conceito de "tela bloqueada" não se aplica do mesmo jeito no
  Wear OS). O toggle "manter tela ligada" existe nos dois.
- **Criar/editar perfis só no celular**: o relógio deixa você *trocar* de perfil (Duo, Tri, ou
  qualquer Custom que já exista), mas criar um Custom novo, editar nomes ou apagar um perfil só dá
  no celular — digitar vários nomes de fatia numa tela redonda pequena não valia a complexidade.
  O que você monta no celular aparece no relógio pela sincronização (§7).
- **Sincronização é "snapshot completo"**, não um log de mudanças incrementais: a cada evento
  relevante (fatia fechada) e a cada 2 minutos como rede de segurança, cada aparelho manda tudo
  (perfis + fatias + sessões dos últimos 60 dias) para o outro, que faz merge por id/updated_at.
  Mais simples de implementar corretamente do que sincronização incremental, e no volume de dados
  de uma pessoa usando o app isso não pesa. Se um dia isso virar gargalo (milhares de sessões),
  vale revisitar.
- **`isActive` nunca sincroniza**: qual perfil está "ativo" é local de cada aparelho de propósito
  — você pode estar rastreando "Tri" no relógio enquanto o celular ainda mostra "Duo" selecionado.

## 10. Verificação e próximo passo

Não há Android SDK neste ambiente para compilar/rodar — a verificação foi manual: conferência de
pacotes/diretórios, de toda referência entre `ViewModel` ↔ `Service` ↔ `Repository` ↔ `DAO`, de
strings usadas vs. declaradas, e dos paths de sincronização batendo entre os manifests e o código.
Não substitui abrir no Android Studio. Próximo passo real: abrir o projeto, deixar o Gradle
sincronizar, rodar no celular e no relógio, e ajustar o que aparecer — em especial a posição dos
rótulos dentro da pizza (é matemática de ângulo que não dá pra validar visualmente sem rodar) e o
comportamento do `BackHandler` dentro do `SwipeDismissableNavHost` do Wear.

### 10.1 Ajustes de UX feitos após o primeiro teste em aparelho físico

Rodando em celular e relógio reais (2026-08), quatro problemas apareceram e foram corrigidos:

- **Leitura confusa de progresso ao trocar de fatia**: o número grande acima da pizza mostrava só
  o tempo da sessão atual (zera a cada nova ativação), dando a impressão de que o tempo por fatia
  não acumulava — mesmo o total no banco (`observeTotalsSince`) já somando corretamente todas as
  sessões fechadas do dia. Correção: o botão central da pizza agora mostra o total acumulado do dia
  da fatia ativa (`liveTodayTotal`), que continua de onde parou ao reativar uma fatia, em vez do
  cronômetro de sessão isolado.
- **Sem botão start/stop visível no centro**: adicionado um círculo clicável no centro da pizza
  (ambas plataformas). Tocar numa fatia continua sendo "start"; tocar no centro quando há uma fatia
  ativa chama o mesmo `pauseActive()` de tocar na fatia ativa de novo ("stop" visual). Sem fatia
  ativa, o centro fica inerte (ícone de play, sem ação — iniciar continua sendo tocar a fatia
  desejada).
- **45/15 mencionado mas inacessível**: o texto do card inicial já citava "52/17, Pomodoro ou 45/15
  com alarme", mas o enum `TimerMode` nunca ganhou esse terceiro valor. Adicionado
  `FORTY_FIVE_FIFTEEN` (45 min foco / 15 min pausa, sem variante de pausa longa) — como as telas de
  seleção de modo iteram `TimerMode.entries`, ele passou a aparecer automaticamente nas duas
  plataformas sem mais nenhuma mudança de UI.
- **Rótulo ilegível em fatia inativa**: o texto usava alpha reduzido sobre uma cor de fatia já
  escurecida, ficando ilegível dependendo da cor/tema. Correção: rótulo sempre em branco 100% opaco
  sobre um scrim escuro semi-transparente (independe da cor da fatia por baixo); só o peso da fonte
  (negrito) ainda distingue a fatia ativa das demais.

### 10.2 Segunda rodada, depois de mais uso real

- **Sem feedback visual ao tocar numa fatia**: as fatias são desenhadas em `Canvas` com gesto
  próprio (matemática de ângulo), então não ganham o ripple automático de um `clickable` comum.
  Agora o toque (`onPress`, antes mesmo de soltar o dedo) já destaca a fatia tocada e escurece as
  demais na hora; esse destaque segue sem piscar até o estado real confirmar a ativação, com um
  timeout de 1,5s como rede de segurança. Vale para celular e relógio.
- **Botão central não fazia nada em repouso**: estava com `enabled = false` quando ocioso, e um
  `clickable` desabilitado consome o toque em vez de deixar passar (ao contrário do que a primeira
  versão desta seção supunha). Agora fica sempre habilitado: ocioso, inicia a primeira fatia da
  lista; ativo, continua pausando.
- **Voltar não deveria abrir menu de confirmação no Modo Tracker**: a versão anterior interceptava
  o botão/gesto de voltar sempre que havia rastreamento ativo, abrindo um menu (Pausar/Parar/
  Reiniciar/Sair). Trocado por: voltar sempre navega normalmente (a sessão roda no serviço em
  segundo plano, independente da tela, igual ao Modo Timer); Pausar/Parar/Reiniciar/Sair viraram um
  menu "⋮" — no celular ao lado do botão de início, no relógio como um chip ao lado de "Hoje" (a
  suposição inicial de que o relógio não teria espaço para isso valia para um fitness tracker
  pequeno, não para um Galaxy Watch).
- **Navegação do Modo Timer**: trocar de aba (Timer/Histórico) empilhava uma entrada nova a cada
  clique em vez de usar o padrão `popUpTo`/`restoreState` de bottom-nav, então voltar repetidamente
  ficava re-percorrendo o histórico de cliques em vez de ir direto para Home. Corrigido para o
  padrão padrão de navegação por abas; adicionado também um ícone de voltar explícito no topo do
  Modo Timer e do Modo Tracker que pula direto para a Home independente da profundidade da pilha.

### 10.3 Sistema de cores único (celular + relógio) e ajustes de densidade visual

Depois de screenshots reais mostrando cores inconsistentes por tela (card amarelo acidental na
Home, chips com cores diferentes sem motivo, fatias muito saturadas competindo com o botão
central), a paleta foi consolidada num único lugar em vez de continuar sendo escolhida tela a
tela:

- **`AppPalette` em `:core`**: `:core` é módulo Kotlin puro (sem dependência de Compose/Android),
  então os valores de cor viraram constantes `Long` (ARGB) num objeto único
  (`core/.../theme/AppPalette.kt`), referenciado tanto pelo `Theme.kt` do celular quanto pelo do
  relógio. Antes disso, as duas plataformas mantinham listas de hex separadas que só coincidiam
  "por acaso" quando alguém lembrava de copiar o valor certo. Agora é literalmente o mesmo arquivo
  compilado nos dois módulos — divergência deixa de ser possível por descuido.
- **Princípios seguidos** (baseados na documentação oficial do Wear OS / Material 3 Expressive —
  developer.android.com/design/ui/wear/guides/styles/color): fundo verdadeiro preto (`#000000`)
  tanto no relógio quanto agora no tema escuro do celular ("build from black" — telas de watch
  precisam funcionar igual de dia e de noite, diferente do dark theme comum de celular que costuma
  ter um leve tingimento); vermelho e verde reservados exclusivamente para o par semântico
  start/stop do botão central (nunca reaproveitados como cor decorativa em outro lugar); tons
  "container" (baixa saturação) para elementos secundários/decorativos, reservando saturação plena
  só para a única ação principal da tela.
- **Fatias da pizza mais opacas**: a paleta de fatias trocou de cores 100% saturadas (incluindo
  vermelho e verde, que competiam visualmente com o significado do botão central) para
  `AppPalette.WEDGE_PALETTE` — seis tons "container" na família de matizes do dourado/teal, sem
  vermelho nem verde. O botão central continua com saturação plena (agora um verde/vermelho um
  pouco menos "neon" que antes) para permanecer o ponto focal da tela.
- **Ícone só, sem texto, no relógio**: botões da Home (Timer/Tracker) e os controles auxiliares da
  tela do Tracker ("Hoje"/"⋮") viraram botões circulares só com ícone vetorial real (sem rótulo de
  texto) — ícone já comunica a ação numa tela pequena. No celular, os cards continuam com
  ícone + texto, já que há espaço de sobra. O botão central da pizza no relógio também trocou os
  glifos de texto aproximados ("▶"/"II") por ícones vetoriais reais (`Icons.Filled.PlayArrow` /
  `Icons.Filled.Pause`), maiores e mais legíveis.
- **Pendências desta rodada, não resolvidas ainda**: um indicador de página em "3 pontos" (padrão
  visto em apps de referência para mostrar/destacar a tela ativa entre várias) foi sugerido mas
  ainda não implementado — depende de repensar a navegação por gestos primeiro. Uma reclamação de
  lentidão ao deslizar no relógio também foi reportada; nenhum problema óbvio de performance foi
  encontrado numa revisão estática do código (o `Canvas`/`pointerInput` da pizza é simples,
  matemática de ângulo O(1) por toque), então a suspeita principal é que o teste via espelhamento
  de tela do Android Studio (device mirroring) adiciona sua própria latência — precisa ser testado
  direto no relógio físico, sem espelhamento, para confirmar se o problema é real ou um artefato do
  método de teste.

### 10.4 Paleta real (gradiente) + Home sem cards + build quebrado corrigido

- **Build quebrado no relógio**: `androidx.compose.material:material-icons-core` (usado por engano
  na rodada anterior) só traz um subconjunto curado de ~40 ícones e não inclui `Pause`, `History`
  nem `Settings` -- exatamente o erro de referência relatado. Trocado para
  `material-icons-extended`, igual ao celular.
- **Preview do Compose não aparecia**: só existia `@Preview` nos arquivos do celular. Adicionado
  `@Preview(device = Devices.WEAR_OS_SMALL_ROUND)` em `ActivityPizza.kt` e `HomeScreen.kt` do
  relógio, para o toggle Code/Split/Design aparecer também nesses arquivos.
- **Paleta amber trocada por um gradiente real**: a cor âmbar/dourada inventada foi rejeitada como
  feia por conta própria. Trocada por um gradiente de referência fornecido pelo usuário (roxo ->
  azul -> teal -> verde: `#C4AAEF #81A3ED #009BDD #0091BD #008390 #00725A`), usado diretamente como
  `WEDGE_PALETTE` (as 6 fatias da pizza usam os 6 tons do gradiente, na ordem). `PRIMARY` e
  `SECONDARY` são dois pontos desse mesmo gradiente (`#009BDD` e `#008390`), agora idênticos entre
  tema claro e escuro (uma marca não devia mudar de cor com o tema). `GO_GREEN` foi deixado mais
  vívido que o verde final do gradiente (`WEDGE_6`) para não se confundir visualmente com ele
  quando os dois aparecem juntos (pizza ociosa: botão central verde + última fatia verde-escura).
- **Papéis de cor do Material 3 sem valor explícito**: `darkColorScheme(...)`/`lightColorScheme(...)`
  só sobrescrevem os papéis passados; qualquer papel não definido (`primaryContainer`, `outline`,
  `scrim`, `inverseSurface`, etc.) cai no roxo padrão do Material 3 por baixo dos panos --
  provavelmente uma segunda causa, não diagnosticada antes, de "cores que não combinam com o resto
  do app" em componentes como `FilledIconButton`/`Switch`/`DropdownMenu`. Todos os ~15 papéis
  restantes agora têm valor explícito derivado da mesma paleta.
- **Home sem cards**: os dois `Card` de 120dp viraram duas zonas de toque de tela cheia (metade de
  cima = Timer, metade de baixo = Tracker), sem fundo em caixa -- só um tingimento bem sutil
  (8-12% de opacidade) na cor de cada modo, ícone grande e texto centralizados. Objetivo: acabar
  com o "muito espaço e pouco aproveitamento" (a tela toda vira alvo de toque, não só duas caixas
  pequenas no meio) e com a aparência de "botão" isolado que não estava agradando. Aplicado igual
  no celular (ícone + título + subtítulo) e no relógio (só ícone, círculo removido, meia-tela cada).
