## 🚀 Quick Start: Integrated System

This repository features a fully integrated microservice architecture, managed by a master launch script. You no longer need to start the Vitals Engine (Member 1) and the Liveness AI server (Member 2) in separate terminals.

### Prerequisites

Ensure you have Python installed, then install the unified dependencies required for both microservices:

```bash
# Install all required libraries
pip install -r Member1/requirements.txt
```

### 💻 Single-Command Launch

To boot the entire system, navigate to the root `VitalView` directory in your terminal and run the master orchestrator script:

```bash
python run_system.py
```

**What this command does automatically:**

1. **Boot:** Launches the FastAPI Liveness AI server in the background on port `8000`.
2. **Warm-up:** Pauses for 3 seconds to allow the server's WebSocket endpoints to fully initialize.
3. **Launch:** Opens the async multithreaded Vitals Engine camera stream, which connects to the AI server instantly with zero lag.

### 🛑 How to Exit

* To exit the application safely, press the **`q`** key while the camera window is active.
* The master script will capture this input, cleanly close the camera, and automatically terminate the background AI server to prevent open ports. Do not use `Ctrl+C` in the terminal unless necessary.

### 🩺 Best Practices for Accurate Vitals

The Vitals Engine uses highly sensitive computer vision to detect microscopic changes in skin color caused by your pulse. To guarantee accurate readings, please follow these environmental rules:

1. **Lighting is Critical:** Ensure your face is well-lit (facing a bright monitor or lamp). Webcams in dim rooms introduce invisible "digital static" that completely hides your pulse.
2. **Do Not Move:** The engine tracks specific regions of your face. Talking, shifting in your chair, or moving your head will trigger the `motion_artifact` safeguard and reset the calculation. 
3. **The 15-Second Calibration:** The system must collect exactly 450 clean frames (15 seconds) to establish a baseline before it displays your BPM. Sit still like a statue until the initial calibration completes!