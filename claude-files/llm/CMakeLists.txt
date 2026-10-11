cmake_minimum_required(VERSION 3.22.1)

project("agriedge_llm" C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable compiler optimization flags for arm64-v8a without -ffast-math
if(${ANDROID_ABI} STREQUAL "arm64-v8a")
    add_compile_options(-O3 -fno-finite-math-only)
endif()

# Disable llama.cpp extras to speed up native build
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)

add_subdirectory("C:/Users/HP/projects/edge-qwen/llama.cpp" llama_build EXCLUDE_FROM_ALL)

add_library(
    agriedge_llm
    SHARED
    LocalLLM.cpp
)

find_library(
    log-lib
    log
)

target_include_directories(
    agriedge_llm PRIVATE
    "C:/Users/HP/projects/edge-qwen/llama.cpp/include"
    "C:/Users/HP/projects/edge-qwen/llama.cpp/ggml/include"
)

target_link_libraries(
    agriedge_llm
    llama
    ggml
    ${log-lib}
)
