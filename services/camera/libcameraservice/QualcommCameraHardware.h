/*
** Copyright 2008, The Android Open-Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License"); 
** you may not use this file except in compliance with the License. 
** You may obtain a copy of the License at 
**
**     http://www.apache.org/licenses/LICENSE-2.0 
**
** Unless required by applicable law or agreed to in writing, software 
** distributed under the License is distributed on an "AS IS" BASIS, 
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
** See the License for the specific language governing permissions and 
** limitations under the License.
*/

/*******
Note this is a minimal header intended for the froyo licamera.so shim
please note it may be incomplete for other purposes
********/

#ifndef ANDROID_HARDWARE_QUALCOMM_CAMERA_HARDWARE_H
#define ANDROID_HARDWARE_QUALCOMM_CAMERA_HARDWARE_H

#include <camera/CameraHardwareInterface.h>
#include <binder/MemoryBase.h>
#include <binder/MemoryHeapBase.h>

extern "C" {
    #include <linux/android_pmem.h>
}

namespace android {

class QualcommCameraHardware : public CameraHardwareInterface {
public:

    virtual sp<IMemoryHeap> getPreviewHeap() const;
    virtual sp<IMemoryHeap> getRawHeap() const;

    virtual status_t    dump(int fd, const Vector<String16>& args) const;
    virtual status_t    startPreview(void* cb, void* user);
    virtual void        stopPreview();
    virtual bool        previewEnabled();
    virtual status_t    startRecording(void* cb, void* user);
    virtual void        stopRecording();
    virtual bool        recordingEnabled();
    virtual void        releaseRecordingFrame(const sp<IMemory>& mem);
    virtual status_t    autoFocus(void*, void *user);
    virtual status_t    takePicture(void*,
                                    void*,
                                    void*,
                                    void* user);
    virtual status_t    cancelPicture(bool cancel_shutter,
                                      bool cancel_raw, bool cancel_jpeg);
    virtual status_t    setParameters(const CameraParameters& params);
    virtual CameraParameters  getParameters() const;

    virtual void release();

    static sp<CameraHardwareInterface> createInstance();
    static sp<QualcommCameraHardware> getInstance();

    void* get_preview_mem(uint32_t size, uint32_t *phy_addr, uint32_t index);
    void* get_raw_mem(uint32_t size, uint32_t *phy_addr, uint32_t index);
    void free_preview_mem(uint32_t *phy_addr, uint32_t size, uint32_t index);
    void free_raw_mem(uint32_t *phy_addr, uint32_t size, uint32_t index);

};

}; // namespace android

#endif


