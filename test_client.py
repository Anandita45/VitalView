import asyncio
import websockets
import json
import math
import random

async def simulate_member_1():
    uri = "ws://localhost:8000/ws/vitals"
    
    async with websockets.connect(uri) as websocket:
        print("Connected to server! Sending realistic skin data...")
        
        for i in range(600):
            # Perfect mathematical time step
            t = i / 30.0 
            
            # Base heartbeat at 1.2 Hz (72 BPM)
            pulse = math.sin(t * 2 * math.pi * 1.2)
            
            # Mimic real blood volume changes (Green absorbs most, Red very little)
            # Add random noise to simulate real camera sensor grain
            fake_rgb_data = {
                "r": 150.0 + (pulse * 0.5) + random.uniform(-0.5, 0.5), 
                "g": 100.0 + (pulse * 3.0) + random.uniform(-0.5, 0.5), 
                "b": 100.0 + (pulse * 1.5) + random.uniform(-0.5, 0.5)
            }
            
            await websocket.send(json.dumps(fake_rgb_data))
            
            response = await websocket.recv()
            print(f"Server response: {response}")
            
            await asyncio.sleep(1/30)

if __name__ == "__main__":
    asyncio.run(simulate_member_1())