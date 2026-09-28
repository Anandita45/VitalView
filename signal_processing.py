import numpy as np
from scipy import signal

def calculate_pos(rgb_window):
    """
    Implements the CHROM algorithm to decouple blood volume changes.
    rgb_window: numpy array of shape (Frames, 3) containing R, G, B means.
    """
    # 1. Temporal normalization 
    mean_rgb = np.mean(rgb_window, axis=0)
    normalized_rgb = rgb_window / mean_rgb  
    
    # 2. CHROM Projection matrices
    X = 3 * normalized_rgb[:, 0] - 2 * normalized_rgb[:, 1]
    Y = 1.5 * normalized_rgb[:, 0] + normalized_rgb[:, 1] - 1.5 * normalized_rgb[:, 2]
    
    # 3. Alpha tuning 
    alpha = np.std(X) / np.std(Y)
    
    # 4. Final pulse signal (MUST be a minus sign for CHROM)
    h = X - alpha * Y  
    return h

def apply_bandpass_filter(signal_data, fs, lowcut, highcut, order=5):
    """Applies a zero-phase Butterworth bandpass filter."""
    nyquist = 0.5 * fs
    low = lowcut / nyquist
    high = highcut / nyquist
    
    b, a = signal.butter(order, [low, high], btype='band')
    # filtfilt prevents phase shifting in the waveform
    filtered_signal = signal.filtfilt(b, a, signal_data)
    return filtered_signal

def extract_frequency(filtered_signal, fs):
    """Uses Welch's method (PSD) to find the dominant frequency."""
    # nperseg is set to length of signal to get high frequency resolution
    frequencies, psd = signal.welch(filtered_signal, fs, nperseg=len(filtered_signal))
    
    peak_idx = np.argmax(psd)
    peak_freq = frequencies[peak_idx]
    
    return peak_freq

from collections import deque

class VitalsProcessor:
    def __init__(self, window_seconds=10, fs=30):
        self.fs = fs
        self.window_size = window_seconds * fs
        
        # deque automatically drops the oldest frames when maxlen is reached
        self.rgb_buffer = deque(maxlen=self.window_size)
        
    def add_rgb_data(self, r, g, b):
        self.rgb_buffer.append([r, g, b])
        
    def process_window(self):
        # Wait until we have enough data (e.g., 300 frames) to run a valid FFT
        if len(self.rgb_buffer) < self.window_size:
            return None, None, None  
            
        rgb_array = np.array(self.rgb_buffer)
        
        # 1. Extract raw pulse via POS
        raw_pulse = calculate_pos(rgb_array)
        
        # 2. Detrend to remove slow drifts (like subtle head movements)
        detrended_pulse = signal.detrend(raw_pulse)
        
        # 3. Heart Rate (HR) extraction (0.75 - 3.0 Hz corresponds to 45 - 180 BPM)
        hr_signal = apply_bandpass_filter(detrended_pulse, self.fs, 0.75, 3.0)
        hr_freq = extract_frequency(hr_signal, self.fs)
        bpm = hr_freq * 60.0
        
        # 4. Respiration Rate (RR) extraction (0.15 - 0.4 Hz corresponds to 9 - 24 Breaths/min)
        rr_signal = apply_bandpass_filter(detrended_pulse, self.fs, 0.15, 0.4)
        rr_freq = extract_frequency(rr_signal, self.fs)
        rpm = rr_freq * 60.0
        
        # Return vitals and the latest point of the HR waveform for UI plotting
        return bpm, rpm, hr_signal[-1]