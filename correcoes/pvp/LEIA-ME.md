# Kill streak — somente os anúncios novos, sem SYS antigo

Remove de `QuakeSystem()` a chamada que ainda emitia `Broadcast.QuakeAnnounce` nos marcos antigos. Esses eram os avisos vermelhos `SYS: ... is Dominating`, `... is Rampage` e demais textos de `QuakeSystemValues`.

Os anúncios novos de 10, 15 e 20 kills permanecem. A contagem e o reset na morte não foram alterados nesta revisão. Os marcos configurados ainda podem acionar a flag/recompensa existente; somente sua mensagem antiga foi removida.

## Instalação

1. Faça backup do `L2PcInstance.java` atual.
2. Substitua a classe em `net.sf.l2j.gameserver.model.actor.instance` pelo arquivo completo em `src` deste ZIP; ou aplique/revise `remover-SYS.patch`, que contém apenas a pequena diferença em relação à versão de anúncios anterior.
3. Recompile o GameServer e instale o artefato gerado.
4. Mantenha `AllowQuakeSystem = True`. Esse sistema continua chamando os nossos anúncios.
5. Teste os marcos antigos e os novos: não deve mais sair Dominating/Rampage pelo QuakeSystem, e deve sair o anúncio novo aos 10/15/20.

Não é necessário alterar `QuakeSystemValues` para esconder seus textos. `AllowQuakeReward` e os IDs de recompensa continuam sendo respeitados. Esta revisão não remove anúncios de outros sistemas, como SELF, PvP geral, entrada de herói ou War Legend.

O arquivo completo parte da classe enviada e adaptada anteriormente nesta conversa. Se sua classe ganhou outras alterações depois, prefira aplicar o patch para mantê-las. A L2Playable de mensagens SELF não precisa ser substituída.

## Validação

Compilação dos métodos editados reais com nível Java 8 e testes isolados passaram: nenhum anúncio Quake antigo é disparado; os novos marcos, destinatários, canal, reset na morte e recompensas configuradas permanecem funcionando. Resultado em `test-results.txt`; execução com `python tests/run.py` usando Python 3 e Java com `jdk.compiler`.

Não foi feita compilação completa com todas as dependências do core, nem teste no GameServer em execução. O ZIP contém fonte, não um JAR pronto.
