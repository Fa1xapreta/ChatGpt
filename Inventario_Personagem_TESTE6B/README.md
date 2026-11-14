# Inventário — TESTE 6B: correção da DLL

O hash enviado pelo usuário corresponde exatamente à DLL do TESTE 6. O Windows retornou erro 193 ao mapear esse arquivo.

Foi identificado um defeito concreto no cabeçalho PE: a seção .iview terminava no endereço alinhado 0x183000, mas a seguinte começava em 0x184000. A correção reserva esse intervalo na seção .iview, tornando consecutivos os intervalos virtuais das seções. Somente dois bytes do cabeçalho mudaram. Código, exportações, importações e relocações permanecem iguais.

## Instalar

Com o jogo e launcher fechados, extraia MileniumPortraitNative.dll de Correcao_DLL_TESTE6B.zip dentro da system, substituindo a DLL do TESTE 6. Mantenha interface.u e interface.xdat do TESTE 6 e seu L2UI_CH3 atual.

Se você restaurou o TESTE 5 inteiro, use o pacote completo Milenium_Inventario_TESTE6B.zip e copie os três arquivos de system.

Se ainda houver erro, execute novamente o verificador e envie Resultado_DLL.txt. Hash da DLL corrigida: C1487932953F911A3ABDF90CC4162CB70D4F5F4154F8E1A53BFE1F6CADEFFE96.

## Validação

Verificados os intervalos consecutivos, tamanho da imagem, integridade das seções e exportação ReleasePreview. Repetidas seis emulações x86 com relocações reais e 36 verificações do fundo. A emulação anterior não verificava a aceitação do cabeçalho pelo carregador do Windows; essa limitação permitiu a entrega do arquivo defeituoso. O cliente Windows não foi executado aqui. A correção precisa da confirmação de carregamento no cliente.
