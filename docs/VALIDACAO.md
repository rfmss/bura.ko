# Validação — fundação 0.1

## Executado em 2026-10-05

Ambiente Linux x86_64, Java 17.0.20, compilador Kotlin 2.1.20, stubs Android SDK 35.

`KOTLINC=/tmp/burako-tools/kotlinc/bin/kotlinc ./scripts/check-core.sh`: PASSOU, 15 verificações. Inclui 1.000 distribuições com conservação das 104 cartas, sequências naturais de todos os naipes/comprimentos, ás alto/baixo, dois natural/substituto, rejeição de trincas e duplicatas, framing com leituras de um byte, todos os prefixos truncados, cabeçalhos inválidos e limites, handshake, 100 sondas bidirecionais, repetição/replay e desafio antigo. Não são testes de rádio.

Compilação direta de todos os fontes Kotlin do núcleo e Android com `android.jar` API 35: PASSOU. Avisos de APIs antigas de insets são esperados para compatibilidade com API 23; isto não substitui empacotamento, lint ou teste visual.

Gradle/empacotamento/lint: em verificação durante esta entrega; resultado final será registrado antes do fechamento. O wrapper inicialmente encontrou rede direta indisponível no ambiente, depois timeout no download via proxy. O compilador independente permitiu verificar o código enquanto a distribuição Gradle era obtida pelo caminho de download disponível.

Revisão de concorrência: identificado e corrigido o caso de timeout cancelado já aguardando o lock. Cada agendamento tem época própria; um callback antigo não encerra uma sessão que acabou de confirmar ou uma sonda nova. Geração de conexão também descarta callbacks e threads de sessões anteriores.

## Não executado / não reivindicado

Não há dois aparelhos Bluetooth acessíveis neste ambiente. Pareamento, OEMs, rádio, diálogos de permissão, visual, TalkBack, rotações, morte real de processo e métricas de memória/fluidez/inicialização ainda precisam de hardware. O app não contém o motor de turnos nem persistência de partida. Reconectar o diagnóstico abre uma conexão nova, não restaura um jogo.

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
