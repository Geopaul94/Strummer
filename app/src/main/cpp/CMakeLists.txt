cmake_minimum_required(VERSION 3.22.1)
project(strummer_audio)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(oboe REQUIRED CONFIG)

add_library(strummer_audio SHARED
    native-lib.cpp
    audio/GuitarEngine.cpp
)

target_include_directories(strummer_audio PRIVATE ${CMAKE_CURRENT_SOURCE_DIR})

find_library(log-lib log)

target_link_libraries(strummer_audio
    oboe::oboe
    android
    ${log-lib}
)
