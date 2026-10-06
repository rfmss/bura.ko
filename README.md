# bura.ko

Buraco presencial para duas pessoas, cada uma no próprio Android, via Bluetooth e sem internet.

**Estado: protótipo jogável 0.3.** O núcleo executa partidas completas; o app tem modo pass-and-play com persistência local e partida compartilhada offline via Bluetooth Classic. O anfitrião mantém o estado autoritativo; o convidado recebe uma projeção filtrada e envia apenas comandos validados.

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

Para jogar via Bluetooth, instale o mesmo APK em dois Androids com Bluetooth Classic. Pareie os celulares nas configurações e finalize a busca. Desligue Wi-Fi e dados móveis, mantendo Bluetooth ativo. Abra o app nos dois, toque em **Jogar via Bluetooth**, escolha **Criar mesa** em um e **Entrar na mesa** no outro, selecionando o par correto. Conceda acesso a dispositivos próximos se solicitado. A mesa abre quando os dois lados confirmam; compras, baixas, extensões, descartes, morto, canastras e pontuação são sincronizados sem internet.

Os botões **Receber conexão**, **Entrar na conexão** e **Testar ida e volta** continuam disponíveis para diagnóstico do rádio. A validação de rádio, permissões, OEM e desempenho requer os aparelhos físicos descritos em [docs/VALIDACAO.md](docs/VALIDACAO.md).

O protótipo fecha a conexão ao sair do aplicativo ou girar a tela; reconexão/retomada de mesa fica para uma etapa posterior.

## Documentação

- [Prompt corrigido e fragilidades](docs/PROMPT-EXECUCAO.md)
- [Regulamento versionado](docs/REGRAS.md)
- [Arquitetura, orçamento e etapas](docs/ARQUITETURA.md)
- [Resultados e roteiro de validação](docs/VALIDACAO.md)
