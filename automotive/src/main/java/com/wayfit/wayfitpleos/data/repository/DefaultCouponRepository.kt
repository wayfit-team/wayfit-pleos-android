package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.model.Coupon
import com.wayfit.wayfitpleos.model.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class DefaultCouponRepository @Inject constructor() : CouponRepository {

    private val _coupons = MutableStateFlow(
        listOf(
            Coupon("c1", "아주대 앞 브루잉랩", "음료 사이즈 업", code = "482913", expiresInDays = 5),
            Coupon("c2", "영통 떡볶이연구소", "튀김 1인분 서비스", code = "730154", expiresInDays = 0, redeemed = true),
        )
    )
    override val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    override suspend fun issue(store: Store): Coupon {
        val coupon = Coupon(
            id = "c${System.currentTimeMillis()}",
            storeName = store.name,
            benefit = store.benefit,
            code = (1..6).joinToString("") { Random.nextInt(10).toString() },
            expiresInDays = 7,
        )
        _coupons.update { listOf(coupon) + it }
        return coupon
    }
}
