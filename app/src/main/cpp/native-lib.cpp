#include <jni.h>
#include "audio/GuitarEngine.h"

// JNI bridge. Each function maps 1:1 to an `external fun` in the Kotlin object
// geo.strummer.data.audio.GuitarEngineBridge. The engine instance lives on the
// native heap; Kotlin holds an opaque jlong handle (a raw C++ pointer).
//
// Naming: Java_<package_underscores>_<Class>_<method>.

static inline GuitarEngine* engine(jlong handle) {
    return reinterpret_cast<GuitarEngine*>(handle);
}

extern "C" {

JNIEXPORT jlong JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeCreate(JNIEnv*, jobject) {
    return reinterpret_cast<jlong>(new GuitarEngine());
}

JNIEXPORT void JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeDestroy(JNIEnv*, jobject, jlong h) {
    delete engine(h);
}

JNIEXPORT void JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeSetDeviceDefaults(
        JNIEnv*, jobject, jlong h, jint sampleRate, jint framesPerBurst) {
    engine(h)->setDeviceDefaults(sampleRate, framesPerBurst);
}

JNIEXPORT jboolean JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeStart(JNIEnv*, jobject, jlong h) {
    return engine(h)->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeStop(JNIEnv*, jobject, jlong h) {
    engine(h)->stop();
}

JNIEXPORT void JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativePluckString(
        JNIEnv*, jobject, jlong h, jint stringIndex, jint midiPitch, jfloat velocity) {
    engine(h)->pluckString(stringIndex, midiPitch, velocity);
}

JNIEXPORT void JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeMuteString(
        JNIEnv*, jobject, jlong h, jint stringIndex) {
    engine(h)->muteString(stringIndex);
}

JNIEXPORT void JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeMuteAll(JNIEnv*, jobject, jlong h) {
    engine(h)->muteAll();
}

// --- Diagnostics ---

JNIEXPORT jint JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeGetSampleRate(JNIEnv*, jobject, jlong h) {
    return engine(h)->getSampleRate();
}

JNIEXPORT jint JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeGetFramesPerBurst(JNIEnv*, jobject, jlong h) {
    return engine(h)->getFramesPerBurst();
}

JNIEXPORT jint JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeGetBufferSize(JNIEnv*, jobject, jlong h) {
    return engine(h)->getBufferSizeFrames();
}

JNIEXPORT jdouble JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeGetLatencyMillis(JNIEnv*, jobject, jlong h) {
    return engine(h)->getLatencyMillis();
}

JNIEXPORT jint JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeGetXRunCount(JNIEnv*, jobject, jlong h) {
    return engine(h)->getXRunCount();
}

JNIEXPORT jboolean JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeIsLowLatency(JNIEnv*, jobject, jlong h) {
    return engine(h)->isLowLatency() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeIsExclusive(JNIEnv*, jobject, jlong h) {
    return engine(h)->isExclusive() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jint JNICALL
Java_geo_strummer_data_audio_GuitarEngineBridge_nativeGetAudioApi(JNIEnv*, jobject, jlong h) {
    return engine(h)->getAudioApi();
}

} // extern "C"
