#pragma once

#include <atomic>
#include <cstddef>

// Lock-free single-producer / single-consumer (SPSC) ring buffer.
//
// The UI thread produces commands ("pluck string 3 at velocity 0.8"); the Oboe
// audio callback consumes them. The callback must NEVER block — no locks, no
// allocation, no logging — or we get an audible glitch. Because there is exactly
// one producer and one consumer, two atomic indices with acquire/release ordering
// are sufficient for correctness without any lock.
//
// Capacity must be a power of two so we can wrap with a cheap bitmask.
template <typename T, size_t Capacity>
class RingBuffer {
    static_assert((Capacity & (Capacity - 1)) == 0, "Capacity must be a power of two");

public:
    // Producer side (UI thread). Returns false if full — we drop the command
    // rather than block, because stalling the UI is worse than losing one
    // command in the (very unlikely) event the queue overflows.
    bool push(const T& item) {
        const size_t writeIndex = mWriteIndex.load(std::memory_order_relaxed);
        const size_t nextWrite = (writeIndex + 1) & kMask;
        if (nextWrite == mReadIndex.load(std::memory_order_acquire)) {
            return false;
        }
        mBuffer[writeIndex] = item;
        mWriteIndex.store(nextWrite, std::memory_order_release);
        return true;
    }

    // Consumer side (audio thread). Returns false if empty.
    bool pop(T& out) {
        const size_t readIndex = mReadIndex.load(std::memory_order_relaxed);
        if (readIndex == mWriteIndex.load(std::memory_order_acquire)) {
            return false;
        }
        out = mBuffer[readIndex];
        mReadIndex.store((readIndex + 1) & kMask, std::memory_order_release);
        return true;
    }

private:
    static constexpr size_t kMask = Capacity - 1;
    T mBuffer[Capacity];
    std::atomic<size_t> mWriteIndex{0};
    std::atomic<size_t> mReadIndex{0};
};
