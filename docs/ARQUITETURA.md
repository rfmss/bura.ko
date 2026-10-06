# Arquitetura, custo e marcos

## Decisões de 2026-10-05

Repositório inicial continha apenas README. A entrega inicia uma aplicação Android nativa, não um site/PWA: a API de Bluetooth escolhida precisa de integração Android. Kotlin puro em `core`; Activity e Views em `app`; sem engine, Compose, biblioteca de rede, banco ou serviços Google. JVM 17 é ferramenta de compilação; o APK usa minSdk 23. Kotlin stdlib é a dependência de runtime direta além do núcleo.

Ferramentas fixadas: Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, compile/target SDK 35. Esta combinação é conhecida e evita atualizar por novidade. Distribuição inicial por APK; exigências futuras da Play Store e testes nos Androids posteriores exigem revisão separada. `distributionSha256Sum` verifica a distribuição do Gradle. Não incluir chaves de assinatura no repo.

RFCOMM seguro, UUID fixo do serviço, um par previamente pareado. Escolha explícita do dispositivo em ambos os celulares; o anfitrião rejeita outra identidade Bluetooth. API 31+ solicita somente CONNECT; o diagnóstico não faz descoberta, não pede SCAN/ADVERTISE/localização nem cancela descoberta do sistema. O usuário deve concluir o pareamento antes de voltar ao app. Descoberta que continue em outro aplicativo pode atrasar a conexão: testar nos aparelhos e só ampliar permissões se a evidência justificar.

O socket bloqueia somente a thread de trabalho. Escrita serial com fila máxima de 8; mensagem com cabeçalho fixo de 10 bytes e conteúdo de até 512 bytes. Timeout de conexão 45 s; handshake 10 s; sonda 8 s. Uma sonda pendente por vez. Fechar sockets interrompe esperas. Versão e identificador de regras são negociados antes das sondas. O desafio por conexão impede reaproveitar a confirmação anterior; não é um protocolo antitrapaça.

Encerrar ou sair da Activity fecha o diagnóstico e suas threads. Não há serviço permanente, escaneamento periódico nem loop de renderização. Rotação também encerra a conexão nesta etapa. Não usar `configChanges` para esconder problemas de ciclo de vida. A aplicação final precisará guardar a sessão fora da Activity e reconectar, sem presumir execução indefinida em segundo plano.

## Próxima camada de partida

Anfitrião autoritativo; cliente envia intenções. Comando conterá versão, ID da partida, sequência do jogador, revisão esperada, ação e argumentos limitados. Identidade do jogador vem da sessão, nunca de um campo livre. Ordem: validar, criar próximo estado, gravar atomicamente em arquivo privado, confirmar, enviar projeções permitidas. Duplicatas retornam recibo anterior sem reexecutar. Guardar sequência alta por jogador e último recibo, evitando histórico ilimitado. Rejeitar revisões antigas e pedir ressincronização.

Para retomada, persistir vínculo do par e token de sessão privado, estado, revisão, sequência e recibos. Não compartilhar seed, mãos adversárias ou estoque na projeção do convidado. Conferir token e regras após reconectar ao mesmo anfitrião. Sem migração automática, espectador ou sincronização por nuvem. Quando ambos divergem, o snapshot persistido do anfitrião é a autoridade. `AtomicFile` é candidato para persistência Android; não usar serialização Java em dados remotos.

O anfitrião conhece todas as cartas e pode adulterar seu próprio aplicativo. Proteção criptográfica contra esse anfitrião está fora do MVP. Isso não impede preservar informações no convidado legítimo, nas mensagens ou em logs. Nesta entrega o protocolo transmite somente diagnóstico, nenhuma carta.

## Orçamento e alternativas

Faixas de planejamento, não horas já gastas ou orçamento contratado: fundação/diagnóstico 2–4 dias de engenharia; motor completo/persistência 4–7; integração multiplayer 3–5; acabamento e validação de hardware 3–5. Total preliminar 12–21 dias de trabalho de uma pessoa experiente. Reestimar após prova física. Não contratar ilustrações/sons nem serviços nesta fase. Infraestrutura de operação: nenhum servidor; CI pode consumir a cota do GitHub do proprietário.

Metas iniciais para aparelho de 2 GB, medidas em build release: APK até 15 MiB; PSS em mesa estável até 150 MiB; abertura fria até 2,5 s; frames de animação p95 até 33 ms; sem trabalho contínuo em mesa parada. Metas são hipóteses, não resultados desta entrega. Registrar modelo, SO, build, tamanho de mão, ferramenta e amostragem; medir 10 aberturas frias e sessão de 15 min. Reduzir assets e trabalho por quadro antes de aumentar requisitos mínimos.

Views nativas ganham no custo inicial e acessibilidade. Canvas só quando medição da mesa cheia mostrar benefício; preservar semântica de toque e TalkBack. BLE/interoperabilidade com iOS exigem nova prova e custam mais que este transporte Android. Fullscreen 3D, partículas e blur não resolvem nenhum problema de leitura do Buraco.

## Marcos e critérios de saída

M0 — Código inicial: build, núcleo de cartas/regras e diagnóstico. Não é uma partida jogável. M0 físico: parear dois Androids, desligar dados/Wi-Fi, confirmar versão, medir ida/volta, interromper e reconectar. Falhas de permissão e rádio devem ser recuperáveis pela tela. Não declarar o rádio validado por testes JVM.

M1 — Motor completo: compra, baixada, ampliação, morto, descarte, batida, rodadas e pontuação conforme REGRAS.md. Testes de invariantes, comandos atômicos e projeção por jogador. Sem arte final.

M2 — Partida real: integrar comandos e persistência, repetir mensagens após perda de ACK, matar processo de ambos os lados, retomar mesmo jogo. Só sair deste marco após uma rodada inteira offline em hardware.

M3 — Polimento: mãos de 11/30/60+ cartas, sequências longas e muitas baixadas; retrato/paisagem, fonte ampliada, naipes e TalkBack. Compor uma tela representativa antes de produzir assets. O visual inicial usa verde profundo, creme e ouro fosco, formas nativas e tipografia do sistema, sem download de fontes.

## Referências primárias consultadas

- https://developer.android.com/develop/connectivity/bluetooth/connect-bluetooth-devices — servidor/cliente RFCOMM e I/O fora da UI.
- https://developer.android.com/develop/connectivity/bluetooth/bt-permissions — permissões por versão.
- https://developer.android.com/develop/connectivity/bluetooth/find-bluetooth-devices — consulta de dispositivos pareados.
- https://developer.android.com/build/releases/agp-8-9-0-release-notes — compatibilidade AGP/Gradle/JDK/SDK.
