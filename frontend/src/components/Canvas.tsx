import { useEffect, useRef, type PointerEvent as RPE } from 'react'
import { useGame, type CanvasEvent } from '../context/GameContext'
import type { DrawBatch, Point, ServerMessage } from '../types/messages'

export interface Tool {
  color: string
  size: number
  eraser: boolean
}

interface Stroke {
  id: string
  color: string
  size: number
  eraser: boolean
  points: Point[]
}

export default function Canvas({ tool }: { tool: Tool }) {
  const { isDrawer, subscribeCanvas, replay, actions } = useGame()

  const wrapRef = useRef<HTMLDivElement>(null)
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const strokesRef = useRef<Stroke[]>([])
  const sizeRef = useRef({ w: 0, h: 0 })

  const toolRef = useRef(tool)
  toolRef.current = tool

  const drawingRef = useRef(false)
  const strokeIdRef = useRef<string | null>(null)
  const pendingRef = useRef<Point[]>([])
  const lastFlushRef = useRef(0)

  const ctx = () => canvasRef.current?.getContext('2d') ?? null
  const toPx = (p: Point) => ({ x: p.x * sizeRef.current.w, y: p.y * sizeRef.current.h })

  const drawSegment = (stroke: Stroke, from: Point | null, pts: Point[]) => {
    const c = ctx()
    if (!c || pts.length === 0) {
      return
    }
    c.lineJoin = 'round'
    c.lineCap = 'round'
    c.globalCompositeOperation = stroke.eraser ? 'destination-out' : 'source-over'
    c.strokeStyle = stroke.color
    c.fillStyle = stroke.color
    c.lineWidth = stroke.size

    const seq = from ? [from, ...pts] : pts
    if (seq.length === 1) {
      const a = toPx(seq[0])
      c.beginPath()
      c.arc(a.x, a.y, stroke.size / 2, 0, Math.PI * 2)
      c.fill()
    } else {
      c.beginPath()
      const s = toPx(seq[0])
      c.moveTo(s.x, s.y)
      for (let i = 1; i < seq.length; i++) {
        const p = toPx(seq[i])
        c.lineTo(p.x, p.y)
      }
      c.stroke()
    }
    c.globalCompositeOperation = 'source-over'
  }

  const clearPixels = () => {
    const c = ctx()
    c?.clearRect(0, 0, sizeRef.current.w, sizeRef.current.h)
  }

  const redrawAll = () => {
    clearPixels()
    for (const stroke of strokesRef.current) {
      drawSegment(stroke, null, stroke.points)
    }
  }

  const applyEvent = (event: CanvasEvent) => {
    if (event.kind === 'CLEAR') {
      strokesRef.current = []
      clearPixels()
      return
    }
    if (event.kind === 'UNDO') {
      strokesRef.current.pop()
      redrawAll()
      return
    }
    const b = event.batch
    let stroke = strokesRef.current.find((s) => s.id === b.strokeId)
    const prevLast = stroke && stroke.points.length ? stroke.points[stroke.points.length - 1] : null
    if (!stroke) {
      stroke = { id: b.strokeId, color: b.color, size: b.size, eraser: b.eraser, points: [] }
      strokesRef.current.push(stroke)
    }
    if (b.points.length) {
      drawSegment(stroke, prevLast, b.points)
      stroke.points.push(...b.points)
    }
  }

  useEffect(() => {
    const wrap = wrapRef.current
    const canvas = canvasRef.current
    if (!wrap || !canvas) {
      return
    }
    const ro = new ResizeObserver(() => {
      const dpr = window.devicePixelRatio || 1
      const w = wrap.clientWidth
      const h = wrap.clientHeight
      sizeRef.current = { w, h }
      canvas.width = Math.round(w * dpr)
      canvas.height = Math.round(h * dpr)
      canvas.style.width = `${w}px`
      canvas.style.height = `${h}px`
      const c = canvas.getContext('2d')
      if (c) {
        c.setTransform(dpr, 0, 0, dpr, 0, 0)
      }
      redrawAll()
    })
    ro.observe(wrap)
    return () => ro.disconnect()
  }, [])

  useEffect(() => subscribeCanvas(applyEvent), [subscribeCanvas])

  useEffect(() => {
    if (!replay) {
      return
    }
    strokesRef.current = []
    clearPixels()
    for (const m of replay as ServerMessage[]) {
      if (m.type === 'DRAW') {
        applyEvent({ kind: 'DRAW', batch: m.data as DrawBatch })
      } else if (m.type === 'CLEAR') {
        applyEvent({ kind: 'CLEAR' })
      } else if (m.type === 'UNDO') {
        applyEvent({ kind: 'UNDO' })
      }
    }
  }, [replay])


  const normPoint = (e: RPE<HTMLCanvasElement>): Point => {
    const rect = canvasRef.current!.getBoundingClientRect()
    return {
      x: (e.clientX - rect.left) / rect.width,
      y: (e.clientY - rect.top) / rect.height,
    }
  }

  const flush = (done: boolean) => {
    const id = strokeIdRef.current
    if (!id) {
      return
    }
    const pts = pendingRef.current
    pendingRef.current = []
    if (pts.length || done) {
      actions.sendDraw({
        strokeId: id,
        color: toolRef.current.color,
        size: toolRef.current.size,
        eraser: toolRef.current.eraser,
        points: pts,
        done,
      })
    }
    lastFlushRef.current = performance.now()
  }

  const onDown = (e: RPE<HTMLCanvasElement>) => {
    if (!isDrawer) {
      return
    }
    e.preventDefault()
    e.currentTarget.setPointerCapture?.(e.pointerId)
    drawingRef.current = true
    strokeIdRef.current = crypto.randomUUID()
    pendingRef.current = [normPoint(e)]
    flush(false)
  }

  const onMove = (e: RPE<HTMLCanvasElement>) => {
    if (!isDrawer || !drawingRef.current) {
      return
    }
    pendingRef.current.push(normPoint(e))
    if (performance.now() - lastFlushRef.current > 40) {
      flush(false)
    }
  }

  const onUp = () => {
    if (!isDrawer || !drawingRef.current) {
      return
    }
    drawingRef.current = false
    flush(true)
    strokeIdRef.current = null
  }

  return (
    <div ref={wrapRef} className="relative h-full w-full overflow-hidden rounded-xl bg-white">
      <canvas
        ref={canvasRef}
        onPointerDown={onDown}
        onPointerMove={onMove}
        onPointerUp={onUp}
        onPointerLeave={onUp}
        className={`block touch-none ${isDrawer ? 'cursor-crosshair' : 'cursor-default'}`}
      />
    </div>
  )
}
