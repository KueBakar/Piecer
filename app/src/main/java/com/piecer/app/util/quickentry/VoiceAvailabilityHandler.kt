package com.piecer.app.util.quickentry

data class VoiceAvailabilityState(
    val isServiceAvailable: Boolean = true,
    val hasShownUnavailableMessage: Boolean = false
) {
    val shouldShowMicButton: Boolean get() = isServiceAvailable
}

data class VoiceAvailabilityResolution(
    val state: VoiceAvailabilityState,
    val showUnavailableMessageNow: Boolean
)

/**
 * Logika murni untuk menangani ketersediaan layanan pengenal suara sistem
 * dan memastikan pesan ketiadaan layanan hanya muncul satu kali.
 */
object VoiceAvailabilityHandler {

    fun evaluateAvailability(
        current: VoiceAvailabilityState,
        serviceAvailable: Boolean
    ): VoiceAvailabilityResolution {
        val nextState = current.copy(isServiceAvailable = serviceAvailable)
        val shouldNotify = !serviceAvailable && !current.hasShownUnavailableMessage
        return VoiceAvailabilityResolution(
            state = if (shouldNotify) nextState.copy(hasShownUnavailableMessage = true) else nextState,
            showUnavailableMessageNow = shouldNotify
        )
    }

    fun onLaunchFailure(current: VoiceAvailabilityState): VoiceAvailabilityResolution {
        val shouldNotify = !current.hasShownUnavailableMessage
        return VoiceAvailabilityResolution(
            state = current.copy(
                isServiceAvailable = false,
                hasShownUnavailableMessage = true
            ),
            showUnavailableMessageNow = shouldNotify
        )
    }
}
