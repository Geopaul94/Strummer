package geo.strummer.domain.guitar

import geo.strummer.domain.guitar.model.StrumDirection
import geo.strummer.domain.guitar.model.StrumEvent
import geo.strummer.domain.guitar.model.Voicing

// Converts a chord voicing + strum gesture into a timed sequence of per-string
// pluck events. This is the core of "sounds like a real strum" — getting the
// string-to-string timing (the "rake") right is what separates a good virtual
// guitar from one that sounds like a keyboard playing six notes at once.
//
// HOW REAL STRUM TIMING WORKS:
// When a guitarist strums, the pick/fingers sweep across the strings over a
// short time window. A fast aggressive strum might rake all 6 strings in ~15ms;
// a slow lazy strum might take ~80ms. The strings don't all sound at once — each
// one fires a few milliseconds after the previous one, creating the characteristic
// "spread" that makes a strum sound like a strum.
//
// Down-strum: low E fires first → high E last (pick moves toward the floor).
// Up-strum: high E fires first → low E last (pick moves toward the ceiling).
//
// The per-string delay is derived from the swipe speed: faster swipe = smaller
// delay between strings = tighter, more percussive strum.
object StrumEngine {

    // Strum feel constants. These control the range of rake timings.
    // A real strum across 6 strings typically takes 15-80ms total.
    private const val MIN_RAKE_PER_STRING_MS = 3L   // fastest possible strum
    private const val MAX_RAKE_PER_STRING_MS = 16L  // slowest lazy strum

    // Speed thresholds (pixels/second from the swipe gesture).
    // These are tuned for typical phone screen sizes; a fast flick is ~4000+ px/s,
    // a slow drag is ~500 px/s.
    private const val FAST_SPEED_THRESHOLD = 3000f
    private const val SLOW_SPEED_THRESHOLD = 500f

    fun strum(
        voicing: Voicing,
        direction: StrumDirection,
        swipeSpeed: Float,
        capo: Int = 0,
        baseVelocity: Float = 0.8f,
    ): List<StrumEvent> {
        val events = mutableListOf<StrumEvent>()

        // Map swipe speed to per-string delay. Faster swipe = smaller delay.
        val rakePerString = mapSpeedToRake(swipeSpeed)

        // String order depends on strum direction.
        val stringOrder = when (direction) {
            StrumDirection.DOWN -> (0..5).toList()  // low E → high E
            StrumDirection.UP -> (5 downTo 0).toList() // high E → low E
        }

        var delay = 0L
        for (stringIndex in stringOrder) {
            if (voicing.isMuted(stringIndex)) {
                // Muted strings still contribute to the rake timing — the pick
                // passes over them even if they don't ring. This maintains the
                // natural rhythm of the strum.
                delay += rakePerString
                continue
            }

            val midiPitch = voicing.midiPitch(stringIndex, capo)
            if (midiPitch < 0) continue

            // Slight velocity variation per string adds realism — the pick
            // doesn't hit every string with identical force.
            val velocityVariation = 1.0f - (stringOrder.indexOf(stringIndex) * 0.03f)
            val velocity = (baseVelocity * velocityVariation).coerceIn(0.1f, 1.0f)

            events.add(StrumEvent(stringIndex, midiPitch, velocity, delay))
            delay += rakePerString
        }

        return events
    }

    // Pick a single string from the voicing (tap-to-pick, not strum).
    fun pick(
        voicing: Voicing,
        stringIndex: Int,
        capo: Int = 0,
        velocity: Float = 0.75f,
    ): StrumEvent? {
        if (voicing.isMuted(stringIndex)) return null
        val pitch = voicing.midiPitch(stringIndex, capo)
        if (pitch < 0) return null
        return StrumEvent(stringIndex, pitch, velocity, 0L)
    }

    // Linear interpolation between fast and slow rake timings.
    private fun mapSpeedToRake(speed: Float): Long {
        val clampedSpeed = speed.coerceIn(SLOW_SPEED_THRESHOLD, FAST_SPEED_THRESHOLD)
        val t = (clampedSpeed - SLOW_SPEED_THRESHOLD) /
                (FAST_SPEED_THRESHOLD - SLOW_SPEED_THRESHOLD)
        // t=0 (slow) → MAX rake, t=1 (fast) → MIN rake.
        return (MAX_RAKE_PER_STRING_MS + t * (MIN_RAKE_PER_STRING_MS - MAX_RAKE_PER_STRING_MS)).toLong()
    }
}
