"""Smoke-test a packaged JAR or native executable; Python is needed only for CI."""
import json
import base64
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request
from pathlib import Path

with tempfile.TemporaryDirectory(prefix="bgt-smoke-") as directory:
    root = Path(directory)
    command = sys.argv[1:] + [
        "--server.port=18081",
        "--better-game-tracker.open-browser=false",
        f"--BETTER_GAME_TRACKER_DATABASE_PATH={root / 'app.db'}",
    ]
    with (root / "app.log").open("w+") as log:
        process = subprocess.Popen(command, stdout=log, stderr=subprocess.STDOUT)
        try:
            base = "http://127.0.0.1:18081"
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

            def request(path, method="GET", body=None):
                data = None if body is None else json.dumps(body).encode()
                req = urllib.request.Request(base + path, data, method=method,
                                             headers={"Content-Type": "application/json"})
                with urllib.request.urlopen(req, timeout=10) as response:
                    payload = response.read()
                    return json.loads(payload) if payload else None

            assert request("/api/v1/session")["hosted"] is False
            game = request("/api/v1/games", "POST", {
                "gameTitle": "Smoke test", "description": "Test", "releasePlatform": "PC",
                "releaseDate": "2026", "developer": "Studio", "metaRating": "90",
                "userRating": "9", "physicalCopy": "No",
            })
            game_path = "/api/v1/games/" + game["id"]
            play = request(game_path + "/plays", "POST", {
                "playthroughRating": "9", "completionDate": "2026", "platformPlayedOn": "PC",
                "timeToBeat": "30", "completionRate": "100%", "location": "Home",
            })
            time_path = game_path + "/plays/" + play["id"] + "/time-entries"
            entry = request(time_path, "POST", {"date": "2026-09-08", "durationMinutes": 30})
            assert request(time_path)[0]["id"] == entry["id"]
            request(time_path + "/" + entry["id"], "PUT", {"date": "2026-09-09", "durationMinutes": 60})
            assert request(time_path)[0]["durationMinutes"] == 60
            review_path = game_path + "/plays/" + play["id"] + "/reviews"
            request(review_path, "POST", {"reviewTitle": "Smoke review"})
            assert request(review_path)[0]["reviewTitle"] == "Smoke review"
            png = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aD3sAAAAASUVORK5CYII=")
            upload = (b'--bgt-boundary\r\nContent-Disposition: form-data; name="file"; filename="cover.png"\r\n'
                      b'Content-Type: image/png\r\n\r\n' + png + b'\r\n--bgt-boundary--\r\n')
            req = urllib.request.Request(base + game_path + "/cover", upload, method="PUT",
                                         headers={"Content-Type": "multipart/form-data; boundary=bgt-boundary"})
            with urllib.request.urlopen(req, timeout=10) as response:
                assert response.status == 204
            with urllib.request.urlopen(base + game_path + "/cover", timeout=10) as response:
                assert response.read() == png
            request(game_path, "DELETE")
            assert request("/api/v1/games") == []
            print("Packaged application smoke test passed")
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
