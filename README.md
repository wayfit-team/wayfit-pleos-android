# 웨이핏 (WayFit) — Pleos Connect 차량용 앱

경로 위 동네 가게를 음성으로 추천하고, 들르면 차량 데이터로 방문을 인증해 쿠폰을 주는 앱.

## 개발 환경
- Android Studio, JDK 17+, minSdk 28 / compileSdk·targetSdk 34
- 에뮬레이터: **Pleos Connect v2.1.0** 시스템 이미지 (API 34, `system-images;android-34;pleos_car_emulator;arm64-v8a`)
  - 일반 Android Automotive 이미지에는 Pleos 서비스가 없어 SDK가 동작하지 않는다 (앱은 DEMO 모드로 실행됨)
  - AVD 설정: Automotive → Pleos Connect v2.1.0, RAM 4GB, Default boot = Cold

## Pleos Playground 키 설정
키는 git에 올리지 않는다. 프로젝트 루트의 `local.properties`(gitignore 대상)에 직접 추가:

```properties
pleos.projectId=<프로젝트 ID>
pleos.clientId=<클라이언트 ID>
pleos.clientSecret=<클라이언트 시크릿>
```

비어 있으면 TTS·STT는 온디바이스로만 동작한다.

## 사용 SDK
| SDK | 버전 | 용도 |
|---|---|---|
| Vehicle | 2.3.2 | 기어 P단(주차), 문 열림, 주행 시간 |
| NaviHelper | 3.0.5 | 현재 위치, 목적지, 경유지 추가, 경유지 도착 |
| FusedLocation | 1.1.3 | 융합 위치, 매장 반경 지오펜스 |
| TextToSpeech | 3.3.1 | 추천·쿠폰 음성 안내 |
| SpeechToText | 3.3.1 | "응/아니" 음성 수락 |

래퍼: `automotive/src/main/java/com/wayfit/wayfitpleos/data/pleos/`

## DEMO 모드 (debug 빌드)
화면 하단 DEMO 줄에 SDK 연결 상태(✓/✗)와 시연 버튼이 있다. SDK 이벤트가 없을 때 같은 흐름을 버튼으로 진행할 수 있다.
