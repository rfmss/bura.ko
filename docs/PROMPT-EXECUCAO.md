# Prompt corrigido — bura.ko

Construa incrementalmente um Buraco 1 × 1, presencial, um Android por pessoa, 100% offline durante instalação por APK e uso. A preparação do ambiente de desenvolvimento pode baixar ferramentas. Alvo inicial: Android 6/API 23 ou superior, incluindo aparelhos com 2 GB de RAM; iOS exige estudo separado. Use Kotlin, Views nativas e Bluetooth Classic RFCOMM seguro, com poucas dependências. Não acrescente contas, backend, anúncios, IA adversária, loja ou assinatura.

Antes de implementar partidas, fixe o regulamento `bura-open-v1` de `REGRAS.md`. O motor deve ser independente de Android e do transporte. O anfitrião controla embaralhamento e validação; o convidado recebe somente sua visão. Não envie a mão adversária, o monte ou a semente aleatória. Declare o limite de confiança no anfitrião, sem prometer proteção contra aplicativo adulterado.

Primeiro entregue diagnóstico real de Bluetooth: escolha explícita de aparelho já pareado, permissões por versão, negociação de versão e regras, mensagens limitadas, tempo limite, encerramento e nova conexão. Parear pelas configurações é uma concessão de UX do protótipo; não peça localização nem escaneie permanentemente. Não confunda reconectar transporte com recuperar partida. Valide este marco em dois aparelhos físicos antes de investir no acabamento final.

Depois implemente comandos com jogador autenticado pela sessão, revisão e sequência; aplicação única; persistência atômica privada antes de confirmar; recuperação após morte do processo e projeção filtrada na reconexão. Quando a conexão cair, congele jogadas. Reuse o mesmo anfitrião, sem migração. Rejeite sessão, regra ou versão incompatível e limite todo tamanho vindo do outro aparelho.

Busque acabamento percebido de jogos da Supercell com identidade própria: composição coesa, naipes legíveis, tipografia, contraste, toques mínimos de 48 dp e feedback breve. MVP: uma mesa, um baralho e poucos sons. Use formas simples e assets reutilizados, sombras estáticas e animações curtas. Não mantenha loop de desenho em repouso. Valide mãos numerosas, fonte ampliada e TalkBack antes de encomendar ilustrações.

Use os limites e marcos de `ARQUITETURA.md`. Registre alternativas, medições, riscos e próximos passos em docs/. Entregue código compilável, testes significativos e APK de diagnóstico quando a ferramenta permitir. Não declare partida pronta, Bluetooth validado, desempenho medido ou qualidade final sem evidência. Trabalhe em branch revisável e não faça merge automático.

## Fragilidades corrigidas em relação ao primeiro prompt

1. “Buraco aberto” não definia um jogo implementável: agora há regulamento versionado com compra, morto, final, pontuação e esgotamento.
2. “Android 6+” era promessa sem matriz: passa a ser alvo de suporte sujeito a provas físicas; versões de build são independentes do minSdk.
3. “Reconexão” misturava socket e jogo: diagnóstico e retomada persistida têm marcos distintos.
4. “Salvar cada jogada” não dizia quando confirmar nem como lidar com repetição: durabilidade precede ACK e a sessão determina a identidade.
5. “Privacidade” ignorava o anfitrião: o cliente recebe uma projeção, mas o host continua confiável por premissa.
6. “Bonito como Supercell” não tinha limite de produção: virou referência de acabamento, direção original e inventário fechado de assets.
7. “Econômico” não tinha critérios: há orçamento de desempenho e faixas de esforço, sem estimativa monetária fictícia.
8. “Entregue APK” ignorava ambiente e hardware: build, testes automatizados e teste de rádio têm resultados separados.
9. Pareamento, acessibilidade, permissões, sair do app e fila de mensagens não estavam especificados: agora são parte do primeiro marco.
