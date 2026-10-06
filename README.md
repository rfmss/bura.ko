# bura.ko

Buraco presencial para duas pessoas, cada uma no próprio Android, via Bluetooth e sem internet.

**Estado: protótipo jogável 0.2.** O núcleo já executa partidas completas e o app inclui um modo pass-and-play no mesmo celular, com persistência local. O diagnóstico Bluetooth continua disponível para validar o rádio; a sincronização da partida entre dois aparelhos é o próximo incremento.

## Compilar

Use JDK 17 e Android SDK 35 com Build Tools 35.0.0. Configure `ANDROID_HOME` ou `sdk.dir` em `local.properties` (ignorado pelo Git).

```sh
./gradlew :core:check :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. O workflow Android publica o artefato `burako-debug` quando passa. Dependências são baixadas na primeira compilação; o aplicativo não pede permissão de internet.

Com Kotlin 2.1.20 instalado, o núcleo pode ser verificado sem SDK/Gradle:

```sh
./scripts/check-core.sh
```

Os testes são executáveis JVM sem biblioteca de teste extra; `:core:verifyCore` está ligado a `:core:check`. Eles não dependem de `assert` desabilitado por padrão.

## Experimentar o diagnóstico

Para jogar agora sem esperar a validação do rádio, abra **Jogar no mesmo celular**. A capa alterna entre as pessoas, evitando expor a mão adversária; compra, descarte, sequências, morto, canastras, batida, pontuação e salvamento já funcionam.

Para testar Bluetooth, instale o mesmo APK em dois Androids com Bluetooth Classic. Pareie os celulares nas configurações e finalize a busca. Desligue Wi-Fi e dados móveis, mantendo Bluetooth ativo. Abra o app nos dois: um escolhe **Receber conexão**, o outro **Entrar na conexão**, ambos selecionando o par correto. Conceda acesso a dispositivos próximos se solicitado. Quando a conexão for confirmada, toque em **Testar ida e volta**. O rádio está validado; a partida compartilhada entre aparelhos ainda está em integração.

O diagnóstico fecha a conexão ao sair do aplicativo ou girar a tela. É possível abrir outra conexão; isso ainda não é recuperação de partida. A validação de rádio e desempenho requer os aparelhos físicos descritos em [docs/VALIDACAO.md](docs/VALIDACAO.md).

## Documentação

- [Prompt corrigido e fragilidades](docs/PROMPT-EXECUCAO.md)
- [Regulamento versionado](docs/REGRAS.md)
- [Arquitetura, orçamento e etapas](docs/ARQUITETURA.md)
- [Resultados e roteiro de validação](docs/VALIDACAO.md)