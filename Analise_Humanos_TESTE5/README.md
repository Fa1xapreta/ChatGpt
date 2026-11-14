# Comparação dos humanos fighter — TESTE 5 e modelo

O usuário confirmou uso do TESTE 5 e relatou a passagem dos pés pelo tecido em todas as capas dos humanos fighter, enquanto outras raças funcionam bem. O vídeo da Freya 31394 confirma a interseção durante a passada. Esta análise usa os arquivos originalmente enviados e o conteúdo publicado do TESTE 5; não é uma leitura atual do cliente Windows.

## Verificações concluídas

- Foram conferidas **109 referências de capa/corpo** para MFighter e FFighter. Os pacotes-base do TESTE 5 são iguais aos do modelo por SHA-256. Nos nove estilos com aliases, as malhas usam os mesmos dados serializados, com a adaptação de nomes já documentada. A Freya não depende desses aliases.
- Nos **25 itens originalmente presentes no DAT do modelo**, os quatro blocos MTX correspondentes aos dois corpos humanos coincidem entre modelo e TESTE 5.
- `CapasClassic.dll` do TESTE 5 é idêntica à do modelo: `20250ed27749bc3e5efee5f307dce3590c982d21d14b3bbbae1e4c747919f3d0`. Os perfis humanos não foram modificados no TESTE 5. `Core.dll` também é idêntica.
- Os 15 registros de `chargrp.dat` foram integralmente interpretados pela definição Interlude. O campo `walkanimframe` coincide em todos; vale 12 nos dois humanos. As diferenças de roupa-base (m000/m001 e texturas) também aparecem nos outros corpos. Isso não prova que sejam irrelevantes, mas não sustenta uma causa exclusiva dos humanos.
- Na seção original de código de `engine.dll`, há 26 bytes diferentes em dois locais: um desvio para um formatador de texto acrescentado e uma alteração UTF-16 no nome da classe de montaria. O formatador percorre texto e interpreta padrões `<numero|numero>`. Não foi localizada alteração estática de instruções da corrida humana. Isso não exclui hooks aplicados em execução nem diferenças nos assets externos.

## Lacuna que impede confirmar a causa

O pacote de Animations recebido contém **Fighter-v.ukx**, mas seus 22 exports são `Engine.SkeletalMesh` de armaduras customizadas. Não contém exports `MeshAnimation`. O pacote de arquivos recebido não inclui **Fighter.ukx**.

`chargrp` referencia malhas como `Fighter.MFighter_m000_f` e `Fighter.FFighter_m000_f`. Para comparar o esqueleto/cadeia de animações usada por esses corpos, é necessário obter o pacote-base correspondente do cliente completo e seguir suas referências de animação. Não é correto tratar Fighter-v.ukx como substituto desse arquivo-base.

Envie, em pastas separadas, `Animations/Fighter.ukx` do cliente atual e do cliente completo do modelo, caso tenha ambos. Se só tiver o patch do modelo, envie primeiro o seu arquivo. Se não existir com esse nome, informe os nomes dos arquivos Fighter, MFighter e FFighter existentes em Animations para localizar a dependência correta. Texturas de ícones não são necessárias nesta etapa.

## Situação do TESTE 6

O TESTE 6 está retirado. Ele alterava um componente do perfil de movimento, sem implementar colisão de pernas; o usuário relatou desaparecimento das capas. A causa interna desse desaparecimento não foi demonstrada. Esse ajuste não deve ser reaplicado nem usado como prova de que o parâmetro era a origem do defeito.

**Causa do clipping ainda não confirmada. Nenhuma nova DLL, DAT ou malha foi substituída nesta investigação.** A próxima comparação será restrita aos assets dos humanos; não há justificativa para trocar os arquivos das outras raças que funcionam.

Os scripts Python dependem da estrutura local de análise e dos arquivos originais. O JSON registra hashes e diferenças verificadas. A definição de chargrp consultada é a [Interlude do L2FileEdit](https://github.com/L2Miko/L2FileEdit/blob/master/data/l2asm-disasm/DAT_defs/Interlude/chargrp.ddf).
