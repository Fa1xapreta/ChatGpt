# Inventory Viewr — TESTE 10

## Instalar

Feche jogo e launcher. Se está usando o TESTE 9, substitua SOMENTE system/MileniumPortraitNative.dll. interface.u e interface.xdat são idênticos aos do TESTE 9. O ZIP contém os três arquivos para quem restaurou uma versão anterior. Mantenha seu L2UI_CH3 atual.

## Enquadramento

Tamanho projetado aproximadamente 11% menor que no TESTE 9. Centro da câmera deslocado para posicionar os pés cerca de 40 pixels abaixo da captura anterior, próximo à marca vermelha sobre a rosa dos ventos. A distância continua baseada no corpo, sem autoajuste pelas armas/set. O alinhamento exato requer conferência visual para a raça do personagem; não foi executado o cliente Windows neste ambiente.

## Capa e encantamento

Além de CloakMesh e dos materiais tradicionais, agora a prévia sincroniza ExtraMeshDatas quando essa propriedade está disponível: cria seus próprios objetos Engine.ExtraMeshData, com Owner apontando para o personagem da prévia, e copia somente malha, material e índice da parte do corpo. Não compartilha MeshInstance nem modifica os objetos do jogador real. A matriz é criada pelo copiador nativo UArrayProperty, evitando compartilhar o armazenamento do jogador.

A análise do Engine fornecido mostrou que GetEnchantedWeaponMesh e GetEnchantedWeaponEffect consultam o identificador da arma (AttackItemClassID), além do nível de encantamento. A prévia agora recebe esses dois valores. Isso é uma implementação candidata para recuperar o efeito; copiar apenas o + não havia funcionado. Emissores customizados e efeitos criados por outros módulos podem usar caminhos adicionais. Não foi confirmado visualmente que todas as capas customizadas ou cores de encantamento já apareçam.

## Conferir

Abra a prévia, gire para ver as costas e troque/retire a capa. Troque entre armas com diferentes encantamentos. Compare o brilho com o jogador real. Confira a posição dos pés na rosa dos ventos. Se capa/brilho ainda faltarem, informe IDs da capa e arma e o valor do encantamento.

## Verificação

Seções PE consecutivas e dentro do tamanho da imagem; seis emulações x86 com relocação aplicada, incluindo clonagem independente das malhas extras, troca/retirada, dono correto e preservação do estado do jogador real; 36 casos de desenho do fundo; XDAT lido e comparado. As chamadas do Engine e Windows são simuladas. Os resultados não substituem o teste visual no cliente.

## Restaurar

Com jogo fechado, copie a DLL de VOLTAR_TESTE9/system para sua system. Nenhum arquivo do servidor, sistema de capas do mundo, aura ou montarias é alterado.
