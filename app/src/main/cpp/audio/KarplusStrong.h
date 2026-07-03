#pragma once

#include <cmath>
#include <cstdint>
#include <cstring>

// Karplus-Strong plucked-string synthesis.
//
// HOW IT WORKS: A short delay line (length = one period of the target pitch) is
// filled with random noise, then each sample is fed back through a simple
// lowpass filter (averaging adjacent samples). The noise decays into a pitched
// tone whose timbre closely resembles a plucked guitar string — this isn't an
// approximation, it's literally how a vibrating string behaves (higher harmonics
// die faster than the fundamental).
//
//   delayLength = sampleRate / frequency   → sets the pitch
//   lowpass feedback (damping)             → sets decay/brightness
//
// This is the standard reference algorithm for plucked-string synthesis. It
// produces guitar-like tones with realistic attack transients and exponential
// decay — no samples needed.
//
// TONE: `brightness` (0..1) controls the damping coefficient. Lower = warmer &
// shorter (nylon), higher = brighter & longer (steel/electric).
//
// PALM MUTE: when engaged, an extra fast-decaying output envelope makes the note
// staccato/damped, the way resting the picking-hand palm on the bridge does.
//
// IMPORTANT: used ONLY on the audio thread. All memory is pre-allocated in the
// constructor; render() does no allocation, locking, or logging.

class KarplusStrong {
public:
    // 48000 / 80 Hz (below low E2 ≈ 82 Hz) ≈ 600 samples. Over-allocate to 2048.
    static constexpr int kMaxDelay = 2048;

    KarplusStrong() { reset(); }

    void reset() {
        mActive = false;
        mPhase = 0;
        mDelayLength = 0;
        mLevel = 0.0f;
        mPrevSample = 0.0f;
        mDamping = 0.5f;
        mMuteEnv = 1.0f;
        mMuteDecay = 1.0f;
        std::memset(mBuffer, 0, sizeof(mBuffer));
    }

    // Pluck the string.
    //   sampleRate: Oboe stream's actual rate (e.g. 48000).
    //   frequency:  target pitch in Hz (e.g. 82.41 for E2).
    //   velocity:   0..1, initial amplitude.
    //   brightness: 0..1, timbre/sustain (0.5 = warm acoustic default).
    //   palmMute:   true = damped staccato note.
    void pluck(int sampleRate, float frequency, float velocity,
               float brightness, bool palmMute) {
        mDelayLength = static_cast<int>(static_cast<float>(sampleRate) / frequency);
        if (mDelayLength > kMaxDelay) mDelayLength = kMaxDelay;
        if (mDelayLength < 2) mDelayLength = 2;

        // Fill the delay line with band-limited noise scaled by velocity.
        // A simple LCG PRNG — we can't call rand() on the audio thread (it may
        // lock internally), and we don't need statistical-quality randomness.
        uint32_t seed = 22222u + static_cast<uint32_t>(frequency * 1000.0f);
        for (int i = 0; i < mDelayLength; ++i) {
            seed = seed * 1664525u + 1013904223u;
            float noise = (static_cast<float>(seed) / 2147483648.0f) - 1.0f;
            mBuffer[i] = noise * velocity * 0.5f;
        }

        mPhase = 0;
        mPrevSample = mBuffer[0];
        mLevel = 1.0f;
        mActive = true;

        // Damping: higher brightness = less energy loss per pass = brighter/longer.
        // Range 0.495 (very warm/short) .. 0.4995 (bright/long).
        mDamping = 0.495f + brightness * 0.0045f;

        // Palm mute engages a fast exponential output envelope (~60ms to silence).
        // Normal notes have no extra envelope (mMuteDecay = 1.0), so they ring
        // out with only the natural KS decay.
        if (palmMute) {
            mMuteEnv = 1.0f;
            // Per-sample decay for a ~60ms fade: 0.001^(1/(0.06*sr)).
            mMuteDecay = std::pow(0.001f, 1.0f / (0.06f * static_cast<float>(sampleRate)));
        } else {
            mMuteEnv = 1.0f;
            mMuteDecay = 1.0f;
        }
    }

    // Render one sample. Returns 0 if inactive. MUST be allocation-free.
    float render() {
        if (!mActive) return 0.0f;

        float current = mBuffer[mPhase];

        // Karplus-Strong lowpass feedback: average current with previous sample,
        // scaled by damping. This makes higher harmonics decay faster than the
        // fundamental — the natural plucked-string timbre.
        float filtered = mDamping * (current + mPrevSample);
        mPrevSample = current;
        mBuffer[mPhase] = filtered;
        mPhase = (mPhase + 1) % mDelayLength;

        // Apply the palm-mute output envelope (no-op for normal notes).
        float outSample = filtered * mMuteEnv;
        mMuteEnv *= mMuteDecay;

        // Track peak for auto-off (frees the voice once it's inaudible).
        float absVal = outSample < 0.0f ? -outSample : outSample;
        if (absVal > mLevel) mLevel = absVal;
        else mLevel *= 0.9999f;

        if (mLevel < 0.0001f) {
            mActive = false;
            return 0.0f;
        }
        return outSample;
    }

    // Quick damp (re-pluck cut / manual mute) — short "chk" then silence.
    void mute() {
        if (!mActive) return;
        for (int i = 0; i < mDelayLength; ++i) mBuffer[i] *= 0.05f;
    }

    bool isActive() const { return mActive; }

private:
    float mBuffer[kMaxDelay];
    int   mDelayLength = 0;
    int   mPhase = 0;
    float mPrevSample = 0.0f;
    float mLevel = 0.0f;
    float mDamping = 0.5f;
    float mMuteEnv = 1.0f;    // palm-mute output envelope level
    float mMuteDecay = 1.0f;  // per-sample envelope decay (1.0 = no decay)
    bool  mActive = false;
};
