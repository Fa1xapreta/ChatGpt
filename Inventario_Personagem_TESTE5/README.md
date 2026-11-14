# Inventário 3D — TESTE 5: correção da relocação da DLL

O usuário confirmou que o TESTE 3 abriu sem erro e que o TESTE 4 falhou ao clicar no botão. Foi encontrada e reproduzida uma falha concreta introduzida no TESTE 4.

## Aplicar a correção

Feche o jogo e o launcher e faça backup.

- Se o TESTE 4 continua instalado: substitua SOMENTE system/MileniumPortraitNative.dll pelo arquivo deste ZIP.
- Se você voltou ao TESTE 3: copie os TRÊS arquivos da pasta system deste ZIP para a system do cliente.

interface.u e interface.xdat são idênticos aos do TESTE 4. O L2UI_CH3 que você editou deve permanecer instalado. A referência continua L2UI_CH3.AbnormalWnd.Inventory_DF_CharacterEquipBg, com o recorte 0,0,284,419 do atlas original 512 x 512.

## Causa confirmada no binário

O TESTE 4 substituiu uma instrução que usava endereço absoluto por um salto relativo, mas deixou ativa a entrada HIGHLOW da instrução antiga na tabela de relocação. Quando o Windows carrega o módulo fora do endereço preferido, essa entrada pode alterar o próprio salto e fazê-lo apontar para fora da DLL. Isso ocorre no caminho que desenha o fundo.

A correção neutraliza SOMENTE essa entrada obsoleta, no RVA 0x2d1f. É uma mudança de um byte no arquivo; o código, as exportações, os demais registros de relocação e o tamanho do arquivo ficam iguais ao TESTE 4.

Os testes anteriores mapeavam a imagem em outras bases, mas não aplicavam as relocações PE do carregador. Essa cobertura era insuficiente. O novo teste aplica a tabela antes de executar as instruções x86.

## Validação executada e limites

A falha do TESTE 4 foi reproduzida com salto para memória não mapeada depois da relocação. A DLL corrigida passou em 36 execuções de instruções x86, com seis bases de carregamento, duas dimensões de janela e três nomes de janela. Também foram verificados os parâmetros de desenho, a pilha, os registradores e a ausência de relocações ativas sobre os três desvios acrescentados.

O cliente Windows não foi executado neste ambiente. Esta é uma correção do defeito reproduzido; a abertura e a aparência final do painel ainda precisam de confirmação no jogo.

## Conferir e restaurar

Abra o inventário e clique no botão. O rodapé continua mostrando Render 3D e Fundo. Envie uma captura se abrir ou o novo erro completo se falhar.

Para voltar ao TESTE 3 confirmado abrindo, feche o jogo e copie os três arquivos de VOLTAR_TESTE3/system para sua system, ou reponha seu backup mais recente. O L2UI_CH3 com a imagem importada permanece como está.

Este ZIP não modifica as DLLs das capas, o sistema de aura, os efeitos de imagem, as animações ou o servidor.
