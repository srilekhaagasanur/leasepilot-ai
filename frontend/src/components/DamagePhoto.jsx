import { useEffect, useState } from 'react'
import { decodeMaskToSVGPath, parseImageStream, recordsOfKind } from '@meta-sam/parser'

// Shows the tenant's photo with the damage SAM 3.1 found outlined on top.
// `segmentation` is SAM's raw answer text, stored by the backend as-is.
export default function DamagePhoto({ src, segmentation, phrase }) {
  const [shapes, setShapes] = useState(null)
  const [showHighlight, setShowHighlight] = useState(true)

  useEffect(() => {
    let cancelled = false
    setShapes(null)
    if (segmentation) {
      decodeSegmentation(segmentation)
        .then((result) => !cancelled && setShapes(result))
        .catch(() => !cancelled && setShapes(null))
    }
    return () => { cancelled = true }
  }, [segmentation])

  return (
    <div className="mt-3">
      <div className="relative inline-block">
        <img src={src} alt="Tenant's photo" className="block max-h-72 rounded-lg border border-slate-200" />
        {shapes && showHighlight && (
          <svg viewBox={`0 0 ${shapes.width} ${shapes.height}`} preserveAspectRatio="none"
            className="pointer-events-none absolute inset-0 h-full w-full">
            {shapes.masks.map((m, i) => (
              <path key={i} d={m.path} transform={`translate(${m.left} ${m.top})`} fillRule="evenodd"
                className="fill-rose-500/35 stroke-rose-500" strokeWidth={2} vectorEffect="non-scaling-stroke" />
            ))}
          </svg>
        )}
      </div>
      {shapes && (
        <div className="mt-1 flex flex-wrap items-center gap-2 text-xs text-slate-500">
          <span>
            <span className="font-medium text-rose-600">SAM 3.1</span> highlighted "{phrase}"
            {shapes.masks.length > 1 ? ` (${shapes.masks.length} areas)` : ''}
          </span>
          <button type="button" onClick={() => setShowHighlight((v) => !v)} className="underline hover:text-slate-700">
            {showHighlight ? 'Hide' : 'Show'} highlight
          </button>
        </div>
      )}
    </div>
  )
}

// Feeds the stored text to Meta's parser as if it had just streamed in, then turns each mask into an SVG outline
async function decodeSegmentation(text) {
  async function* events() {
    yield { type: 'response.output_text.delta', item_id: 'sam', output_index: 0, content_index: 0, delta: text }
    yield { type: 'response.output_text.done', item_id: 'sam', output_index: 0, content_index: 0, text }
    yield { type: 'response.completed' }
  }
  const result = await parseImageStream(events()).finalResult

  // Every box token repeats the photo's full size as w= and h=
  const size = text.match(/;w=(\d+);h=(\d+)/)
  const masks = recordsOfKind(result.records, 'mask').map((record) => ({
    path: decodeMaskToSVGPath(record.mask),
    left: record.bounds.left,
    top: record.bounds.top,
  }))
  if (!size || masks.length === 0) return null
  return { width: Number(size[1]), height: Number(size[2]), masks }
}
