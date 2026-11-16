# Auditoria das classes Java — 16/11/2026

**Há defeitos e interferências comuns capazes de afetar cliques em vários HTMLs. O problema não deve ser tratado apenas no AutoFarm.** A falha mais diretamente demonstrada é a fila que pode ficar sem trabalhador até chegar outro pacote. O limitador compartilhado entre consultas automáticas e ações manuais é outra causa demonstrável de cliques descartados.

Esta é uma auditoria do fonte enviado. Não prova qual desses mecanismos ocorreu em cada falha no servidor em execução. Nenhuma classe do pacote original foi modificada e nenhuma alteração foi instalada no servidor.

## Escopo e método

- 1.817 arquivos Java passaram por leitura sintática e triagem automatizada; 14.337 métodos foram indexados.
- Houve revisão manual **focal** em 32 arquivos centrais, sobretudo rede, HTML, flood, pools, AutoFarm, mineração e reward. Isso não significa revisão linha a linha de todos os arquivos do servidor.
- Não apareceram erros de sintaxe após tratar UTF-8 e as 50 fontes com codificação legada. Não é uma compilação completa com resolução de tipos: não foram fornecidos build, bibliotecas, mmocore, configuração efetiva, templates HTML, SQL nem um servidor executável com banco/geodata.
- Sete executáveis de regressão foram compilados e executados. Os métodos sob investigação foram extraídos do fonte; motor, transporte, relógio e scheduler foram substituídos por fixtures. Nenhum cliente Windows ou gameserver real foi iniciado.
- Os novos comandos startAutoFarm2/stopAutoFarm2 já aparecem neste arquivo enviado. A correção anterior de ON/OFF não atravessa o bloqueio comum de flood e não corrige o problema da fila. Presença no fonte não demonstra qual binário o servidor está carregando.
- `coverage.csv` registra arquivo, linhas, quantidade de métodos, codificação e hash. `command-overrides.csv` registra sobrescritas. `scan-summary.json`, `test-results.json` e `findings.json` permitem conferir a cobertura e os resultados.

## Prioridades

P1 = tratar primeiro por afetar responsividade, ciclo de execução ou controle do farm; P2 = integridade/interferência que precisa ser corrigida ou medida; P3 = caminho específico de menor alcance. Prioridade não indica que a causa foi observada em produção.

| ID | Prioridade | Achado | Evidência |
|---|---|---|---|
| F01 | P1 | Fila de pacotes pode ficar sem trabalhador até chegar outro pacote | reproduzido em teste isolado |
| F02 | P1 | Consultas automáticas e botões manuais disputam o mesmo ServerBypass | comportamento reproduzido; incidência depende da configuração real |
| F03 | P1 | Whitelist de HTML pode ser construída antes da substituição de %objectId% | mismatch reproduzido sob ordem específica; frequência real não medida |
| F04 | P1 | phantom.task.ThreadPool pré-inicia 750 threads agendadas por processador lógico | configuração confirmada no fonte; impacto real exige medição |
| F05 | P1 | Contador dos pools pode estourar e causar perda silenciosa de agendamentos | reproduzido em teste isolado |
| F06 | P2 | Todos os bypasses entram no monitor global da mineração antes de verificar o prefixo | risco de contenção confirmado no desenho; duração não medida |
| F07 | P2 | OFF do AutoFarm disputa o monitor do jogador com a rotina completa | risco de contenção confirmado no desenho; duração não medida |
| F08 | P1 | AutoFarm antigo pode ser iniciado enquanto o secundário está ativo | caminho possível confirmado por análise estática |
| F09 | P2 | Classes registram nomes de comandos que sobrescrevem outros handlers | sobrescrita reproduzida e nomes reais identificados |
| F10 | P2 | Reward diário pode entregar item mesmo se a persistência do resgate falhar | falha de propagação confirmada; integridade concorrente depende do banco |
| F11 | P2 | Banco sem prazo de checkout pode prender workers de pacotes | espera ilimitada confirmada na configuração do fonte |
| F12 | P2 | AutoGoldBar mantém thread por jogador sem saída normal por logout | ausência de controle de ciclo de vida confirmada no fonte |
| F13 | P2 | Consultas de interface podem remover proteção de spawn | efeito confirmado no código; regra desejada precisa ser confirmada |
| F14 | P3 | Troca de classe mantém um worker dormindo antes de desconectar | bloqueio de 5 segundos confirmado; caminho específico |
| F15 | P2 | Spam de skills pode fazer a proteção de pacotes descartar até botões de HTML | proteção global reproduzida; vínculo com tráfego real ainda não medido |

## Evidências detalhadas

### F01 — Fila de pacotes pode ficar sem trabalhador até chegar outro pacote

**P1 · reproduzido em teste isolado.** Um clique já recebido pode ficar pendente; outro pacote o faz executar. Afeta o cliente inteiro, incluindo HTML e AutoFarm.

execute() não agenda outro trabalhador quando _queueLock está ocupado. run() retorna ao atingir o limite de burst ou encontrar a fila vazia; finally apenas libera o lock. Há uma janela em que chega trabalho e ninguém o agenda. Há também sobra de lote sem reagendamento.

**Condição e limite:** Concorrência entre entrada e término do processamento, ou lote que atinge CLIENT_PACKET_QUEUE_MAX_BURST_SIZE. O limite declarado é 70, com fila de 80; ambos vêm do fonte, não de uma medição do processo.

**Encaminhamento:** Reverificar a fila após liberar o lock e agendar a continuação, preservando os limites de burst. A proposta PROPOSTA_fila.diff foi testada isoladamente e não foi aplicada ao servidor.

**Verificação:** QueueRegression reproduz os dois cenários com execute/run extraídos do fonte. QueueFixRegression passa nos dois e verifica cliente desconectado e rota CONNECTED.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/network/L2GameClient.java:879` — `public void execute(ReceivablePacket<L2GameClient> packet)`.
- `java/net/sf/l2j/gameserver/network/L2GameClient.java:932` — `public void run()`.

### F02 — Consultas automáticas e botões manuais disputam o mesmo ServerBypass

**P1 · comportamento reproduzido; incidência depende da configuração real.** Uma consulta de cabeçalho ou estado pode consumir a janela de tempo e fazer OFF, NPC ou outro HTML ser descartado silenciosamente.

RequestBypassToServer chama getServerBypass().tryPerformAction(_command) antes das rotas NpcHeaderInfo, voiced_ e npc_. FloodProtectorAction usa um único _nextGameTick; não separa por comando. Config usa padrão de 5 ticks, e cada tick vale 100 ms. Rejeições geralmente retornam sem informar o jogador.

**Condição e limite:** Se a configuração efetiva mantiver o padrão, a janela é 500 ms. O arquivo config/main/floodprotector.properties não veio no RAR. Classes UC anteriores mostram consultas automáticas NpcHeaderInfo e autoFarmUiStatus2; não se verificou aqui qual versão UC está em execução.

**Encaminhamento:** Medir o contador e o comando que abriu o bloqueio. Separar consultas estritamente de leitura em um limitador próprio, mantendo proteção para ações de inventário, compra e outras mutações. Não remover globalmente a proteção.

**Verificação:** FloodRegression: consulta no tick 100 bloqueia dois comandos diferentes nos ticks 101/104 e permite no tick 105.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/network/clientpackets/RequestBypassToServer.java:143` — `if (!getClient().getFloodProtectors().getServerBypass().tryPerformAction(_command))`.
- `java/net/sf/l2j/gameserver/util/FloodProtectorAction.java:59` — `public boolean tryPerformAction`.
- `java/net/sf/l2j/Config.java:2083` — `FLOOD_PROTECTOR_SERVER_BYPASS, "ServerBypass", "5"`.
- `java/net/sf/l2j/gameserver/GameTimeController.java:43` — `public static final int TICKS_PER_SECOND = 10`.

### F03 — Whitelist de HTML pode ser construída antes da substituição de %objectId%

**P1 · mismatch reproduzido sob ordem específica; frequência real não medida.** Botões npc_ ou Quest podem ter texto permitido diferente do comando que o cliente envia e ser rejeitados.

NpcHtmlMessage.runImpl() extrai os bypasses de _html. A troca de %objectId% e %playername% só acontece em writeImpl(). L2GameClient.sendPacket() entrega à conexão e depois chama runImpl(), deixando a relação entre serialização e cadastro dependente do transporte. A biblioteca mmocore não veio no RAR.

**Condição e limite:** O HTML precisa conter um desses placeholders dentro de uma action e o cadastro precisa ocorrer antes da normalização. Não se afirma que todos os templates usam placeholders nem que essa é a ordem observada em produção.

**Encaminhamento:** Preparar uma versão final e imutável do HTML antes de cadastrar os comandos e antes de enviá-lo. O texto que alimenta a whitelist deve ser exatamente o texto enviado.

**Verificação:** HtmlRegression usa scanner e validateBypass reais: cadastra npc_%objectId%_Chat 1, que não aceita npc_123_Chat 1.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/network/serverpackets/NpcHtmlMessage.java:47` — `public void runImpl()`.
- `java/net/sf/l2j/gameserver/network/serverpackets/NpcHtmlMessage.java:85` — `protected final void writeImpl()`.
- `java/net/sf/l2j/gameserver/network/L2GameClient.java:219` — `getConnection().sendPacket(gsp);`.

### F04 — phantom.task.ThreadPool pré-inicia 750 threads agendadas por processador lógico

**P1 · configuração confirmada no fonte; impacto real exige medição.** Consumo muito alto de memória nativa, descritores e escalonamento; pode prejudicar o servidor inteiro ou impedir sua inicialização.

init() cria availableProcessors() pools, cada um com ScheduledThreadPoolExecutor(750), e chama prestartAllCoreThreads() em todos. Também cria/preinicia pools instantâneos com 8 threads por processador. GameServer chama ThreadPool.init(). Este sistema coexiste com ThreadPoolManager.

**Condição e limite:** Quantidade depende do número de processadores que a JVM enxerga e de este fonte corresponder ao binário em execução. Exemplo: 8 processadores implicam 6.000 threads agendadas, mais 64 instantâneas deste subsistema, antes dos demais pools.

**Encaminhamento:** Medir threads reais e memória nativa e substituir valores fixos por limites dimensionados para a carga. Não aumentar indiscriminadamente o número de threads.

**Verificação:** Verificação estática da fórmula e do prestart. Não se criaram milhares de threads no ambiente de auditoria.

Referências no arquivo enviado:

- `java/phantom/task/ThreadPool.java:44` — `new ScheduledThreadPoolExecutor(750)`.
- `java/phantom/task/ThreadPool.java:57` — `threadPool.prestartAllCoreThreads()`.
- `java/net/sf/l2j/gameserver/GameServer.java:229` — `ThreadPool.init();`.

### F05 — Contador dos pools pode estourar e causar perda silenciosa de agendamentos

**P1 · reproduzido em teste isolado.** Após suficiente uso, a seleção pode produzir índice negativo; schedule/scheduleAtFixedRate retornam null e execute ignora a falha.

getPool usa _threadPoolRandomizer++ % threadPools.length. Após Integer.MAX_VALUE o contador fica negativo. Os wrappers capturam Exception e retornam null ou não fazem nada.

**Condição e limite:** Depende de ultrapassar o limite do int e de haver mais de um pool; não foi medida a posição atual do contador. Com N=3 o primeiro valor negativo já falha. Uma chamada falha também incrementa o contador, logo não é correto dizer que ele fica permanentemente congelado em um índice.

**Encaminhamento:** Usar seleção com índice não negativo e contador atômico; registrar ou propagar falhas de agendamento em vez de engoli-las.

**Verificação:** PoolOverflowRegression usa o getPool original, contador próximo do limite e 3 pools; reproduz ArrayIndexOutOfBoundsException sem criar threads.

Referências no arquivo enviado:

- `java/phantom/task/ThreadPool.java:197` — `return threadPools[_threadPoolRandomizer++ % threadPools.length];`.
- `java/phantom/task/ThreadPool.java:104` — `public static ScheduledFuture<?> scheduleAtFixedRate`.

### F06 — Todos os bypasses entram no monitor global da mineração antes de verificar o prefixo

**P2 · risco de contenção confirmado no desenho; duração não medida.** Um jogador pode esperar por operações de mineração de outros jogadores mesmo clicando em um HTML sem relação com mineração.

RequestBypassToServer chama MiningInteractionManager.handle para toda solicitação antes do flood. handle é synchronized e verifica o prefixo dentro do monitor. tick() também é synchronized e percorre tentativas. O caminho de início/fim alcança TreasureHuntSession, manipulação de inventário e acesso a arquivos de recuperação.

**Condição e limite:** Exige mineração em andamento e/ou custo relevante dentro dessas operações. Não há prova de deadlock observado.

**Encaminhamento:** Fazer a seleção barata do prefixo antes do monitor global e reduzir o trabalho protegido ao estado necessário. Medir o tempo de espera e a ordem dos locks antes de mudar a sincronização interna.

**Verificação:** Análise de encadeamento de chamadas e monitores; sem mineração real nesta auditoria.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/network/clientpackets/RequestBypassToServer.java:135` — `if (MiningInteractionManager.getInstance().handle(`.
- `java/net/sf/l2j/gameserver/model/entity/events/treasurehunt/MiningInteractionManager.java:251` — `public synchronized boolean handle(`.
- `java/net/sf/l2j/gameserver/model/entity/events/treasurehunt/MiningInteractionManager.java:422` — `private synchronized void tick()`.
- `java/net/sf/l2j/gameserver/model/entity/events/treasurehunt/TreasureHuntSession.java:195` — `private void saveJournal()`.

### F07 — OFF do AutoFarm disputa o monitor do jogador com a rotina completa

**P2 · risco de contenção confirmado no desenho; duração não medida.** O comando de parada pode aguardar cálculos de alvo, geodata ou outras ações feitas pela rotina, bloqueando também a fila de pacotes desse jogador.

onUpdate mantém synchronized(player) durante executeRoutine(). stopFarm entra no mesmo monitor. A rotina percorre candidatos e usa PathFinding. A iteração de todos os jogadores ocorre em um único Runnable, sem paralelismo entre as entradas desse mapa.

**Condição e limite:** Um cálculo ou chamada dentro da rotina precisa estar lento ou esperando outro lock. O código sozinho não comprova quanto tempo isso ocorre no servidor.

**Encaminhamento:** Medir duração por jogador e espera do monitor; reduzir a região crítica com cancelamento explícito e verificação de rotina obsoleta antes de ações. Não retirar locks cegamente.

**Verificação:** Análise de controle e encadeamento; não foi executada a geodata real.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/instancemanager/autofarmsecondary/AutofarmSecondaryManager.java:49` — `routine.executeRoutine();`.
- `java/net/sf/l2j/gameserver/instancemanager/autofarmsecondary/AutofarmSecondaryManager.java:163` — `public void stopFarm(L2PcInstance player)`.
- `java/net/sf/l2j/gameserver/instancemanager/autofarmsecondary/AutofarmSecondaryPlayerRoutine.java:418` — `getKnownMonstersInRadius(`.

### F08 — AutoFarm antigo pode ser iniciado enquanto o secundário está ativo

**P1 · caminho possível confirmado por análise estática.** Desligar um motor pode deixar o outro controlando o personagem. Isso pode parecer um OFF que não funciona.

O registro mantém VoicedAutoFarm e VoicedAutoFarmSecondary. O comando enableAutoFarm continua chamando o manager antigo. O início do secundário interrompe o antigo, mas startFarm do antigo não faz a verificação inversa. stopAutoFarm2 para apenas o secundário. UseNewAutoFarm altera a abertura do painel, não elimina a rota antiga.

**Condição e limite:** É necessário acionar a rota antiga depois de ligar o secundário, por macro, comando legado ou outro caminho. Não foi confirmado que isso aconteceu com o personagem do usuário.

**Encaminhamento:** Definir exclusão mútua simétrica ou desativar/redirecionar as rotas legadas quando o modo novo estiver selecionado. Conferir ambos os mapas e flags ao diagnosticar a parada.

**Verificação:** Comparação dos caminhos de registro, start e stop dos dois managers.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/handler/VoicedCommandHandler.java:88` — `registerHandler(new VoicedAutoFarm());`.
- `java/net/sf/l2j/gameserver/handler/voicedcommandhandlers/VoicedAutoFarm.java:185` — `if (command.startsWith("enableAutoFarm"))`.
- `java/net/sf/l2j/gameserver/instancemanager/autofarm/AutofarmManager.java:79` — `public void startFarm(L2PcInstance player)`.
- `java/net/sf/l2j/gameserver/instancemanager/autofarmsecondary/AutofarmSecondaryManager.java:124` — `AutofarmManager.INSTANCE.stopFarm(player);`.

### F09 — Classes registram nomes de comandos que sobrescrevem outros handlers

**P2 · sobrescrita reproduzida e nomes reais identificados.** O código esperado por um botão pode nunca ser chamado, ou pode abrir o painel de outro módulo.

VoicedCommandHandler armazena por hash e faz put sem verificar colisão/sobrescrita. VoicedCharInfo substitui status/inventory/skills/rank de VoicedStatus. AutoFarmSecondary substitui enableAntiKs/enableSummonAttack do antigo. VoicedAutoGB substitui setAutoGoldBar de VoicedMenu. VoicedRanking é registrado duas vezes, fazendo rankingpk/pvp voltar ao handler de ranking depois dos handlers dedicados.

**Condição e limite:** Algumas inscrições dependem de Config.ALLOW_MOD_MENU e outras flags. Os valores reais não vieram. Algumas substituições podem ser intencionais e precisam ser documentadas. Não se encontrou colisão de hash entre strings distintas nos comandos estáticos resolvidos; há um handler com nomes dinâmicos dependentes das zonas.

**Encaminhamento:** Definir um dono por comando, dar nomes específicos aos módulos e avisar ao registrar duplicatas. Usar String como chave elimina colisões de hash; isso não resolve duplicatas do mesmo texto sem decidir seu dono.

**Verificação:** RegistryRegression reproduz a semântica de sobrescrita; command-overrides.csv lista todas as ocorrências e sua ordem.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/handler/VoicedCommandHandler.java:108` — `_datatable.put(ids[i].hashCode(), handler);`.
- `java/net/sf/l2j/gameserver/handler/VoicedCommandHandler.java:89` — `registerHandler(new VoicedAutoFarmSecondary());`.
- `java/net/sf/l2j/gameserver/handler/VoicedCommandHandler.java:76` — `registerHandler(new VoicedRanking());`.

### F10 — Reward diário pode entregar item mesmo se a persistência do resgate falhar

**P2 · falha de propagação confirmada; integridade concorrente depende do banco.** Banco e inventário podem ficar divergentes; uma falha de gravação não impede a entrega. Resgates em contas distintas compartilhando IP/HWID também precisam de atomicidade.

claimDailyReward chama updateLastReward/insertNewParentOfPlayerIPHWID e depois addItem. Os helpers de gravação capturam Exception, imprimem e retornam void. Não retornam sucesso nem obrigam o chamador a interromper a entrega. A sequência de leituras e gravações usa conexões separadas, sem transação abrangente visível.

**Condição e limite:** Exige falha de banco, contenda ou pedidos concorrentes. Os índices/constraints da tabela reward_manager não vieram; não se afirma que uma duplicação ocorreu.

**Encaminhamento:** Fazer a gravação condicional/atômica e retornar resultado verificável antes de entregar. Definir recuperação para falha de entrega após reserva; revisar transação, idempotência e constraints no SQL.

**Verificação:** Análise de fluxo e tratamento de exceções; não se executou banco real nem se testou duplicação de itens.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/model/actor/instance/NewDailyRewardManager.java:203` — `public void claimDailyReward(`.
- `java/net/sf/l2j/gameserver/model/actor/instance/NewDailyRewardManager.java:379` — `public void updateLastReward(`.
- `java/net/sf/l2j/gameserver/model/actor/instance/NewDailyRewardManager.java:483` — `public void insertNewParentOfPlayerIPHWID(`.

### F11 — Banco sem prazo de checkout pode prender workers de pacotes

**P2 · espera ilimitada confirmada na configuração do fonte.** Um HTML que consulta banco pode ocupar indefinidamente um worker e atrasar outros pedidos, inclusive de jogadores diferentes.

L2DatabaseFactory usa setCheckoutTimeout(0), retentativas ilimitadas e getConnection em while(con == null). Há consultas síncronas em RequestBypassToServer e vários voiced handlers. O pool de pacotes usa fila sem limite.

**Condição e limite:** O banco ou o pool de conexões precisa estar lento/indisponível/exaurido. Não há prova de indisponibilidade atual.

**Encaminhamento:** Definir prazos finitos, medir tempo de obtenção e consulta e responder claramente à falha. Tratar consultas caras fora de workers críticos com limites de concorrência.

**Verificação:** Análise de configuração e caminhos síncronos; sem medição de MySQL.

Referências no arquivo enviado:

- `java/net/sf/l2j/L2DatabaseFactory.java:50` — `_source.setCheckoutTimeout(0);`.
- `java/net/sf/l2j/L2DatabaseFactory.java:143` — `while (con == null)`.
- `java/net/sf/l2j/gameserver/ThreadPoolManager.java:119` — `_generalPacketsThreadPool = new ThreadPoolExecutor`.

### F12 — AutoGoldBar mantém thread por jogador sem saída normal por logout

**P2 · ausência de controle de ciclo de vida confirmada no fonte.** Threads e referências a personagens podem permanecer após a saída; erros e religamento podem deixar flag e mapa divergentes.

Autogb.run usa while(true), com sleep(2000), sem verificar online nem isAutoGB. O mapa static HashMap é acessado pela thread do jogador e pelos workers sem sincronização. A pesquisa global encontrou userGB apenas nessa classe, sem cancelamento no logout. finally remove a chave sem verificar se ainda aponta para aquela thread, podendo remover uma geração nova.

**Condição e limite:** É necessário ativar AutoGB; a duração e o número de threads reais não foram medidos. A espera de 2 s acontece em uma thread própria, não é um sleep direto no worker de HTML.

**Encaminhamento:** Usar tarefa cancelável gerenciada por objectId, verificar sessão/online e remover condicionalmente a instância correta. Integrar logout e manter flag e registro consistentes.

**Verificação:** Análise de ciclo de vida e pesquisa de referências no conjunto completo.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/handler/voicedcommandhandlers/VoicedAutoGB.java:21` — `static final HashMap<String, Thread> userGB`.
- `java/net/sf/l2j/gameserver/handler/voicedcommandhandlers/VoicedAutoGB.java:78` — `while (true)`.
- `java/net/sf/l2j/gameserver/handler/voicedcommandhandlers/VoicedAutoGB.java:101` — `userGB.remove(activeChar.toString());`.

### F13 — Consultas de interface podem remover proteção de spawn

**P2 · efeito confirmado no código; regra desejada precisa ser confirmada.** Uma consulta automática que deveria apenas ler estado pode retirar proteção do personagem ao entrar/selecionar alvo.

RequestBypassToServer herda triggersOnActionRequest=true. L2GameClientPacket.run chama actor.onActionRequest após runImpl, inclusive se runImpl retornou cedo por flood. L2PcInstance.onActionRequest remove a proteção. As consultas NpcHeaderInfo e autoFarmUiStatus2 passam por essa classe.

**Condição e limite:** O personagem precisa estar protegido e a UC enviar uma dessas consultas. A regra de gameplay pode considerar qualquer bypass uma ação; confirmar intenção antes de alterar.

**Encaminhamento:** Separar consultas de leitura das ações que devem cancelar a proteção, com uma lista restrita e testes de gameplay.

**Verificação:** Análise do wrapper de execução e do método do personagem; sem sessão real.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/network/clientpackets/L2GameClientPacket.java:93` — `if (triggersOnActionRequest())`.
- `java/net/sf/l2j/gameserver/network/clientpackets/L2GameClientPacket.java:132` — `protected boolean triggersOnActionRequest()`.
- `java/net/sf/l2j/gameserver/model/actor/instance/L2PcInstance.java:10784` — `public void onActionRequest()`.

### F14 — Troca de classe mantém um worker dormindo antes de desconectar

**P3 · bloqueio de 5 segundos confirmado; caminho específico.** Durante esse caminho específico, o jogador não processa novos pacotes e um worker fica ocupado por cinco segundos.

RequestBypassToServer.Finish chama waitSecs(5), que usa Thread.sleep, e depois closeNow. É uma desconexão deliberada após troca de classe.

**Condição e limite:** Somente ao executar essa troca de classe; não explica HTMLs genéricos isoladamente.

**Encaminhamento:** Agendar a desconexão sem dormir no worker, preservando o bloqueio necessário da sessão durante a troca.

**Verificação:** Análise direta da chamada e do helper.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/network/clientpackets/RequestBypassToServer.java:2688` — `waitSecs(5);`.
- `java/net/sf/l2j/gameserver/network/clientpackets/RequestBypassToServer.java:2760` — `Thread.sleep(i * 1000);`.

### F15 — Spam de skills pode fazer a proteção de pacotes descartar até botões de HTML

**P2 · proteção global reproduzida; vínculo com tráfego real ainda não medido.** Há um segundo bloqueio, anterior ao ServerBypass: quando o limite global de pacotes é atingido, o tipo de pacote ainda nem foi roteado. Um OFF pode não alcançar RequestBypassToServer.

L2GamePacketHandler chama client.dropPacket antes de ler o opcode. ClientStats detecta excesso de pacotes de qualquer tipo. Os limites declarados são 160 por segundo e média de 120. No Interface.u enviado anteriormente, o bytecode de AutoSkillWnd.xxwemakajjas solicita timer de 10 ms; sua rotina tenta skills/ações em slots e usa a rede do mesmo cliente.

**Condição e limite:** A taxa real depende de FPS, guardas da UC, slots configurados e outros pacotes. Timer de 10 ms não prova 100 pacotes efetivos por segundo, nem prova que o limite foi atingido no servidor do usuário.

**Encaminhamento:** Contar pacotes por tipo e observar ClientStats enquanto o farm está ativo. Reduzir tentativas redundantes e respeitar cooldown/estado na UC. Não aumentar ou desativar a proteção global sem medição.

**Verificação:** StatsRegression compila ClientStats e provoca um burst controlado; comprova que o bloqueio genérico fica ativo. A correlação com a rotina UC ainda requer captura de métricas reais.

Referências no arquivo enviado:

- `java/net/sf/l2j/gameserver/network/L2GamePacketHandler.java:47` — `if (client.dropPacket())`.
- `java/net/sf/l2j/gameserver/network/ClientStats.java:180` — `private final synchronized boolean countPacket()`.
- `java/net/sf/l2j/Config.java:1812` — `CLIENT_PACKET_QUEUE_MAX_PACKETS_PER_SECOND = 160`.

## A whitelist não explica todos os comandos

`NpcHtmlMessage.runImpl` também chama `clearBypass` sempre que chega outro HTML. Em teste, isso invalida o comando da página anterior e reabri-la o cadastra novamente. **Essa substituição de contexto é normalmente necessária à validação**, não deve ser eliminada globalmente. Torna-se suspeita se páginas concorrentes ou outra janela atualizam o contexto sem corresponder à página em que o jogador clica.

Além disso, neste RequestBypassToServer, a chamada a `validateBypass` aparece nas rotas `npc_` e `Quest`. A rota `voiced_` é encaminhada antes e não passa por essa validação. Portanto, cache/whitelist não é explicação universal para o AutoFarm; o flood e a fila são mecanismos comuns às rotas.

## Registro de comandos: duplicatas não são todas defeitos

Foram encontradas substituições reais do mesmo nome, e não colisões demonstradas entre hashes de strings distintas nos nomes estáticos resolvidos. Algumas inscrições são condicionais. `VoicedVotePvpZone` gera nomes a partir das zonas; esses nomes dependem dos dados ausentes e não foram totalmente resolvidos.

`VoicedStatus` e `VoicedCharInfo` possuem funcionalidade sobreposta; isso por si só não comprova um clique perdido. Há, porém, diferença concreta no método de ranking: o `VoicedStatus.showRankHtm` trata mais faixas, enquanto o `VoicedCharInfo.showRankHtm` enviado só substitui os placeholders para Unranked e Iron I. Como o segundo handler assume `rank`, faixas acima podem perder a renderização esperada se o template usar os mesmos placeholders. Os HTMLs não vieram para conferir o resultado visual.

## O que não foi concluído

Não se comprovou perda de pacote na rede, defeito no cliente, corrupção de XDAT, deadlock ocorrido em produção, indisponibilidade atual do MySQL, duplicação efetiva de recompensa ou correspondência entre este fonte e o binário do gameserver. Tampouco a presença de dois pools ou de comandos duplicados basta, sozinha, para provar conflito observado.

## Ordem recomendada de trabalho

1. Obter rastreamento curto por personagem para distinguir fila, flood, validação e handler lento; conferir a configuração efetiva do flood.
2. Validar a proposta mínima de continuação da fila em uma instância de teste. Ela mantém os limites e não desativa proteções.
3. Medir o número real de threads e corrigir dimensionamento/seleção do pool. Tratar falhas silenciosas de agendamento.
4. Separar limitação das consultas automáticas estritamente de leitura da limitação das ações manuais. Não liberar indiscriminadamente compras, rewards ou manipulação de itens.
5. Normalizar HTML antes de validar/enviar; observar os contextos de página concorrentes. Não retirar validateBypass.
6. Garantir exclusão mútua dos dois motores de AutoFarm, corrigir donos de comandos e medir contenção em mineração/rotina.
7. Tratar persistência do reward, ciclo de vida do AutoGB e prazos do banco em mudanças separadas, com testes próprios.

`PROPOSTA_fila.diff` é uma proposta para revisão, não um instalador nem uma classe já compilada do servidor. O teste isolado passa, mas o build completo e o teste integrado são necessários antes de produção.
