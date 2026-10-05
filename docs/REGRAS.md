# bura-open-v1 — variante explícita para duas pessoas

Esta é a variante proposta para o produto, não uma afirmação de que todo Buraco brasileiro usa estas regras. Alterações futuras mudam o identificador de regras; os dois celulares devem concordar com ele. Somente cartas, distribuição e validação de sequências estão implementadas neste marco.

## Preparação e ordem

Dois baralhos franceses completos, sem curingas impressos: 104 cartas com identidade física distinta. Cada jogador recebe 11 cartas, distribuídas alternadamente. Separe dois mortos de 11, vire uma carta para o descarte e mantenha 59 no monte. A ordem do monte e dos mortos é privada do anfitrião. O primeiro jogador inicial é o anfitrião; alterne o inicial entre rodadas. Uma partida soma rodadas até alguém alcançar 2.000 pontos; empate nesse momento exige outra rodada.

## Turno

O jogador compra uma carta do monte OU todo o descarte, sem obrigação de baixar sua carta superior. Comprar é obrigatório e acontece uma vez. Depois pode baixar e ampliar quantas sequências próprias válidas desejar. Termina descartando exatamente uma carta. Não se pode rearranjar ou retirar cartas já baixadas nem jogar nas sequências do adversário. Não há abertura mínima de pontos. Descartar um 2 é permitido e não congela o descarte.

## Sequências e curingas

Somente sequências do mesmo naipe, de 3 a 13 cartas, sem trincas. O ás vale abaixo do 2 ou acima do rei, nunca nos dois extremos simultaneamente. Cartas de mesmo valor e naipe dos dois baralhos não podem ocupar a mesma posição natural.

O 2 do próprio naipe pode ser natural na posição 2. Um 2 de qualquer naipe pode substituir uma única posição faltante; no máximo um substituto por sequência. É permitido um 2 natural e outro 2 substituto. Se as cartas formam sequência sem substituição, classifique como limpa. Não há retorno natural K–A–2; um 2 usado como dama numa sequência 2–K–A representa Q–K–A e é suja. O papel e a posição escolhidos pelo motor devem aparecer na mesa futura. Uma ampliação só é válida se todo o conjunto continuar válido.

Canastra tem pelo menos 7 cartas. Limpa: nenhuma substituição, bônus 200. Suja: uma substituição, bônus 100. O bônus não cresce acima de 7. Não há canastra real nem bônus especiais nesta variante.

## Morto e batida

Cada jogador pode pegar um morto, o próximo disponível. Ao zerar a mão baixando cartas pela primeira vez, pega o morto imediatamente e continua o mesmo turno sem nova compra. Ao zerar pelo descarte pela primeira vez, pega o morto e espera o próximo turno. A posse do morto é registrada separadamente do conteúdo da mão.

Depois de pegar o morto, para encerrar a rodada o jogador precisa ter uma canastra limpa e terminar descartando a última carta. Não pode zerar a mão por uma baixada nesta fase nem descartar a última carta sem satisfazer os requisitos. A jogada inteira inválida é rejeitada sem mudança parcial de estado.

## Monte esgotado e pontuação

Se a última carta do monte for comprada, o jogador conclui o turno e a rodada termina mesmo que ainda haja descarte. Mortos não substituem o monte. Se houver batida válida no mesmo turno, aplique seu bônus. Sem batida, não há bônus de encerramento.

Valor das cartas: ás 15; dois 20; 3 a 7 valem 5; 8 a rei valem 10. Some cartas baixadas e bônus de canastras; subtraia cartas na mão. Subtraia 100 de quem não pegou o morto. Some 100 a quem bateu. Pontuações podem ser negativas. A comparação usa os totais acumulados após a rodada, não o jogador que bateu isoladamente.

## Casos obrigatórios de aceitação do futuro motor de turnos

Compra dupla, ação fora do turno, carta alheia, carta repetida, baixada parcialmente inválida, pegar morto direto/indireto, segundo morto, batida sem canastra limpa, ampliação com dois substitutos, esgotamento do monte e empate acima de 2.000. O invariante é conservar exatamente as 104 identidades físicas em todas as zonas, inclusive após recuperar uma sessão.
