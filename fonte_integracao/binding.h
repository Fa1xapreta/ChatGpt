#ifndef MIL_AURA_BINDING_H
#define MIL_AURA_BINDING_H
#include <stdint.h>
static int binding_ready(uintptr_t current,uintptr_t shim,uintptr_t core){return core && core!=shim && current==core;}
static int binding_replace_allowed(uintptr_t current,uintptr_t shim,uintptr_t core){return core && core!=shim && current==shim;}
#endif
