# bura.ko

Buraco presencial para duas pessoas, cada uma no próprio Android, via Bluetooth e sem internet.

**Estado: fundação 0.1. Não há partida jogável ainda.** A aplicação oferece uma prévia local de cartas e um diagnóstico real de conexão Bluetooth entre aparelhos pareados. O núcleo já distribui os dois baralhos e valida sequências/canastras da variante proposta.

## Compilar

Use JDK 17 e Android SDK 35 com Build Tools 35.0.0. Configure `ANDROID_HOME` ou `sdk.dir` em `local.properties` (ignorado pelo Git).

```sh
./gradlew :core:check :app:assembleDebug :app:lintDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. O workflow Android também publica o artefato `burako-debug` quando passa. Dependências são baixadas na primeira compilação; o aplicativo não pede permissão de internet.

Com Kotlin 2.1.20 instalado, o núcleo pode ser verificado sem SDK/Gradle:

```sh
./scripts/check-core.sh
```

Os testes são executáveis JVM sem biblioteca de teste extra; `:core:verifyCore` está ligado a `:core:check`. Eles não dependem de `assert` desabilitado por padrão.

## Experimentar o diagnóstico

Instale o mesmo APK em dois Androids com Bluetooth Classic. Pareie os celulares nas configurações e finalize a busca. Desligue Wi-Fi e dados móveis, mantendo Bluetooth ativo. Abra o app nos dois: um escolhe **Receber conexão**, o outro **Entrar na conexão**, ambos selecionando o par correto. Conceda acesso a dispositivos próximos se solicitado. Quando a conexão for confirmada, toque em **Testar ida e volta**. Não há compra, descarte ou partida compartilhada nesta etapa.

O diagnóstico fecha a conexão ao sair do aplicativo ou girar a tela. É possível abrir outra conexão; isso ainda não é recuperação de partida. A validação de rádio e desempenho requer os aparelhos físicos descritos em [docs/VALIDACAO.md](docs/VALIDACAO.md).

## Documentação

- [Prompt corrigido e fragilidades](docs/PROMPT-EXECUCAO.md)
- [Regulamento versionado](docs/REGRAS.md)
- [Arquitetura, orçamento e etapas](docs/ARQUITETURA.md)
- [Resultados e roteiro de validação](docs/VALIDACAO.md)
