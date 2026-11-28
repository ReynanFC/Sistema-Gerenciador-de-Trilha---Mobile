# TrilhaApp — Sistema gerenciador de trilhas

App Android em **Java** para gravar trilhas com o GPS, salvar o percurso em **SQLite** e depois consultar,
renomear, apagar e ver cada trilha no mapa.

Trabalho da disciplina *Computação para Dispositivos Móveis* (Avaliação N2).

---

## Sumário

1. [Funcionalidades](#funcionalidades)
2. [Tecnologias](#tecnologias)
3. [Como rodar](#como-rodar)
4. [Testes](#testes)
5. [Estrutura de pastas](#estrutura-de-pastas)
6. [Arquitetura em resumo](#arquitetura-em-resumo)
7. [Documentação](#documentação)
8. [Fluxo de desenvolvimento (Git)](#fluxo-de-desenvolvimento-git)
9. [Problemas comuns](#problemas-comuns)

---

## Funcionalidades

| Tela | O que faz |
|---|---|
| **Principal** | Menu para as outras telas e chips com a configuração atual |
| **Configuração** | Tipo de mapa (vetorial / satélite) e forma de navegação (North Up / Course Up), válidos para o app todo |
| **Registrar trilha** | Mapa com a posição atual (marcador próprio e círculo de acurácia), cronômetro com pausa, velocidade instantânea e máxima, distância percorrida, North Up / Course Up e salvar a trilha com todos os pontos |
| **Consultar trilhas** | Lista com total de trilhas e km, renomear, apagar (uma, por intervalo de datas ou todas) e abrir para visualizar |
| **Visualizar trilha** | Percurso no mapa com pinos de início e fim, câmera enquadrando o trajeto e painel com início, distância, duração e velocidades |

O visual segue o protótipo em [`docs/prototipo/`](docs/prototipo/).

---

## Tecnologias

| Item | Versão / detalhe |
|---|---|
| Linguagem | Java (fonte e alvo Java 21) |
| Android | `minSdk 28` (Android 9) · `targetSdk 37` · `compileSdk 37` |
| Build | Gradle (wrapper incluído) · Android Gradle Plugin 9.3 · version catalog em `gradle/libs.versions.toml` |
| Interface | AndroidX AppCompat, Material Components 3, View Binding |
| Mapa | Google Maps SDK for Android (`play-services-maps`) |
| Localização | Fused Location Provider (`play-services-location`) |
| Banco | SQLite com `SQLiteOpenHelper` (2 tabelas relacionadas) |
| Testes | JUnit 4 + AndroidX Test (testes instrumentados) |

---

## Como rodar

### 1. Pré-requisitos

- **Android Studio** atualizado (com o Android SDK 37 instalado pelo SDK Manager).
- **JDK 21**. O que vem com o Android Studio serve.
- Um **celular Android 9 ou superior** com o modo desenvolvedor ativado. O emulador abre o app, mas o GPS dele
  não simula uma caminhada de verdade.
- Uma **chave de API do Google Maps** (passo 3).

### 2. Clonar o projeto

```bash
git clone git@github.com:ReynanFC/Sistema-Gerenciador-de-Trilha---Mobile.git
cd Sistema-Gerenciador-de-Trilha---Mobile
```

Abra a pasta no Android Studio (**File → Open**) e espere o Gradle sincronizar.

### 3. Configurar a chave do Google Maps

A chave **não fica no Git**. Ela vai no arquivo `local.properties`, na raiz do projeto, que o `.gitignore` ignora.

1. No [Google Cloud Console](https://console.cloud.google.com/), crie um projeto e ative a **Maps SDK for Android**.
   O Google pede uma conta de faturamento, mas carregar mapas no SDK Android não é cobrado.
2. Em **APIs e serviços → Credenciais**, crie uma **chave de API**.
3. Restrinja a chave a **apps Android** com:
   - pacote: `com.reynanfc.sistema_gerenciador_trilha`
   - SHA-1 do seu certificado de debug, que aparece com:
     ```bash
     ./gradlew signingReport
     ```
4. Adicione ao `local.properties` (o Android Studio já cria esse arquivo com o `sdk.dir`):
   ```properties
   MAPS_API_KEY=sua_chave_aqui
   ```

O Gradle lê esse valor e o coloca no `AndroidManifest.xml` pelo placeholder `${MAPS_API_KEY}`.

> O SHA-1 de debug é diferente em cada computador. Quem for rodar em outra máquina precisa adicionar o SHA-1 dela
> na chave, ou usar a própria chave.

### 4. Rodar no celular

**Pelo Android Studio:** conecte o celular (USB ou depuração sem fio), escolha o aparelho na barra superior e clique
em **Run ▶**.

**Pelo terminal:**

```bash
./gradlew installDebug        # compila e instala no aparelho conectado
```

O APK fica em `app/build/outputs/apk/debug/app-debug.apk`.

Na primeira vez em que abrir **Registrar trilha**, o app pede a permissão de localização. Para gravar, deixe a
localização do celular ligada e prefira um lugar aberto, onde o GPS fica mais preciso.

---

## Testes

Os testes são **instrumentados**: rodam no aparelho conectado.

```bash
./gradlew connectedDebugAndroidTest
```

| Classe | O que verifica |
|---|---|
| `database/TrailDaoTest` | Inserção em transação (1000 pontos rápido e rollback se falhar no meio), consultas, renomear, apagar por id / intervalo / todas e cascade dos pontos |
| `location/TrailRecorderTest` | Parado a distância não sobe; caminhando soma; leituras imprecisas e o que se anda durante a pausa são ignorados; cronômetro congela na pausa |

Os testes do banco usam um **banco em memória**, então não apagam as trilhas salvas no aparelho.
O relatório fica em `app/build/reports/androidTests/connected/`.

---

## Estrutura de pastas

```
.
├── README.md                     este arquivo
├── docs/                         documentação do projeto (ver seção Documentação)
│   └── prototipo/                imagens do protótipo de interface, uma por tela
├── app/
│   ├── build.gradle.kts          configuração do módulo: SDKs, View Binding, chave do Maps, dependências
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml   Activities, permissões de localização e chave do Maps
│       │   ├── java/com/reynanfc/sistema_gerenciador_trilha/
│       │   │   ├── activities/   uma Activity por tela + RecordTrailViewModel
│       │   │   ├── adapter/      TrailAdapter (RecyclerView da lista)
│       │   │   ├── database/     DbHelper (esquema SQLite) e TrailDao (operações no banco)
│       │   │   ├── dialog/       RenameTrailDialog e DeleteTrailsDialog
│       │   │   ├── location/     LocationTracker (GPS) e TrailRecorder (estado da gravação e cálculos)
│       │   │   ├── model/        Trail e TrackPoint
│       │   │   └── util/         SettingsPrefs, Formatters, Async, MapIcons
│       │   └── res/
│       │       ├── layout/       telas (activity_*), item da lista (item_*), diálogos (dialog_*),
│       │       │                 bottom sheet (bottom_sheet_*) e partes reaproveitadas (component_*)
│       │       ├── drawable/     ícones vetoriais, marcador, pinos e fundos
│       │       ├── color/        seletores de cor dos cartões de opção
│       │       ├── menu/         menu da app bar da lista
│       │       ├── values/       strings, cores, dimensões, estilos e tema
│       │       └── mipmap-anydpi/ ícone do app (adaptive icon)
│       ├── androidTest/          testes instrumentados (TrailDaoTest, TrailRecorderTest)
│       └── test/                 teste unitário de exemplo
├── gradle/
│   ├── libs.versions.toml        versões das bibliotecas (version catalog)
│   └── wrapper/                  Gradle wrapper
├── build.gradle.kts / settings.gradle.kts
└── local.properties              (não versionado) caminho do SDK e MAPS_API_KEY
```

### Telas e classes

| Tela | Activity | Layout |
|---|---|---|
| Principal | `MainActivity` | `activity_main.xml` |
| Configuração | `SettingsActivity` | `activity_settings.xml` |
| Registrar trilha | `RecordTrailActivity` | `activity_record_trail.xml` + `bottom_sheet_finish_trail.xml` |
| Consultar trilhas | `TrailListActivity` | `activity_trail_list.xml` + `item_trail.xml` |
| Visualizar trilha | `TrailDetailActivity` | `activity_trail_detail.xml` |

---

## Arquitetura em resumo

- **Uma Activity por tela.** Ela monta a interface e chama as outras camadas.
- **Regra principal: Activity não tem SQL e o DAO não mexe em View.** Os diálogos também não falam com o banco:
  devolvem a escolha para a Activity.
- **GPS e cálculos fora da Activity:** o `LocationTracker` entrega as posições, e o `TrailRecorder` filtra a precisão,
  soma a distância, controla o tempo com pausa e guarda os pontos. Ele fica num `ViewModel` para sobreviver à rotação.
- **Banco:** tabelas `trail` e `track_point` (1:N) com `ON DELETE CASCADE`; a trilha é salva com todos os pontos numa
  transação, fora da UI thread.
- **Unidades:** o banco guarda metros, m/s e milissegundos. A conversão para km, km/h e hh:mm:ss acontece só na tela
  (`Formatters`).
- **Views** acessadas com View Binding, sem `findViewById`.
- **Configurações** em `SharedPreferences`, centralizadas em `SettingsPrefs`.

Os detalhes e os diagramas estão em [`docs/arquitetura.md`](docs/arquitetura.md).

---

## Documentação

Toda a documentação fica em [`docs/`](docs/), em português, com diagramas Mermaid que o GitHub mostra direto no
navegador.

| Documento | Conteúdo |
|---|---|
| [Arquitetura](docs/arquitetura.md) | Camadas e pacotes, diagrama do banco, caminho de um ponto do GPS até o mapa, decisões de projeto |
| [Principal e Configuração](docs/principal-e-configuracao.md) | Menu, chips da configuração atual e `SharedPreferences` |
| [Registrar trilha](docs/registrar-trilha.md) | Permissão, GPS, marcador e círculo, cálculos de distância e velocidade, pausa, North/Course Up, salvar |
| [Consultar trilhas](docs/consultar-trilhas.md) | Lista, adapter, renomear e apagar (uma, intervalo, todas) |
| [Visualizar trilha](docs/visualizar-trilha.md) | Percurso, pinos, enquadramento da câmera e painel |
| [Preparação para a defesa](docs/defesa.md) | Perguntas prováveis da arguição, com respostas e onde está o código |
| [Protótipo](docs/prototipo/) | Imagens de referência de cada tela |

No código, as classes e os pontos mais difíceis (GPS, cálculos, transação, permissão) têm Javadoc e comentários em
português explicando o porquê.

---

## Fluxo de desenvolvimento (Git)

O projeto segue **git flow**:

| Branch | Uso |
|---|---|
| `main` | Versão entregue |
| `develop` | Integração das funcionalidades |
| `feature/<issue>-<nome>` | Uma branch por issue, criada a partir da `develop` (ex.: `feature/25-distance`) |

- Cada funcionalidade é uma **issue** no GitHub, agrupada por etapa (milestones).
- Commits em inglês, com o número da issue: `feat: total distance with GPS jitter filter (#25)`.
- Merge com `--no-ff` na `develop`. A `develop` vai para a `main` quando a etapa está pronta.

---

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| Mapa em branco, só com o logo do Google | Chave inválida, Maps SDK desativado ou chave sem o pacote / SHA-1 deste computador | Conferir o passo 3; no Logcat, filtrar por `Google Maps` para ver o motivo |
| Chip "Buscando GPS…" não muda | Celular em local fechado ou localização do aparelho desligada | Ir para um lugar aberto e conferir se a localização está ligada |
| Chip "Sem permissão de GPS" | Permissão negada | Tocar em "Tentar novamente" ou em Configurações → Apps → TrilhaApp → Permissões |
| Distância parada em 0 | Ainda não andou mais que a margem de erro do GPS (mín. 5 m), ou a gravação está pausada | Andar alguns metros; conferir se o selo mostra "Gravando" |
| `INSTALL_FAILED_USER_RESTRICTED` ao instalar | Aparelho Xiaomi/MIUI bloqueia instalação pelo adb | Ativar "Instalar via USB" nas opções do desenvolvedor e aceitar o aviso na tela |
