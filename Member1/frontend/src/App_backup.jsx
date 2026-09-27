
import { useEffect, useRef, useState } from "react";
import PulseChart from "./PulseChart";
import {
  FaceLandmarker,
  FilesetResolver,
} from "@mediapipe/tasks-vision";
import "./App.css";

function App() {
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const streamRef = useRef(null);
  const landmarkerRef = useRef(null);
  const animationRef = useRef(null);

  const [cameraOn, setCameraOn] = useState(false);
  const [modelReady, setModelReady] = useState(false);
  const [faceDetected, setFaceDetected] = useState(false);
  const [fps, setFps] = useState(0);
const fpsCountRef = useRef(0);
const fpsStartRef = useRef(performance.now());
  const [error, setError] = useState("");
  const [signalData, setSignalData] = useState([]);
const signalCanvasRef = useRef(null);
const lastSampleTimeRef = useRef(0);
const signalHistoryRef = useRef([]);
const filteredSignalRef = useRef(0);
  // Load the MediaPipe face landmark model
  useEffect(() => {
    let cancelled = false;

    async function loadModel() {
      try {
        const vision = await FilesetResolver.forVisionTasks(
          "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@latest/wasm"
        );

        const landmarker =
          await FaceLandmarker.createFromOptions(vision, {
            baseOptions: {
              modelAssetPath: "/face_landmarker.task",
              delegate: "GPU",
            },
            runningMode: "VIDEO",
            numFaces: 1,
          });

        if (cancelled) {
          landmarker.close();
          return;
        }

        landmarkerRef.current = landmarker;
        setModelReady(true);
      } catch (err) {
        console.error(err);
        setError(
          "Could not load the face model. Check your internet connection and model file."
        );
      }
    }

    loadModel();

    return () => {
      cancelled = true;
      landmarkerRef.current?.close();
      landmarkerRef.current = null;
    };
  }, []);

  // Start webcam
  async function startCamera() {
    try {
      setError("");

      const stream =
        await navigator.mediaDevices.getUserMedia({
         video: {
  width: { ideal: 480 },
  height: { ideal: 360 },
  frameRate: { ideal: 30, max: 30 },
  facingMode: "user",
},
          audio: false,
        });

      streamRef.current = stream;
      videoRef.current.srcObject = stream;

      await videoRef.current.play();
      setCameraOn(true);
    } catch (err) {
      console.error(err);
      setError("Please allow camera access in your browser.");
    }
  }

  // Stop webcam and clear overlay
  function stopCamera() {
    if (animationRef.current) {
      cancelAnimationFrame(animationRef.current);
      animationRef.current = null;
    }

    streamRef.current?.getTracks().forEach(track => {
      track.stop();
    });

    streamRef.current = null;

    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }

    const canvas = canvasRef.current;
    const ctx = canvas?.getContext("2d");

    if (ctx) {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
    }

    setCameraOn(false);
    setFaceDetected(false);
  }

  // Detect face and draw the three regions
  useEffect(() => {
    if (!cameraOn || !modelReady) return;

    const video = videoRef.current;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext("2d");

   let lastVideoTime = -1;
let lastFaceState = false;
let lastResult = null;
    function drawFrame() {
      if (
        !video ||
        video.readyState < 2 ||
        !landmarkerRef.current
      ) {
        animationRef.current =
          requestAnimationFrame(drawFrame);
        return;
      }
      

      const width = video.videoWidth;
const height = video.videoHeight;

if (video.currentTime !== lastVideoTime) {
  lastVideoTime = video.currentTime;

        if (
          canvas.width !== width ||
          canvas.height !== height
        ) {
          canvas.width = width;
          canvas.height = height;
        }

        ctx.clearRect(0, 0, width, height);

        try {

const result = landmarkerRef.current.detectForVideo(
  video,
  performance.now()
);

// Count frames actually processed by MediaPipe
fpsCountRef.current++;

const fpsNow = performance.now();

if (fpsNow - fpsStartRef.current >= 1000) {
  setFps(fpsCountRef.current);
  fpsCountRef.current = 0;
  fpsStartRef.current = fpsNow;
}

const landmarks = result.faceLandmarks?.[0];

          if (landmarks && landmarks.length > 0) {
            if (!lastFaceState) {
              setFaceDetected(true);
              lastFaceState = true;
            }

            // Get the overall face landmark bounds
            const xs = landmarks.map(point => point.x);
            const ys = landmarks.map(point => point.y);

            const minX = Math.min(...xs) * width;
            const maxX = Math.max(...xs) * width;
            const minY = Math.min(...ys) * height;
            const maxY = Math.max(...ys) * height;

            const faceW = maxX - minX;
            const faceH = maxY - minY;

            // Approximate regions relative to face bounds
            const regions = [
              {
                label: "Forehead",
                x: minX + faceW * 0.28,
                y: minY + faceH * 0.08,
                w: faceW * 0.44,
                h: faceH * 0.20,

              },
              
{
  label: "Left Cheek",
  x: minX + faceW * 0.10,
  y: minY + faceH * 0.48,
  w: faceW * 0.22,
  h: faceH * 0.15,
},
{
  label: "Right Cheek",
  x: minX + faceW * 0.68,
  y: minY + faceH * 0.48,
  w: faceW * 0.22,
  h: faceH * 0.15,
},
              
            ];
                        // Sample the forehead and cheek camera pixels
            const now = performance.now();

            if (now - lastSampleTimeRef.current >= 100) {
              lastSampleTimeRef.current = now;

              const signalCanvas = signalCanvasRef.current;

              if (signalCanvas) {
                const signalCtx = signalCanvas.getContext("2d", {
                  willReadFrequently: true,
                });

                let totalGreen = 0;
                let totalPixels = 0;

                regions.forEach(region => {
                  const x = Math.max(0, Math.floor(region.x));
                  const y = Math.max(0, Math.floor(region.y));
                  const w = Math.min(
                    Math.floor(region.w),
                    width - x
                  );
                  const h = Math.min(
                    Math.floor(region.h),
                    height - y
                  );

                  if (w > 0 && h > 0) {
                    signalCtx.drawImage(
                      video,
                      x, y, w, h,
                      0, 0, w, h
                    );

                    const pixels = signalCtx.getImageData(
                      0, 0, w, h
                    ).data;

                    for (let i = 0; i < pixels.length; i += 4) {
                      totalGreen += pixels[i + 1];
                      totalPixels++;
                    }
                  }
                });
if (totalPixels > 0) {
  const averageGreen = totalGreen / totalPixels;

  // Store the latest camera measurement
  const history = signalHistoryRef.current;

  history.push({
    time: now,
    green: averageGreen,
  });

  // Keep only the latest 3 seconds of samples
  const cutoff = now - 3000;

  signalHistoryRef.current = history.filter(
    sample => sample.time >= cutoff
  );

  const recentSamples = signalHistoryRef.current;

  // Calculate the slow-changing brightness baseline
  const baseline =
    recentSamples.reduce(
      (sum, sample) => sum + sample.green,
      0
    ) / recentSamples.length;

  // Remove the baseline to highlight smaller changes
 const rawSignal = averageGreen - baseline;

// Smooth the camera signal
filteredSignalRef.current =
  0.3 * rawSignal +
  0.7 * filteredSignalRef.current;

const filteredGreen = filteredSignalRef.current;

  // Send the filtered signal to the graph
  setSignalData(previous => [
    ...previous,
    {
      time: now,
      pulse: filteredGreen,
    },
  ].slice(-150));
}
                
              }
            }

            regions.forEach(region => {
              ctx.strokeStyle = "#00ff88";
              ctx.lineWidth = 3;

              ctx.strokeRect(
                region.x,
                region.y,
                region.w,
                region.h
              );

              ctx.font = "bold 14px Arial";
ctx.fillStyle = "#00ff88";
ctx.textAlign = "center";

if (region.label === "Forehead") {
  ctx.fillText(
    region.label,
    region.x + region.w / 2,
    region.y - 10
  );
} else {
  ctx.fillText(
    region.label,
    region.x + region.w / 2,
    region.y + region.h + 18
  );
}

ctx.textAlign = "left";
            });
          } else if (lastFaceState) {
            setFaceDetected(false);
            lastFaceState = false;
          }
        } catch (err) {
          console.error("Face detection error:", err);
        }
      }

      animationRef.current =
        requestAnimationFrame(drawFrame);
    }

    animationRef.current =
      requestAnimationFrame(drawFrame);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
        animationRef.current = null;
      }
    };
  }, [cameraOn, modelReady]);

  // Release camera when component is removed
  useEffect(() => {
    return () => {
      streamRef.current?.getTracks().forEach(track => {
        track.stop();
      });

      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, []);

  return (
    <div className="app">
      <canvas
  ref={signalCanvasRef}
  style={{ display: "none" }}
/>
      <PulseChart data={signalData} />
      <h1>AI Pulse Monitor</h1>
      <p>Real-Time Facial Signal Analysis</p>
      <p className="fps-counter">
  Camera Processing: {fps} FPS
</p>

      <div className="camera-box">
        <video
          ref={videoRef}
          autoPlay
          muted
          playsInline
          
        />

        <canvas
          ref={canvasRef}
          className="face-overlay"
        />

        {!cameraOn && (
          <div className="placeholder">
            Camera is currently off
          </div>
        )}
      </div>

      <div className="controls">
        <button onClick={startCamera}>
          Start Camera
        </button>

        <button onClick={stopCamera}>
          Stop Camera
        </button>
      </div>

      <p className="status">
        {!modelReady
          ? "Loading MediaPipe model..."
          : cameraOn
          ? faceDetected
            ? "Face detected — regions highlighted"
            : "Camera on — position your face"
          : "Model ready — waiting for camera"}
      </p>

      {error && (
        <p style={{ color: "#ff6666" }}>{error}</p>
      )}
    </div>
  );
}

export default App;
