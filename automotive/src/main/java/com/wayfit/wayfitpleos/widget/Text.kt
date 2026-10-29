package com.wayfit.wayfitpleos.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Text as M3Text

private const val WORD_JOINER = '⁠'

/**
 * CSS `word-break: keep-all` 흉내. 공백이 아닌 글자 사이에 WORD JOINER 를 넣어
 * 한글이 음절 중간("아메리카/노")이 아니라 공백에서만 줄바꿈되게 한다.
 * (Android 의 Phrase 줄바꿈은 한국어를 지원하지 않는 버전이 있음)
 */
internal fun String.keepAll(): String = buildString(length * 2) {
    var prev = -1
    var i = 0
    while (i < this@keepAll.length) {
        // 코드포인트 단위로 순회: 이모지(서로게이트 쌍) 사이에 끼워 넣으면 글자가 깨진다
        val cp = this@keepAll.codePointAt(i)
        if (prev != -1 && Character.isLetterOrDigit(prev) && Character.isLetterOrDigit(cp)) append(WORD_JOINER)
        appendCodePoint(cp)
        prev = cp
        i += Character.charCount(cp)
    }
}

/** 앱 전체에서 material3 Text 대신 사용 */
@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
) {
    val display = remember(text) { text.keepAll() }
    M3Text(
        text = display,
        modifier = modifier,
        color = color,
        style = style,
        textAlign = textAlign,
        maxLines = maxLines,
        softWrap = softWrap,
    )
}
