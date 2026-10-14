import socket
import concurrent.futures
import subprocess
import sys

TARGET_IP = "192.168.1.21"
print(f"Scanning {TARGET_IP} for active ADB Wireless port (30000-50000)...")

open_ports = []

def check_port(port):
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(0.3)
        if s.connect_ex((TARGET_IP, port)) == 0:
            return port
    return None

with concurrent.futures.ThreadPoolExecutor(max_workers=250) as executor:
    futures = [executor.submit(check_port, p) for p in range(30000, 50000)]
    for f in concurrent.futures.as_completed(futures):
        res = f.result()
        if res:
            open_ports.append(res)
            print(f"Found open port: {TARGET_IP}:{res}")

if not open_ports:
    print(f"No open port found on {TARGET_IP} in range 30000-50000.")
    print("Please check the port displayed under 'Wireless debugging' on your phone.")
else:
    for port in open_ports:
        print(f"Attempting to connect: adb connect {TARGET_IP}:{port}")
        out = subprocess.run(["adb", "connect", f"{TARGET_IP}:{port}"], capture_output=True, text=True)
        print(out.stdout.strip())
        devices = subprocess.run(["adb", "devices"], capture_output=True, text=True)
        print(devices.stdout.strip())
        if "device\n" in devices.stdout or devices.stdout.strip().endswith("device"):
            print("Successfully connected!")
            break
