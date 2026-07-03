#include "GuitarEngine.h"

using namespace oboe;

GuitarEngine::GuitarEngine() = default;

GuitarEngine::~GuitarEngine() {
    stop();
}

void GuitarEngine::setDeviceDefaults(int sampleRate, int framesPerBurst) {
    if (sampleRate > 0)     mDeviceSampleRate = sampleRate;
    if (framesPerBurst > 0) mDeviceFramesPerBurst = framesPerBurst;
}

bool GuitarEngine::start() {
    std::lock_guard<std::mutex> lock(mStreamLock);
    if (mStream) return true;

    AudioStreamBuilder builder;
    builder.setDirection(Direction::Output)
        ->setPerformanceMode(PerformanceMode::LowLatency)
        ->setSharingMode(SharingMode::Exclusive)
        ->setFormat(AudioFormat::Float)
        ->setChannelCount(ChannelCount::Stereo)
        ->setSampleRate(mDeviceSampleRate)
        ->setSampleRateConversionQuality(SampleRateConversionQuality::Medium)
        ->setUsage(Usage::Media)
        ->setContentType(ContentType::Music)
        ->setDataCallback(this)
        ->setErrorCallback(this);

    Result result = builder.openStream(mStream);
    if (result != Result::OK || !mStream) {
        mStream.reset();
        return false;
    }

    const int32_t burst = mStream->getFramesPerBurst();
    mStream->setBufferSizeInFrames(burst * 2);

    result = mStream->requestStart();
    if (result != Result::OK) {
        mStream->close();
        mStream.reset();
        return false;
    }
    return true;
}

void GuitarEngine::stop() {
    std::lock_guard<std::mutex> lock(mStreamLock);
    if (mStream) {
        mStream->stop();
        mStream->close();
        mStream.reset();
    }
}

void GuitarEngine::pluckString(int stringIndex, int midiPitch, float velocity) {
    if (stringIndex < 0 || stringIndex >= kNumStrings) return;
    mCommands.push({Command::Type::Pluck, stringIndex, midiPitch, velocity});
}

void GuitarEngine::muteString(int stringIndex) {
    if (stringIndex < 0 || stringIndex >= kNumStrings) return;
    mCommands.push({Command::Type::Mute, stringIndex, 0, 0.0f});
}

void GuitarEngine::muteAll() {
    mCommands.push({Command::Type::MuteAll, 0, 0, 0.0f});
}

void GuitarEngine::setTone(float brightness) {
    if (brightness < 0.0f) brightness = 0.0f;
    if (brightness > 1.0f) brightness = 1.0f;
    mBrightness.store(brightness, std::memory_order_relaxed);
}

void GuitarEngine::setPalmMute(bool enabled) {
    mPalmMute.store(enabled, std::memory_order_relaxed);
}

void GuitarEngine::startRecording() {
    // Drain any stale samples so a new recording starts clean.
    float scratch;
    while (mRecFifo.pop(scratch)) { /* discard */ }
    mRecording.store(true, std::memory_order_release);
}

void GuitarEngine::stopRecording() {
    mRecording.store(false, std::memory_order_release);
}

bool GuitarEngine::isRecording() const {
    return mRecording.load(std::memory_order_acquire);
}

int GuitarEngine::drainRecording(float* out, int maxFloats) {
    int count = 0;
    float sample;
    while (count < maxFloats && mRecFifo.pop(sample)) {
        out[count++] = sample;
    }
    return count;
}

void GuitarEngine::handlePluck(int stringIndex, int midiPitch, float velocity) {
    if (stringIndex < 0 || stringIndex >= kNumStrings) return;

    int sampleRate = mStream ? mStream->getSampleRate() : mDeviceSampleRate;
    float freq = midiToFrequency(midiPitch);
    float vel = velocity < 0.0f ? 0.0f : (velocity > 1.0f ? 1.0f : velocity);

    // Per-string monophony: re-plucking reinitializes the same KS delay line,
    // which cuts the previous note. A real string can't sound two pitches at once.
    float brightness = mBrightness.load(std::memory_order_relaxed);
    bool palmMute = mPalmMute.load(std::memory_order_relaxed);
    mStrings[stringIndex].pluck(sampleRate, freq, vel, brightness, palmMute);
}

void GuitarEngine::handleMute(int stringIndex) {
    if (stringIndex < 0 || stringIndex >= kNumStrings) return;
    mStrings[stringIndex].mute();
}

DataCallbackResult GuitarEngine::onAudioReady(AudioStream* stream,
                                               void* audioData,
                                               int32_t numFrames) {
    // 1) Drain all pending commands.
    Command cmd;
    while (mCommands.pop(cmd)) {
        switch (cmd.type) {
            case Command::Type::Pluck:
                handlePluck(cmd.stringIndex, cmd.midiPitch, cmd.velocity);
                break;
            case Command::Type::Mute:
                handleMute(cmd.stringIndex);
                break;
            case Command::Type::MuteAll:
                for (int i = 0; i < kNumStrings; ++i) mStrings[i].mute();
                break;
        }
    }

    float* out = static_cast<float*>(audioData);
    const int channels = stream->getChannelCount();

    // 2) Clear the output buffer.
    for (int i = 0; i < numFrames * channels; ++i) out[i] = 0.0f;

    // 3) Render each string and mix. Strings are subtly panned low-E left →
    //    high-E right to simulate real string positions across the neck.
    static constexpr float kPan[kNumStrings] = {
        -0.30f, -0.18f, -0.06f, 0.06f, 0.18f, 0.30f
    };

    for (int si = 0; si < kNumStrings; ++si) {
        if (!mStrings[si].isActive()) continue;

        // Equal-power panning: L = cos(θ), R = sin(θ), θ = (pan+1)*π/4.
        float theta = (kPan[si] + 1.0f) * 0.7853981633f;
        float gainL = std::cos(theta);
        float gainR = std::sin(theta);

        for (int f = 0; f < numFrames; ++f) {
            float sample = mStrings[si].render();
            if (channels >= 2) {
                out[f * channels + 0] += sample * gainL;
                out[f * channels + 1] += sample * gainR;
            } else {
                out[f * channels] += sample;
            }
        }
    }

    // 4) If recording, capture the mixed output into the lock-free FIFO. We push
    //    interleaved stereo (or duplicate mono to stereo) so Kotlin can write a
    //    standard 2-channel WAV. push() drops silently if the FIFO is full, which
    //    only happens if Kotlin stalls — better than blocking the audio thread.
    if (mRecording.load(std::memory_order_acquire)) {
        for (int f = 0; f < numFrames; ++f) {
            if (channels >= 2) {
                mRecFifo.push(out[f * channels + 0]);
                mRecFifo.push(out[f * channels + 1]);
            } else {
                float s = out[f * channels];
                mRecFifo.push(s);
                mRecFifo.push(s);
            }
        }
    }

    return DataCallbackResult::Continue;
}

void GuitarEngine::onErrorAfterClose(AudioStream* /*stream*/, Result /*error*/) {
    mStream.reset();
    start();
}

// --- Diagnostics ---
int GuitarEngine::getSampleRate() const {
    return mStream ? mStream->getSampleRate() : mDeviceSampleRate;
}
int GuitarEngine::getFramesPerBurst() const {
    return mStream ? mStream->getFramesPerBurst() : mDeviceFramesPerBurst;
}
int GuitarEngine::getBufferSizeFrames() const {
    return mStream ? mStream->getBufferSizeInFrames() : 0;
}
double GuitarEngine::getLatencyMillis() const {
    if (!mStream) return -1.0;
    auto result = mStream->calculateLatencyMillis();
    return result ? result.value() : -1.0;
}
int GuitarEngine::getXRunCount() const {
    if (!mStream) return -1;
    auto result = mStream->getXRunCount();
    return result ? result.value() : -1;
}
bool GuitarEngine::isLowLatency() const {
    return mStream && mStream->getPerformanceMode() == PerformanceMode::LowLatency;
}
bool GuitarEngine::isExclusive() const {
    return mStream && mStream->getSharingMode() == SharingMode::Exclusive;
}
int GuitarEngine::getAudioApi() const {
    return mStream ? static_cast<int>(mStream->getAudioApi()) : 0;
}
