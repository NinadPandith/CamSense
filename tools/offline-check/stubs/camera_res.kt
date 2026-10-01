@file:Suppress("unused")
package androidx.camera.core.resolutionselector
class ResolutionSelector { class Builder {
    fun setAspectRatioStrategy(s: AspectRatioStrategy): Builder = this
    fun setResolutionStrategy(s: ResolutionStrategy): Builder = this
    fun build(): ResolutionSelector = ResolutionSelector() } }
class AspectRatioStrategy { companion object { val RATIO_16_9_FALLBACK_AUTO_STRATEGY = AspectRatioStrategy() } }
class ResolutionStrategy(size: android.util.Size, rule: Int) { companion object { const val FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER = 1 } }
