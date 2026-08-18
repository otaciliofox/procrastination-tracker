# Procrastination Tracker

Recriação do app **Procrastination Timer** (`com.tomuozawa.procrastinationtimer`), removido da Play Store por incompatibilidade (o app não é atualizado desde 2019 e não atende mais aos requisitos de target API da Play Store). Esta versão foi renomeada para **Procrastination Tracker** e ganhou uma versão nativa para Galaxy Watch (Wear OS), além do app de celular.

## O que o app original fazia

Segundo a ficha na APKPure, o Procrastination Timer era um cronômetro simples de produtividade com dois métodos:

- **52/17** — 52 minutos de foco, 17 minutos de pausa.
- **Pomodoro** — ciclos de 25 minutos de foco, pausas de 5 minutos, e uma pausa de 30 minutos a cada 4 ciclos.

Ele media o tempo produtivo vs. o tempo de procrastinação para você comparar os dois.

## O que este projeto entrega

O app tem duas seções independentes, escolhidas na tela inicial:

**Modo Timer** — a recriação do app original:
- **Mesma lógica dos dois modos** (52/17 e Pomodoro), implementada em Kotlin puro no módulo `:core`, compartilhada 100% entre celular e relógio.
- Cronômetro, notificação persistente, histórico de sessões e comparação "produtivo vs. procrastinado" (hoje e histórico completo).
- **Sincronização relógio → celular**: quando uma sessão termina no relógio, ela é enviada via Wearable Data Layer API e aparece no histórico do celular.

**Modo Tracker** — modo novo (ver `spec/002-activity-tracker-mode.md` para a especificação completa), pra quem quer rastrear várias categorias de tempo em vez de só foco/procrastinação:
- **"Pizza" de fatias** (2 a 6), nome livre — Trabalho, Estudo, Treino, Hobby, Procrastinando, o que fizer sentido. Toque numa fatia pra rastrear, toque de novo pra pausar, troque à vontade o dia todo.
- **Perfis de layout**: Duo e Tri vêm prontos e são fixos (nunca sobrescritos); editar e salvar sempre cria um novo perfil Custom, e você pode ter vários Custom ao mesmo tempo.
- **Roda em segundo plano** com notificação (nome da fatia + tempo, com ações Pausar/Parar), distingue ir pra outro app (não interrompe) de fechar o app pelos recentes (para e salva).
- **Funciona offline em cada aparelho** (SQLite local via módulo `:trackerdata`, compartilhado entre `:app` e `:wear`), sincroniza os dois lados quando estão por perto.
- Resumo hoje/semana por fatia, sem alarme/som (esse modo é cronômetro livre, não intervalo).

## Estrutura do projeto

```
ProcrastinationTracker/
├── core/         → lógica pura em Kotlin do Modo Timer (TimerEngine, modos, Session) — sem dependência de Android
├── trackerdata/  → módulo Android library com Room (perfis/fatias/sessões do Modo Tracker) + TrackerRepository, compartilhado por app e wear
├── app/          → app de celular (Jetpack Compose + Material 3)
└── wear/         → app do Galaxy Watch (Compose for Wear OS, standalone)
```

## Requisitos para compilar

- **Android Studio** Koala (2024.1) ou mais recente.
- **JDK 17** (o Android Studio já vem com um embutido).
- Um **Galaxy Watch com Wear OS 3+** (Watch4, Watch5, Watch6, Watch7, Watch Ultra) para instalar o `:wear`. Modelos com Wear OS 2 (Watch3 ou anteriores) não são suportados por esta versão (`minSdk 30`).

## Como abrir e compilar

1. Abra a pasta `ProcrastinationTracker` no Android Studio (**File → Open**).
2. Deixe o Android Studio sincronizar o Gradle. Como este projeto não inclui o `gradlew`/`gradle-wrapper.jar` binário, o Android Studio vai oferecer para gerá-lo automaticamente na primeira sincronização — aceite. Se preferir gerar manualmente, rode `gradle wrapper --gradle-version 8.7` uma vez com um Gradle já instalado na máquina.
3. Espere a indexação/sync terminar (primeira vez baixa as dependências, pode demorar alguns minutos).

## Como instalar no Galaxy Watch

1. No relógio: **Configurações → Sobre → toque 5x na versão do software** para ativar o modo desenvolvedor, depois **Configurações → Desenvolvedor → Depuração ADB** (ative) e **Depurar via Wi-Fi** (ative).
2. O relógio vai mostrar um IP e porta (ex.: `192.168.0.42:5555`).
3. No terminal do computador (ou no terminal do Android Studio):
   ```
   adb connect 192.168.0.42:5555
   ```
4. No Android Studio, selecione o run configuration do módulo **wear** (ou crie um em **Run → Edit Configurations → + → Android App**, módulo `wear`) e escolha o relógio conectado como dispositivo de destino.
5. Clique em **Run ▶**. O app instala e abre direto no relógio — não precisa do celular aberto (é standalone).

## Como instalar no celular

1. Ative **Opções do desenvolvedor → Depuração USB** no celular (ou depuração via Wi-Fi, igual ao relógio).
2. Conecte o celular via cabo ou `adb connect`.
3. No Android Studio, selecione o run configuration do módulo **app** e o celular como destino.
4. **Run ▶**.

Como o app é instalado diretamente pelo Android Studio (sideload), ele não passa pelas regras de compliance da Play Store — é para uso pessoal.

## Personalizar durações e nomes

Tudo fica em `core/src/main/kotlin/.../core/TimerMode.kt`:

```kotlin
FIFTY_TWO_SEVENTEEN(
    label = "52/17",
    focusMinutes = 52,
    shortBreakMinutes = 17,
    ...
)
```

Mude os minutos ali e recompile — celular e relógio pegam o valor novo automaticamente, já que compartilham o mesmo `:core`.

## Modo Tracker: o que fica só no celular

Criar, editar ou excluir um perfil Custom (e adicionar/remover fatias) só é possível no celular —
digitar vários nomes numa tela redonda pequena não compensava a complexidade. No relógio dá pra
*trocar* entre os perfis que já existem (Duo, Tri, ou qualquer Custom criado no celular); o que
você monta lá aparece no relógio pela sincronização. "Bloquear tela agora" também é só no celular
(via `AccessibilityService`, ative uma vez em Configurações → Acessibilidade); "manter tela
ligada" existe nos dois.

## Possíveis melhorias futuras (não incluídas nesta primeira versão)

- Tile do Wear OS para iniciar/pausar direto da tela de tiles, sem abrir o app.
- Complicação no mostrador do relógio mostrando o tempo restante.
- Tela de configurações para durações customizadas no Modo Timer (hoje só 52/17 e Pomodoro fixos; 45/15 e modo totalmente customizável estão especificados mas ainda não implementados).
- Gráfico de histórico (dia a dia) no celular.
- Vibração customizada no relógio ao fim de cada sessão (hoje usa a notificação padrão).
- Modo Tracker Fase 2 (subtarefas) e Fase 3 (pomodoro por fatia) — ver `spec/002-activity-tracker-mode.md`.
- Criar/editar perfis Custom diretamente no relógio.

## Sobre os nomes de pacote

- Celular: `com.foxlab.procrastinationtracker`
- Relógio: `com.foxlab.procrastinationtracker.watch`

Troque `foxlab` pelo namespace que preferir antes de compilar, se quiser — é só um find-and-replace nos `build.gradle.kts`, `AndroidManifest.xml` e nas pastas de pacote Kotlin.
