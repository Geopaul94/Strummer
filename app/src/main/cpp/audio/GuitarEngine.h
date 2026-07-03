#pragma once

#include <oboe/Oboe.h>
#include <atomic>
#include <mutex>

#include "RingBuffer.h"
#include "KarplusStrong.h"

// Native guitar audio engine.
//
// Models a 6-string guitar where each string sounds exactly one note at a time
// (per-string monophony). Re-plucking a ringing string cuts the previous note —
// just like a real string. Synthesis is Karplus-Strong physically-modeled
// plucked strings (see KarplusStrong.h).
//
// Threading contract:
//   - pluck/mute/tone/recording setters: UI (JNI) thread.
//   - onAudioReady: Oboe real-time audio thread. No allocation, no locks, no
//     logging, no JVM calls. Only reads the command queue, renders strings, and
//     (when recording) pushes output samples into a lock-free FIFO.
class GuitarEngine : public oboe::AudioStreamDataCallback,
                     public oboe::AudioStreamErrorCallback {
public:
    static constexpr int kNumStrings = 6;

    GuitarEngine();
    ~GuitarEngine() override;

    // --- UI thread ---
    void setDeviceDefaults(int sampleRate, int framesPerBurst);
    bool start();
    void stop();

    void pluckString(int stringIndex, int midiPitch, float velocity);
    void muteString(int stringIndex);
    void muteAll();

    // Tone brightness 0..1 (nylon ≈ 0.3, acoustic ≈ 0.5, electric ≈ 0.75).
    void setTone(float brightness);
    // Palm mute: damped staccato notes.
    void setPalmMute(bool enabled);

    // Recording: captures the final mixed stereo output into a lock-free FIFO.
    void startRecording();
    void stopRecording();
    bool isRecording() const;
    // Drain up to maxFloats interleaved-stereo samples into out; returns count.
    int  drainRecording(float* out, int maxFloats);

    // --- Oboe callbacks (audio thread) ---
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream,
                                          void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

    // --- Diagnostics (UI thread) ---
    int    getSampleRate() const;
    int    getFramesPerBurst() const;
    int    getBufferSizeFrames() const;
    double getLatencyMillis() const;
    int    getXRunCount() const;
    bool   isLowLatency() const;
    bool   isExclusive() const;
    int    getAudioApi() const;

private:
    struct Command {
        enum class Type { Pluck, Mute, MuteAll } type;
        int   stringIndex;
        int   midiPitch;
        float velocity;
    };

    void handlePluck(int stringIndex, int midiPitch, float velocity);
    void handleMute(int stringIndex);

    static float midiToFrequency(int midiPitch) {
        return 440.0f * std::pow(2.0f, (static_cast<float>(midiPitch) - 69.0f) / 12.0f);
    }

    KarplusStrong mStrings[kNumStrings];
    RingBuffer<Command, 256> mCommands;

    // Tone/feel state — atomics so the UI thread can set them while the audio
    // thread reads them without a lock. Read once per pluck (not per sample).
    std::atomic<float> mBrightness{0.5f};
    std::atomic<bool>  mPalmMute{false};

    // Recording FIFO. 1<<18 floats ≈ 1 MB ≈ 2.7 s of stereo @ 48 kHz — ample if
    // Kotlin drains every ~50-100 ms. The audio thread pushes; a Kotlin coroutine
    // pops via drainRecording(). Lock-free, so the callback never blocks.
    static constexpr int kRecFifoCapacity = 1 << 18;
    RingBuffer<float, kRecFifoCapacity> mRecFifo;
    std::atomic<bool> mRecording{false};

    std::shared_ptr<oboe::AudioStream> mStream;
    std::mutex mStreamLock; // guards start/stop only; NEVER in the callback

    int mDeviceSampleRate = 48000;
    int mDeviceFramesPerBurst = 192;
};
