# Strummer ProGuard / R8 rules.

# ── JNI bridge ──────────────────────────────────────────────────────────────
# The native library resolves these methods by their fully-qualified names via
# JNI mangling. R8 must NOT rename the class or its native methods, or the JNI
# lookup fails at runtime with UnsatisfiedLinkError.
-keep class geo.strummer.data.audio.GuitarEngineBridge { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# ── Room ─────────────────────────────────────────────────────────────────────
# Room ships its own consumer rules, but keep entities/DAOs defensively.
-keep class geo.strummer.data.palette.** { *; }

# ── Kotlin / coroutines / Compose are handled by their bundled rules. ─────────
# Hilt generates code that R8 keeps automatically.
