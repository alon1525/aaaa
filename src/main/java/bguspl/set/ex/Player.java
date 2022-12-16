package bguspl.set.ex;

import java.util.Queue;
import java.util.logging.Level;
import java.util.LinkedList;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    public Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;
    private Dealer dealer;
    /**
     * The current score of the player.
     */
    private int score;

    public int[] currentTokens;

    public int tokenCount;

    public boolean isFrozen = false;

    public boolean isLegal = false;

    
    public Queue<Integer> numberPressed;
    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        currentTokens = new int[3];
        tokenCount = 0;
        this.dealer = dealer;
        numberPressed = new LinkedList<Integer>();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
           while(!numberPressed.isEmpty() & dealer.tableIsFull)
            {
                pressKey();
            }
            if(isFrozen)
           {      
                if(!isLegal){
                try {
                    env.ui.setFreeze(id, env.config.penaltyFreezeMillis);
                    dealer.isFrozen[id] = true;
                    Thread.sleep(env.config.penaltyFreezeMillis);
                    dealer.isFrozen[id] = false;
                    env.ui.setFreeze(id, 0);
                } catch (InterruptedException ignored) {}
                isFrozen = false;
                }
                else
                {
                    try{
                    env.ui.setFreeze(id, env.config.pointFreezeMillis);
                    dealer.isFrozen[id] = true;
                    Thread.sleep(env.config.pointFreezeMillis);
                    dealer.isFrozen[id] = false;
                    env.ui.setFreeze(id, 0);}
                    catch (InterruptedException ignored) {}
                    isFrozen = false;
                }
           } 
            
            
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            int max = 11;
            while (!terminate) {
                int randomNubmer = (int)(Math.random()*(max+1));
                if(numberPressed.size()<3 & dealer.tableIsFull){
                    numberPressed.add(randomNubmer);
                }
                try {
                    synchronized (this) { Thread.sleep(100); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if(numberPressed.size()<3 & dealer.tableIsFull == true){
            numberPressed.add(slot);
        }
        
    }

    public void pressKey()
    {
        while(!numberPressed.isEmpty() & dealer.tableIsFull == true){
            int slot = numberPressed.remove();
            boolean tokenIsThere = false;
            int i =0;
            while (i<tokenCount & !tokenIsThere){
            if(currentTokens[i]==slot){
                tokenIsThere = true;
                table.removeToken(this, slot);
            }
            i++;
            }
            if(!tokenIsThere & tokenCount<3){
                if(tokenCount==2)
                {
                    isFrozen = true;
                    isLegal = isSet(slot);
                }
                table.placeToken(this, slot);
            }
        }
    }
    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        resetTokens();
        score++;
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, score);
    }

    public Boolean isSet(int slot)
    {
        int var1 = table.slotToCard[currentTokens[0]];
        int var2 = table.slotToCard[currentTokens[1]];
        int var3 = table.slotToCard[slot];
        int[] vars = {var1,var2,var3};
        return env.util.testSet(vars);
    }

    public void resetTokens()
    {
        tokenCount = 0;
        env.ui.removeToken(id,currentTokens[0]);
        env.ui.removeToken(id,currentTokens[1]);
        env.ui.removeToken(id,currentTokens[2]);
    }
    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
       

    }

    public int getScore() {
        return score;
    }
}
