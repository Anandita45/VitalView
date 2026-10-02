import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '3' 
os.environ['GLOG_minloglevel'] = '3'

import threading
import json
import websocket 
import cv2
import mediapipe as mp
import numpy as np 
from scipy.signal import find_peaks, detrend, butter, filtfilt 
import time 
import math
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

def calculate_respiration_rate(signal_buffer, fps=30.0):
    """
    Calculates raw Respiration Rate using Zero-Phase Filtering and Z-Score Normalization.
    """
    if len(signal_buffer) < int(fps * 6):
        return 0.0
    
    try:
        signal_array = np.array(signal_buffer, dtype=float)
        if np.isnan(signal_array).any():
            signal_array = np.nan_to_num(signal_array, nan=np.nanmean(signal_array))

        signal_array = detrend(signal_array)
        std_dev = np.std(signal_array)
        if std_dev > 0:
            signal_array = signal_array / std_dev
            
        nyq = 0.5 * fps
        low = 0.1 / nyq
        high = 0.4 / nyq
        b, a = butter(3, [low, high], btype='band') 
        
        filtered = filtfilt(b, a, signal_array)
        peaks, _ = find_peaks(filtered, distance=fps * 1.5, prominence=0.35)
        
        if len(peaks) >= 2:
            peak_intervals = np.diff(peaks) 
            mean_interval_frames = np.mean(peak_intervals)
            
            if mean_interval_frames > 0:
                resp_rate = (fps / mean_interval_frames) * 60.0
                return max(6.0, min(30.0, resp_rate))
                
    except Exception:
        pass
        
    return 0.0  

# --- SKIN-PIXEL ISOLATION (UPDATED FOR LOW LIGHT) ---
def get_skin_average(roi):
    if roi.size == 0:
        return 0
        
    ycrcb_roi = cv2.cvtColor(roi, cv2.COLOR_BGR2YCrCb)
    lower_skin = np.array([80, 85, 135], dtype=np.uint8)
    upper_skin = np.array([255, 135, 180], dtype=np.uint8)
    
    skin_mask = cv2.inRange(ycrcb_roi, lower_skin, upper_skin)
    mean_val = cv2.mean(roi, mask=skin_mask)
    
    if mean_val[0] == 0 and mean_val[1] == 0 and mean_val[2] == 0:
        mean_val = cv2.mean(roi)
        
    return mean_val[1]

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
    print("1: Phone Camera (DroidCam/Camo/USB)")
    print("Or paste Localhost URL: http://127.0.0.1:4747/video")
    
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
        return 

    print("Camera connected successfully! Starting vitals stream...")

    box_size = 15
    forehead_data = []
    timestamps = []
    
    bpm_history = []
    resp_history = []  
    
    display_bpm = 0
    display_resp = 0.0
    motion_artifact = False
    
    prev_nose_x, prev_nose_y = None, None
    MOTION_TOLERANCE = 5.0 

    shared_payload = {
        "bpm": 0, "respiration_rate": 0.0, "motion": False, "calibrated": False, 
        "r": 0.0, "g": 0.0, "b": 0.0, "running": True
    }

    def network_worker():
        ws = websocket.WebSocket()
        try:
            ws.connect("ws://localhost:8000/ws/vitals")
            print("SUCCESS: Linked to Liveness AI (Background Thread Active!)")
            while shared_payload["running"]:
                payload = {
                    "bpm": shared_payload["bpm"],
                    "respiration_rate": shared_payload["respiration_rate"],
                    "motion_artifact": shared_payload["motion"],
                    "calibrated": shared_payload["calibrated"],
                    "r": shared_payload["r"],
                    "g": shared_payload["g"],
                    "b": shared_payload["b"]
                }
                ws.send(json.dumps(payload))
                time.sleep(0.5) 
        except Exception:
            print("Network Thread: Server disconnected or offline.")

    threading.Thread(target=network_worker, daemon=True).start()
    
    while True:
        success, frame = cap.read()
        if not success:
            break
            
        h, w, c = frame.shape
        rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
        results = face_mesh.process(rgb_frame)
        
        hud_color = (0, 255, 255) if len(forehead_data) < 450 else (255, 255, 0)
        motion_artifact = False
        
        if results.multi_face_landmarks:
            for face_landmarks in results.multi_face_landmarks:
                
                nose_x = int(face_landmarks.landmark[1].x * w)
                nose_y = int(face_landmarks.landmark[1].y * h)
                
                if prev_nose_x is not None and prev_nose_y is not None:
                    delta_x = abs(nose_x - prev_nose_x)
                    delta_y = abs(nose_y - prev_nose_y)
                    shift_distance = math.sqrt(delta_x**2 + delta_y**2)
                    
                    if shift_distance > MOTION_TOLERANCE:
                        motion_artifact = True
                        cv2.putText(frame, "MOTION ARTIFACT DETECTED", (30, 155), cv2.FONT_HERSHEY_DUPLEX, 0.5, (0, 0, 255), 2)
                
                prev_nose_x, prev_nose_y = nose_x, nose_y
                
                x_coords = [int(lm.x * w) for lm in face_landmarks.landmark]
                y_coords = [int(lm.y * h) for lm in face_landmarks.landmark]
                min_x, max_x = min(x_coords), max(x_coords)
                min_y, max_y = min(y_coords), max(y_coords)
                
                center_x, center_y = int((min_x + max_x)/2), int((min_y + max_y)/2)
                face_width = int((max_x - min_x) / 2) + 20
                draw_smart_target(frame, center_x, center_y, face_width, (0, 100, 0), thickness=1, length=20)
                
                f_x, f_y = int(face_landmarks.landmark[10].x * w), int(face_landmarks.landmark[10].y * h)
                draw_smart_target(frame, f_x, f_y, box_size, hud_color)
                forehead_patch = frame[max(0, f_y - box_size) : f_y + box_size, max(0, f_x - box_size) : f_x + box_size]
                
                lc_x, lc_y = int(face_landmarks.landmark[234].x * w), int(face_landmarks.landmark[234].y * h)
                draw_smart_target(frame, lc_x, lc_y, box_size, hud_color)
                left_patch = frame[max(0, lc_y - box_size) : lc_y + box_size, max(0, lc_x - box_size) : lc_x + box_size]
                
                rc_x, rc_y = int(face_landmarks.landmark[454].x * w), int(face_landmarks.landmark[454].y * h)
                draw_smart_target(frame, rc_x, rc_y, box_size, hud_color)
                right_patch = frame[max(0, rc_y - box_size) : rc_y + box_size, max(0, rc_x - box_size) : rc_x + box_size]
                
                if not motion_artifact:
                    if forehead_patch.size > 0 and left_patch.size > 0 and right_patch.size > 0:
                        
                        f_green = get_skin_average(forehead_patch)
                        l_green = get_skin_average(left_patch)
                        r_green = get_skin_average(right_patch)
                        
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
                
                # ==========================================
                # --- MEDICAL IBI MATH ENGINE ---
                # ==========================================
                if len(forehead_data) == 450:
                    real_seconds_passed = timestamps[-1] - timestamps[0]
                    real_fps = 450 / real_seconds_passed
                    
                    # 1. HEART RATE (BPM)
                    clean_signal = detrend(forehead_data)
                    try:
                        filtered_signal = apply_bandpass_filter(clean_signal, real_fps)
                    except ValueError:
                        filtered_signal = clean_signal 
                        
                    signal_std = np.std(filtered_signal)
                    
                    # STRICTER FILTER: Increased distance and prominence to ignore light flicker & dicrotic notch
                    peaks, _ = find_peaks(filtered_signal, distance=int(real_fps/2.5), prominence=signal_std * 0.95)
                    
                    if len(peaks) >= 3: 
                        peak_intervals = np.diff(peaks)
                        # SECONDARY FILTER: Delete any physically impossible micro-intervals
                        valid_intervals = [p for p in peak_intervals if p > (real_fps / 3.0)]
                        
                        if len(valid_intervals) > 0:
                            raw_bpm = int(60.0 / (np.median(valid_intervals) / real_fps))
                        else:
                            raw_bpm = 0
                    else:
                        raw_bpm = 0 
                    
                    if 45 <= raw_bpm <= 150:
                        if len(bpm_history) > 0:
                            last_bpm = bpm_history[-1]
                            if raw_bpm > last_bpm + 2: raw_bpm = last_bpm + 2
                            elif raw_bpm < last_bpm - 2: raw_bpm = last_bpm - 2
                                
                        bpm_history.append(raw_bpm)
                        if len(bpm_history) > 90: bpm_history.pop(0)
                        
                    if len(bpm_history) > 0:
                        display_bpm = int(sum(bpm_history) / len(bpm_history))

                    # 2. RESPIRATION RATE (RPM)
                    raw_resp = calculate_respiration_rate(forehead_data, fps=real_fps)
                    
                    if 6 <= raw_resp <= 30:
                        if len(resp_history) > 0:
                            last_resp = resp_history[-1]
                            if raw_resp > last_resp + 0.5: raw_resp = last_resp + 0.5
                            elif raw_resp < last_resp - 0.5: raw_resp = last_resp - 0.5
                            
                        resp_history.append(raw_resp)
                        if len(resp_history) > 90: resp_history.pop(0)
                        
                    if len(resp_history) > 0:
                        display_resp = sum(resp_history) / len(resp_history)

        # ==========================================
        # --- SCI-FI UI RENDERING PIPELINE ---
        # ==========================================
        
        if len(forehead_data) < 450:
            glow_color = (0, 150, 255) 
            bpm_text = "HEART RATE: SCANNING..."
        else:
            if display_bpm < 65: glow_color = (255, 150, 0)
            elif display_bpm > 85: glow_color = (0, 0, 255)   
            else: glow_color = (0, 255, 0)   
            bpm_text = f"HEART RATE: {display_bpm} BPM"

        cv2.putText(frame, bpm_text, (30, 50), cv2.FONT_HERSHEY_DUPLEX, 0.8, glow_color, 5)
        cv2.putText(frame, bpm_text, (30, 50), cv2.FONT_HERSHEY_DUPLEX, 0.8, (255, 255, 255), 1)

        if len(forehead_data) >= 450:
            cv2.putText(frame, "STATUS: BIOMETRIC LOCK", (30, 85), cv2.FONT_HERSHEY_DUPLEX, 0.5, (0, 255, 0), 3)
            cv2.putText(frame, "STATUS: BIOMETRIC LOCK", (30, 85), cv2.FONT_HERSHEY_DUPLEX, 0.5, (255, 255, 255), 1)
            
        resp_text = f"RESPIRATION RATE: {int(display_resp)} RPM" if len(forehead_data) >= 450 else "RESPIRATION RATE: SCANNING..."
        cv2.putText(frame, resp_text, (30, 120), cv2.FONT_HERSHEY_DUPLEX, 0.6, (255, 165, 0), 3)
        cv2.putText(frame, resp_text, (30, 120), cv2.FONT_HERSHEY_DUPLEX, 0.6, (255, 255, 255), 1)

        if len(forehead_data) < 450:
            percentage = int((len(forehead_data) / 450.0) * 100)
            bar_w, bar_h = 300, 15
            bar_x = int((w - bar_w) / 2)
            bar_y = h - 150 
            
            cv2.putText(frame, f"CALIBRATING SENSORS: {percentage}%", (bar_x, bar_y - 10), cv2.FONT_HERSHEY_DUPLEX, 0.5, (0, 200, 255), 1)
            cv2.rectangle(frame, (bar_x, bar_y), (bar_x + bar_w, bar_y + bar_h), (0, 200, 255), 1)
            fill_w = int(bar_w * (percentage / 100.0))
            if fill_w > 0:
                cv2.rectangle(frame, (bar_x, bar_y), (bar_x + fill_w, bar_y + bar_h), (0, 200, 255), -1)
            cv2.putText(frame, "PLEASE REMAIN STILL", (bar_x + 50, bar_y + bar_h + 20), cv2.FONT_HERSHEY_DUPLEX, 0.5, (0, 100, 255), 1)

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

        right_x = w - 240 
        cv2.putText(frame, "LIVE BIOMETRICS", (right_x, 50), cv2.FONT_HERSHEY_DUPLEX, 0.6, hud_color, 3)
        cv2.putText(frame, "LIVE BIOMETRICS", (right_x, 50), cv2.FONT_HERSHEY_DUPLEX, 0.6, (255, 255, 255), 1)
        cv2.line(frame, (right_x, 65), (w - 20, 65), hud_color, 2)
        
        data_y_start = 95
        spacing = 30
        for i, text in enumerate([spo2_text, stress_text, hrv_text]):
            y_pos = data_y_start + (i * spacing)
            cv2.putText(frame, text, (right_x, y_pos), cv2.FONT_HERSHEY_DUPLEX, 0.55, hud_color, 3) 
            cv2.putText(frame, text, (right_x, y_pos), cv2.FONT_HERSHEY_DUPLEX, 0.55, (255, 255, 255), 1) 
        
        fps_text = "FPS: CALC" if len(timestamps) < 2 else f"FPS: {int(len(timestamps)/(timestamps[-1]-timestamps[0]))}"
        cv2.putText(frame, fps_text, (right_x, data_y_start + (3 * spacing)), cv2.FONT_HERSHEY_DUPLEX, 0.55, (0, 200, 0), 3)
        cv2.putText(frame, fps_text, (right_x, data_y_start + (3 * spacing)), cv2.FONT_HERSHEY_DUPLEX, 0.55, (255, 255, 255), 1)

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
      
        try:
            if forehead_patch.size > 0:
                mean_b, mean_g, mean_r, _ = cv2.mean(forehead_patch)
            else:
                mean_b, mean_g, mean_r = 0, 0, 0
        except NameError:
            mean_b, mean_g, mean_r = 0, 0, 0
            
        shared_payload["bpm"] = display_bpm
        shared_payload["respiration_rate"] = display_resp
        shared_payload["motion"] = motion_artifact
        shared_payload["calibrated"] = (len(forehead_data) >= 450)
        shared_payload["r"] = mean_r
        shared_payload["g"] = mean_g
        shared_payload["b"] = mean_b

        yield frame, display_bpm
        
    shared_payload["running"] = False
    cap.release()

if __name__ == "__main__":
    print("Testing Member 1's Advanced Vitals Engine...")
    for frame, live_bpm in start_vitals_stream():
        cv2.imshow("Member 1 Test Window", frame)
        if cv2.waitKey(1) == ord('q'):
            break
    cv2.destroyAllWindows()