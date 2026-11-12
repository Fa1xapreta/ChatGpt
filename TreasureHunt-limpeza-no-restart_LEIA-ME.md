# TreasureHunt — limpeza das pedras no restart

As pedras permanecem durante a execução normal, sem expiração adicionada por esta alteração. Ao iniciar uma nova execução do GameServer, o registro de pedras antigas é zerado e elas não são restauradas. A primeira rodada nova aguarda o intervalo configurado em `TREASURE_HUNT_INTERVAL`, em segundos.

## Instalação

1. Pare o GameServer e guarde um backup das duas classes atuais.
2. Substitua no seu projeto, no pacote `net.sf.l2j.gameserver.model.entity.events.treasurehunt`:
   - `TreasureHuntActiveStore.java`
   - `TreasureHuntManager.java`
3. Recompile o GameServer pelo procedimento normal do seu projeto e instale o artefato gerado.
4. Inicie o GameServer. O console deve registrar: `TreasureHunt: previous-run stones cleared; old stones will not be restored.`
5. As pedras anteriores não devem reaparecer. Aguarde o intervalo configurado para a primeira rodada nova.

O ZIP contém fontes, não um `gameserver.jar` pronto. O patch `limpeza-no-restart.patch` mostra as alterações para revisão; seu cabeçalho usa a pasta `java`, ajuste a raiz se o seu projeto usa `src`.

## Alterações

`TreasureHuntActiveStore` deixa de reproduzir o journal da execução anterior. Abre `./data/treasurehunt-active-stones.log`, obtém o lock exclusivo e só então trunca e força a gravação do arquivo vazio. Os registros novos continuam funcionando durante essa execução, inclusive a retirada de pedras mineradas. Um arquivo ativo antigo incompleto é descartado na inicialização, conforme a nova política; se o arquivo estiver bloqueado por outro processo, não é truncado.

`TreasureHuntManager.start()` mantém a proteção contra inicialização duplicada e agenda a primeira rodada para depois do intervalo. O restante do gerenciamento e o reload permanecem como estavam. Um reload explícito continua usando a regra de agendamento que já existia, podendo antecipar a próxima rodada; ele não faz a limpeza.

## O que permanece

- `data/treasurehunt-used-locations.log`: preservado. Os pontos antigos continuam registrados como usados.
- `data/treasurehunt-sessions/`: preservada. A recuperação das ferramentas temporárias e dos equipamentos do jogador continua disponível.
- Classes `TreasureHuntSession`, `TreasureHunt`, `TreasureHuntSpawnPoint` e demais fontes: sem alterações de produção.
- Configuração de chances, regiões, NPCs e recompensas: sem alterações.

Não é necessário apagar arquivos manualmente. O reset ocorre na abertura do singleton de armazenamento, uma vez por nova execução do GameServer. Reconectar um jogador, reiniciar apenas o cliente ou executar reload não abre esse singleton novamente.

## Validação

Foi confirmada a correspondência das classes originais do repositório com os dois textos anexados (`TreasureHuntActiveStore` e `TreasureHuntSession`). O Manager alterado parte do mesmo código apresentado na conversa.

A compilação com nível Java 8 e os testes isolados passaram. Eles usam as classes reais do mod e simulações explícitas das dependências do core, não o core de produção. Cobrem:

- limpeza após uma execução de JVM encerrada abruptamente;
- partida com o evento ativado e desativado;
- espera pelo primeiro intervalo e geração normal posterior;
- reload/inicialização duplicada sem apagar pedras atuais;
- retirada de uma pedra minerada sem ressuscitá-la na mesma execução;
- preservação byte a byte do histórico de locais e dos arquivos de recuperação;
- conflito de lock sem truncar o journal do outro processo;
- recriação de journal ausente e descarte de registro antigo incompleto.

Resultado completo em `test-results.txt`. Para repetir, execute `python tests/run.py` na pasta extraída, usando Python 3 e Java com o módulo `jdk.compiler` (o teste usa Java 9+ para acionar o compilador, com fontes/alvo Java 8). Os arquivos em `tests/fixtures` e `tests/core_doubles.py` são exclusivos dos testes: **não copie esses arquivos para o GameServer**.

Ainda é necessário recompilar com o seu projeto e conferir o comportamento no servidor de teste antes de colocar em produção. Nenhum arquivo de um servidor em execução foi apagado durante este trabalho.
