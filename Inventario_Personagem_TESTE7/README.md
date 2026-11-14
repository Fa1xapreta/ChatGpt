# Inventory Viewr — TESTE 7

## Instalar

Com jogo e launcher fechados, copie system/MileniumPortraitNative.dll para a system do cliente. Se já usa o TESTE 6B, somente a DLL mudou; interface.u e interface.xdat são idênticos. O pacote completo também contém esses dois arquivos para quem restaurou uma versão anterior. Mantenha seu L2UI_CH3 atual.

## Mudanças

A câmera foi afastada: o tamanho projetado é aproximadamente 20% menor que no TESTE 6B, mantendo a referência fixa do corpo ao trocar o set.

A consulta agora sincroniza explicitamente as malhas separadas de mão direita, esquerda, braços, capa e cobertura da capa, materiais, pontos de ligação e escala da arma. Isso ocorre mesmo quando o cache da armadura informa que nada mudou. Retirar um equipamento também copia a referência vazia para a prévia. As MeshInstances e estruturas de física do jogador real não são compartilhadas com a prévia.

A rotação ao clicar e arrastar e o título Inventory Viewr permanecem.

## Conferir no jogo

Sem fechar a prévia: troque a arma, retire a arma, equipe outra, troque o escudo e troque/retire a capa. Gire para conferir as costas. Confirme se o personagem fica inteiro e com margem nas bordas. Caso uma capa continue sem atualizar, informe seu ID e se ela aparece ao fechar e reabrir a prévia: capas customizadas podem usar recursos adicionais além de CloakMesh.

## Validação e limites

O cabeçalho mantém as seções consecutivas que corrigiram o erro 193. Foram executadas seis emulações x86 com as relocações PE aplicadas e 36 casos de fundo. As simulações verificam troca/retirada de arma e capa com o cache da armadura inalterado, preservação do ator real, câmera e giro. As chamadas do Engine e Windows são simuladas; a renderização final ainda precisa da confirmação no cliente.

Para restaurar, copie a DLL de VOLTAR_TESTE6B/system com o jogo fechado. Nenhum arquivo do servidor, das montarias, das capas ou da aura é substituído.
