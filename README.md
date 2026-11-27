# TrilhaApp — Sistema gerenciador de trilhas

App Android em **Java** para gravar trilhas com GPS, salvar o percurso em **SQLite** e consultar,
renomear, apagar e visualizar as trilhas no mapa.
Trabalho da disciplina *Computação para Dispositivos Móveis* (Avaliação N2).

## Funcionalidades

| Tela | O que faz |
|---|---|
| **Principal** | Menu para as outras telas e chips com a configuração atual |
| **Configuração** | Tipo de mapa (vetorial / satélite) e navegação (North Up / Course Up), válidos para o app todo |
| **Registrar trilha** | Mapa com a posição atual (marcador próprio + círculo de acurácia), cronômetro com pausa, velocidade instantânea e máxima, distância, North/Course Up e salvar a trilha com todos os pontos |
| **Consultar trilhas** | Lista com totais, renomear, apagar (uma, por intervalo de datas ou todas) e abrir para visualizar |
| **Visualizar trilha** | Percurso no mapa com pinos de início e fim, câmera enquadrada e painel com início, distância, duração e velocidades |

## Como rodar

1. Android Studio com JDK 21. **minSdk 28** (Android 9).
2. Crie uma chave do **Maps SDK for Android** no Google Cloud e coloque no `local.properties` (não versionado):
   ```
   MAPS_API_KEY=sua_chave
   ```
   Se a chave tiver restrição de app, registre o pacote `com.reynanfc.sistema_gerenciador_trilha`
   e o SHA-1 do seu keystore (`./gradlew signingReport`).
3. Rode em um aparelho real (o GPS do emulador não representa uma caminhada).

### Testes

```
./gradlew connectedDebugAndroidTest
```

Testes instrumentados (rodam no aparelho, com banco **em memória**, sem tocar nas trilhas salvas):

- `TrailDaoTest` — inserção em transação (1000 pontos, rollback), consultas, renomear, apagar por id/intervalo/todas e cascade.
- `TrailRecorderTest` — distância parado não sobe, caminhando soma, leituras imprecisas e pausa ignoradas, cronômetro congela na pausa.

## Arquitetura (resumo)

```
com.reynanfc.sistema_gerenciador_trilha
├── activities/   uma Activity por tela (só interface + chamar as outras camadas)
├── adapter/      TrailAdapter (RecyclerView da lista)
├── database/     DbHelper (esquema SQLite) e TrailDao (CRUD)
├── dialog/       diálogos reutilizáveis (renomear, apagar)
├── location/     LocationTracker (GPS) e TrailRecorder (estado da gravação)
├── model/        Trail e TrackPoint
└── util/         SettingsPrefs, Formatters, Async, MapIcons
```

Regra principal: **Activity não tem SQL e o DAO não mexe em View**.
As views são acessadas com **View Binding** (`buildFeatures.viewBinding = true`), sem `findViewById`.

## Documentação

| Documento | Conteúdo |
|---|---|
| [Arquitetura](docs/arquitetura.md) | Camadas, banco de dados, fluxo de ponta a ponta e decisões |
| [Principal e Configuração](docs/principal-e-configuracao.md) | Menu, chips e SharedPreferences |
| [Registrar trilha](docs/registrar-trilha.md) | Permissão, GPS, cálculos, gravação e salvamento |
| [Consultar trilhas](docs/consultar-trilhas.md) | Lista, adapter, renomear e apagar |
| [Visualizar trilha](docs/visualizar-trilha.md) | Percurso, pinos, enquadramento e painel |
| [Preparação para a defesa](docs/defesa.md) | Perguntas prováveis com respostas |

Os diagramas usam **Mermaid** e aparecem renderizados direto no GitHub.
Protótipo de interface: [`docs/prototipo/`](docs/prototipo/).
