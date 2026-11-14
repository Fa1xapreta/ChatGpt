# Milenium — capas, catálogo 70 e integração gráfica TESTE 5

[Baixar o kit em um único ZIP](https://github.com/Fa1xapreta/ChatGpt/archive/refs/heads/kit-capas-integrado-70.zip)

1. Feche o jogo. Extraia o ZIP inteiro na Área de Trabalho, **fora da pasta do jogo**.
2. Abra a pasta extraída e dê dois cliques em **INSTALAR.bat**. Aceite a elevação do Windows e aguarde a conclusão.
3. Dê dois cliques em **INSTALAR_XML.bat** e escolha a pasta **items** do gameserver. Ele guarda backup, substitui os cadastros antigos destes IDs e verifica duplicatas. Reinicie o gameserver pelo seu procedimento habitual.

Não copie DLLs/DATs isoladamente. Todos os recursos necessários para os itens habilitados estão neste ZIP. O cliente padrão é `C:\Program Files (x86)\LINEAGE ROYAL NOVO`; se não encontrar, aparece uma janela para selecionar.

Se já compilou o gerenciador Java do kit anterior, mantenha-o: a classe não mudou. Para instalação inicial, leia `servidor/LEIA-ME.txt`.

## O que está incluído

- **70 itens** em `servidor/itens/Milenium_Capas_70.xml`, `armorgrp.dat` e `itemname-e.dat` compatíveis entre si.
- **44 capas com malhas para os 14 corpos e 26 de corpos específicos**. Confira ID, corpo e situação em `CATALOGO_CAPAS.csv`. Os 45 cadastros acrescentados ao kit anterior ainda não foram testados dentro do jogo.
- Os 1.467 registros de armadura e 10.255 nomes originais foram preservados byte a byte; os novos totais são 1.537 e 10.325. Os 42 itens antigos permanecem para preservar os inventários. Não há conversão de itens no banco.
- 100 pacotes de capas e demais dependências do modelo. Para nove estilos Festival, o instalador cria 126 aliases de malha nos 14 pacotes correspondentes, conservando os dados de geometria originais.
- Nos três corpos que apontavam para roupa genérica no item 31390, foram usadas as malhas correspondentes já presentes no modelo. Esta correção também precisa de teste no jogo.
- Bônus do XML enviado: +5% HP/CP, slot back, sem enchant, restrição de Olympiad. Algumas famílias usam o ícone genérico válido do modelo.

**Não é o catálogo inteiro de 91 entradas funcionando:** 21 entradas identificadas no material não têm todas as dependências necessárias e estão documentadas em `catalogo/pendentes.json`. Não foram habilitadas com malhas inventadas. As capas de corpos específicos não têm fallback para corpos sem malha.

## Cores, nitidez e efeito do jogador

O kit anterior substituiu o `d3d9.dll` do MileniumFX pelo proxy das capas. Isso explica a perda do tratamento de imagem. Este kit inclui o MileniumFX original, sem alteração, como `MileniumGraphics.dll`, e uma **ponte gráfica nova** em `d3d9.dll`. Ela chama a sincronização das capas antes do EndScene, na mesma posição do proxy de referência, e encaminha a renderização ao MileniumFX.

O núcleo original do contorno do jogador (`L2modExCore.dll`) foi preservado. O `CliExt.dll` muda somente o nome do carregador, para aguardar a ativação das capas antes de carregar esse núcleo. O núcleo já reconhece o encadeamento de CapasClassic. O carregador aguarda a etapa ativa das capas por até cinco minutos; se não houver confirmação, registra isso no log e tenta carregar o núcleo original mesmo assim. Os logs ficam na system: Milenium_Aura_Integracao.log e Milenium_Capas_Integracao.log.

**Esta integração foi compilada para Windows x86 e testada com interfaces simuladas; não foi executada no cliente Windows. A recuperação visual precisa ser confirmada no jogo.** Não se afirma que cores/contorno já estejam corrigidos em execução. O caminho Direct3DCreate9Ex é apenas encaminhado; o cliente recebido usa Direct3DCreate9.

Seu `MileniumFX.ini` e seu `CapasClassic.ini` existentes são mantidos. Se faltarem, o kit fornece os originais com os ajustes de montaria já aprovados (stiffness 0.45/0.60, wind cap 24). No preset original, Ctrl+F10 alterna o efeito e Ctrl+F11 troca o preset; se personalizou o INI, prevalecem suas teclas. O D3DDrv foi devolvido à versão original do cliente: a DLL da aura verifica esse arquivo e recusa o driver do modelo usado no TESTE 3. Quem já instalou o kit deve usar a [restauração A1](https://github.com/Fa1xapreta/ChatGpt/blob/main/Restaurar_Aura_Milenium_A1/README.md), preservando o ajuste TESTE 7. Após entrar no personagem, F10 sem Ctrl alterna a aura. Engine, NWindow, interface, autoequip e arquivos de montarias não são substituídos.

**O movimento de abrir/cair suavemente ao equipar ainda não foi implementado.** Não foi encontrado um ajuste comprovado que o reproduza; não alteramos resets para simular esse comportamento.

## Retorno seguro

O instalador cria `Milenium_Integrado_Backup_DATA_HORA` na pasta do jogo:

- **RESTAURAR.bat** devolve o estado encontrado antes deste kit. Para pacotes apenas acrescidos de aliases, restaura cabeçalho/comprimento e confere o hash completo.
- **MODO_COMPATIVEL.bat** mantém catálogo/recursos, mas volta ao proxy original de capas se a ponte nova impedir o jogo de abrir. Nesse modo, cores/nitidez do MileniumFX podem ficar ausentes novamente.

Para desfazer apenas os XMLs, execute `servidor/DESFAZER_XML.bat` nesta mesma pasta extraída. Não apague a pasta do kit nem os backups enquanto estiver testando. XMLs personalizados conflitantes são recusados antes da troca. Não apague seu antigo Cloak.xml nem arquivos de montaria manualmente.

O motor de terceiros e suas verificações de licença não foram alterados. O controlador externo do modelo não é instalado. Os pacotes permanecem separados para preservar suas referências; não foram reunidos em um único UTX. A autoria e as licenças originais dos componentes foram conservadas.
