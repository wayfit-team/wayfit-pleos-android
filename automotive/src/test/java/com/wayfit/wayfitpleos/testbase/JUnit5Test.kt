package com.wayfit.wayfitpleos.testbase

import io.mockk.MockKAnnotations
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * ViewModel 테스트 공통 베이스.
 * viewModelScope 가 쓰는 Main 디스패처를 [testDispatcher] 로 바꿔, 가상 시간(advanceTimeBy)으로 흐름을 진행한다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class JUnit5Test {

    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()
    protected val testScope = TestScope(testDispatcher)

    @BeforeEach
    open fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    open fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }
}
