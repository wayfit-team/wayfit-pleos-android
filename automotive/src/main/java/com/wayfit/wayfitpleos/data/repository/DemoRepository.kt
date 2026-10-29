package com.wayfit.wayfitpleos.data.repository

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 시연용 명령. 실제 앱에서는 백엔드 추천·SDK 이벤트가 같은 흐름을 만든다 */
enum class DemoCommand { RECOMMEND, ARRIVE_AND_PARK }

@Singleton
class DemoRepository @Inject constructor() {

    private val _commands = MutableSharedFlow<DemoCommand>(extraBufferCapacity = 4)
    val commands: SharedFlow<DemoCommand> = _commands.asSharedFlow()

    fun send(command: DemoCommand) {
        _commands.tryEmit(command)
    }
}
