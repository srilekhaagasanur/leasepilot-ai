// Muse Voice Transcribe needs WAV audio: mono, 16-bit PCM, 16 kHz.
// Browsers record in other formats (webm/opus, mp4), so we convert the recording here.

const TARGET_SAMPLE_RATE = 16000

export async function toWav16kMono(recordedBlob) {
  // 1. Decode whatever the browser recorded into raw audio samples
  const audioContext = new AudioContext()
  const decoded = await audioContext.decodeAudioData(await recordedBlob.arrayBuffer())
  await audioContext.close()

  // 2. Resample to 16 kHz and mix down to one channel
  const length = Math.ceil(decoded.duration * TARGET_SAMPLE_RATE)
  const offline = new OfflineAudioContext(1, length, TARGET_SAMPLE_RATE)
  const source = offline.createBufferSource()
  source.buffer = decoded
  source.connect(offline.destination)
  source.start()
  const rendered = await offline.startRendering()

  // 3. Wrap the samples in a WAV file
  return new Blob([encodeWav(rendered.getChannelData(0), TARGET_SAMPLE_RATE)], { type: 'audio/wav' })
}

function encodeWav(samples, sampleRate) {
  const bytesPerSample = 2
  const dataSize = samples.length * bytesPerSample
  const buffer = new ArrayBuffer(44 + dataSize)
  const view = new DataView(buffer)

  const writeText = (offset, text) => [...text].forEach((ch, i) => view.setUint8(offset + i, ch.charCodeAt(0)))

  // Standard 44-byte WAV header
  writeText(0, 'RIFF')
  view.setUint32(4, 36 + dataSize, true)
  writeText(8, 'WAVE')
  writeText(12, 'fmt ')
  view.setUint32(16, 16, true)             // size of this header section
  view.setUint16(20, 1, true)              // format: PCM
  view.setUint16(22, 1, true)              // channels: mono
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * bytesPerSample, true) // bytes per second
  view.setUint16(32, bytesPerSample, true) // bytes per frame
  view.setUint16(34, 16, true)             // bits per sample
  writeText(36, 'data')
  view.setUint32(40, dataSize, true)

  // Convert float samples (-1..1) to 16-bit integers
  let offset = 44
  for (const sample of samples) {
    const clamped = Math.max(-1, Math.min(1, sample))
    view.setInt16(offset, clamped < 0 ? clamped * 0x8000 : clamped * 0x7fff, true)
    offset += bytesPerSample
  }
  return buffer
}
