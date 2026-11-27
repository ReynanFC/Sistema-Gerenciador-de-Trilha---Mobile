# Arquitetura

Visão geral de como o TrilhaApp é organizado: camadas, banco de dados e o caminho de um ponto de GPS
desde o sensor até o mapa da tela Visualizar.

---

## 1. Camadas e pacotes

```mermaid
flowchart TB
    subgraph UI["Interface"]
        MA["MainActivity"]
        SA["SettingsActivity"]
        RA["RecordTrailActivity"]
        LA["TrailListActivity"]
        DA["TrailDetailActivity"]
        DLG["dialog: RenameTrailDialog / DeleteTrailsDialog"]
        AD["adapter: TrailAdapter"]
    end
    subgraph LOC["location"]
        LT["LocationTracker"]
        TR["TrailRecorder"]
    end
    subgraph DB["database"]
        DAO["TrailDao"]
        HLP["DbHelper"]
    end
    subgraph UT["util"]
        SP["SettingsPrefs"]
        FM["Formatters"]
        AS["Async"]
        MI["MapIcons"]
    end
    MODEL["model: Trail / TrackPoint"]

    RA --> LT
    RA --> TR
    RA --> DAO
    LA --> AD
    LA --> DLG
    DA --> DLG
    LA --> DAO
    DA --> DAO
    DAO --> HLP
    UI --> SP
    UI --> FM
    UI --> AS
    TR --> MODEL
    DAO --> MODEL
```

| Pacote | Responsabilidade | Não pode |
|---|---|---|
| `activities` | Montar a tela, reagir a cliques e ao ciclo de vida, chamar as outras camadas | Ter SQL ou cálculo de GPS |
| `adapter` | Ligar cada `Trail` a um cartão da lista e repassar os cliques | Decidir o que o clique faz |
| `dialog` | Coletar e validar a escolha do usuário (novo nome, modo de apagar) | Chamar o DAO |
| `location` | GPS (`LocationTracker`) e estado da gravação (`TrailRecorder`) | Mexer em View |
| `database` | Esquema (`DbHelper`) e operações (`TrailDao`) | Mexer em View |
| `model` | Dados: `Trail` (mutável, tem id e nome editável) e `TrackPoint` (record imutável) | — |
| `util` | Preferências, formatação, thread de fundo, ícones do mapa | — |

---

## 2. Banco de dados

```mermaid
erDiagram
    trail ||--o{ track_point : "tem"
    trail {
        INTEGER id PK "AUTOINCREMENT"
        TEXT name "NOT NULL"
        INTEGER start_time "epoch ms"
        INTEGER end_time "epoch ms"
        REAL average_speed "m/s"
        REAL max_speed "m/s"
        REAL distance "metros"
    }
    track_point {
        INTEGER id PK "AUTOINCREMENT"
        INTEGER trail_id FK "ON DELETE CASCADE"
        REAL latitude
        REAL longitude
        REAL speed "m/s"
        REAL accuracy "metros"
        INTEGER timestamp "epoch ms"
    }
```

- **2 tabelas relacionadas (1:N)**: uma trilha tem muitos pontos.
- **`ON DELETE CASCADE`** + `setForeignKeyConstraintsEnabled(true)` no `onConfigure`: apagar a trilha apaga os pontos.
  O SQLite vem com chaves estrangeiras **desligadas**; sem essa linha o cascade seria ignorado.
- **Índice `idx_point_trail`** em `track_point(trail_id)`: os pontos são sempre buscados por trilha.
- **Datas em milissegundos (`INTEGER`)**: o SQLite não tem tipo data; `long` em ms é o formato que o Android já
  entrega, ordena com `ORDER BY` e funciona com `BETWEEN` no "apagar por intervalo".
- **Unidades SI no banco** (m, m/s, ms). Conversão para km, km/h e hh:mm:ss só na tela, em `Formatters`.
- **Inserção em transação**: trilha + centenas de pontos são gravados de uma vez; se algo falhar, nada fica pela metade.
- **Parâmetros `?`** em todas as consultas (`selectionArgs`), nunca concatenação: evita SQL injection.

---

## 3. Fluxo de ponta a ponta: GPS → memória → banco → lista → mapa

```mermaid
sequenceDiagram
    participant GPS as Play Services (GPS)
    participant LT as LocationTracker
    participant RA as RecordTrailActivity
    participant TR as TrailRecorder
    participant DAO as TrailDao
    participant SQL as SQLite
    participant LA as TrailListActivity
    participant DA as TrailDetailActivity

    GPS-->>LT: Location (a cada 1–2 s)
    LT->>RA: onLocation(location)
    RA->>TR: addLocation(location)
    Note over TR: filtra precisão, soma distância,<br/>guarda TrackPoint aceito
    RA->>RA: atualiza marcador, círculo, câmera e números
    RA->>DAO: insert(trail com pontos) em thread de fundo
    DAO->>SQL: BEGIN / INSERT trail / INSERT pontos / COMMIT
    LA->>DAO: findAll() no onResume
    DAO-->>LA: trilhas (sem pontos)
    LA->>DA: Intent com EXTRA_TRAIL_ID
    DA->>DAO: findById(id)
    DAO-->>DA: trilha + pontos ordenados por timestamp
    DA->>DA: Polyline, pinos e enquadramento
```

1. **GPS → memória**: o `LocationTracker` entrega cada `Location`; o `TrailRecorder` decide o que entra na trilha.
2. **Memória → banco**: ao finalizar, a Activity monta um `Trail` com a lista de `TrackPoint` e o `TrailDao` grava tudo
   numa transação, fora da UI thread (`Async`).
3. **Banco → lista**: `findAll()` traz só os resumos (sem pontos), da mais recente para a mais antiga.
4. **Lista → mapa**: só o **id** viaja no `Intent`; a tela Visualizar carrega a trilha completa do banco.

---

## 4. Decisões de projeto

| Decisão | Por quê |
|---|---|
| Java + SQLite puro (`SQLiteOpenHelper`), sem Room | O enunciado pede Java e SQLite; o SQL fica visível e explicável |
| View Binding em vez de `findViewById` | Classe gerada por layout com cada view já tipada: id errado não compila, sem cast e sem `null` escondido |
| minSdk 28 | As bibliotecas atuais do Google Play Services não suportam o 17 do enunciado |
| `TrailRecorder` dentro de um `ViewModel` | Sobrevive à rotação de tela sem salvar centenas de pontos no `Bundle` |
| GPS liga no `onResume` e desliga no `onPause` | Bateria e privacidade; consequência: gravação não continua em segundo plano (exigiria Foreground Service) |
| `Async` com uma única thread de fundo | Banco fora da UI thread e gravações em ordem, sem duas transações ao mesmo tempo |
| Diálogos não chamam o DAO | Mantém a regra "View não sabe de banco"; a Activity decide e salva |
| `TrackPoint` como `record` | Uma leitura de GPS não muda depois de lida; o `trail_id` é responsabilidade do DAO |
| Chave do Maps no `local.properties` + placeholder no manifest | Credencial fora do Git |
| Tema sempre claro | O protótipo só define o tema claro; mapas e cartões foram desenhados para ele |
