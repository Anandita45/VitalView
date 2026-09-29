from fastapi import FastAPI, WebSocket, WebSocketDisconnect
import json
import uvicorn
from signal_processing import VitalsProcessor

app = FastAPI()
@app.get("/")
async def root():
    return {"message": "The rPPG server is running perfectly!"}
processor = VitalsProcessor(window_seconds=10, fs=30)

@app.websocket("/ws/vitals")
async def websocket_endpoint(websocket: WebSocket):
    await websocket.accept()
    print("Client connected to vitals stream.")
    
    try:
        while True:
            # Receive data from Member 1's script
            # Expected format: {"r": 120.5, "g": 90.2, "b": 85.1}
            data_str = await websocket.receive_text()
            data = json.loads(data_str)
            
            processor.add_rgb_data(data['r'], data['g'], data['b'])
            
            # Process sliding window
            bpm, rpm, waveform_point = processor.process_window()
            
            if bpm is not None:
                # Buffer is full, emit actual vitals
                response = {
                    "status": "processing",
                    "bpm": round(bpm, 1),
                    "rpm": round(rpm, 1),
                    "waveform": round(waveform_point, 4)
                }
                await websocket.send_json(response)
            else:
                # Still filling the 10-second buffer
                buffer_fill_percent = (len(processor.rgb_buffer) / processor.window_size) * 100
                await websocket.send_json({
                    "status": "buffering", 
                    "progress": round(buffer_fill_percent, 1)
                })
                
    except WebSocketDisconnect:
        print("Client disconnected. Resetting buffer.")
        processor.rgb_buffer.clear()

if __name__ == "__main__":
    # Start the server
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)