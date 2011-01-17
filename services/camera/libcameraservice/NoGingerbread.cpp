#include "QualcommCameraHardware.h"

/****
Simple "NoGingerbread" libcamera.so shim
Assumes we have 1 back facing camera
****/

namespace android {

static CameraInfo sCameraInfo[] = {
    {  
        CAMERA_FACING_BACK,
        90,  /* orientation */
    }
};

extern "C" int HAL_getNumberOfCameras()
{
    return sizeof(sCameraInfo) / sizeof(sCameraInfo[0]);
}

extern "C" void HAL_getCameraInfo(int cameraId, struct CameraInfo* cameraInfo)
{
    memcpy(cameraInfo, &sCameraInfo[cameraId], sizeof(CameraInfo));
}

extern "C" sp<CameraHardwareInterface> HAL_openCameraHardware(int cameraId)
{
    return QualcommCameraHardware::createInstance();
}

}
