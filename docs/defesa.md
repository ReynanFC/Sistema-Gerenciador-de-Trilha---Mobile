# Preparação para a defesa

Perguntas prováveis da arguição com respostas curtas e onde está o código. Para o fluxo completo de cada tela,
veja os documentos em [`docs/`](../README.md#documentação).

---

## Roteiro de ponta a ponta (para explicar em 1 minuto)

1. **Configuração** grava o tipo de mapa e a navegação em `SharedPreferences` (`SettingsPrefs`).
2. **Registrar**: pede permissão, liga o GPS (`LocationTracker`) no `onResume`; cada posição vai para o
   `TrailRecorder`, que filtra a precisão, soma a distância e guarda os pontos aceitos. A tela desenha marcador,
   círculo e câmera.
3. **Finalizar**: resumo + nome; `TrailDao.insert` grava trilha e pontos numa **transação**, fora da UI thread.
4. **Consultar**: `findAll` no `onResume` (sem pontos); renomear e apagar via diálogos que devolvem a escolha para a
   Activity, que chama o DAO.
5. **Visualizar**: só o id vem no `Intent`; `findById` traz a trilha com os pontos, que viram uma `Polyline` com pinos e
   a câmera enquadra tudo.

---

## Configuração e ciclo de vida

**Por que ler a configuração no `onResume`?**
Ao salvar na Configuração e dar `finish()`, a tela anterior volta sem ser recriada: o `onCreate` não roda de novo, o
`onResume` sim. (`MainActivity.onResume`, `TrailListActivity.onResume`)

**Por que salvar o enum pelo nome e não pelo número (`ordinal`)?**
Se alguém reordenar as constantes, o número muda e o usuário passaria a ler outra configuração. O nome não muda.
E um nome desconhecido cai no padrão em vez de quebrar. (`SettingsPrefs.readEnum`)

**Por que `apply()` e não `commit()`?**
`apply()` grava em segundo plano; `commit()` é síncrono e trava a UI thread.

**O que acontece se girar a tela durante a gravação?**
O Android destrói e recria a Activity, mas entrega o mesmo `ViewModel`; o `TrailRecorder` (tempo, distância,
pontos) está dentro dele, então nada se perde. (`RecordTrailViewModel`)

---

## GPS e mapa

**Como a localização é obtida?**
`FusedLocationProviderClient` com `LocationRequest` de alta precisão a cada 2 s (mín. 1 s). As posições chegam no
`LocationCallback`, e o `LocationTracker` repassa para a tela. (`LocationTracker`)

**Por que parar o GPS no `onPause`?**
Bateria e privacidade: o app só usa a localização com a tela visível. A consequência é que a gravação não continua
em segundo plano; para isso seria preciso um *Foreground Service* com notificação.

**E se o usuário negar a permissão?**
O app não quebra: chip "Sem permissão de GPS" e diálogo com "Tentar novamente" ou, se a recusa for permanente,
"Abrir configurações". Pedimos FINE e COARSE juntas porque desde o Android 12 o usuário pode conceder só a
aproximada. (`RecordTrailActivity.onLocationPermissionDenied`)

**Por que `getMapAsync`?**
O mapa carrega em segundo plano; o `GoogleMap` só existe dentro do callback. Antes disso o campo é `null`.

**Como é o marcador personalizado?**
Um vetor (`ic_position_marker`) desenhado num `Bitmap` e passado como `BitmapDescriptor`. É criado uma vez e depois
só movido (`setPosition`) e girado (`setRotation` com o rumo do GPS). `flat(true)` faz a rotação ser relativa ao
norte. (`updatePositionMarker`, `MapIcons`)

**O que é o círculo de acurácia?**
Um `Circle` com raio = `Location.getAccuracy()`, em metros: a posição real está dentro dele com ~68% de chance.
(`updateAccuracyCircle`)

**Diferença entre North Up e Course Up no código?**
North Up: bearing da câmera = 0. Course Up: bearing = `location.getBearing()`, só quando a velocidade ≥ 0,5 m/s,
porque parado o rumo do GPS é ruído. (`followCamera`)

---

## Cálculos

**Como a distância é calculada e por que filtrar pela precisão?**
Soma de `distanceTo` entre pontos aceitos. Uma leitura só conta se a precisão for ≤ 25 m **e** se o deslocamento desde
o último ponto aceito for maior que a margem de erro dela (mínimo 5 m). Parado, o GPS "passeia" alguns metros a cada
leitura; sem o filtro a distância subiria sozinha. Testado em `TrailRecorderTest.standingStillDoesNotAddDistance`.

**Velocidade instantânea e máxima?**
Instantânea = `getSpeed()` (m/s) quando `hasSpeed()`; máxima só considera leituras precisas (as imprecisas dão picos).
Na tela, × 3,6 para km/h. (`TrailRecorder.addLocation`, `Formatters.speedKmh`)

**Velocidade média?**
Distância ÷ tempo em movimento (pausas fora), calculada ao finalizar. (`RecordTrailActivity.averageSpeed`)

**Como funciona o cronômetro com pausa?**
`SystemClock.elapsedRealtime()` (não muda se o relógio do celular mudar). Guarda o tempo acumulado dos trechos já
encerrados + o início do trecho atual. Um `Handler` só redesenha o texto enquanto a tela está visível.

---

## Banco de dados

**Quais tabelas?**
`trail` (id, name, start_time, end_time, average_speed, max_speed, distance) e `track_point` (id, trail_id,
latitude, longitude, speed, accuracy, timestamp). 1:N. (`DbHelper`)

**Como funciona a transação ao salvar os pontos?**
`beginTransaction` → insere a trilha e pega o id → insere cada ponto com esse id → `setTransactionSuccessful` →
`endTransaction` no `finally`. Se qualquer insert falhar, nada é salvo. Usamos `insertOrThrow` porque o `insert`
devolve -1 em silêncio e a transação seria confirmada pela metade. Também é mais rápido: um único commit em disco.
Testado com 1000 pontos e com falha no meio. (`TrailDao.insert`, `TrailDaoTest`)

**Por que os pontos são apagados junto com a trilha?**
`FOREIGN KEY ... ON DELETE CASCADE`, e `setForeignKeyConstraintsEnabled(true)` no `onConfigure`, porque o SQLite vem
com chaves estrangeiras desligadas. (`DbHelper.onConfigure`)

**Por que datas em `INTEGER` (milissegundos)?**
O SQLite não tem tipo data. `long` em ms é o que o Android entrega (`currentTimeMillis`, `Location.getTime`), ordena e
funciona com `BETWEEN` no "apagar por intervalo".

**Por que `?` nas consultas?**
Os valores vão em `selectionArgs`, nunca concatenados no SQL: evita SQL injection e problemas com aspas.

**Por que o banco roda fora da UI thread?**
Operações de disco na UI thread travam a tela. `Async` usa uma única thread de fundo (gravações em ordem) e devolve o
resultado na UI thread.

**Por que `onUpgrade` apaga e recria as tabelas?**
Ainda não há versão publicada com dados de usuários. Num app publicado, cada versão teria um script de migração
(`ALTER TABLE ...`) em vez de `DROP`.

**Por que `TrackPoint` é `record` e `Trail` não?**
Uma leitura de GPS não muda depois de lida → imutável. A trilha nasce sem id, recebe o id do banco e pode ser
renomeada → mutável.

---

## Organização

**Por que separar `LocationTracker` e `TrailRecorder` da Activity?**
A Activity cuida da interface; o tracker cuida do GPS; o recorder faz os cálculos sem conhecer View (dá para testar
isolado). Se a gravação fosse para um *Service*, bastaria trocar quem usa essas classes.

**Por que os diálogos não chamam o DAO?**
Regra das camadas: View não sabe de banco. O diálogo devolve a escolha (novo nome, modo de apagar) por callback e a
Activity chama o DAO.

**Por que só o id vai no `Intent` para a tela Visualizar?**
Uma trilha pode ter milhares de pontos; o `Intent` tem limite de tamanho. A tela de destino busca no banco.

---

## Projeto

**Por que minSdk 28?**
As bibliotecas atuais do Google Play Services (Maps e Location) não suportam o minSdk 17 do enunciado.

**Onde fica a chave da API e por que não está no Git?**
No `local.properties` (no `.gitignore`); o Gradle injeta no manifest por placeholder (`${MAPS_API_KEY}`). Chave no Git
fica pública e pode ser usada por terceiros. A chave é restrita ao pacote do app + SHA-1 do certificado.

**Como foi testado?**
Testes instrumentados no aparelho (`TrailDaoTest`, `TrailRecorderTest`), com banco em memória para não tocar nas
trilhas reais, e teste manual no celular (mapa, permissão, gravação, lista, visualização).
