package com.skribbl.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;


public class Game {

    public enum GuessOutcome { CORRECT, CLOSE, WRONG, IGNORED }

    private static final int MIN_GUESS_POINTS = 50;
    private static final int MAX_GUESS_BONUS = 400;
    private static final int DRAWER_MAX_POINTS = 250;

    private final Room room;
    private final int totalRounds;

    private int currentRound = 0;            
    private List<String> drawerOrder = new ArrayList<>();
    private int drawerIndex = -1;
    private String currentDrawerId;

    private GamePhase phase = GamePhase.LOBBY;
    private List<String> wordOptions = new ArrayList<>();
    private String currentWord;

    private long turnStartMillis;
    private long turnEndMillis;

    private final Set<String> guessedPlayers = new LinkedHashSet<>();     
    private final Map<String, Double> guessFraction = new HashMap<>();     
    private final Map<String, Integer> roundGains = new HashMap<>();       
    private final Set<Integer> revealedHints = new HashSet<>();           

    private final Random random = new Random();

    public Game(Room room) {
        this.room = room;
        this.totalRounds = room.getSettings().getRounds();
    }

    public void start() {
        currentRound = 1;
        buildDrawerOrder();
        drawerIndex = 0;
        currentDrawerId = drawerOrder.isEmpty() ? null : drawerOrder.get(0);
        beginChoosing();
    }

    private void buildDrawerOrder() {
        drawerOrder = room.connectedPlayerIds();
    }

    private void beginChoosing() {
        phase = GamePhase.CHOOSING;
        wordOptions = new ArrayList<>();
        currentWord = null;
        guessedPlayers.clear();
        guessFraction.clear();
        roundGains.clear();
        revealedHints.clear();
    }

    public void setWordOptions(List<String> options) {
        this.wordOptions = new ArrayList<>(options);
    }

    public List<String> getWordOptions() {
        return wordOptions;
    }

    public boolean chooseWord(String word) {
        if (phase != GamePhase.CHOOSING) {
            return false;
        }
        String chosen = null;
        for (String opt : wordOptions) {
            if (opt.equalsIgnoreCase(word)) {
                chosen = opt;
                break;
            }
        }
        if (chosen == null) {
            if (wordOptions.isEmpty()) {
                return false;
            }
            chosen = wordOptions.get(0);
        }
        currentWord = chosen;
        phase = GamePhase.DRAWING;
        turnStartMillis = System.currentTimeMillis();
        turnEndMillis = turnStartMillis + room.getSettings().getDrawTime() * 1000L;
        guessedPlayers.clear();
        guessFraction.clear();
        roundGains.clear();
        revealedHints.clear();
        return true;
    }

    public GuessOutcome registerGuess(String playerId, String text) {
        if (phase != GamePhase.DRAWING) {
            return GuessOutcome.IGNORED;
        }
        if (playerId.equals(currentDrawerId) || guessedPlayers.contains(playerId)) {
            return GuessOutcome.IGNORED;
        }
        if (matches(text)) {
            double frac = fractionLeft();
            int points = MIN_GUESS_POINTS + (int) Math.round(MAX_GUESS_BONUS * frac);
            guessedPlayers.add(playerId);
            guessFraction.put(playerId, frac);
            Player p = room.getPlayer(playerId);
            if (p != null) {
                p.addScore(points);
            }
            roundGains.merge(playerId, points, Integer::sum);
            return GuessOutcome.CORRECT;
        }
        if (isClose(text, currentWord)) {
            return GuessOutcome.CLOSE;
        }
        return GuessOutcome.WRONG;
    }

    public boolean everyoneGuessed() {
        List<String> others = new ArrayList<>(room.connectedPlayerIds());
        others.remove(currentDrawerId);
        if (others.isEmpty()) {
            return true;
        }
        return guessedPlayers.containsAll(others);
    }

    public void endTurn() {
        if (!guessFraction.isEmpty()) {
            double avg = guessFraction.values().stream().mapToDouble(Double::doubleValue).average().orElse(0);
            int drawerGain = (int) Math.round(avg * DRAWER_MAX_POINTS);
            Player drawer = room.getPlayer(currentDrawerId);
            if (drawer != null && drawerGain > 0) {
                drawer.addScore(drawerGain);
                roundGains.merge(currentDrawerId, drawerGain, Integer::sum);
            }
        }
        phase = GamePhase.ROUND_END;
    }

    public boolean advance() {
        int maxSteps = (drawerOrder.size() + 1) * 2 + 4; 
        for (int step = 0; step < maxSteps; step++) {
            drawerIndex++;
            if (drawerIndex >= drawerOrder.size()) {
                currentRound++;
                if (currentRound > totalRounds) {
                    phase = GamePhase.GAME_OVER;
                    return false;
                }
                buildDrawerOrder();
                drawerIndex = 0;
                if (drawerOrder.isEmpty()) {
                    phase = GamePhase.GAME_OVER;
                    return false;
                }
            }
            String candidate = drawerOrder.get(drawerIndex);
            if (room.isConnected(candidate)) {
                currentDrawerId = candidate;
                beginChoosing();
                return true;
            }
        }
        phase = GamePhase.GAME_OVER;
        return false;
    }

    public String maskedWord() {
        if (currentWord == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < currentWord.length(); i++) {
            char c = currentWord.charAt(i);
            if (!Character.isLetter(c)) {
                sb.append(c);
            } else if (revealedHints.contains(i)) {
                sb.append(c);
            } else {
                sb.append('_');
            }
        }
        return sb.toString();
    }

    public String revealNextHint() {
        if (currentWord == null) {
            return "";
        }
        List<Integer> hidden = new ArrayList<>();
        for (int i = 0; i < currentWord.length(); i++) {
            if (Character.isLetter(currentWord.charAt(i)) && !revealedHints.contains(i)) {
                hidden.add(i);
            }
        }
        if (hidden.size() <= 1) {
            return maskedWord();
        }
        revealedHints.add(hidden.get(random.nextInt(hidden.size())));
        return maskedWord();
    }

    public int wordLength() {
        return currentWord == null ? 0 : currentWord.length();
    }

    public int secondsLeft() {
        return (int) Math.max(0, Math.round((turnEndMillis - System.currentTimeMillis()) / 1000.0));
    }

    public long getTurnEndMillis() {
        return turnEndMillis;
    }

    private double fractionLeft() {
        double total = turnEndMillis - turnStartMillis;
        if (total <= 0) {
            return 0;
        }
        return Math.max(0, Math.min(1, (turnEndMillis - System.currentTimeMillis()) / total));
    }

    private boolean matches(String guess) {
        return normalize(guess).equals(normalize(currentWord));
    }

    public static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    public static boolean isClose(String guess, String word) {
        String g = normalize(guess);
        String w = normalize(word);
        if (w.length() <= 3 || g.isEmpty()) {
            return false;
        }
        return levenshtein(g, w) == 1;
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }

    public void markGameOver() {
        this.phase = GamePhase.GAME_OVER;
    }


    public GamePhase getPhase() {
        return phase;
    }

    public String getCurrentDrawerId() {
        return currentDrawerId;
    }

    public String getCurrentWord() {
        return currentWord;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getTotalRounds() {
        return totalRounds;
    }

    public Set<String> getGuessedPlayers() {
        return guessedPlayers;
    }

    public Map<String, Integer> getRoundGains() {
        return roundGains;
    }
}
