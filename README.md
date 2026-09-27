# VitalView 🫀

VitalView is an advanced, contactless health monitoring and biometric screening system. It leverages computer vision, machine learning, and robust backend microservices to extract, analyze, and persist human vital signs directly from standard video feeds (rPPG).

## 🚀 Features

- **Medical-Grade Contactless Vitals (rPPG):** Extracts real-time heart rate and HRV using spatial averaging and Butterworth bandpass filtering on facial video feeds.
- **Biometric Liveness Detection:** AI-driven anti-spoofing to differentiate between live human subjects and photographs.
- **Robust Backend Persistence:** Securely stores biometric sessions and patient data.
- **Automated PDF Reporting:** Generates downloadable clinical-style reports based on extracted biometric benchmarks.
- **Sci-Fi HUD UI:** Features a custom neon-glowing interface with live EKG rendering and continuous vital diagnostics.

## 🏗️ System Architecture & Team Contributions

VitalView is built as a modular system, with distinct services managed by dedicated team members:

* **Member 1 (Vitals Engine):** `[Your Name/Folder]` - Responsible for the core Computer Vision pipeline. Utilizes OpenCV and MediaPipe for facial landmark tracking, signal detrending, and real-time biometric UI rendering.
* **Member 2 (Liveness AI):** `[Friend's Name/Folder]` - Responsible for the Anti-Spoofing and Machine Learning engine to ensure biometric security.
* **Member 3 (Screening Backend):** `Member3/screening-backend` - Manages the primary screening services, APIs, and data routing.
* **Member 4 (Vitals Backend):** `Member4/vitals-backend` - Handles database persistence, biometric benchmarking, and PDF report generation.

## 💻 Technology Stack

* **Computer Vision:** OpenCV, MediaPipe
* **Signal Processing:** SciPy, NumPy (Butterworth filters, peak detection)
* **Backend Services:** Java (Spring Boot / Maven)
* **Architecture:** Modular Microservices

## 🛠️ Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone [https://github.com/Anandita45/VitalView.git](https://github.com/Anandita45/VitalView.git)
   cd VitalView
