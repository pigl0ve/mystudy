# Copyright 2013 The Android Open Source Project
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

LOCAL_PATH:= vendor/mediatek/proprietary/bootani/res
#LOCAL_WALLPAPER_PATH:= vendor/mediatek/proprietary/bootani/wallpapers

### default  ###

ifeq ($(LOCAL_PATH)/bootanimation.zip ,$(wildcard $(LOCAL_PATH)/bootanimation.zip))
PRODUCT_COPY_FILES +=   \
	$(LOCAL_PATH)/bootanimation.zip:system/media/bootanimation.zip
endif
ifeq ($(LOCAL_PATH)/shutdownanimation.zip ,$(wildcard $(LOCAL_PATH)/shutdownanimation.zip))
PRODUCT_COPY_FILES +=   \
	$(LOCAL_PATH)/shutdownanimation.zip:system/media/shutdownanimation.zip
endif
ifeq ($(LOCAL_PATH)/bootsound.mp3 ,$(wildcard $(LOCAL_PATH)/bootsound.mp3))
PRODUCT_COPY_FILES +=   \
	$(LOCAL_PATH)/bootsound.mp3:system/media/bootsound.mp3
endif
ifeq ($(LOCAL_PATH)/shutdownsound.mp3 ,$(wildcard $(LOCAL_PATH)/shutdownsound.mp3))
PRODUCT_COPY_FILES +=   \
	$(LOCAL_PATH)/shutdownsound.mp3:system/media/shutdownsound.mp3
endif

### default  ###







