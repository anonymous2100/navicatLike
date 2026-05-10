cmake_minimum_required(VERSION 3.20)

# Link with GTest
find_package(GTest QUIET)
if(NOT GTest_FOUND)
    include(FetchContent)
    FetchContent_Declare(googletest
        GIT_REPOSITORY https://github.com/google/googletest.git
        GIT_TAG v1.15.2
    )
    FetchContent_MakeAvailable(googletest)
endif()

set(TEST_SOURCES
    test_basic.cpp
    test_dialect.cpp
    test_sqlbuilder.cpp
    test_cipher.cpp
)

add_executable(LightDBViewerTests ${TEST_SOURCES})

target_include_directories(LightDBViewerTests PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../src
)

target_link_libraries(LightDBViewerTests PRIVATE
    Qt6::Core
    Qt6::Sql
    Qt6::Xml
    Qt6::Test
    GTest::gtest
    GTest::gmock
    OpenSSL::SSL
    OpenSSL::Crypto
)

include(GoogleTest)
gtest_discover_tests(LightDBViewerTests)
