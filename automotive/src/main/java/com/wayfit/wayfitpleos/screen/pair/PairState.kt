package com.wayfit.wayfitpleos.screen.pair

import com.wayfit.wayfitpleos.model.Pairing
import com.wayfit.wayfitpleos.screen.base.State

data class PairState(
    val pairing: Pairing = Pairing.NotPaired,
) : State
