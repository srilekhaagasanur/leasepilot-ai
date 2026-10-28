import { useEffect, useMemo, useRef, useState } from 'react'
import { toWav16kMono } from '../wav'

// Records a voice note with the microphone and hands back a WAV blob via onChange
export default function VoiceRecorder({ value, onChange }) {
  const [recording, setRecording] = useState(false)
  const [seconds, setSeconds] = useState(0)
  const [error, setError] = useState(null)
  const recorderRef = useRef(null)
  const timerRef = useRef(null)
  const audioUrl = useMemo(() => (value ? URL.createObjectURL(value) : null), [value])

  useEffect(() => () => audioUrl && URL.revokeObjectURL(audioUrl), [audioUrl])
  useEffect(() => () => clearInterval(timerRef.current), [])

  async function start() {
    setError(null)
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true })
      const recorder = new MediaRecorder(stream)
      const chunks = []
      recorder.ondataavailable = (e) => chunks.push(e.data)
      recorder.onstop = async () => {
        stream.getTracks().forEach((t) => t.stop())
        try {
          onChange(await toWav16kMono(new Blob(chunks, { type: recorder.mimeType })))
        } catch {
          setError('Could not process the recording. Please try again.')
        }
      }
      recorder.start()
      recorderRef.current = recorder
      setSeconds(0)
      timerRef.current = setInterval(() => setSeconds((s) => s + 1), 1000)
      setRecording(true)
    } catch {
      setError('Microphone access was blocked. Allow it in your browser to record.')
    }
  }

  function stop() {
    recorderRef.current?.stop()
    clearInterval(timerRef.current)
    setRecording(false)
  }

  return (
    <div className="space-y-2">
      <div className="flex flex-wrap items-center gap-3">
        {recording ? (
          <button type="button" onClick={stop}
            className="inline-flex items-center gap-2 rounded-lg bg-red-600 px-4 py-2 text-sm font-semibold text-white hover:bg-red-700">
            <span className="h-2.5 w-2.5 animate-pulse rounded-full bg-white" />
            Stop recording · {seconds}s
          </button>
        ) : (
          <button type="button" onClick={start}
            className="inline-flex items-center gap-2 rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-50">
            🎙️ {value ? 'Record again' : 'Record a voice note'}
          </button>
        )}
        {value && !recording && (
          <button type="button" onClick={() => onChange(null)} className="text-sm text-slate-500 underline hover:text-slate-700">
            Remove
          </button>
        )}
      </div>
      {audioUrl && !recording && <audio controls src={audioUrl} className="h-10 w-full max-w-sm" />}
      {error && <p className="text-sm text-rose-600">{error}</p>}
    </div>
  )
}
