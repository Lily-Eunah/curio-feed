# Curio Feed Phase 1 배포 및 검증 가이드 (도메인 구매 전)

이 가이드는 실제 서비스용 도메인을 구매하기 전, Oracle Cloud Always Free VM과 Neon PostgreSQL DB, Cloudflare Pages 프론트엔드를 활용해 안전하게 인프라를 구동하고 임시로 기능을 검증하기 위한 가이드라인입니다.

---

## ⚠️ 중요 보안 및 제약 사항

1. **외부 8080 포트 차단 (보안)**:
   * Spring Boot 백엔드는 외부의 임의 공격에 노출되지 않도록 Oracle VM의 내부 루프백 주소인 `127.0.0.1:8080`에만 바인딩되어 실행됩니다.
   * VM 외부의 8080 포트를 방화벽으로 개방하지 않습니다.
2. **Mixed Content 제한 우회**:
   * Cloudflare Pages는 항상 HTTPS (`https://*.pages.dev`)로 서비스됩니다.
   * 도메인 구매 전, 프론트엔드에서 Oracle VM의 공인 IP(`http://<VM_IP>:8080`)로 HTTP 통신을 요청하는 경우 브라우저 보안에 의해 차단(Mixed Content)됩니다.
   * 따라서, 도메인 구매 전 실기기 연동 테스트 시에는 **Cloudflare Quick Tunnel**을 임시 HTTPS 터널로 사용하여 검증을 진행합니다.
3. **민감 정보 보호**:
   * `.env` 파일(데이터베이스 비밀번호, API 키 등 포함)은 절대 GitHub 등 원격 레포지토리에 푸시하지 않습니다.

---

## 1. 데이터베이스 준비 (Neon PostgreSQL)

1. [Neon.tech](https://neon.tech/)에 가입하고 프로젝트를 생성합니다.
2. 생성된 PostgreSQL의 **Connection String(JDBC URL)**을 획득합니다.
   * JDBC URL은 반드시 SSL 모드를 요구하는 다음 형식을 따라야 합니다:
     `jdbc:postgresql://<neon-host-name>/curio_feed?sslmode=require`

---

## 2. Oracle VM 서버 세팅 및 백엔드 실행

1. **TimeZone 설정**:
   ```bash
   sudo timedatectl set-timezone Asia/Seoul
   ```
2. **필수 패키지 및 Docker/Compose 설치**:
   * Ubuntu VM에 Docker 및 Docker Compose가 설치되어 있어야 합니다.
3. **방화벽 설정 (UFW)**:
   ```bash
   sudo ufw default deny incoming
   sudo ufw default allow outgoing
   sudo ufw allow 22/tcp      # SSH (접속 허용)
   sudo ufw allow 80/tcp      # HTTP (Caddy 인증용)
   sudo ufw allow 443/tcp     # HTTPS
   sudo ufw enable
   ```
4. **환경변수 파일 작성**:
   * VM 내 `/opt/curiofeed/infra/` 경로에 `.env` 파일을 생성하고 내용을 채웁니다. (`.env.example` 파일 참조)
5. **백엔드 최초 배포 명령어 (순서대로 실행)**:
   * 배포 디렉토리(`/opt/curiofeed/`)에서 아래 명령어를 **순서대로** 실행합니다.
     ```bash
     # 1. 설정 유효성 검증 (컨테이너 실제 실행 전 dry-run)
     docker compose -f infra/docker-compose.prod.yml config

     # 2. 이미지 빌드 (최초 실행 또는 코드 변경 후)
     docker compose -f infra/docker-compose.prod.yml build

     # 3. 컨테이너 백그라운드 실행
     docker compose -f infra/docker-compose.prod.yml up -d

     # 4. 헬스체크 — "status":"UP" 응답이 나와야 정상
     curl http://127.0.0.1:8080/actuator/health
     ```
   * `config` 명령은 `.env` 변수 치환 및 YAML 파싱 오류를 실행 전에 미리 잡아줍니다.
   * `curl` 응답이 `{"status":"UP"}` 이면 Flyway 마이그레이션 및 DB 연결 모두 정상입니다.

---

## 3. Cloudflare Quick Tunnel을 통한 임시 HTTPS 검증

도메인 구매 전에 Cloudflare Pages에 배포된 프론트엔드와 VM의 백엔드를 연동해 보려면 아래의 임시 터널링 방식을 이용합니다.

1. **VM 서버 내에서 Quick Tunnel 실행**:
   * VM 내부에서 아래 명령어를 실행하여 백엔드 루프백(`127.0.0.1:8080`)을 안전한 임시 HTTPS 주소로 터널링합니다.
     ```bash
     cloudflared tunnel --url http://localhost:8080
     ```
   * 터미널 로그에 생성되는 `https://*.trycloudflare.com` 형식의 임시 URL 주소를 복사합니다.
2. **CORS 및 프론트엔드 환경변수 적용**:
   * **Backend**: VM의 `infra/.env` 파일에서 `APP_CORS_ALLOWED_ORIGINS` 값에 복사한 터널 도메인을 추가하고 백엔드 컨테이너를 재시작합니다.
   * **Frontend**: Cloudflare Pages 대시보드의 환경변수 설정에서 `VITE_API_BASE_URL` 값을 복사한 터널 도메인 주소로 입력하고 재배포를 진행합니다.
3. **기능 검증**:
   * 이제 Pages 기본 주소(`https://*.pages.dev`)에 접속하여 Mixed Content 차단 없이 모바일 UI에서 아티클 조회 및 퀴즈 등의 전체 기능을 수월하게 검증할 수 있습니다.
   * *주의*: Quick Tunnel 도메인은 터널 프로세스가 종료되면 만료되는 **1회용 임시 테스트 주소**입니다. 실제 서비스 오픈은 도메인 구매 후 진행합니다.

---

## 4. 실서버 점검 및 관리 명령어

VM 및 컨테이너 상태를 상시 진단할 때 아래 명령을 활용합니다.

```bash
# 1. 디스크 공간 상태 점검
df -h

# 2. 시스템 메모리 여유 공간 점검
free -m

# 3. Docker 리소스 및 빌드 캐시 점검
docker system df

# 4. 백엔드 컨테이너 작동 여부 확인
docker compose -f infra/docker-compose.prod.yml ps

# 5. 실시간 에러/로그 출력 (최신 100줄)
docker compose -f infra/docker-compose.prod.yml logs --tail=100 backend

# 6. 로컬 헬스체크 정상 작동 판별
curl http://127.0.0.1:8080/actuator/health
```

---

## 5. Oracle VM 아키텍처 호환성

Oracle Cloud Always Free VM은 **AMD (x86_64)** 또는 **ARM Ampere A1 (aarch64)** 인스턴스를 선택할 수 있습니다.

백엔드 `Dockerfile`은 `eclipse-temurin:21-jdk-alpine` 및 `eclipse-temurin:21-jre-alpine` 이미지를 기반으로 합니다. Eclipse Temurin Java 21 이미지는 **multi-arch 이미지**로 제공되어 `linux/amd64`와 `linux/arm64` 모두 지원합니다. Oracle ARM 인스턴스에서도 별도 설정 없이 `docker compose build` 및 실행이 가능합니다.

단, 아래 조건에 해당하는 경우 추가 확인이 필요합니다:

* JNI(Java Native Interface)를 사용하는 라이브러리가 의존성에 포함된 경우
* native binary가 포함된 third-party JAR을 사용하는 경우

현재 백엔드는 PostgreSQL JDBC, Spring Boot, Flyway 등 순수 JVM 기반 라이브러리만 사용하므로 ARM 호환 문제는 없습니다.

---

## ⛔ 금지 및 주의사항

* 도메인 연결 설정(`api.curiofeed.com`)을 아직 구매하지 않은 상태에서 강제로 Caddy에 주입하거나 DNS 설정을 시도하지 마세요. (인증서 발급에 연속 실패하여 IP 차단 등의 제재를 받을 수 있습니다.)
* `.env` 파일을 로컬 또는 원격 Git 저장소에 푸시(Commit)하지 않도록 항시 검증하십시오.
