# Six Can Do Eat Backend

백엔드 협업을 위한 공통 규칙 문서입니다.

아래 내용은 팀이 함께 맞춰갈 패키지 구조, 공통 설계 원칙, 예외 처리 방식, 엔티티 공통 규칙, Git 협업 규칙을 정리한 기준 문서입니다. 초기 세팅 단계에서는 실제 코드와 일부 차이가 있을 수 있으며, 규칙이나 구조가 바뀌면 이 문서도 함께 업데이트합니다.

## 패키지 구조

```text
src/main/java/com/team6/backend
│
├── global
│   └── infrastructure
│       ├── config
│       ├── entity
│       └── exception
└── 도메인명
    ├── application
    │   └── service
    ├── domain
    │   ├── entity
    │   └── repository
    └── presentation
        ├── controller
        └── dto
            ├── request
            └── response
```

`도메인명`에는 `user`, `auth`, `store`, `order` 같은 도메인 패키지가 들어갑니다.

### global 패키지 역할

- `config`: 스프링 전역 설정, 보안 설정, JPA Auditing, QueryDSL 설정
- `config.security`: 인증/인가 관련 설정과 JWT 처리
- `entity`: 모든 엔티티가 공통으로 상속하는 베이스 클래스
- `exception`: 전역 예외 처리와 공통 에러 응답

### global 작성 규칙

- `global`에는 특정 도메인에 종속된 비즈니스 로직을 넣지 않습니다.
- 여러 도메인에서 함께 쓰는 코드만 둡니다.
- 특정 도메인에서만 쓰는 예외, DTO, 서비스는 각 도메인 패키지 내부에 둡니다.
- 공통 관심사라고 해도 도메인 로직이 섞이면 `global`이 아니라 해당 도메인으로 이동합니다.

## 패키지 역할

| 패키지 | 역할 |
| --- | --- |
| `global` | 모든 도메인에서 공통으로 사용하는 코드 |
| `application.service` | 서비스 계층 |
| `domain.entity` | 도메인 엔티티 |
| `domain.repository` | Repository 인터페이스 |
| `presentation.controller` | API 엔드포인트 |
| `presentation.dto.request` | 요청 DTO |
| `presentation.dto.response` | 응답 DTO |

## BaseEntity 규칙

현재 공통 엔티티는 [`BaseEntity`](src/main/java/com/team6/backend/global/infrastructure/entity/BaseEntity.java)를 기준으로 사용합니다.

### 포함 필드

- `createdAt`
- `createdBy`
- `updatedAt`
- `updatedBy`
- `deletedAt`
- `deletedBy`

### 사용 규칙

- 모든 JPA 엔티티는 특별한 이유가 없으면 `BaseEntity`를 상속합니다.
- 삭제는 기본적으로 `soft delete`를 사용하고 `markDeleted(String deletedBy)`로 처리합니다.
- 복구가 필요한 경우 `restore()`를 사용합니다.
- 실제 삭제가 정말 필요한 경우에만 물리 삭제를 고려합니다.
- 삭제된 데이터 조회 여부는 서비스/리포지토리 레벨에서 명확히 관리합니다.

### Auditing 설정

- [`JpaAuditingConfig`](src/main/java/com/team6/backend/global/infrastructure/config/JpaAuditingConfig.java)에서 `@EnableJpaAuditing`을 활성화합니다.
- `createdBy`, `updatedBy`를 자동 채우려면 추후 `AuditorAware<String>` 구현이 필요합니다.
- 인증 기능이 붙기 전까지는 `createdBy`, `updatedBy` 처리 전략을 팀 내에서 먼저 합의합니다.

## 예외 처리 규칙

예외 처리는 전역으로 모아 관리합니다.

권장 구조:

```text
global/infrastructure/exception
├── GlobalExceptionHandler.java
├── BusinessException.java
├── ErrorCode.java
└── ErrorResponse.java
```

### 기본 원칙

- 서비스 계층에서 비즈니스 예외를 던집니다.
- 컨트롤러에서 `try-catch`로 비즈니스 예외를 직접 처리하지 않습니다.
- 공통 예외 응답은 `GlobalExceptionHandler`에서 내려줍니다.
- 예외 메시지는 사용자에게 보여줄 메시지와 로그용 메시지를 구분할 수 있도록 설계합니다.
- 내부 구현 상세나 민감 정보는 응답에 포함하지 않습니다.

### 에러 코드 규칙

- 형식: `DOMAIN_REASON`
- 예시:
  - `COMMON_INVALID_INPUT`
  - `AUTH_UNAUTHORIZED`
  - `AUTH_INVALID_TOKEN`
  - `USER_NOT_FOUND`
  - `STORE_NOT_FOUND`
  - `ORDER_FORBIDDEN`

### 권장 에러 응답 형식

```json
{
  "status": 404,
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다.",
  "timestamp": "2026-04-17T15:00:00"
}
```

### 권장 처리 흐름

1. 서비스에서 `BusinessException(ErrorCode.USER_NOT_FOUND)` 발생
2. `GlobalExceptionHandler`가 예외를 캐치
3. HTTP 상태 코드와 표준 에러 응답 바디 반환

## DTO 규칙

- 요청 DTO와 응답 DTO는 반드시 분리합니다.
- 요청 DTO는 `presentation.dto.request`
- 응답 DTO는 `presentation.dto.response`
- 엔티티를 그대로 외부에 노출하지 않습니다.
- 컨트롤러는 DTO를 받고 DTO를 반환합니다.

## Git 협업 규칙

### 브랜치 전략

- `main`: 최종 검증이 끝난 안정 브랜치
- `dev`: 기능 개발이 먼저 모이는 통합 브랜치
- `feature/*`: 기능 개발 브랜치
- `fix/*`: 버그 수정
- `refactor/*`: 리팩토링
- `docs/*`: 문서 수정
- `chore/*`: 설정, 빌드, 기타 작업
- `hotfix/*`: 운영 중 긴급 수정 브랜치

브랜치 용도는 아래 기준으로 구분합니다.

- `main`: 배포 가능하거나 최종 기준이 되는 코드만 반영합니다.
- `dev`: 팀원들의 기능 브랜치를 먼저 합치고 테스트하는 기본 브랜치입니다.
- `feature/*`: 새로운 기능 작업 시 `dev`에서 분기해서 사용합니다.
- `hotfix/*`: 운영 이슈를 빠르게 수정해야 할 때 `main`에서 분기해서 사용합니다.

예시:

- `main`
- `dev`
- `feature/user-signup`
- `feature/store-api`
- `fix/jwt-filter`
- `docs/readme-rules`

### 작업 흐름

1. `dev` 최신 내용 pull
2. `feature/*` 브랜치 생성
3. 작업 후 commit
4. 원격 브랜치 push
5. GitHub에서 `feature/* -> dev` Pull Request 생성
6. 리뷰와 테스트 확인 후 `dev`에 merge
7. 배포 또는 최종 반영 시점에 `dev -> main` Pull Request 생성
8. 최종 확인 후 `main`에 merge

### 커밋 메시지 규칙

- `feat: add user signup API`
- `fix: resolve JWT token parsing issue`
- `refactor: separate auth service responsibilities`
- `docs: add backend package rules`
- `chore: initialize project`

가능하면 한 커밋에는 하나의 의미 있는 변경만 담습니다.

### Pull Request 규칙

- PR은 가능한 한 작은 단위로 올립니다.
- PR 제목만 보고 어떤 작업인지 알 수 있게 작성합니다.
- 리뷰 반영은 같은 브랜치에 추가 커밋 후 다시 push 합니다.
- 기능 개발은 `feature/* -> dev` 흐름을 기본으로 합니다.
- 충분한 테스트와 검증이 끝난 뒤 `dev -> main`으로 최종 반영합니다.
- 승인 없이 `dev`에 직접 push 하지 않습니다.
- 승인 없이 `main`에 직접 push 하지 않습니다.

## 기타 규칙

- Git은 빈 폴더를 추적하지 않으므로 필요한 경우 `.gitkeep` 파일을 사용합니다.
- 구조를 먼저 잡기 위한 빈 패키지는 `.gitkeep`으로 유지할 수 있습니다.
- 공통 규칙이 바뀌면 코드만 수정하지 말고 README도 함께 수정합니다.
