import math, pathlib, struct, wave
out=pathlib.Path('app/src/main/assets/neutral-tone.wav'); out.parent.mkdir(parents=True,exist_ok=True)
with wave.open(str(out),'wb') as w:
 w.setnchannels(1); w.setsampwidth(2); w.setframerate(16000)
 for n in range(16000*12):
  fade=min(n/1600,(16000*12-n)/1600,1)
  w.writeframesraw(struct.pack('<h',int(2000*fade*math.sin(2*math.pi*440*n/16000))))
print('Generated neutral 12-second test tone; no speech.')
