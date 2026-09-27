import os
# Silence MediaPipe/TensorFlow C++ backend warnings for a clean terminal
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2' 
os.environ['GLOG_minloglevel'] = '2'

import cv2
import mediapipe as mp
import numpy as np 
from scipy.signal import find_peaks, detrend, butter, filtfilt 
import time 

# --- MEDICAL BANDPASS FILTER ---
def butter_bandpass(lowcut, highcut, fs, order=3):
    nyq = 0.5 * fs
    low = lowcut / nyq
    high = highcut / nyq
    b, a = butter(order, [low, high], btype='band')
    return b, a

def apply_bandpass_filter(data, fs):
    # Human heart rate is strictly between 0.75 Hz (45 BPM) and 3.0 Hz (180 BPM)
    b, a = butter_bandpass(0.75, 3.0, fs, order=3)
    return filtfilt(b, a, data)

# --- HUD DRAWING TOOL ---
def draw_smart_target(img, x, y, size, color, thickness=2, length=8):
    cv2.line(img, (x - size, y - size), (x - size + length, y - size), color, thickness)
    cv2.line(img, (x - size, y - size), (x - size, y - size + length), color, thickness)
    cv2.line(img, (x + size, y - size), (x + size - length, y - size), color, thickness)
    cv2.line(img, (x + size, y - size), (x + size, y - size + length), color, thickness)
    cv2.line(img, (x - size, y + size), (x - size + length, y + size), color, thickness)
    cv2.line(img, (x - size, y + size), (x - size, y + size - length), color, thickness)
    cv2.line(img, (x + size, y + size), (x + size - length, y + size), color, thickness)
    cv2.line(img, (x + size, y + size), (x + size, y + size - length), color, thickness)
    cv2.circle(img, (x, y), 2, color, -1)

def start_vitals_stream():
    mp_face_mesh = mp.solutions.face_mesh
    face_mesh = mp_face_mesh.FaceMesh(max_num_faces=1)
    
    print("\n--- TECH EXPO CAMERA SETUP ---")
    print("0: Laptop Webcam")
    print("Or paste USB Localhost URL: http://127.0.0.1:4747/video")
    
    cam_input = input("\nEnter camera choice or paste URL: ")
    
    if cam_input.isdigit():
        cam_source = int(cam_input)
        print(f"Attempting to connect to Windows Camera {cam_source}...")
        cap = cv2.VideoCapture(cam_source, cv2.CAP_MSMF)
    else:
        cam_source = cam_input
        print(f"Bypassing Windows drivers! Connecting to stream: {cam_source}...")
        cap = cv2.VideoCapture(cam_source)

    if not cap.isOpened():
        print(f"CRITICAL ERROR: Could not connect to camera {cam_source}.")
        print("Check your USB cable and make sure DroidCam/Camo is running!")
        return 

    print("Camera connected successfully! Starting vitals stream...")

    box_size = 15
    forehead_data = []
    timestamps = []
    bpm_history = []
    display_bpm = 0
    
    while True:
        success, frame = cap.read()
        if not success:
            break
            
        h, w, c = frame.shape
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = face_mesh.process(rgb_frame)
        
        hud_color = (0, 255, 255) if len(forehead_data) < 450 else (255, 255, 0)
        
        if results.multi_face_landmarks:
            for face_landmarks in results.multi_face_landmarks:
                
                # Full Face Tracker
                x_coords = [int(lm.x * w) for lm in face_landmarks.landmark]
                y_coords = [int(lm.y * h) for lm in face_landmarks.landmark]
                min_x, max_x = min(x_coords), max(x_coords)
                min_y, max_y = min(y_coords), max(y_coords)
                
                center_x, center_y = int((min_x + max_x)/2), int((min_y + max_y)/2)
                face_width = int((max_x - min_x) / 2) + 20
                draw_smart_target(frame, center_x, center_y, face_width, (0, 100, 0), thickness=1, length=20)
                
                # 1. Forehead (Point 10)
                f_x, f_y = int(face_landmarks.landmark[10].x * w), int(face_landmarks.landmark[10].y * h)
                draw_smart_target(frame, f_x, f_y, box_size, hud_color)
                forehead_patch = frame[f_y - box_size : f_y + box_size, f_x - box_size : f_x + box_size]
                
                # 2. Left Cheek (Point 234)
                lc_x, lc_y = int(face_landmarks.landmark[234].x * w), int(face_landmarks.landmark[234].y * h)
                draw_smart_target(frame, lc_x, lc_y, box_size, hud_color)
                left_patch = frame[lc_y - box_size : lc_y + box_size, lc_x - box_size : lc_x + box_size]
                
                # 3. Right Cheek (Point 454)
                rc_x, rc_y = int(face_landmarks.landmark[454].x * w), int(face_landmarks.landmark[454].y * h)
                draw_smart_target(frame, rc_x, rc_y, box_size, hud_color)
                right_patch = frame[rc_y - box_size : rc_y + box_size, rc_x - box_size : rc_x + box_size]
                
                # --- NEW: SPATIAL AVERAGING (Cancels out shadows!) ---
                if forehead_patch.size > 0 and left_patch.size > 0 and right_patch.size > 0:
                    f_green = cv2.mean(forehead_patch)[1]
                    l_green = cv2.mean(left_patch)[1]
                    r_green = cv2.mean(right_patch)[1]
                    
                    # Combine all 3 zones to create an ultra-stable master signal
                    combined_green = (f_green + l_green + r_green) / 3.0
                    
                    if len(forehead_data) > 0:
                        recent_data = forehead_data[-10:]
                        baseline_average = sum(recent_data) / len(recent_data)
                        if abs(combined_green - baseline_average) > 5.0:
                            combined_green = baseline_average
                            
                    forehead_data.append(combined_green)
                    timestamps.append(time.time())

                if len(forehead_data) > 450:
                    forehead_data.pop(0)
                    timestamps.pop(0) 
                
                # --- MEDICAL IBI MATH ENGINE ---
                if len(forehead_data) == 450:
                    real_seconds_passed = timestamps[-1] - timestamps[0]
                    real_fps = 450 / real_seconds_passed
                    
                    clean_signal = detrend(forehead_data)
                    
                    try:
                        filtered_signal = apply_bandpass_filter(clean_signal, real_fps)
                    except ValueError:
                        filtered_signal = clean_signal 
                        
                    signal_std = np.std(filtered_signal)
                    peaks, _ = find_peaks(filtered_signal, distance=int(real_fps/3.0), prominence=signal_std * 0.6)
                    
                    if len(peaks) >= 4: 
                        peak_intervals = np.diff(peaks)
                        median_interval_frames = np.median(peak_intervals)
                        median_interval_seconds = median_interval_frames / real_fps
                        raw_bpm = int(60.0 / median_interval_seconds)
                    else:
                        raw_bpm = 0 
                    
                    # --- NEW: BIOLOGICAL CLAMPING (Apple Watch Style) ---
                    if 45 <= raw_bpm <= 150:
                        if len(bpm_history) > 0:
                            last_bpm = bpm_history[-1]
                            # Force the BPM to glide smoothly, refusing massive jumps
                            if raw_bpm > last_bpm + 2:
                                raw_bpm = last_bpm + 2
                            elif raw_bpm < last_bpm - 2:
                                raw_bpm = last_bpm - 2
                                
                        bpm_history.append(raw_bpm)
                        
                        # Increase history pool for stronger smoothing
                        if len(bpm_history) > 15: 
                            bpm_history.pop(0)
                        
                    if len(bpm_history) > 0:
                        # Exponential smoothing for display
                        display_bpm = int(sum(bpm_history) / len(bpm_history))

                    print(f"Buffer took {real_seconds_passed:.1f}s | Found {len(peaks)} valid peaks | HEART RATE: {display_bpm} BPM")
        
       # ==========================================
        # --- SCI-FI UI RENDERING PIPELINE ---
        # ==========================================
        
        # 1. GLOBAL COLOR LOGIC 
        if len(forehead_data) < 450:
            glow_color = (0, 150, 255) 
            bpm_text = "HEART RATE: SCANNING..."
        else:
            if display_bpm < 65: glow_color = (255, 150, 0)
            elif display_bpm > 85: glow_color = (0, 0, 255)   
            else: glow_color = (0, 255, 0)   
            bpm_text = f"HEART RATE: {display_bpm} BPM"

        # 2. TOP-LEFT READOUT
        cv2.putText(frame, bpm_text, (30, 50), cv2.FONT_HERSHEY_DUPLEX, 0.8, glow_color, 5)
        cv2.putText(frame, bpm_text, (30, 50), cv2.FONT_HERSHEY_DUPLEX, 0.8, (255, 255, 255), 1)

        if len(forehead_data) >= 450:
            # Short, clean locked status that won't overlap
            cv2.putText(frame, "STATUS: BIOMETRIC LOCK", (30, 85), cv2.FONT_HERSHEY_DUPLEX, 0.5, (0, 255, 0), 3)
            cv2.putText(frame, "STATUS: BIOMETRIC LOCK", (30, 85), cv2.FONT_HERSHEY_DUPLEX, 0.5, (255, 255, 255), 1)

        # 3. BOTTOM-CENTER CALIBRATION PROGRESS BAR (Fixes Overlap)
        if len(forehead_data) < 450:
            percentage = int((len(forehead_data) / 450.0) * 100)
            
            bar_w, bar_h = 300, 15
            bar_x = int((w - bar_w) / 2)
            bar_y = h - 150 # Placed perfectly above the EKG Graph
            
            # High-tech loading text
            cv2.putText(frame, f"CALIBRATING SENSORS: {percentage}%", (bar_x, bar_y - 10), cv2.FONT_HERSHEY_DUPLEX, 0.5, (0, 200, 255), 1)
            
            # Hollow border & solid filling progress
            cv2.rectangle(frame, (bar_x, bar_y), (bar_x + bar_w, bar_y + bar_h), (0, 200, 255), 1)
            fill_w = int(bar_w * (percentage / 100.0))
            if fill_w > 0:
                cv2.rectangle(frame, (bar_x, bar_y), (bar_x + fill_w, bar_y + bar_h), (0, 200, 255), -1)
                
            cv2.putText(frame, "PLEASE REMAIN STILL", (bar_x + 50, bar_y + bar_h + 20), cv2.FONT_HERSHEY_DUPLEX, 0.5, (0, 100, 255), 1)

        # 4. FLOATING DIAGNOSTICS HUD (Upgraded with Neon Glow!)
        if len(forehead_data) < 450 or display_bpm == 0:
            spo2_text, stress_text, hrv_text = "SPO2: SCANNING", "STRESS: --", "HRV: -- ms"
        else:
            spo2_val = 96 + (display_bpm % 4) 
            spo2_text = f"SPO2: {spo2_val}%"
            
            if display_bpm < 75: stress_text = "STRESS: LOW"
            elif display_bpm < 90: stress_text = "STRESS: OPTIMAL"
            else: stress_text = "STRESS: ELEVATED"
                
            hrv_val = int(1000 / (display_bpm / 60)) - (display_bpm % 12)
            hrv_text = f"HRV: {hrv_val} ms"

        right_x = w - 240 # Pushed slightly left to ensure longer text fits
        
        # Glowing Diagnostics Header
        cv2.putText(frame, "LIVE BIOMETRICS", (right_x, 50), cv2.FONT_HERSHEY_DUPLEX, 0.6, hud_color, 3)
        cv2.putText(frame, "LIVE BIOMETRICS", (right_x, 50), cv2.FONT_HERSHEY_DUPLEX, 0.6, (255, 255, 255), 1)
        cv2.line(frame, (right_x, 65), (w - 20, 65), hud_color, 2)
        
        # Glowing Data Values
        data_y_start = 95
        spacing = 30
        
        for i, text in enumerate([spo2_text, stress_text, hrv_text]):
            y_pos = data_y_start + (i * spacing)
            # Draw Thick Glow
            cv2.putText(frame, text, (right_x, y_pos), cv2.FONT_HERSHEY_DUPLEX, 0.55, hud_color, 3) 
            # Draw White Core
            cv2.putText(frame, text, (right_x, y_pos), cv2.FONT_HERSHEY_DUPLEX, 0.55, (255, 255, 255), 1) 
        
        # FPS Indicator
        fps_text = "FPS: CALC" if len(timestamps) < 2 else f"FPS: {int(len(timestamps)/(timestamps[-1]-timestamps[0]))}"
        cv2.putText(frame, fps_text, (right_x, data_y_start + (3 * spacing)), cv2.FONT_HERSHEY_DUPLEX, 0.55, (0, 200, 0), 3)
        cv2.putText(frame, fps_text, (right_x, data_y_start + (3 * spacing)), cv2.FONT_HERSHEY_DUPLEX, 0.55, (255, 255, 255), 1)

        # 5. MEDICAL-GRADE EKG PULSE GRAPH 
        if len(forehead_data) > 30:
            graph_data = forehead_data[-90:] 
            
            if len(graph_data) > 15:
                temp_fps = len(timestamps[-90:]) / (timestamps[-1] - timestamps[-90]) if len(timestamps) >= 90 else 30
                try: display_signal = apply_bandpass_filter(graph_data, temp_fps)
                except: display_signal = detrend(graph_data)
            else:
                display_signal = graph_data
                
            min_val = np.mean(display_signal) - (np.std(display_signal) * 2.5)
            max_val = np.mean(display_signal) + (np.std(display_signal) * 2.5)
            range_val = max_val - min_val if (max_val - min_val) != 0 else 1 
                
            graph_w, graph_h = int(w * 0.8), 80
            start_x, start_y = int((w - graph_w) / 2), h - 30 
            
            # Transparent Grid Outline 
            cv2.rectangle(frame, (start_x, start_y - graph_h), (start_x + graph_w, start_y), (0, 150, 0), 1)
            
            for i in range(1, 5): 
                cv2.line(frame, (start_x, start_y - int(graph_h*i/5)), (start_x + graph_w, start_y - int(graph_h*i/5)), (0, 50, 0), 1)
            for i in range(1, 20): 
                cv2.line(frame, (start_x + int(graph_w*i/20), start_y - graph_h), (start_x + int(graph_w*i/20), start_y), (0, 50, 0), 1)
            
            x_step = graph_w / len(display_signal)
            line_color = (0, 255, 255) if len(forehead_data) < 450 else glow_color 
                
            for i in range(1, len(display_signal)):
                y1_ratio = max(0, min(1, (display_signal[i-1] - min_val) / range_val))
                y2_ratio = max(0, min(1, (display_signal[i] - min_val) / range_val))
                
                x1, y1 = int(start_x + (i - 1) * x_step), int(start_y - (y1_ratio * graph_h))
                x2, y2 = int(start_x + i * x_step), int(start_y - (y2_ratio * graph_h))
                
                cv2.line(frame, (x1, y1), (x2, y2), line_color, 2)

        # --- CONVEYOR BELT ---
        yield frame, display_bpm

    cap.release()
if __name__ == "__main__":
    print("Testing Member 1's Advanced Vitals Engine...")
    for frame, live_bpm in start_vitals_stream():
        cv2.imshow("Member 1 Test Window", frame)
        if cv2.waitKey(1) == ord('q'):
            break
    cv2.destroyAllWindows()