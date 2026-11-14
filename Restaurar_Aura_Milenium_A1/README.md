# Restauração da aura Milenium — A1

[Baixar a restauração](Milenium_Restaurar_Aura_A1.zip).

1. Feche completamente o jogo, extraia o ZIP e execute **RESTAURAR_AURA.bat**.
2. Abra o jogo, entre no personagem e pressione **F10 sem Ctrl** para ligar a aura.
3. Confira a aura, as capas e a montaria. O Fighter.ukx e o ajuste de corrida que estiver instalado são preservados.

## Bloqueio encontrado

A DLL original `L2modExCore.dll` compara o tamanho e o FNV32 de `D3DDrv.dll` antes de instalar os hooks. Ela exige tamanho `0x138014` e FNV32 `0x167faa65`, que coincidem exatamente com o driver original enviado pelo usuário. O driver do modelo instalado nos TESTES 3/5 tem o mesmo tamanho, mas FNV32 `0xc2b0e85b`; o caminho de rejeição escreve **“D3DDrv diferente da system auditada. Efeito desativado. FNV32”** e encerra a inicialização.

A restauração substitui apenas `system/D3DDrv.dll` pelo original recebido, sem alterar a verificação. Fighter.ukx do TESTE 7, DLL de capas, INIs, d3d9/MileniumGraphics, carregadores e dados do servidor permanecem iguais. O bloqueio do driver foi demonstrado por análise estática; ainda é necessário confirmar o retorno visual no cliente Windows. Outros erros de inicialização poderão aparecer depois dessa etapa.

## Backup e diagnóstico

O driver anterior é guardado em `Milenium_Aura_Backup_A1`. **DESFAZER.bat** devolve esse driver; a aura poderá ficar desativada novamente.

Se continuar sem aura após entrar no personagem e tentar F10, execute **VERIFICAR_AURA.bat** com o jogo aberto e envie `Resultado_Aura_A1.txt`. Ele registra apenas hashes, módulos selecionados e trechos dos logs do próprio efeito e da integração. Não injeta código nem escreve em memória do jogo.

O instalador confere o payload e a versão instalada, cria backup antes da troca e restaura o arquivo anterior em caso de falha de gravação. O verificador teve sua leitura conferida em fixture. Os testes usam PowerShell 7 Linux com bytes reais dos dois drivers e interface/elevação Windows simuladas. Não houve execução do jogo neste ambiente.

Os scripts Python de auditoria e testes dependem das pastas de análise originais e não são necessários para instalar. [AUDITORIA.json](AUDITORIA.json) contém hashes, valores e o trecho de código que rejeita o driver do modelo.
