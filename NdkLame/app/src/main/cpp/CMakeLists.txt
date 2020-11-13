# Sets the minimum version of CMake required to build the native library.
cmake_minimum_required(VERSION 3.4.1)
set(CMAKE_LIBRARY_OUTPUT_DIRECTORY ${PROJECT_SOURCE_DIR}/lameLibs/${ANDROID_ABI})
file(GLOB lame lamemp3/*.c)
file(GLOB lame2 lamemp3/*.cpp)
include_directories(${CMAKE_CURRENT_LIST_DIR}/lamemp3)

add_library( # Sets the name of the library.
        native-lib
        SHARED
        native-lib.cpp
        ${lame}
        ${lame2}
        )

find_library( # Sets the name of the path variable.
        log-lib
        log)

target_link_libraries(
        native-lib
        ${log-lib})