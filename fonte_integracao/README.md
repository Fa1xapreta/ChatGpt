# Fontes da ponte Milenium

Código novo sob MIT em LICENSE.txt. Terceiros mantêm suas licenças.
Compilação utilizada: Zig 0.13.0, Windows x86.

```sh
zig cc -target x86-windows-msvc -O2 -fms-extensions -c sync_guard.c -o sync_guard.obj
zig cc -target x86-windows-gnu -shared -O2 -fms-extensions -Wall -Wextra d3d9_chain.c sync_guard.obj exports.def -o d3d9.dll
zig cc -target x86-windows-gnu -shared -O2 -Wall -Wextra aura_wait.c -o MilAuraWait.dll
zig cc -O2 test_chain.c -o test_chain
./test_chain
```

O teste de interfaces simuladas cobre encaminhamento, sincronização antes de EndScene, preservação de slots e liberação. Não substitui execução do jogo no Windows. CliExt foi alterado apenas na literal de carregamento L2modExCore.dll para MilAuraWait.dll, com o mesmo comprimento. Nenhum bypass de licença foi implementado.
