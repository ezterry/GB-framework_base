# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

copy_from :=                \
    DroidSans.ttf           \
    DroidSans-Bold.ttf      \
    DroidSansArabic.ttf     \
    DroidSansHebrew.ttf     \
    DroidSansThai.ttf       \
    DroidSerif-Regular.ttf  \
    DroidSerif-Bold.ttf     \
    DroidSerif-Italic.ttf   \
    DroidSerif-BoldItalic.ttf   \
    DroidSansMono.ttf        \
    Clockopia.ttf

ifeq ($(INCLUDE_FONT_DROIDSANSJAPANESE),true)
    copy_from += DroidSansJapanese.ttf \
                 MTLc3m.ttf
endif

ifneq ($(NO_FALLBACK_FONT),true)
ifeq ($(filter %system/fonts/DroidSansFallback.ttf,$(PRODUCT_COPY_FILES)),)
    # if the product makefile has set the the fallback font, don't override it.
    copy_from += DroidSansFallback.ttf
endif
endif

ifeq ($(TARGET_SQUASH_FONTS),true)

file := $(TARGET_OUT)/fonts/fonts.sqf
input_font_files := $(foreach cf,$(copy_from),$(LOCAL_PATH)/$(cf))

ALL_PREBUILT += $(file)

$(file) : $(input_font_files) | $(ACP)
	#remove any old SYSFONT_INTERMEDIATES dir
	rm -rf $(TARGET_OUT_INTERMEDIATES)/SYSFONT_INTERMEDIATES
	#make sure new dirs exist
	mkdir -p $(TARGET_OUT_INTERMEDIATES)/SYSFONT_INTERMEDIATES
	mkdir -p $(TARGET_OUT)/fonts
	#copy to working dir
	cp  $(input_font_files) $(TARGET_OUT_INTERMEDIATES)/SYSFONT_INTERMEDIATES
	#verify perms
	chmod 0644 $(TARGET_OUT_INTERMEDIATES)/SYSFONT_INTERMEDIATES/*.ttf
	#create the sqf
	mksquashfs $(TARGET_OUT_INTERMEDIATES)/SYSFONT_INTERMEDIATES \
	           $(TARGET_OUT)/fonts/fonts.sqf -all-root -noappend

else
copy_file_pairs := $(foreach cf,$(copy_from),$(LOCAL_PATH)/$(cf):system/fonts/$(cf))
PRODUCT_COPY_FILES += $(copy_file_pairs)
endif
