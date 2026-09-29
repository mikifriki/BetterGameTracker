"""Smoke-test a packaged JAR or native executable; Python is needed only for CI."""
import json
import base64
from http.cookiejar import CookieJar
import os
import shutil
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path
from contextlib import contextmanager


@contextmanager
def application(command, cwd, environment, log_path, arguments=(), expect_failure=False):
    urllib.request.install_opener(urllib.request.build_opener(
        urllib.request.ProxyHandler({}), urllib.request.HTTPCookieProcessor(CookieJar())))
    with socket.socket() as port_socket:
        port_socket.bind(("127.0.0.1", 0))
        port = port_socket.getsockname()[1]
    with log_path.open("w+") as log:
        process = subprocess.Popen(command + [
            "--spring.profiles.active=local", f"--server.port={port}",
            *arguments,
        ], cwd=cwd, env=environment, stdout=log, stderr=subprocess.STDOUT)
        try:
            if expect_failure:
                assert process.wait(timeout=90) != 0, "Obstructed storage unexpectedly started"
                log.seek(0)
                assert "Could not initialize local storage" in log.read()
                yield None
                return
            base = f"http://127.0.0.1:{port}"
            for attempt in range(90):
                if process.poll() is not None:
                    raise RuntimeError("Application exited before startup")
                try:
                    with urllib.request.urlopen(base + "/", timeout=2) as response:
                        assert b"BetterGameTracker" in response.read()
                    break
                except (urllib.error.URLError, TimeoutError):
                    time.sleep(1)
            else:
                raise RuntimeError("Application did not start within 90 seconds")

            yield base
        except Exception:
            log.seek(0)
            output = log.read()
            Path("build").mkdir(exist_ok=True)
            Path("build/smoke-failure.log").write_text(output)
            print("\n".join(output.splitlines()[-60:]))
            raise
        finally:
            process.terminate()
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait()


def request(base, path, method="GET", body=None):
    data = None if body is None else json.dumps(body).encode()
    req = urllib.request.Request(base + path, data, method=method,
                                 headers={"Content-Type": "application/json"})
    if method != "GET":
        session = request(base, "/api/v1/session")
        if session["csrfToken"]:
            req.add_header(session["csrfHeader"], session["csrfToken"])
    with urllib.request.urlopen(req, timeout=10) as response:
        payload = response.read()
        return json.loads(payload) if payload else None


with tempfile.TemporaryDirectory(prefix="bgt-smoke-") as directory:
    root = Path(directory)
    installation = root / "Installation with spaces ü"
    first_launch = root / "first launch"
    second_launch = root / "second launch"
    home = root / "isolated home"
    for path in (installation, first_launch, second_launch, home):
        path.mkdir()
    environment = {key: value for key, value in os.environ.items()
                   if not key.startswith(("BETTER_GAME_TRACKER_", "SPRING_", "SERVER_"))
                   and key not in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")}
    environment.update(HOME=str(home), USERPROFILE=str(home),
                       LOCALAPPDATA=str(home / "local"), APPDATA=str(home / "roaming"),
                       XDG_DATA_HOME=str(home / "data"))
    command = sys.argv[1:]
    if len(command) == 3 and command[1] == "-jar":
        artifact_index = 2
        command[0] = shutil.which(command[0]) or command[0]
    elif len(command) == 1:
        artifact_index = 0
    else:
        raise SystemExit("Usage: smoke.py java -jar app.jar OR native-executable")
    artifact = Path(command[artifact_index]).resolve(strict=True)
    packaged_artifact = installation / artifact.name
    shutil.copy2(artifact, packaged_artifact)
    command[artifact_index] = str(packaged_artifact)

    with application(command, first_launch, environment, root / "first.log") as base:
        assert request(base, "/api/v1/session")["hosted"] is False
        assert request(base, "/api/v1/session")["csrfToken"]
        for blocked in (
            urllib.request.Request(base + "/api/v1/games", b'{"gameTitle":"Blocked"}',
                                   headers={"Content-Type": "application/json"}, method="POST"),
            urllib.request.Request(base + "/api/v1/session", headers={"Origin": "https://evil.example"}),
            urllib.request.Request(base + "/api/v1/session", headers={"Host": "evil.example"}),
        ):
            try:
                urllib.request.urlopen(blocked, timeout=10)
                raise AssertionError("Unsafe LAN request was accepted")
            except urllib.error.HTTPError as error:
                assert error.code == 403
        print("Host/Origin checks and CSRF protection passed", flush=True)
        game = request(base, "/api/v1/games", "POST", {
            "gameTitle": "Smoke test", "description": "Test", "releasePlatform": "PC",
            "releaseDate": "2026-01-01", "developer": "Studio", "metaRating": 9,
            "userRating": 9, "physicalCopy": False,
        })
        game_path = "/api/v1/games/" + game["id"]
        play = request(base, game_path + "/plays", "POST", {
            "playthroughRating": 9, "completionDate": "2026-01-01", "platformPlayedOn": "PC",
            "timeToBeatMinutes": 30, "completionStatus": "COMPLETE", "location": "Home",
        })
        time_path = game_path + "/plays/" + play["id"] + "/time-entries"
        entry = request(base, time_path, "POST", {"date": "2026-09-08", "durationMinutes": 30})
        assert request(base, time_path)[0]["id"] == entry["id"]
        request(base, time_path + "/" + entry["id"], "PUT", {"date": "2026-09-09", "durationMinutes": 60})
        assert request(base, time_path)[0]["durationMinutes"] == 60
        play_path = game_path + "/plays/" + play["id"]
        assert request(base, play_path)["timeToBeatMinutes"] == 30
        assert request(base, play_path)["calculatedTimeMinutes"] == 60
        assert request(base, game_path + "/plays")[0]["calculatedTimeMinutes"] == 60
        review_path = game_path + "/plays/" + play["id"] + "/reviews"
        review = request(base, review_path, "POST", {"reviewTitle": "Smoke review"})
        assert request(base, review_path)[0]["reviewTitle"] == "Smoke review"
        png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD3sAAAAASUVORK5CYII=")
        upload = (b'--bgt-boundary\r\nContent-Disposition: form-data; name="file"; filename="cover.png"\r\n'
                  b'Content-Type: image/png\r\n\r\n' + png + b'\r\n--bgt-boundary--\r\n')
        req = urllib.request.Request(base + game_path + "/cover", upload, method="PUT",
                                     headers={"Content-Type": "multipart/form-data; boundary=bgt-boundary"})
        session = request(base, "/api/v1/session")
        if session["csrfToken"]:
            req.add_header(session["csrfHeader"], session["csrfToken"])
        with urllib.request.urlopen(req, timeout=10) as response:
            assert response.status == 204
        with urllib.request.urlopen(base + game_path + "/cover", timeout=10) as response:
            assert response.read() == png
        assert (installation / "db/better-game-tracker.db").is_file()
        cover = installation / "db/covers" / (game["id"] + ".image")
        assert cover.read_bytes() == png

    with application(command, second_launch, environment, root / "restart.log") as base:
        assert request(base, game_path) == game
        assert request(base, play_path)["startDate"] == play["startDate"]
        assert request(base, play_path)["timeToBeatMinutes"] == 30
        assert request(base, play_path)["calculatedTimeMinutes"] == 60
        assert request(base, time_path)[0]["id"] == entry["id"]
        assert request(base, time_path)[0]["durationMinutes"] == 60
        assert request(base, review_path) == [review]
        with urllib.request.urlopen(base + game_path + "/cover", timeout=10) as response:
            assert response.read() == png
        request(base, game_path, "DELETE")
        assert request(base, "/api/v1/games") == []
        assert not cover.exists()
    for path in (first_launch, second_launch, home):
        assert not list(path.rglob("*.db")), f"Unexpected database under {path}"
        assert not list(path.rglob("*.image")), f"Unexpected cover under {path}"
    print("Default packaged storage and persistence across launch directories passed", flush=True)

    override = root / "explicit storage" / "override.db"
    with application(command, first_launch, environment, root / "override.log",
                     [f"--BETTER_GAME_TRACKER_DATABASE_PATH={override}"]) as base:
        assert request(base, "/api/v1/games") == []
        assert override.is_file()
        assert Path(str(override) + ".covers").is_dir()
        request(base, "/api/v1/games", "POST", {"gameTitle": "Separate database"})
    print("Explicit database override passed", flush=True)

    (installation / "db").rename(installation / "saved-db")
    (installation / "db").write_text("Storage obstruction for smoke test")
    with application(command, second_launch, environment, root / "obstructed.log",
                     expect_failure=True):
        pass
    for path in (first_launch, second_launch, home):
        assert not list(path.rglob("*.db")), f"Unexpected fallback database under {path}"
    print("Obstructed storage fails without fallback; packaged application smoke test passed", flush=True)
