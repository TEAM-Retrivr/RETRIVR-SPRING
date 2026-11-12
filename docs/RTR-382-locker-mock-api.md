# RTR-382 사물함 목 API 전달 문서

- 브랜치: `feat/RTR-382-mock-rental-api` (`develop` 기준)
- 범위: 2.1 대여자 / 2.4 대여관리 / 2.5 대여 연장 관리
- 기준 화면: `대여자_26.10.17`, `관리자_26.10.17` 영역.
- Swagger: 개발 서버의 `/swagger-ui/index.html`, `MOCK / 2.1`, `MOCK / 2.4`, `MOCK / 2.5` 태그

## 호출 규칙

모든 성공 응답은 HTTP 200, JSON이며 별도 `data` 래퍼가 없습니다. 날짜는 `yyyy-MM-dd`, 일시는 기존 Jackson 설정에 따라 `yyyy-MM-dd HH:mm`입니다.

`/api/public/v1/**`는 JWT 없이 호출합니다. `/api/admin/v1/**`는 기존 로그인에서 발급한 `Authorization: Bearer <accessToken>`이 필요합니다. 토큰과 로그인 가능한 단체를 검사하는 기존 인증 필터를 그대로 사용합니다. 인증 누락/잘못된 토큰은 401입니다.

이 문서에 나열한 신규 API는 **고정 성공 예시**입니다. 형식 검증은 수행하지만, 존재 여부·소유권·대여 가능 여부·전화 인증 토큰·관리자 코드 토큰의 실제 유효성 검증은 하지 않습니다. DB 조회/저장, SMS/카카오톡 발송, 실제 승인/반납/연장은 수행하지 않습니다. 관리자 인증에 필요한 기존 단체 조회만 발생합니다.

URL ID, 검색어, 필터, 페이지, 요청 본문 값은 성공 응답에 반영하지 않습니다. 아무 유효한 UUID로 호출해도 동일 예시를 반환하며, 승인/반납/연장 후 재조회해도 기존 예시 상태를 유지합니다. 일괄 API도 입력 개수와 무관하게 결과 한 건을 반환합니다. 목록은 항상 `page=0`, `size=20`입니다. 날짜·만료 시각도 고정이므로 현재 시각 기준 타이머 검증에는 사용하지 않습니다.

기존 `/api/admin/v1/locker-zones/**`는 실제 API이며 변경하지 않았습니다. 목 구역 UUID를 기존 실제 API에 전달하지 마세요. 목 관리 화면은 아래 `locker-rentals/overview`로 연동합니다.

## 고정 식별자

| 용도 | 값 |
| --- | --- |
| 4층 구역 | `10000000-0000-0000-0000-000000000001` |
| 사물함 1 | `20000000-0000-0000-0000-000000000001` |
| 대여 요청 | `30000000-0000-0000-0000-000000000001` |
| 대여 공개 ID | `mock-locker-rental-requested` |
| 연장 대상 대여 | `30000000-0000-0000-0000-000000000002` |
| 연장 대상 공개 ID | `mock-locker-rental-2` |
| 연장 요청 | `40000000-0000-0000-0000-000000000001` |

## 2.1 대여자 API

아래 경로 앞에 `/api/public/v1`을 붙입니다.

| 메서드 | 경로 | 용도 / 응답 모델 |
| --- | --- | --- |
| GET | `/organizations/{organizationId}/locker-zones` | 구역 탭 / `LockerMockZones` |
| GET | `/locker-zones/{zoneId}/lockers?availableOnly=true` | 배치·목록 공용 / `LockerMockLayout` |
| GET | `/lockers/{lockerId}/rental-form` | 선택 사물함, 정책, 추가 입력 항목, 약관, 예상 기간 / `LockerMockRentalForm` |
| POST | `/lockers/{lockerId}/rentals` | 신청 / `LockerMockCreated` |
| GET | `/locker-rentals/{publicId}` | 신청 대기 상태 / `LockerMockCreated` |
| POST | `/locker-rentals/{publicId}/approve` | 현장 승인 버튼 / `LockerMockAction` |
| GET | `/lockers/{lockerId}/extension-form` | 연장 전·후 반납일, 추가 입력 항목 / `LockerMockExtensionForm` |
| POST | `/lockers/{lockerId}/extensions` | 연장 신청 / `LockerMockExtensionCreated` |
| GET | `/locker-rentals/{publicId}/extensions/latest` | 연장 대기 상태 / `LockerMockExtensionCreated` |
| POST | `/locker-rentals/{publicId}/extensions/latest/approve` | 현장 연장 승인 버튼 / `LockerMockAction` |

`organizationId`는 기존 단체 ID와 같은 Long, 사물함·구역 ID는 UUID입니다. 공개 사물함 배치와 신청 상태 응답에는 대여자 이름/연락처가 없습니다. 배치와 목록 전환에는 같은 응답을 사용합니다. 좌표 `rowIndex`, `columnIndex`는 0부터 시작합니다. `parentId=null`은 병합되지 않은 사물함입니다.

### 신청 본문

```json
{
  "borrower": {
    "name": "홍길동",
    "phone": "01000000000",
    "email": "borrower@example.com",
    "additionalInfo": {
      "department": "컴퓨터공학과",
      "studentId": "20260000"
    },
    "requestNote": "반납기한 연장 요청",
    "phoneVerificationToken": "mock-phone-token"
  },
  "termsAgreed": true,
  "termsVersion": 1
}
```

연장 신청은 위 본문의 `borrower`만 전달합니다. `name`, `phone`, `additionalInfo`, `phoneVerificationToken`은 필수입니다. 연락처는 숫자 10~11자리, 요청사항은 선택이며 최대 30자입니다. 이메일은 선택입니다. 추가 입력 항목의 키는 폼 응답 `borrowerFields[].fieldKey`를 사용합니다. 목은 맵의 형식만 검사하고 정책별 필수 키 유무까지 검사하지 않습니다.

대여 신청은 `termsAgreed=true`, 양수 `termsVersion`이 필요합니다. 전화 인증 UI는 목 토큰 `mock-phone-token`을 사용할 수 있습니다. 신규 SMS 발송 API는 추가하지 않았습니다.

신청 응답:

```json
{
  "rentalId": "30000000-0000-0000-0000-000000000001",
  "publicId": "mock-locker-rental-requested",
  "status": "REQUESTED",
  "requestedAt": "2026-11-12 10:00",
  "expiresAt": "2026-11-12 10:15"
}
```

현장 승인 본문은 기존 물품 현장 승인 API와 같은 필드입니다. 목에서는 토큰의 공백 여부만 검사합니다.

```json
{
  "adminNameToApprove": "김관리",
  "adminCodeVerificationToken": "mock-admin-code-token"
}
```

## 2.4 대여관리 API

아래 경로 앞에 `/api/admin/v1`을 붙이고 JWT를 전달합니다.

| 메서드 | 경로 | 용도 / 응답 모델 |
| --- | --- | --- |
| GET | `/locker-rentals/overview` | 관리 화면 구역·수량·배치·대여자 / `LockerMockOverview` |
| GET | `/locker-rentals` | 대여 목록·이력 / `LockerMockRentals` |
| GET | `/locker-rentals/{rentalId}` | 대여 상세 / `LockerMockRental` |
| POST | `/locker-rentals/{rentalId}/approve` | 승인 / `LockerMockAction` |
| POST | `/locker-rentals/{rentalId}/reject` | 거절 / `LockerMockAction` |
| POST | `/locker-rentals/{rentalId}/return` | 반납 / `LockerMockAction` |
| POST | `/locker-rentals/approve` | 선택 일괄 승인 / `LockerMockBatchAction` |
| POST | `/locker-rentals/return` | 선택 일괄 반납 / `LockerMockBatchAction` |
| PATCH | `/lockers/availability` | 선택 사물함 가능·불가 처리 / `LockerMockBatchAction` |

`overview`는 `zoneId`, `filter`, `keyword`를 선택 쿼리로 받습니다. `filter`는 `PENDING`, `AVAILABLE`, `RENTED`, `UNAVAILABLE`, `DISABLED`입니다. 선택하지 않으면 전체입니다. `summary`는 선택 구역의 상태별 수량이며, 목의 4층은 전체 6개/승인 대기 1/대여 가능 2/대여 중 2/대여 불가 1입니다. `zones`는 구역 탭용 수량입니다. `lockers[].locker`가 배치 정보, `lockers[].borrower`가 대여자 정보이며 비어 있는 사물함에서는 `borrower=null`입니다.

대여 목록은 `zoneId`, `status`, `keyword`, `page`, `size`를 선택 쿼리로 받습니다. 응답은 `{rentals, page, size, totalElements, totalPages}`입니다. 연체 여부는 `overdue`로 제공하며 enum 상태와 별개입니다.

단건 승인:

```json
{"managerName":"김관리", "collateralConfirmed":true}
```

거절:

```json
{"managerName":"김관리", "rejectReason":"대여 조건 미충족"}
```

단건 반납 및 연장 승인:

```json
{"managerName":"김관리"}
```

일괄 승인:

```json
{
  "rentalIds":["30000000-0000-0000-0000-000000000001"],
  "managerName":"김관리",
  "collateralConfirmed":true
}
```

일괄 반납은 위 본문에서 `collateralConfirmed`를 제외합니다. 선택 ID는 최소 1개, 최대 100개입니다.

대여 가능·불가 처리:

```json
{"lockerIds":["20000000-0000-0000-0000-000000000001"], "available":false}
```

이 목 API는 `available` 값과 무관하게 `UNAVAILABLE` 한 건을 반환합니다. 선택 ID는 최소 1개, 최대 100개입니다.

단건 처리 응답은 `{id, status, processedAt}`, 일괄은 `{results: [{id, status, processedAt}]}`입니다.

## 2.5 대여 연장 관리 API

아래 경로 앞에 `/api/admin/v1`을 붙이고 JWT를 전달합니다.

| 메서드 | 경로 | 용도 / 응답 모델 |
| --- | --- | --- |
| GET | `/locker-rental-extensions` | 연장 요청 목록 / `LockerMockExtensions` |
| GET | `/locker-rental-extensions/{extensionId}` | 요청 상세 / `LockerMockExtension` |
| POST | `/locker-rental-extensions/{extensionId}/approve` | 승인 / `LockerMockAction` |
| POST | `/locker-rental-extensions/{extensionId}/reject` | 거절 / `LockerMockAction` |

목록 쿼리는 `zoneId`, `status`, `keyword`, `page`, `size`입니다. 응답은 `{extensions, page, size, totalElements, totalPages}`입니다. `previousDueDate=2026-11-15`, `newDueDate=2026-11-22`의 7일 연장 예시를 제공합니다. 관리자 승인/거절 본문은 위 예시와 같습니다.

## 상태와 설계 근거

| 구분 | 상태 |
| --- | --- |
| 사물함 운영 표시 | `PENDING`, `AVAILABLE`, `RENTED`, `UNAVAILABLE`, `DISABLED` |
| 대여 | `REQUESTED`, `RENTED`, `REJECTED`, `RETURNED` |
| 연장 요청 | `REQUESTED`, `APPROVED`, `REJECTED` |

`LockerRental`, `LockerRentalExtension`, `LockerBorrowerInfo`, `LockerRentalTerms`, `LockerPolicy`, `LockerBorrowerField`와 V15 사물함 스키마를 기준으로 설계했습니다. enum은 기존 도메인 enum을 그대로 사용합니다.

학과·학번은 JSON 추가 정보에 대응합니다. 화면의 요청사항(`requestNote`), 담보 안내(`collateral`), 담보 확인(`collateralConfirmed`), 요청 만료(`expiresAt`)는 화면을 위한 임시 DTO 계약이며 현재 사물함 테이블에 전용 컬럼이 없습니다. 실제 구현 시 저장 위치와 만료 정책을 확정해야 합니다. 목의 정책값은 대여 3일/최대 연장 2회/연장 7일, 약관 버전 1입니다. 디자인 화면 예시와 DB 구조를 참고한 초안이며 기존 확정 명세를 대체하는 문서는 아닙니다.

새 서비스·리포지토리·마이그레이션은 추가하지 않았습니다. 실제 구현으로 전환할 때 동일 경로의 MockController를 대체하면 됩니다.

## 개발 서버 반영

기존 개발 배포 워크플로는 `develop` push 시 실행됩니다. 이 브랜치 변경을 리뷰·병합하여 기존 배포 흐름으로 반영하면 됩니다. 추가 환경변수나 DB 마이그레이션은 필요하지 않습니다. 기존 서비스의 DB·JWT 등 실행 설정은 그대로 필요합니다. 이 작업에서는 원격 push/병합/배포를 실행하지 않았습니다.

호출 예시 (`BASE_URL`은 배포된 개발 서버 주소):

```sh
curl "$BASE_URL/api/public/v1/organizations/1/locker-zones"
curl "$BASE_URL/api/admin/v1/locker-rentals/overview" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```
