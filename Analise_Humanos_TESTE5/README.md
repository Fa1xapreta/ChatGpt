# Comparação dos humanos fighter — TESTE 5 e modelo

O usuário confirmou uso do TESTE 5 e relatou a passagem dos pés pelo tecido em todas as capas dos humanos fighter, enquanto outras raças funcionam bem. O vídeo da Freya 31394 confirma a interseção durante a passada. Esta análise usa os arquivos originalmente enviados e o conteúdo publicado do TESTE 5; não é uma leitura atual do cliente Windows.

## Verificações concluídas

- Foram conferidas **109 referências de capa/corpo** para MFighter e FFighter. Os pacotes-base do TESTE 5 são iguais aos do modelo por SHA-256. Nos nove estilos com aliases, as malhas usam os mesmos dados serializados, com a adaptação de nomes já documentada. A Freya não depende desses aliases.
- Nos **25 itens originalmente presentes no DAT do modelo**, os quatro blocos MTX correspondentes aos dois corpos humanos coincidem entre modelo e TESTE 5.
- `CapasClassic.dll` do TESTE 5 é idêntica à do modelo: `20250ed27749bc3e5efee5f307dce3590c982d21d14b3bbbae1e4c747919f3d0`. Os perfis humanos não foram modificados no TESTE 5. `Core.dll` também é idêntica.
- Os 15 registros de `chargrp.dat` foram integralmente interpretados pela definição Interlude. O campo `walkanimframe` coincide em todos; vale 12 nos dois humanos. As diferenças de roupa-base (m000/m001 e texturas) também aparecem nos outros corpos. Isso não prova que sejam irrelevantes, mas não sustenta uma causa exclusiva dos humanos.
- Na seção original de código de `engine.dll`, há 26 bytes diferentes em dois locais: um desvio para um formatador de texto acrescentado e uma alteração UTF-16 no nome da classe de montaria. O formatador percorre texto e interpreta padrões `<numero|numero>`. Não foi localizada alteração estática de instruções da corrida humana. Isso não exclui hooks aplicados em execução nem diferenças nos assets externos.

## Fighter.ukx recebido do cliente com TESTE 5

O usuário confirmou a origem: **seu cliente atual com TESTE 5**. SHA-256: `8a22d41ceaa78d52a10b856b923462eb8eb08d61b203d0d7904e464857ae988c`.

O arquivo foi aberto sem alterações e contém 143 malhas e três conjuntos de animação: MFighter_anim (70 ossos, 114 sequências), FFighter_anim (76 ossos, 113 sequências) e Skirt_FFighter (46 ossos, 113 sequências). Os três conjuntos foram interpretados até o fim dos respectivos payloads, incluindo blocos de movimento, trilhas e sequências.

Foram conferidos os dois corpos humanos:

- As contagens de 70/76 ossos coincidem com as esperadas nos perfis da DLL original.
- Os seis índices de coxas, canelas e pés coincidem com o esqueleto recebido: masculino 38/39/40 e 43/44/45; feminino 42/43/44 e 47/48/49.
- Nas malhas auxiliares DynCover da Freya, Draconic e Vesper, os nomes, índices e pais desses ossos, da coluna e da âncora de capa coincidem com as malhas-base do arquivo recebido. As 18 comparações de pose-base ficaram abaixo de 0,0001 unidade de posição e 0,001 grau de orientação normalizada.
- As seis sequências Run de cada sexo foram localizadas e lidas. O osso Cape_Bone está no índice 36 nos dois corpos, com trilhas presentes. As sequências masculinas têm 13 frames e as femininas 12, a 15 fps. Isso descreve o conteúdo recebido e não demonstra, por si só, um defeito.

Esses resultados **não sustentam a hipótese de índices das pernas trocados** nessa comparação. A compatibilidade do esqueleto também não prova que o tecido mantenha folga durante toda a passada. Não houve alteração do arquivo Fighter.ukx nem publicação de novo ajuste.

O relatório detalhado está em [Fighter_RESUMO.json](Fighter_RESUMO.json).

## Dependência restante para comparação entre os dois clientes

O patch do modelo recebido contém **Fighter-v.ukx**, com 22 malhas de armaduras e sem exports MeshAnimation. Continua faltando **Fighter.ukx do cliente completo do modelo** para comparar suas sequências de corrida com as do cliente atual. A ausência é do material enviado, não uma afirmação de que o cliente de referência não o possui.

Se houver acesso ao cliente usado para filmar o modelo, envie `Animations/Fighter.ukx` desse cliente, identificado como modelo. Se não existir com esse nome, é necessário localizar o pacote-base que fornece as referências `Fighter.MFighter_*` e `Fighter.FFighter_*`. Sem a contraparte, não é possível afirmar que as duas corridas sejam diferentes ou copiar a corrida do modelo.

Os formatos binários foram consultados no [UEViewer](https://github.com/gildor2/UEViewer/tree/master/Unreal/UnrealMesh), em especial FMeshBone, UMeshAnimation e USkeletalMesh. Os parsers disponibilizados são auxiliares de análise e dependem dos arquivos e da estrutura local desta sessão; não são instaladores.

## Situação do TESTE 6

O TESTE 6 está retirado. Ele alterava um componente do perfil de movimento, sem implementar colisão de pernas; o usuário relatou desaparecimento das capas. A causa interna desse desaparecimento não foi demonstrada. Esse ajuste não deve ser reaplicado nem usado como prova de que o parâmetro era a origem do defeito.

**Causa do clipping ainda não confirmada. Nenhuma nova DLL, DAT ou malha foi substituída nesta investigação.** A próxima comparação será restrita aos assets dos humanos; não há justificativa para trocar os arquivos das outras raças que funcionam.

Os scripts Python dependem da estrutura local de análise e dos arquivos originais. O JSON registra hashes e diferenças verificadas. A definição de chargrp consultada é a [Interlude do L2FileEdit](https://github.com/L2Miko/L2FileEdit/blob/master/data/l2asm-disasm/DAT_defs/Interlude/chargrp.ddf).

## Continuação sem o arquivo-base do modelo

O usuário informou que não possui Fighter.ukx no material do modelo e confirmou que desligar o MileniumFX por Ctrl+F10 não remove a interseção. A comparação exata entre as duas corridas permanece indisponível. Foi preparada uma [adaptação experimental de âncora em Run](../Ajuste_Humanos_Corrida_TESTE7/README.md), restrita aos humanos do arquivo recebido e com restauração. Isso não confirma a causa interna nem equivale a copiar uma animação do modelo.
