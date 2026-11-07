#!/usr/bin/env python3
"""차량 앱이 쓰는 백엔드 API를 앱과 같은 순서·같은 본문으로 호출해 보는 E2E 점검.

    python3 scripts/api_e2e.py                      # 운영 서버
    WAYFIT_API=http://localhost:8787 python3 scripts/api_e2e.py

실행할 때마다 새 테스트 기기를 등록한다. 실제 서버에 기록이 남는다:
기기 1대, 페어링 코드 1개, 방문 4건(취소·거절·인증·하루 제한), 쿠폰 1장.
노출(impression)은 사장님 통계에 잡히므로 --with-impression 을 줄 때만 보낸다.
표준 라이브러리만 쓴다. 토큰은 출력하지 않는다.
"""
import argparse
import json
import os
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

BASE = os.environ.get("WAYFIT_API", "https://wayfit-backend.dannyldj.workers.dev").rstrip("/")
# 앱 DefaultStoreRepository 의 데모 경로 (아주대 → 광교호수공원)
ORIGIN = (37.2830, 127.0443)
DEST = (37.2850, 127.0660)
# Cloudflare 가 Python 기본 User-Agent 를 403 으로 막는다
USER_AGENT = "wayfit-api-e2e/1.0"

passed = 0


def call(method, path, token=None, body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(BASE + path, data=data, method=method)
    req.add_header("User-Agent", USER_AGENT)
    if data is not None:
        req.add_header("Content-Type", "application/json")
    if method == "POST" and data is None:
        req.data = b""
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=15) as res:
            status, raw = res.status, res.read()
    except urllib.error.HTTPError as e:
        status, raw = e.code, e.read()
    try:
        return status, json.loads(raw) if raw else None
    except ValueError:
        return status, raw.decode(errors="replace")


def check(name, condition, detail=""):
    global passed
    if not condition:
        print(f"  FAIL  {name}  {detail}")
        sys.exit(1)
    passed += 1
    print(f"  ok    {name}")


def expect(name, result, status, code=None):
    got_status, body = result
    got_code = body.get("error", {}).get("code") if isinstance(body, dict) else None
    ok = got_status == status and (code is None or got_code == code)
    check(name, ok, f"→ HTTP {got_status} {got_code or ''} {str(body)[:200]}")
    return body


def now():
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z")


def evidence(kind, at=None, source="FUSED"):
    e = {"type": kind, "occurredAt": now()}
    if at:
        e.update(lat=at[0], lng=at[1])
        if kind in ("LOCATION", "GEOFENCE_ENTER"):
            e["source"] = source
    return e


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--with-impression", action="store_true", help="노출 기록도 보낸다 (통계에 남음)")
    args = parser.parse_args()
    print(f"서버: {BASE}")

    print("[기본]")
    expect("헬스체크", call("GET", "/v1/health"), 200)
    expect("토큰 없이 호출하면 401", call("GET", "/v1/devices/me/coupons"), 401, "UNAUTHORIZED")
    expect("잘못된 토큰이면 401", call("GET", "/v1/devices/me/coupons", "wfd_bogus"), 401, "UNAUTHORIZED")

    print("[기기 등록]")
    reg = expect("POST /v1/devices", call("POST", "/v1/devices"), 201)
    token = reg["deviceToken"]
    check("토큰은 wfd_ 로 시작", token.startswith("wfd_"))

    print("[매장 추천]")
    q = f"lat={ORIGIN[0]}&lng={ORIGIN[1]}&destLat={DEST[0]}&destLng={DEST[1]}"
    stores = expect("경로 위 매장 조회", call("GET", f"/v1/stores/nearby?{q}", token), 200)["stores"]
    check("데모 경로에 매장이 있다", len(stores) > 0)
    store = stores[0]
    check("앱 모델에 필요한 필드가 다 있다",
          all(k in store for k in ("storeId", "name", "category", "benefitText", "lat", "lng", "distanceM", "detourM")))
    check("카테고리가 앱이 아는 값", store["category"] in ("CAFE", "FOOD", "BAKERY", "ETC"), store["category"])
    expect("잘못된 좌표는 400", call("GET", "/v1/stores/nearby?lat=999&lng=127", token), 400, "VALIDATION_FAILED")
    trip = "trip_e2e_" + datetime.now().strftime("%Y%m%d_%H%M%S")
    if args.with_impression:
        first = expect("노출 기록", call("POST", "/v1/impressions", token, {"storeId": store["storeId"], "tripId": trip}), 200)
        again = expect("같은 트립 중복 노출", call("POST", "/v1/impressions", token, {"storeId": store["storeId"], "tripId": trip}), 200)
        check("중복이면 recorded=false, 같은 ID", not again["recorded"] and again["impressionId"] == first["impressionId"])

    print("[방문 취소]")
    v = expect("방문 생성", call("POST", "/v1/visits", token, {"storeId": store["storeId"], "tripId": trip}), 201)["visit"]
    check("상태 ACCEPTED", v["status"] == "ACCEPTED", v["status"])
    expect("방문 취소", call("DELETE", f"/v1/visits/{v['id']}", token), 200)
    expect("취소한 방문은 인증 불가", call("POST", f"/v1/visits/{v['id']}/verify", token, {"evidence": []}), 409)

    print("[인증 거절]")
    v = expect("방문 생성", call("POST", "/v1/visits", token, {"storeId": store["storeId"], "tripId": trip}), 201)["visit"]
    far = (store["lat"] + 0.01, store["lng"])  # 약 1.1km 북쪽
    expect("반경 밖이면 422 VISIT_OUT_OF_RADIUS",
           call("POST", f"/v1/visits/{v['id']}/verify", token, {"evidence": [evidence("LOCATION", far), evidence("GEAR_PARK", far)]}),
           422, "VISIT_OUT_OF_RADIUS")

    print("[인증 성공]")
    v = expect("방문 생성", call("POST", "/v1/visits", token, {"storeId": store["storeId"], "tripId": trip}), 201)["visit"]
    at = (store["lat"], store["lng"])
    # 앱 VisitUseCase 와 같은 모양: 도착 증거(좌표·출처) + 경유지 도착 + P단(좌표, 출처 없음)
    proof = [evidence("GEOFENCE_ENTER", at), evidence("WAYPOINT_ARRIVED", at), evidence("GEAR_PARK", at)]
    res = expect("증거 제출 → 인증", call("POST", f"/v1/visits/{v['id']}/verify", token, {"evidence": proof}), 200)
    coupon = res.get("coupon") or {}
    check("방문 VERIFIED", res["visit"]["status"] == "VERIFIED", res["visit"]["status"])
    check("쿠폰 코드 6자리 숫자", re.fullmatch(r"\d{6}", coupon.get("code6", "")) is not None)
    check("쿠폰 필드(앱 CouponDto)",
          all(k in coupon for k in ("id", "storeNameSnapshot", "benefitSnapshot", "status", "issuedAt", "expiresAt")))
    again = expect("같은 방문 다시 인증(멱등)", call("POST", f"/v1/visits/{v['id']}/verify", token, {"evidence": proof}), 200)
    check("같은 쿠폰을 돌려준다", (again.get("coupon") or {}).get("id") == coupon["id"])
    mine = expect("기기 쿠폰함", call("GET", "/v1/devices/me/coupons", token), 200)["coupons"]
    check("방금 받은 쿠폰이 맨 앞", mine and mine[0]["id"] == coupon["id"])
    v = expect("같은 날 같은 매장 방문 생성", call("POST", "/v1/visits", token, {"storeId": store["storeId"], "tripId": trip}), 201)["visit"]
    expect("하루 1장 제한 422 DAILY_LIMIT",
           call("POST", f"/v1/visits/{v['id']}/verify", token, {"evidence": proof}), 422, "DAILY_LIMIT")

    print("[폰 연결]")
    code = expect("페어링 코드 발급", call("POST", "/v1/pairing-codes", token), 201)
    check("8자리 숫자", re.fullmatch(r"\d{8}", code["code"]) is not None, code["code"])
    check("QR 주소에 코드가 들어 있다", code["pairingUrl"].endswith(code["code"]), code["pairingUrl"])
    status = expect("연결된 폰 수", call("GET", "/v1/devices/me/pairing", token), 200)
    check("새 기기는 0대", status["linkedUserCount"] == 0)

    print(f"\n통과 {passed}개")


if __name__ == "__main__":
    main()
