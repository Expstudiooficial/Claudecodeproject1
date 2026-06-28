cmake_minimum_required(VERSION 3.22.1)
project("localai")

# ---------------------------------------------------------------------------
# llama.cpp is fetched into ./llama.cpp by scripts/setup_native.sh (not
# vendored, to keep the repo small). If present we build it; otherwise we
# still compile the JNI bridge so the Kotlin/Java side links cleanly and the
# app builds for UI development without the full native stack.
# ---------------------------------------------------------------------------
set(LLAMA_DIR "${CMAKE_CURRENT_SOURCE_DIR}/llama.cpp")

add_library(localai SHARED llama_bridge.cpp)

if(EXISTS "${LLAMA_DIR}/CMakeLists.txt")
    message(STATUS "Found llama.cpp at ${LLAMA_DIR} — building full inference backend")

    # Mobile-friendly llama.cpp options.
    set(LLAMA_BUILD_TESTS    OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
    set(LLAMA_BUILD_SERVER   OFF CACHE BOOL "" FORCE)
    set(GGML_OPENMP          OFF CACHE BOOL "" FORCE)

    add_subdirectory(${LLAMA_DIR} ${CMAKE_BINARY_DIR}/llama.cpp)
    target_link_libraries(localai llama)
    target_compile_definitions(localai PRIVATE HAVE_LLAMA=1)
else()
    message(WARNING "llama.cpp not found at ${LLAMA_DIR}. "
                    "Building JNI bridge in STUB mode (no real inference). "
                    "Run scripts/setup_native.sh to enable the backend.")
endif()

find_library(log-lib log)
target_link_libraries(localai ${log-lib})
