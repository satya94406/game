import { useGame } from '../context/GameContext'

/** The word being guessed: real letters for the drawer, masked blanks for everyone else. */
export default function WordHint() {
  const { phase, isDrawer, chosenWord, maskedWord, wordLength } = useGame()
  if (phase !== 'DRAWING') {
    return null
  }
  const display = isDrawer && chosenWord ? chosenWord : maskedWord

  return (
    <div className="flex flex-col items-center">
      <div className="flex items-end gap-1.5 text-2xl font-extrabold tracking-wide text-slate-800">
        {display.split('').map((ch, i) => {
          if (ch === ' ') {
            return <span key={i} className="w-3" />
          }
          if (ch === '_') {
            return <span key={i} className="w-4 border-b-[3px] border-slate-400" />
          }
          return (
            <span key={i} className="w-4 border-b-[3px] border-transparent text-center">
              {ch}
            </span>
          )
        })}
      </div>
      <span className="mt-1 text-xs font-semibold text-slate-400">{wordLength} letters</span>
    </div>
  )
}
