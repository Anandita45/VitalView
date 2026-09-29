import subprocess
import time
import sys
import os

def main():
    print("--- VITALVIEW INTEGRATED SYSTEM ---")
    print("[1/2] Booting Liveness AI Server...")
    
    # Start Member 2's server as a background process
    server_cwd = os.path.join(os.getcwd(), "member2", "rppg_folder")
    server_process = subprocess.Popen(
        [sys.executable, "-m", "uvicorn", "main:app", "--port", "8000"],
        cwd=server_cwd
    )
    
    # Give the server 3 seconds to fully wake up before launching the camera
    time.sleep(3)
    
    print("[2/2] Launching Vitals Engine Camera...")
    camera_cwd = os.path.join(os.getcwd(), "Member1")
    camera_process = subprocess.Popen(
        [sys.executable, "vitals.py"],
        cwd=camera_cwd
    )
    
    try:
        # Keep the master script alive until the user closes the camera
        camera_process.wait()
    except KeyboardInterrupt:
        pass
    finally:
        print("\nShutting down integrated system...")
        server_process.terminate() # Safely kill Member 2's server
        print("System offline.")

if __name__ == "__main__":
    main()