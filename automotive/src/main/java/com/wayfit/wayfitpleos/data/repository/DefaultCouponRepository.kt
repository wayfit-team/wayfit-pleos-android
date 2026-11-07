package com.wayfit.wayfitpleos.data.repository

import com.wayfit.wayfitpleos.data.remote.WayfitApi
import com.wayfit.wayfitpleos.data.remote.apiCall
import com.wayfit.wayfitpleos.data.remote.toModel
import com.wayfit.wayfitpleos.model.Coupon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultCouponRepository @Inject constructor(
    private val api: WayfitApi,
) : CouponRepository {

    private val _coupons = MutableStateFlow<List<Coupon>>(emptyList())
    override val coupons: StateFlow<List<Coupon>> = _coupons.asStateFlow()

    override suspend fun refresh() {
        val now = System.currentTimeMillis()
        _coupons.value = apiCall { api.coupons() }.coupons.map { it.toModel(now) }
    }

    override fun add(coupon: Coupon) {
        _coupons.update { list -> listOf(coupon) + list.filterNot { it.id == coupon.id } }
    }
}
