package com.wayfit.wayfitpleos.screen.pair

import com.wayfit.wayfitpleos.model.PairingCode
import com.wayfit.wayfitpleos.screen.base.State

data class PairState(
    val code: PairingCode? = null,
    val phoneCount: Int = 0,
    /** 이 화면에 들어온 뒤 새 폰이 연결됐으면 true → "연결됐어요" 화면 */
    val justPaired: Boolean = false,
) : State
