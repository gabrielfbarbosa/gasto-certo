# GastoCerto

Controle de gastos da casa para **Android, iOS e Web**, feito com Kotlin Multiplatform e Compose Multiplatform. Projeto final da disciplina de Análise e Projeto de Sistemas Orientados a Objetos (APSOO).

O app substitui uma planilha mensal em que as regras ficavam implícitas no texto das células. Ele:

- coloca cada gasto no mês certo, inclusive na fatura do cartão (fechamento e vencimento);
- divide compras parceladas sem perder centavos;
- separa o que é gasto de terceiros (ex.: do pai), a receber;
- converte moeda estrangeira com cotação;
- gera os previstos do mês (contas fixas, salário, aporte);
- fecha e reabre meses;
- mostra a sobra do mês, da semana e do ano.

## Funcionalidades

| Aba | O que faz | Casos de uso |
|---|---|---|
| **Resumo** | Sobra, receitas e gastos do mês, da semana ou do ano. Filtro por titular, total por meio de pagamento, quanto cada terceiro deve e a fatura de cada cartão | UC04, UC05 |
| **Lançar** | Gasto à vista, em moeda estrangeira ou em nome de terceiro, e compra parcelada com prévia `k/N – valor – fatura`. Se o mês estiver fechado, oferece reabrir | UC01, UC02 |
| **Fechar mês** | Resumo do mês, conferência dos previstos com o valor real, fechar (abre o próximo mês com os previstos) e reabrir | UC03 |
| **Mais › Cadastros** | Pessoas, instituições e meios de pagamento. Meio com lançamentos só pode ser desativado | UC06 |
| **Mais › Contas fixas e salário** | Recorrências que geram os previstos de cada mês | UC07 |
| **Mais › Lançamentos e receita avulsa** | Receita extra; conferir, corrigir ou excluir lançamentos do mês | UC08, UC09 |
| **Mais › Investimentos** | Aporte mensal planejado e venda como receita | UC10 |
| **Mais › Reembolsos** | Saldo a receber de cada terceiro e registro do que ele pagou de volta | UC11 |

Na primeira execução, o app carrega **dados de demonstração** e avisa com um aviso na parte de baixo da tela: cartões, conta, carteira em dólar, corretoras, contas fixas, salário, gastos do pai, parcelados e um reembolso. Tudo é gravado pelos próprios casos de uso, então todas as regras valem. O mês anterior já vem fechado, e o mês atual já tem os previstos para conferir. A interface está em **português** (aparelho em pt-BR) e **inglês** (os demais idiomas).

## Pré-requisitos

| Para | Precisa de |
|---|---|
| Tudo | **JDK 21**. O Gradle baixa sozinho o JDK do daemon (`gradle/gradle-daemon-jvm.properties`); o wrapper usa Gradle 9.9.2 |
| Android | **Android Studio** (AGP 9.5) com SDK 37 e um emulador ou aparelho com **Android 8.0+ (API 26)** |
| iOS | **macOS** com **Xcode 27.2** (`xcode-select` apontando para ele) e um simulador ou iPhone com **iOS 16+** |
| Web | Um navegador moderno com WebAssembly GC (Chrome, Edge, Firefox ou Safari recentes). O Gradle baixa o Node sozinho |

## Instalação

```bash
git clone https://github.com/gabrielfbarbosa/gasto-certo.git
cd gasto-certo
```

O Android Studio cria o `local.properties` com o caminho do SDK ao abrir o projeto (esse arquivo não vai para o git). Não há chaves, contas nem serviços externos para configurar: o app funciona offline.

## Execução

**Android**: rode a configuração `androidApp` no Android Studio, ou:

```bash
./gradlew :androidApp:installDebug
```

**iOS**: abra `iosApp/iosApp.xcodeproj` no Xcode e rode no simulador (ou use a configuração `iosApp` do Android Studio).

**Web** (abre em http://localhost:8080):

```bash
./gradlew :webApp:wasmJsBrowserDevelopmentRun
```

Para gerar a versão de produção da Web, em `webApp/build/dist/wasmJs/productionExecutable`:

```bash
./gradlew :webApp:wasmJsBrowserDistribution
```

No Android e no iOS, os dados ficam num banco SQLite (Room) e persistem entre execuções. Na Web, os repositórios ficam em memória (a persistência é substituível) e os dados voltam à demonstração ao recarregar a página.

## Testes

São 250 testes na JVM, 278 no iOS e 249 em Wasm e JS. Os testes comuns rodam nos 4 alvos; a JVM soma a verificação do Koin e o iOS soma os testes de Room.

```bash
./gradlew :shared:testAndroidHostTest :shared:iosSimulatorArm64Test :shared:wasmJsTest :shared:jsTest
```

- **Regras de negócio**: cada RN (RN01 a RN11) tem teste de sucesso e de cenário inválido, com o ID no nome (ex.: ``RN02 - splits 100 reais into 3 installments without losing cents``).
- **Casos de uso e ViewModels**: rodam sobre repositórios em memória.
- **Persistência**: o mesmo contrato (`RepositoriesContract`) vale para os repositórios em memória e para os de Room. Os de Room rodam no simulador iOS, com SQLite de verdade, e incluem o fechamento do mês desfeito por rollback.
- **Carga (RNF05)**: o resumo com 1.000 lançamentos fica pronto em até 1 s.
- **Arquitetura (RNF07)**: `./gradlew :shared:verifyDomain` falha se o pacote `domain` importar Compose, Room, Koin, Android ou iOS. Essa verificação também roda no `check`.

## Estrutura

```
shared/src/
├── commonMain/kotlin/br/com/gabriel/gastocerto/
│   ├── domain/model        entidades e valores (Money, MonthYear, Entry, PaymentMethod…)
│   ├── domain/rule         Strategy (AccountingMonthPolicy), Factory (EntryFactory), exceções de regra
│   ├── domain/repository   portas (interfaces) dos repositórios e Transaction
│   ├── usecase             um *UseCase por caso de uso (Controller GRASP)
│   ├── data/memory         repositórios em memória (Web e testes)
│   ├── di                  módulos Koin
│   └── ui                  telas Compose, ViewModels (MVVM + UDF), tema e componentes
├── commonMain/composeResources   textos en (values) e pt-BR (values-pt-rBR), ícones
├── mobileMain/             Room: entidades, DAOs, mappers, Room*Repository (Android + iOS)
├── androidMain/ iosMain/ webMain/   banco de cada plataforma e platformModule (expect/actual)
└── commonTest/ iosTest/ androidHostTest/   testes
androidApp/  iosApp/  webApp/   pontos de entrada de cada plataforma
docs/uml/                       diagramas PlantUML (fonte do design) e PNG/SVG gerados
docs/apresentacao/              versões editoriais das sequências UC02 e UC03
```

## Arquitetura e decisões

- **Camadas**: `ui → usecase → domain ← data`. O domínio não conhece banco nem interface.
- **Padrões**:
  - **Strategy** (`AccountingMonthPolicy`): o mês de um gasto depende do meio de pagamento.
  - **Factory** (`EntryFactory`): cria e valida os lançamentos.
  - **Dependency Injection** (Koin): Room no celular, memória na Web e nos testes.
- **GRASP**: Information Expert, Creator, Controller, Low Coupling e High Cohesion.
- **Interface**: MVVM com fluxo unidirecional. Cada tela tem um `XUiState` imutável (`StateFlow`) e eventos únicos `XEvent`; a navegação usa Navigation 3.

Decisões principais (detalhadas no documento técnico, seção 13.4):

| | Decisão |
|---|---|
| D1 | Dinheiro em centavos (`Long`); cotação com 4 casas em `Long`; conversão com arredondamento half-up. Nada de ponto flutuante (RNF04) |
| D2 | Identidade por `id` (UUID gerado no domínio); igualdade por `id` |
| D3 | Previstos guardam `originId` da recorrência, para o fechamento não duplicar |
| D4 | Repositórios e casos de uso `suspend`; `Transaction` tudo ou nada no fechamento do mês |
| D5 | A compra parcelada não tem tabela: é remontada pelas parcelas (`purchaseId`) |
| D14 | Código em inglês; textos do usuário nos arquivos de strings en/pt-BR |
| D16 | Room com chaves estrangeiras; testes de Room no simulador iOS pelo mesmo contrato da memória |
| D18 | Dados de demonstração gravados pelos casos de uso, de forma idempotente |
| D19 | O ViewModel guarda a exceção de regra e a tela traduz; o estado muda antes do evento |

## Documentação

- **Documento técnico**: requisitos, casos de uso, modelo de domínio, design, sequências e decisões.
- **Diagramas**: `docs/uml/*.puml`. Para regerar os PNG e SVG:

  ```bash
  cd docs/uml && plantuml -tpng *.puml && plantuml -tsvg *.puml
  ```
