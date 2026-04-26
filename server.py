import asyncio
import json
import os
import threading
import time
from http.server import HTTPServer, SimpleHTTPRequestHandler

import firebase_admin
import websockets
from firebase_admin import credentials, db
from dotenv import load_dotenv

load_dotenv()

FIREBASE_CRED_PATH = os.path.join(os.path.dirname(__file__), "serviceAccountKey.json")
FIREBASE_DB_URL = os.getenv("FIREBASE_DB_URL")
USER_ID = "demo_user_1"
HTTP_PORT = 8080
WS_PORT = 8765
HR_WARN = 100
HR_CRIT = 130

connected_clients: set = set()
main_loop: asyncio.AbstractEventLoop | None = None

overlay_settings = {
    "hr_monitor": True,
    "squat_counter": True,
    "hydration": True,
}

async def _broadcast(payload: dict):
    if not connected_clients:
        return
    msg = json.dumps(payload)
    dead = set()
    for client in list(connected_clients):
        try:
            await client.send(msg)
        except Exception:
            dead.add(client)
    connected_clients.difference_update(dead)

def broadcast(payload: dict):
    if main_loop:
        asyncio.run_coroutine_threadsafe(_broadcast(payload), main_loop)

def on_vitals(event):
    data = event.data
    if not isinstance(data, dict):
        return
    hr = data.get("hr")
    if hr is None:
        return
    state = "normal"
    if hr >= HR_CRIT:
        state = "critical"
    elif hr >= HR_WARN:
        state = "warning"
    broadcast({"msg_type": "hr_update", "hr": hr, "state": state})
    print(f"-> HR: {hr} bpm [{state}]")

def on_settings(event):
    data = event.data
    if not isinstance(data, dict):
        return
    overlay_settings.update(data)
    broadcast({"msg_type": "settings_update", "settings": overlay_settings.copy()})

def poll_firebase():
    base = f"users/{USER_ID}"
    last_squats = {}
    last_hydration = {}

    while True:
        try:
            squats = db.reference(f"{base}/squats").get()
            if isinstance(squats, dict) and squats != last_squats:
                last_squats = squats.copy()
                count = squats.get("count", 0)
                done = squats.get("done", 0)
                broadcast({"msg_type": "squat_update", "count": count, "done": done})
                print(f"-> Squats: count={count}, done={done}")

            hydration = db.reference(f"{base}/hydration").get()
            if isinstance(hydration, dict) and hydration != last_hydration:
                last_hydration = hydration.copy()
                count = hydration.get("count", 0)
                broadcast({"msg_type": "hydration_update", "count": count})
                print(f"-> Hydration: {count}")

        except Exception as e:
            print(f"[poll] Error: {e}")

        time.sleep(1)

def start_firebase():
    cred = credentials.Certificate(FIREBASE_CRED_PATH)
    firebase_admin.initialize_app(cred, {"databaseURL": FIREBASE_DB_URL})
    print(f"[firebase] Connected to {FIREBASE_DB_URL}")
    base = f"users/{USER_ID}"
    db.reference(f"{base}/vitals").listen(on_vitals)
    db.reference(f"{base}/settings/overlays").listen(on_settings)
    print("[firebase] Listeners started")
    threading.Thread(target=poll_firebase, daemon=True).start()
    print("[firebase] Polling started")

async def ws_handler(ws):
    connected_clients.add(ws)
    print(f"[ws] +client ({len(connected_clients)} connected)")

    await ws.send(json.dumps({
        "msg_type": "settings_update",
        "settings": overlay_settings.copy(),
    }))

    try:
        base = f"users/{USER_ID}"
        squats = db.reference(f"{base}/squats").get()
        if isinstance(squats, dict):
            await ws.send(json.dumps({
                "msg_type": "squat_update",
                "count": squats.get("count", 0),
                "done": squats.get("done", 0),
            }))
        hydration = db.reference(f"{base}/hydration").get()
        if isinstance(hydration, dict):
            await ws.send(json.dumps({
                "msg_type": "hydration_update",
                "count": hydration.get("count", 0),
            }))
        vitals = db.reference(f"{base}/vitals").get()
        if isinstance(vitals, dict) and vitals.get("hr"):
            hr = vitals["hr"]
            state = "normal"
            if hr >= HR_CRIT:
                state = "critical"
            elif hr >= HR_WARN:
                state = "warning"
            await ws.send(json.dumps({
                "msg_type": "hr_update",
                "hr": hr,
                "state": state,
            }))
    except Exception as e:
        print(f"[ws] Error sending initial state: {e}")

    try:
        await ws.wait_closed()
    finally:
        connected_clients.discard(ws)

class SilentHandler(SimpleHTTPRequestHandler):
    def log_message(self, *args):
        pass

def start_http():
    HTTPServer(("localhost", HTTP_PORT), SilentHandler).serve_forever()

async def main():
    global main_loop
    main_loop = asyncio.get_event_loop()
    threading.Thread(target=start_firebase, daemon=True).start()
    threading.Thread(target=start_http, daemon=True).start()
    print(f"[http] http://localhost:{HTTP_PORT}/overlay.html")
    print(f"[ws]   ws://localhost:{WS_PORT}")
    print("Waiting for events...\n")
    async with websockets.serve(ws_handler, "localhost", WS_PORT):
        await asyncio.Future()

if __name__ == "__main__":
    asyncio.run(main())