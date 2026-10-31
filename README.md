# Super Mario Bros. NES – Android Studio Project

투명 온스크린 컨트롤 + 일시정지 + 체크포인트 저장/불러오기가 포함된 NES 플레이어 프로젝트입니다.

## 포함된 기능

| 기능 | 설명 |
|------|------|
| 투명 버튼 | 좌 / 우 / 점프(A) / 가속·총알(B) / START / SELECT |
| 일시정지 | 우측 상단 II 버튼 또는 뒤로가기 키 |
| 체크포인트 저장 | Pause 메뉴 → Save Checkpoint |
| 체크포인트 불러오기 | Pause 메뉴 → Load Checkpoint |
| 진동 피드백 | 버튼 누를 때 짧은 진동 |

## 사용 방법

### 1. ROM 준비
```
app/src/main/assets/smb.nes
```
위 경로에 Super Mario Bros. `.nes` 파일을 넣습니다.

### 2. Android Studio에서 열기
1. Android Studio → Open → `SMB_APK_Project` 폴더 선택
2. Gradle Sync
3. Run

### 3. 라이브러리
- `io.github.beseting:NesEmulator:1.0.1` (Apache 2.0)
- 기본 컨트롤러는 숨기고 커스텀 투명 버튼을 사용합니다.

## 조작

| 버튼 | 기능 |
|------|------|
| ◀ ▶ | 좌 / 우 이동 |
| A Jump | 점프 |
| B Run | 가속 + 파이어볼 |
| START / SELECT | 게임 내 버튼 |
| II (우측 상단) | 일시정지 메뉴 |

일시정지 메뉴에서:
- **Resume** – 계속하기
- **Save Checkpoint** – 현재 상태 저장
- **Load Checkpoint** – 저장한 상태 불러오기
- **Exit** – 앱 종료

## 주의사항

1. **ROM은 직접 넣어야 합니다.** (저작권 문제로 포함하지 않음)
2. Beseting 라이브러리의 세이브 스테이트 지원 정도는 버전에 따라 다를 수 있습니다.  
   완전히 동작하지 않으면 Logcat을 확인하세요.
3. 라이브러리 패키지명이 다를 경우를 대비해 Reflection을 사용했습니다.

## 빌드

```bash
./gradlew assembleDebug
```

APK 위치: `app/build/outputs/apk/debug/app-debug.apk`

## 다음 개선 아이디어

- 더 정확한 세이브 스테이트를 위해 suzukiplan 또는 순수 Kotlin 코어(ktnes)로 교체
- 버튼 위치/크기 조절 UI
- 빠른 저장 슬롯 여러 개
