#pragma once

#include <oboe/Oboe.h>
#include <mutex>

#include "RingBuffer.h"
#include "KarplusStrong.h"

// Native guitar audio engine.
//
// Models a 6-string guitar where each string can sound exactly one note at a
// time (per-string monophony). Plucking a string that's already ringing cuts
// the previous note and starts a new one — just like a real guitar string.
//
// Synthesis: Karplus-Strong physically-modeled plucked strings. Each string has
// its own KS delay line, producing realistic guitar tones with natural attack
// transients and exponential decay. Phase 1 will add SF2 sample playback as an
// alternative/upgrade; the KS engine stays as a lightweight fallback.
//
// Threading contract (same as the piano app):
//   - pluckString/muteString/start/stop: called from the UI (JNI) thread.
//   - onAudioReady: Oboe's real-time audio thread. No allocation, no locks,
//     no logging, no JVM calls. Only reads the command queue and renders strings.
class GuitarEngine : public oboe::AudioStreamDataCallback,
                     public oboe::AudioStreamErrorCallback {
public:
    static constexpr int kNumStrings = 6;

    GuitarEngine();
    ~GuitarEngine() override;

    // --- Called from the UI thread ---
    void setDeviceDefaults(int sampleRate, int framesPerBurst);
    bool start();
    void stop();

    // Pluck a specific string at the given MIDI pitch and velocity (0..1).
    // If the string is already sounding, cuts it first (per-string monophony).
    void pluckString(int stringIndex, int midiPitch, float velocity);

    // Mute a specific string (palm mute or finger lift).
    void muteString(int stringIndex);

    // Mute all strings at once.
    void muteAll();

    // --- Oboe callbacks (audio thread) ---
    oboe::DataCallbackResult onAudioReady(oboe::AudioStream* stream,
                                          void* audioData,
                                          int32_t numFrames) override;
    void onErrorAfterClose(oboe::AudioStream* stream, oboe::Result error) override;

    // --- Diagnostics (read from UI thread) ---
    int    getSampleRate() const;
    int    getFramesPerBurst() const;
    int    getBufferSizeFrames() const;
    double getLatencyMillis() const;
    int    getXRunCount() const;
    bool   isLowLatency() const;
    bool   isExclusive() const;
    int    getAudioApi() const;

private:
    // Commands from UI thread to audio thread.
    struct Command {
        enum class Type { Pluck, Mute, MuteAll } type;
        int   stringIndex;
        int   midiPitch;
        float velocity;
    };

    void handlePluck(int stringIndex, int midiPitch, float velocity);
    void handleMute(int stringIndex);

    // Convert MIDI note number to frequency in Hz.
    // Standard tuning: A4 = MIDI 69 = 440 Hz.
    static float midiToFrequency(int midiPitch) {
        return 440.0f * std::pow(2.0f, (static_cast<float>(midiPitch) - 69.0f) / 12.0f);
    }

    // One KarplusStrong instance per string — pre-allocated, no runtime allocation.
    KarplusStrong mStrings[kNumStrings];

    RingBuffer<Command, 256> mCommands;

    std::shared_ptr<oboe::AudioStream> mStream;
    std::mutex mStreamLock; // guards start/stop only; NEVER in the callback

    int mDeviceSampleRate = 48000;
    int mDeviceFramesPerBurst = 192;
};
