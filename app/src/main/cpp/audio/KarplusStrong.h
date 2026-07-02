#pragma once

#include <cmath>
#include <cstdint>
#include <cstring>

// Karplus-Strong plucked-string synthesis.
//
// HOW IT WORKS: A short delay line (length = one period of the target pitch) is
// filled with random noise, then each sample is fed back through a simple
// lowpass filter (averaging two adjacent samples). The noise decays into a
// pitched tone whose timbre closely resembles a plucked guitar string — this
// isn't an approximation, it's literally how a vibrating string behaves (the
// higher harmonics die faster than the fundamental).
//
// The delay-line length sets the pitch: delayLength = sampleRate / frequency.
// The lowpass feedback controls the decay/brightness: a simple two-point average
// (y[n] = 0.5*(x[n] + x[n-1])) gives a warm acoustic tone; blending in more of
// the current sample makes it brighter/longer.
//
// This is the standard reference algorithm for plucked-string synthesis, used in
// academic literature and professional synths. It naturally produces guitar-like
// tones with realistic attack transients and exponential decay — no samples needed.
//
// IMPORTANT: This class is used ONLY on the audio thread. All its memory is
// pre-allocated in the constructor; render() does no allocation, locking, or
// logging (safe for the Oboe callback).

class KarplusStrong {
public:
    // Max delay line length. 48000 / 80 Hz (lowest guitar E2 ~ 82 Hz) ≈ 585 samples.
    // We over-allocate to 2048 to handle any sample rate up to ~164 kHz at E2.
    static constexpr int kMaxDelay = 2048;

    KarplusStrong() {
        reset();
    }

    void reset() {
        mActive = false;
        mPhase = 0;
        mDelayLength = 0;
        mLevel = 0.0f;
        mPrevSample = 0.0f;
        mDamping = 0.5f;
        std::memset(mBuffer, 0, sizeof(mBuffer));
    }

    // Pluck the string at the given frequency and velocity.
    // sampleRate: the Oboe stream's actual sample rate (typically 48000).
    // frequency: target pitch in Hz (e.g. 82.41 for E2).
    // velocity: 0..1, controls initial amplitude.
    // brightness: 0..1, higher = brighter/longer sustain (default 0.5 = warm acoustic).
    void pluck(int sampleRate, float frequency, float velocity, float brightness = 0.5f) {
        // Delay line length determines the pitch.
        mDelayLength = static_cast<int>(static_cast<float>(sampleRate) / frequency);
        if (mDelayLength > kMaxDelay) mDelayLength = kMaxDelay;
        if (mDelayLength < 2) mDelayLength = 2;

        // Fill the delay line with band-limited noise scaled by velocity.
        // Using a simple LCG PRNG — no need for high-quality randomness here,
        // and we can't call rand() on the audio thread (it may lock internally).
        uint32_t seed = 12345u + static_cast<uint32_t>(frequency * 1000.0f);
        for (int i = 0; i < mDelayLength; ++i) {
            seed = seed * 1664525u + 1013904223u;
            // Map to [-1, 1] range, scale by velocity.
            float noise = (static_cast<float>(seed) / 2147483648.0f) - 1.0f;
            mBuffer[i] = noise * velocity * 0.5f;
        }

        mPhase = 0;
        mPrevSample = mBuffer[0];
        mLevel = 1.0f;
        mActive = true;

        // Damping controls how much of the lowpass filter output feeds back.
        // Higher brightness = less damping = brighter, longer sustain.
        // Range: 0.495 (very warm/short) to 0.4995 (bright/long).
        mDamping = 0.495f + brightness * 0.0045f;
    }

    // Render one sample. Returns 0 if the string is inactive.
    // This is called per-frame inside onAudioReady — it MUST be allocation-free.
    float render() {
        if (!mActive) return 0.0f;

        // Read current sample from the delay line.
        float current = mBuffer[mPhase];

        // Karplus-Strong lowpass feedback: average the current sample with the
        // previous one, scaled by the damping factor. This is what makes higher
        // harmonics decay faster than the fundamental, producing the natural
        // plucked-string timbre.
        float filtered = mDamping * (current + mPrevSample);
        mPrevSample = current;

        // Write the filtered sample back into the delay line (feedback loop).
        mBuffer[mPhase] = filtered;

        // Advance the read/write position (circular buffer).
        mPhase = (mPhase + 1) % mDelayLength;

        // Check if the string has decayed to silence.
        // We track the peak of the filtered output; once it drops below a
        // threshold, mark the string inactive to save CPU.
        float absVal = filtered < 0.0f ? -filtered : filtered;
        if (absVal > mLevel) mLevel = absVal;
        else mLevel *= 0.9999f;

        if (mLevel < 0.0001f) {
            mActive = false;
            return 0.0f;
        }

        return filtered;
    }

    // Mute the string (palm mute / re-pluck cut). Rapid fade to silence.
    void mute() {
        if (!mActive) return;
        // Quickly damp the delay line contents to produce a short "chk" sound.
        for (int i = 0; i < mDelayLength; ++i) {
            mBuffer[i] *= 0.05f;
        }
        // The string will naturally decay to silence in a few ms.
    }

    bool isActive() const { return mActive; }

private:
    float mBuffer[kMaxDelay];
    int   mDelayLength = 0;
    int   mPhase = 0;
    float mPrevSample = 0.0f;
    float mLevel = 0.0f;
    float mDamping = 0.5f;
    bool  mActive = false;
};
