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

    // Keep buffer small for low latency.
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

void GuitarEngine::handlePluck(int stringIndex, int midiPitch, float velocity) {
    if (stringIndex < 0 || stringIndex >= kNumStrings) return;

    int sampleRate = mStream ? mStream->getSampleRate() : mDeviceSampleRate;
    float freq = midiToFrequency(midiPitch);
    float vel = velocity < 0.0f ? 0.0f : (velocity > 1.0f ? 1.0f : velocity);

    // Per-string monophony: re-plucking a string automatically cuts the
    // previous note because we reinitialize the same KS delay line. The old
    // tone is replaced by fresh noise, which then decays into the new pitch.
    // This is exactly how a real guitar string behaves — you can't sound two
    // notes on the same string simultaneously.
    mStrings[stringIndex].pluck(sampleRate, freq, vel);
}

void GuitarEngine::handleMute(int stringIndex) {
    if (stringIndex < 0 || stringIndex >= kNumStrings) return;
    mStrings[stringIndex].mute();
}

DataCallbackResult GuitarEngine::onAudioReady(AudioStream* stream,
                                               void* audioData,
                                               int32_t numFrames) {
    // 1) Drain all pending commands from the ring buffer.
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

    // 3) Render each string and mix into the output.
    // Guitar strings are panned slightly to simulate real string positions:
    // low E is slightly left, high E slightly right.
    // Pan values: -0.3, -0.18, -0.06, 0.06, 0.18, 0.3 (subtle, not extreme).
    static constexpr float kPan[kNumStrings] = {
        -0.30f, -0.18f, -0.06f, 0.06f, 0.18f, 0.30f
    };

    for (int si = 0; si < kNumStrings; ++si) {
        if (!mStrings[si].isActive()) continue;

        // Pre-compute stereo gain from pan position.
        // Equal-power panning: L = cos(θ), R = sin(θ), where θ = (pan+1)*π/4.
        float theta = (kPan[si] + 1.0f) * 0.7853981633f; // π/4
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
