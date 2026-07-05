#
# Copyright (c) 2018 Nordic Semiconductor
#
# SPDX-License-Identifier: LicenseRef-Nordic-5-Clause
#
cmake_minimum_required(VERSION 3.20.0)

set(BOOT_SIGNATURE_KEY_FILE
    ${CMAKE_CURRENT_SOURCE_DIR}/fan_private_2048.pem
)

if(DEFINED BOOT_SIGNATURE_KEY_FILE)
    set(mcuboot_CONFIG_BOOT_SIGNATURE_KEY_FILE 
      \"${BOOT_SIGNATURE_KEY_FILE}\"
    )
    set(hci_ipc_CONFIG_SB_SIGNING_KEY_FILE
      \"${BOOT_SIGNATURE_KEY_FILE}\"
    )

    message("Signing Key is applied: ${mcuboot_CONFIG_BOOT_SIGNATURE_KEY_FILE}")
endif()

# if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/spm.conf")
#   set(spm_CONF_FILE
#     prj.conf
#     ${CMAKE_CURRENT_LIST_DIR}/spm.conf
#   )
# endif()

# if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/mcuboot.conf")
#   set(mcuboot_CONF_FILE
#     prj.conf
#     ${CMAKE_CURRENT_LIST_DIR}/mcuboot.conf
#   )
# endif()

# if (EXISTS "${CMAKE_CURRENT_SOURCE_DIR}/${BOARD}.overlay")
#   set(mcuboot_DTC_OVERLAY_FILE "${CMAKE_CURRENT_SOURCE_DIR}/${BOARD}.overlay")
#   set(spm_DTC_OVERLAY_FILE "${CMAKE_CURRENT_SOURCE_DIR}/${BOARD}.overlay")
# endif()


find_package(Zephyr REQUIRED HINTS $ENV{ZEPHYR_BASE})
project(NONE)


# NORDIC SDK APP START
target_sources(app PRIVATE
  src/main.c
  src/my_fbs.c
)

# NORDIC SDK APP END
zephyr_library_include_directories(.)
