# Windows voice search (sora_speech_win.cpp). C++/WinRT, which only MSVC ships, so it is its own
# project: the rest of native/ builds with MinGW. Built by buildSpeechNative in build.gradle.kts
# with whatever Visual Studio is installed; without one the mic falls back to Win+H.
cmake_minimum_required(VERSION 3.20)
project(sora_speech CXX)

# The app unpacks this DLL into its own directory, where no MSVC runtime DLLs sit beside it.
set(CMAKE_MSVC_RUNTIME_LIBRARY "MultiThreaded")

add_library(sora_speech SHARED sora_speech_win.cpp)
# C++20: under 17, C++/WinRT reaches for <experimental/coroutine>, which current MSVC refuses.
set_target_properties(sora_speech PROPERTIES
    CXX_STANDARD 20
    CXX_STANDARD_REQUIRED ON
    RUNTIME_OUTPUT_DIRECTORY_RELEASE ${CMAKE_BINARY_DIR})
target_compile_options(sora_speech PRIVATE /EHsc /permissive-)
target_link_libraries(sora_speech PRIVATE windowsapp)
