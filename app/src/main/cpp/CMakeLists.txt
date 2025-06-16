# CMakeLists.txt for FGO Bot Native Components
# 
# This file configures the native build system for the FGO Bot Android application.
# Since we're using OpenCV via Maven Central (Java bindings), this CMake file
# is prepared for future native extensions but doesn't require OpenCV native libraries.

cmake_minimum_required(VERSION 3.22.1)

# Project configuration
project("fgobot_native")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Include directories
include_directories(${CMAKE_SOURCE_DIR})

# Check if we have any native source files
file(GLOB_RECURSE NATIVE_SRCS
    "${CMAKE_SOURCE_DIR}/*.cpp"
    "${CMAKE_SOURCE_DIR}/*.c"
)

file(GLOB_RECURSE NATIVE_HDRS
    "${CMAKE_SOURCE_DIR}/*.hpp"
    "${CMAKE_SOURCE_DIR}/*.h"
)

# Only create library if we have source files
if(NATIVE_SRCS)
    # Create native library
    add_library(fgobot_native SHARED ${NATIVE_SRCS})

    # Link libraries
    target_link_libraries(fgobot_native
        # Android libraries
        android
        log
        jnigraphics
        
        # System libraries
        m
        z
    )

    # Compiler flags
    target_compile_options(fgobot_native PRIVATE
        -Wall
        -Wextra
        -O3
        -ffast-math
        -DANDROID
    )

    # Preprocessor definitions
    target_compile_definitions(fgobot_native PRIVATE
        ANDROID_NDK
    )

    # Set target properties
    set_target_properties(fgobot_native PROPERTIES
        ANDROID_ARM_MODE arm
        ANDROID_STL c++_shared
    )
else()
    # Create a minimal placeholder library for future native extensions
    add_library(fgobot_native SHARED
        # Create a minimal source file if none exist
        ${CMAKE_CURRENT_BINARY_DIR}/placeholder.cpp
    )
    
    # Generate placeholder source file
    file(WRITE ${CMAKE_CURRENT_BINARY_DIR}/placeholder.cpp
        "// Placeholder for future native extensions\n"
        "extern \"C\" {\n"
        "    void fgobot_native_placeholder() {\n"
        "        // This function exists to create a valid shared library\n"
        "    }\n"
        "}\n"
    )
    
    # Link basic libraries
    target_link_libraries(fgobot_native
        android
        log
    )
endif() 