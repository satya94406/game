import { useGame } from '../context/GameContext'

export default function WordPicker() {
  const { phase, isDrawer, wordOptions, drawerName, actions } = useGame()
  if (phase !== 'CHOOSING') {
    return null
  }

  return (
    <div className="absolute inset-0 z-10 flex items-center justify-center bg-slate-900/40 backdrop-blur-sm">
      <div className="animate-pop rounded-2xl bg-white px-8 py-6 text-center shadow-2xl">
        {isDrawer && wordOptions && wordOptions.length > 0 ? (
          <>
            <h3 className="mb-4 text-lg font-bold text-slate-700">Choose a word to draw</h3>
            <div className="flex flex-wrap justify-center gap-3">
              {wordOptions.map((w) => (
                <button
                  key={w}
                  type="button"
                  onClick={() => actions.chooseWord(w)}
                  className="rounded-xl bg-indigo-500 px-5 py-2.5 text-lg font-bold text-white shadow transition hover:bg-indigo-600"
                >
                  {w}
                </button>
              ))}
            </div>
          </>
        ) : (
          <p className="text-lg text-slate-600">
            <span className="font-bold text-indigo-600">{drawerName ?? 'The drawer'}</span> is choosing a word…
          </p>
        )}
      </div>
    </div>
  )
}
