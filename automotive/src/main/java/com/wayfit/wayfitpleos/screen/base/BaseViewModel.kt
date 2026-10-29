package com.wayfit.wayfitpleos.screen.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 화면 상태(State)와 일회성 이벤트(Event)를 가진 ViewModel 기반.
 * - 구독은 파생 클래스의 init 에서 한 번만 시작한다 (화면이 다시 그려질 때 중복 구독 방지)
 * - 상태는 파생 클래스 안에서만 바꾼다. 현재 값을 바탕으로 바꿀 때는 [updateState] 로 원자적으로
 */
abstract class BaseViewModel<S : State, E : Event> : ViewModel() {

    abstract fun getInitialState(): S

    private val _stateFlow = MutableStateFlow(getInitialState())
    val stateFlow: StateFlow<S> = _stateFlow.asStateFlow()

    private val _eventFlow = MutableSharedFlow<E>()
    val eventFlow: SharedFlow<E> = _eventFlow.asSharedFlow()

    protected fun setState(state: S) {
        _stateFlow.value = state
    }

    /** 읽고-바꾸고-쓰기를 한 번에 (여러 비동기 작업이 같은 상태를 건드릴 때) */
    protected fun updateState(transform: (S) -> S) {
        _stateFlow.update(transform)
    }

    /** 현재 상태가 [T] 일 때만 바꾼다. 확인과 변경이 원자적이다 */
    protected inline fun <reified T : S> updateStateIf(crossinline transform: (T) -> S) {
        updateState { current -> if (current is T) transform(current) else current }
    }

    protected fun sendEvent(event: E) {
        viewModelScope.launch {
            _eventFlow.emit(event)
        }
    }
}
