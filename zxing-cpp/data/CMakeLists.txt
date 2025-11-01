cmake_minimum_required(VERSION 3.14)
project(ZxingJnaWrapper)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING OFF CACHE BOOL "" FORCE)

add_subdirectory(./zxing-cpp zxing_build)

add_library(zxing_jna SHARED zxing_wrapper.cpp)

target_link_libraries(zxing_jna PRIVATE ZXing::ZXing)

set_target_properties(zxing_jna PROPERTIES
    POSITION_INDEPENDENT_CODE ON
    VISIBILITY_INLINES_HIDDEN ON
)

# --- AJOUT SECURITE : Linkage statique de la lib C++ (UNIQUEMENT LINUX) ---
if(CMAKE_SYSTEM_NAME STREQUAL "Linux")
    # Cela évite l'erreur "GLIBCXX_... not found" si le serveur a un vieux libstdc++
    target_link_options(zxing_jna PRIVATE
        -static-libgcc
        -static-libstdc++
        -Wl,-Bsymbolic
    )
endif()