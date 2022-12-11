package bguspl.set.ex;

import bguspl.set.Env;

import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.sql.ShardingKey;
import java.util.*;
/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;
    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;
    private long currentTime = 60;
    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = 0;

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
        while (!shouldFinish()) {
            placeCardsOnTable();
            timerLoop();
            removeAllCardsFromTable();
            updateTimerDisplay(true);
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && currentTime > reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            currentTime--;
            removeCardsFromTable();
            placeCardsOnTable();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        for(Player player : players)
        {
            boolean isSet = env.util.testSet(player.currentTokens);
            if(isSet)
            {
                player.point();
            }
        }
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        for(int i =0;i<=11;i++)//for every empty slot we put a card there
        {
            if(!deck.isEmpty() && table.slotToCard[i]==null )
            {
                Integer card = deck.remove(0);
                table.placeCard(card, i);
            }
        }
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}
        
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        if(!reset)
        {
            if(currentTime <= 5)
                env.ui.setCountdown(currentTime*1000, true);
            else
            {
                env.ui.setCountdown(currentTime*1000, false);
            }
        }
        else
        {
            currentTime = 60;
            env.ui.setCountdown(currentTime*1000, false);
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        for(int i =0;i<=11;i++)//removing all the cards on the table
        {
            table.removeCard(i);
        }
        Collections.shuffle(deck);//rearrange the cards so it will be different
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        for (int i = 1; i < players.length; i++)
        {
            Player player = players[i];
            int j = i-1;
            while (j >= 0 && players[j].getScore() > player.getScore())
            {
                players[j + 1] = players[j];
                j = j - 1;
            }
            players[j + 1] = player;
        }
        int[] bestPlayers = new int[players.length];
        int bestScore = players[players.length-1].getScore();
        int index = players.length-1;
        while(players[index].getScore() == bestScore)
        {
            bestPlayers[index] = players[index].id;
            index--;
        }
        env.ui.announceWinner(bestPlayers);
    }

    

}
