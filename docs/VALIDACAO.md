
# Validação — fundação 0.1

## Executado em 2026-10-05

Ambiente Linux x86_64, Java 17.0.20, compilador Kotlin 2.1.20, stubs Android SDK 35.

`KOTLINC=/tmp/burako-tools/kotlinc/bin/kotlinc ./scripts/check-core.sh`: PASSOU, 15 verificações. Inclui 1.000 distribuições com conservação das 104 cartas, sequências naturais de todos os naipes/comprimentos, ás alto/baixo, dois natural/substituto, rejeição de trincas e duplicatas, framing com leituras de um byte, todos os prefixos truncados, cabeçalhos inválidos e limites, handshake, 100 sondas bidirecionais, repetição/replay e desafio antigo. Não são testes de rádio.

Compilação direta de todos os fontes Kotlin do núcleo e Android com `android.jar` API 35: PASSOU. Avisos de APIs antigas de insets são esperados para compatibilidade com API 23; isto não substitui empacotamento, lint ou teste visual.

`:core:check :app:assembleDebug :app:lintDebug`: PASSOU, 49 tarefas, build completo em 2 min 2 s após preparar dependências. Lint: 0 erros e 9 avisos (8 sobre textos ainda fora de resources para tradução; 1 sobre regras de extração/backup Android 12+). O MVP está em português; centralizar textos e definir exclusões explícitas de transferência/backup antes de adicionar persistência de partidas em M2. Não foram suprimidos avisos para obter este resultado.

APK debug: **867.569 bytes (847,24 KiB)**, `minSdk 23`, `targetSdk 35`. `aapt dump badging` confirmou somente permissões Bluetooth, sem INTERNET. `apksigner verify --verbose` validou assinaturas v1 e v2, incluindo o esquema necessário para API 23. SHA-256 local: `4898134d64ff2d3775c825191bcd2043e9762fe86aad3c548c7a2212ecaf2ffb`. A chave é debug local; um APK do CI terá outra assinatura/hash. Tamanho de debug não é medição de memória nem resultado release.

Ambiente: o wrapper inicialmente encontrou rede direta indisponível e timeout via proxy. A distribuição oficial foi baixada com seu SHA-256 conferido; foi necessário preparar JDK 17 completo (o Java inicial não tinha javac), configurar o proxy e usar o truststore de certificados do próprio ambiente. Nenhuma dessas configurações de máquina foi incluída no repo e a validação TLS não foi desativada.

CI: primeira execução falhou no setup Android porque o pacote padrão `tools` deixou de existir. Corrigido para instalar explicitamente `platform-tools`, `platforms;android-35` e `build-tools;35.0.0`; evitadas execuções duplicadas de push e PR. A execução corrigida estava na fila em 2026-10-05 durante o fechamento; não é contada como aprovada. Build/lint aprovados acima são locais. Acompanhar o resultado e o artefato no PR https://github.com/rfmss/bura.ko/pull/1.

Revisão de concorrência: identificado e corrigido o caso de timeout cancelado já aguardando o lock. Cada agendamento tem época própria; um callback antigo não encerra uma sessão que acabou de confirmar ou uma sonda nova. Geração de conexão também descarta callbacks e threads de sessões anteriores.

## Não executado / não reivindicado

Não há dois aparelhos Bluetooth acessíveis neste ambiente. Pareamento, OEMs, rádio, diálogos de permissão, visual, TalkBack, rotações, morte real de processo e métricas de memória/fluidez/inicialização ainda precisam de hardware. O app já contém o motor de turnos, persistência de partida e modo pass-and-play local. Reconectar o diagnóstico abre uma conexão nova, não restaura uma partida compartilhada entre aparelhos.

## Executado em 2026-10-06

Após a implementação do motor e do modo local, o CI passou em `:core:check`, `:app:assembleDebug` e `:app:lintDebug`. O artefato `burako-debug` foi publicado no workflow 37405343942, associado ao commit `140dc2a782b9c8a32a4bc71422efed43022f1b4a`; tamanho do ZIP 886.527 bytes. O APK pode ser baixado na execução do workflow. O checksum do artefato remoto é `sha256:618abb69f639003436660bebd9dd6d98a9a55e7d6425dedcd6c17deef593e57c7`.

## Roteiro físico obrigatório para M0

1. Android API 23–30 como anfitrião e API 31+ como convidado; depois inverter. Repetir com uma versão Android atual e aparelho de 2 GB. Registrar modelos/versões; não inferir compatibilidade de um único par.
2. Instalar o mesmo APK nos dois, parear pelas configurações, finalizar descoberta. Desligar dados móveis e Wi-Fi. Confirmar que ambos conseguem chegar à tela sem rede.
3. Escolher explicitamente o par correto, um recebe e outro entra. Confirmar protocolo e executar 20 sondas de cada lado. Registrar RTT, falhas e tempo de conexão.
4. Negar permissão CONNECT; conceder depois pelas configurações. Desligar Bluetooth antes e durante a conexão. Selecionar dispositivo errado. Tentar duas entradas/duas recepções. Nenhum caso deve travar ou encerrar o app inesperadamente.
5. Cancelar, sair, girar a tela, afastar aparelhos e matar processo. O protótipo deve fechar ou expirar e permitir conexão nova. Não exigir retomada de partida antes de M2.
6. Tocar repetidamente em testar; deve haver só uma sonda pendente. Trocar versão/regras num APK de teste: rejeitar conexão incompatível.
7. Verificar 320 dp de largura, fonte a 200%, TalkBack e botões de 48 dp ou mais. Verificar barra do sistema no Android 15+; os insets são aplicados explicitamente. Só então aprovar o acabamento visual.

## Próxima implementação

Motor de turnos conforme REGRAS.md, testes de todas as zonas e atomicidade, projeção filtrada por jogador; em seguida armazenamento transacional e integração Bluetooth. A prova física M0 permanece gate para investir em arte final e declarar multiplayer validado.

