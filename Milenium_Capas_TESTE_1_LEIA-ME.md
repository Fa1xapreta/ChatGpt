# Milenium — troca de capas, versão de teste 1

**Teste em uma cópia do cliente e em um servidor de testes.** Este pacote foi conferido por análise estática. O motor nativo de referência não foi executado nesta sessão. Não há garantia de funcionamento na Engine de destino nem de aceitação da licença. Não é uma versão final validada no jogo.

## O que está pronto

- dsetup.dll com alteração localizada de carregamento: retira a inicialização de dsetup_base.dll e encaminha a chamada DirectX para dsetup_ms.dll, como o motor antigo já fazia. CustomWndSchuster permanece carregado.
- armorgrp.dat e itemname-e.dat do seu cliente com **25 capas novas** acrescentadas. Os registros antigos foram preservados byte a byte. Mantidos os 42 itens antigos para não invalidar inventários na fase de teste; a conversão definitiva deles não faz parte desta versão.
- Ícones: CapasClassicIcons.utx e iconfox-v3.utx da referência, com os 25 objetos usados conferidos.
- XML para os 25 itens novos, slot back, mesmo bônus de suas capas antigas: +5% HP/CP, restrição de Olympiad e sem enchant.
- Fonte CloakSystemManager para substituir o emissor antigo sem quebrar suas chamadas existentes: reload/getRecordCount/sendConfig/getInstance permanecem. Não envia mais os controles do motor antigo.
- Instalador com conferência SHA-256, backup, restauração e retirada dos recursos antigos apenas no cliente indicado.

## Recursos grandes necessários no seu computador

O motor e as malhas permanecem nos arquivos originais do modelo que você enviou. Extraia a **system do modelo de referência** para uma pasta separada. O instalador copia dela somente os 107 arquivos listados em recursos_system_referencia.json, verificando cada hash. O ZIP deste kit não repete 1,6 GB de recursos já presentes nesse modelo.

Não aponte SystemModelo para a sua system atual. Não copie a system inteira do modelo por cima da sua. A pasta precisa conter CapasClassic.dll, d3d9.dll e as famílias CC/CapasClassic da referência.

Nenhum pacote/controlador LucDev é copiado. L2.ini, Engine.dll, NWindow.dll, interface.u, interface.xdat e os arquivos de montarias não são substituídos. Não se afirma independência completa de LucDev sem teste de execução. A configuração das capas usa o nome original CapasClassic por compatibilidade; não foi feita renomeação binária ou alteração de licença.

Seu d3d9.dll de efeitos Milenium será substituído pelo do modelo, conforme sua decisão de manter somente capas e montarias. O motor antigo e os efeitos de drop nele incorporados deixam de ser carregados pelo caminho identificado. O pacote Animations/DynastyCloak.ukx, se existir no destino, é guardado no backup e retirado para usar a versão DynastyCloak.u da system de referência; nenhuma montaria é removida.

## Preparar o cliente

1. Extraia este kit inteiro e mantenha seus arquivos juntos.
2. Feche o jogo. Abra PowerShell na pasta do kit.
3. Execute, ajustando os caminhos:

```powershell
& .\INSTALAR_TESTE.ps1 -ClienteTeste 'C:\L2_Milenium_TESTE' -SystemModelo 'C:\Modelo\system'
```

ClienteTeste é a pasta que contém system, Animations e SysTextures. O instalador verifica se os arquivos principais são da versão enviada. Se encontrar hash diferente, ele para; não force a substituição. Garanta espaço para aproximadamente 1,7 GB de recursos novos e backup dos arquivos existentes.

O script não altera seu servidor nem procura credenciais. Não desativa proteções ou licença do modelo. Caso PowerShell bloqueie scripts, examine o arquivo e use sua política habitual; não é necessário alterar a política global do Windows para executar este kit.

## Preparar o servidor de testes

1. Faça backup da sua classe atual CloakSystemManager.java e do XML de capas.
2. Acrescente servidor/MileniumCloaks_313xx.xml à mesma pasta de itens onde está seu Cloak.xml. **Mantenha o XML antigo durante o teste**, para os itens existentes continuarem válidos. Os 25 IDs novos não colidem com os IDs presentes no itemname do cliente enviado; confira também os XMLs completos do servidor, que não foram fornecidos integralmente.
3. Substitua somente a classe net.sf.l2j.gameserver.model.cloak.CloakSystemManager pela fonte em servidor/java, compile e instale seu gameserver pelo procedimento usual. O kit não contém gameserver compilado. Não substitua classes Player/CharInfo/UserInfo por arquivos de outra base.
4. ExCloakSystem pode permanecer no projeto, mas o novo gerenciador não o envia. Se houver outros pontos de envio direto no seu código, eles precisam ser removidos; o projeto completo do servidor não foi recebido para conferir todos os chamadores.
5. Reinicie o servidor de testes. Crie os itens pelos IDs de CAPAS_IDS.csv usando seu comando GM habitual. Por exemplo, comece por **31382 (Draconic)** ou **31383 (Vesper)**.

O objetivo é confirmar primeiro o motor de referência com seus próprios IDs e nomes de modelos. Os IDs antigos 311xx não foram automaticamente redirecionados para os 313xx: 42 capas não têm correspondência um a um com os 25 visuais mapeados. Nenhum item foi apagado do banco.

## Conferir no jogo

Teste equipar/remover, parado, caminhada/corrida, giro, sentado, morte/revive e montaria. Veja o próprio personagem e outro jogador. Repita para raça/sexo, incluindo humana fighter e humanos magos. Confira inventário/autoequip e montarias. A configuração original do modelo inclui fx_remotos=0; efeitos vistos por observadores podem diferir.

Se o jogo falhar ao abrir, não troque a Engine para tentar corrigir: restaure o backup e envie a mensagem exata. A troca integral da Engine poderia afetar outros mods. A inicialização/licença de CapasClassic ainda não foi validada neste cliente.

## Restaurar

O instalador cria Milenium_Capas_Backup_DATA_HORA na pasta do cliente. Abra essa pasta e execute:

```powershell
& .\RESTAURAR.ps1 -Cliente 'C:\L2_Milenium_TESTE'
```

Isso restaura os arquivos anteriores e remove os novos arquivos copiados pelo instalador. No servidor, restaure sua classe Java anterior, recompile e retire somente o XML novo. Não apague os itens antigos.

## Limitações desta versão

Esta é uma integração candidata para teste, não confirmação de funcionamento. A compatibilidade da Engine, a licença e eventuais carregamentos dinâmicos permanecem pendentes. Não há código-fonte do motor de referência no kit. As texturas não foram concentradas num único UTX; foram mantidos os pacotes originais para preservar suas referências. Os avisos de autoria/licença permanecem intactos.
