package com.example.maybefinalp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.ContentValues;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.ScrollView;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import android.database.sqlite.SQLiteDatabase;

public class BlackjackActivity extends AppCompatActivity {

    LinearLayout llMain;
    private int coins;
    private int betAmount;
    private int playerScore;
    private int dealerScore;
    // Add this at the top of your class, along with your other variables
    private int hitCount = 0;  // Track the number of hits
    private boolean hasDoubled = false;
    private boolean playerHasMoved = false; // Track if the player has made a move
    private boolean hasSplit = false; // Track if the player has split
    private Random random = new Random();
    private boolean playingFirstHand = false;
    private List<Integer> playerHand = new ArrayList<>();
    private List<Integer> splitHand = new ArrayList<>();
    private List<Integer> dealerHand = new ArrayList<>();
    private int cardsDrawn = 0;
    private TextView playerScoreTextView, dealerScoreTextView, resultTextView, coinCountTextView;
    private EditText betInput;
    private List<Integer> deck = new ArrayList<>();
    private Button hitButton, standButton, dealButton, doubleButton, splitButton, helpButton;
    private Button returnButton;
    private boolean isPlaying = true;
    private boolean isPlayerStanding = false;
    private boolean isRoundActive = false;
    // ImageView for displaying cards
    MediaPlayer backgroundMusic;
    private SharedPreferences sharedPreferences;
    private int initialBackgroundDrawable;
    private int initialRankDrawable;
    private LinearLayout playerCardsLayout;
    private LinearLayout dealerCardsLayout;
    private MediaPlayer mediaPlayer;
    private String currentUserEmail;
    private int rankDrawable;
    private TextView firstHandResultText;
    private boolean isPlayingFirstHand = true;
    private String firstHandResult = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_blackjack);

            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

            // Check if user is logged in
            String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
            if (currentUserEmail == null) {
                Toast.makeText(this, "Please log in first!", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }

            // Get current coins
            coins = sharedPreferences.getInt("coins_" + currentUserEmail, 0);

            // Initialize UI components with null checks
            llMain = findViewById(R.id.llMain);
            if (llMain == null) {
                throw new IllegalStateException("Main layout not found");
            }

            // Initialize text views
            coinCountTextView = findViewById(R.id.coinCount);
            playerScoreTextView = findViewById(R.id.playerScore);
            dealerScoreTextView = findViewById(R.id.dealerScore);
            resultTextView = findViewById(R.id.resultText);
            betInput = findViewById(R.id.betInput);
            firstHandResultText = findViewById(R.id.firstHandResultText);

            if (coinCountTextView == null || playerScoreTextView == null || 
                dealerScoreTextView == null || resultTextView == null || betInput == null || firstHandResultText == null) {
                throw new IllegalStateException("Required text views not found");
            }

            // Update username display
            TextView usernameDisplay = findViewById(R.id.usernameDisplay);
            if (usernameDisplay != null) {
                String username = sharedPreferences.getString("username_" + currentUserEmail, "Guest");
                usernameDisplay.setText("Player: " + username);
            }

            // Initialize buttons
            dealButton = findViewById(R.id.dealButton);
            hitButton = findViewById(R.id.hitButton);
            standButton = findViewById(R.id.standButton);
            doubleButton = findViewById(R.id.doubleDownButton);
            splitButton = findViewById(R.id.splitButton);
            returnButton = findViewById(R.id.returnButton);
            helpButton = findViewById(R.id.helpButton);
            playerCardsLayout = findViewById(R.id.playerCardsLayout);
            dealerCardsLayout = findViewById(R.id.dealerCardsLayout);
            Button btnToggleMusic = findViewById(R.id.btnToggleMusic);

            if (dealButton == null || hitButton == null || standButton == null || 
                doubleButton == null || splitButton == null || returnButton == null || 
                helpButton == null || playerCardsLayout == null || dealerCardsLayout == null) {
                throw new IllegalStateException("Required buttons or layouts not found");
            }

            // Set up button click listeners
            dealButton.setOnClickListener(v -> startNewRound());
            hitButton.setOnClickListener(v -> playerHit());
            standButton.setOnClickListener(v -> playerStand());
            doubleButton.setOnClickListener(v -> playerDouble());
            splitButton.setOnClickListener(v -> playerSplit());
            returnButton.setOnClickListener(v -> {
                if (isRoundActive) {
                    Toast.makeText(this, "You must finish the round before leaving!", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent resultIntent = new Intent();
                resultIntent.putExtra("coins", coins);
                setResult(RESULT_OK, resultIntent);
                finish();
            });
            helpButton.setOnClickListener(v -> showHelpDialog());

            // Initialize music player
            try {
                backgroundMusic = MediaPlayer.create(this, R.raw.background_music);
                if (backgroundMusic != null) {
                    backgroundMusic.setLooping(true);
                    // Get saved music state for this specific user
                    isPlaying = sharedPreferences.getBoolean("music_playing_" + currentUserEmail, true);
                    if (isPlaying) {
                        backgroundMusic.start();
                        btnToggleMusic.setText("stop music");
                    } else {
                        btnToggleMusic.setText("start music");
                    }
                }
            } catch (Exception e) {
                Log.e("Blackjack", "Error initializing background music: " + e.getMessage());
            }

            // Set up music toggle button
            if (btnToggleMusic != null) {
                btnToggleMusic.setOnClickListener(v -> {
                    if (backgroundMusic != null) {
                        if (isPlaying) {
                            backgroundMusic.pause();
                            btnToggleMusic.setText("start music");
                        } else {
                            backgroundMusic.start();
                            btnToggleMusic.setText("stop music");
                        }
                        isPlaying = !isPlaying;
                        // Save music state for this specific user
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("music_playing_" + currentUserEmail, isPlaying);
                        editor.apply();
                    }
                });
            }

            // Update initial UI state
            coinCountTextView.setText("Coins: " + coins);
            updateButtonStates();
            updateBackground();

            // Initialize game state
            initializeGame();
            updateUI();

        } catch (Exception e) {
            Log.e("BlackjackActivity", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing game: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (backgroundMusic != null) {
            backgroundMusic.release();
            backgroundMusic = null;
        }
    }

    private void initializeDeck() {
        deck.clear(); // Make sure to clear before adding new cards
        for (int i = 1; i <= 13; i++) { // Cards from 1 to 13
            for (int j = 0; j < 4; j++) { // Each appears 4 times
                deck.add(i);
            }
        }
        Collections.shuffle(deck); // Shuffle to randomize the order
    }

    private void updateCardImagesForPlayerHand() {
        LinearLayout playerCardsLayout = findViewById(R.id.playerCardsLayout);
        playerCardsLayout.removeAllViews(); // Clear previous views before updating

        int totalCards = playerHand.size();
        Context context = this;

        int bigWidth = context.getResources().getDimensionPixelSize(R.dimen.card_width_big);
        int bigHeight = context.getResources().getDimensionPixelSize(R.dimen.card_height_big);
        int mediumWidth = context.getResources().getDimensionPixelSize(R.dimen.card_width_medium);
        int mediumHeight = context.getResources().getDimensionPixelSize(R.dimen.card_height_medium);
        int smallWidth = context.getResources().getDimensionPixelSize(R.dimen.card_width_small);
        int smallHeight = context.getResources().getDimensionPixelSize(R.dimen.card_height_small);
        int tinyWidth = context.getResources().getDimensionPixelSize(R.dimen.card_width_tiny);
        int tinyHeight = context.getResources().getDimensionPixelSize(R.dimen.card_height_tiny);

        for (int i = 0; i < totalCards; i++) {
            ImageView cardImageView = new ImageView(this);
            cardImageView.setImageResource(getCardImageResource(playerHand.get(i)));

            LinearLayout.LayoutParams layoutParams;

            if (i < 3) {
                // First three cards (big)
                layoutParams = new LinearLayout.LayoutParams(bigWidth, bigHeight);
            } else if (i < 5) {
                // Next two cards (medium)
                layoutParams = new LinearLayout.LayoutParams(mediumWidth, mediumHeight);
            } else if (i < 7) {
                // Next two cards (small)
                layoutParams = new LinearLayout.LayoutParams(smallWidth, smallHeight);
            } else {
                // Remaining cards (tiny)
                layoutParams = new LinearLayout.LayoutParams(tinyWidth, tinyHeight);
            }

            layoutParams.setMargins(5, 0, 5, 0); // Adjust margins for spacing
            cardImageView.setLayoutParams(layoutParams);
            cardImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

            playerCardsLayout.addView(cardImageView);
        }

        // Refresh the UI
        playerCardsLayout.invalidate();
        playerCardsLayout.requestLayout();
    }

    private void updateCardImagesForSplitHand() {
        if (splitHand.size() > 2) {
            LinearLayout playerCardsLayout = findViewById(R.id.playerCardsLayout);
            ImageView newCardImageView = new ImageView(this);
            newCardImageView.setImageResource(getCardImageResource(splitHand.get(splitHand.size() - 1)));
            
            // Add the new card to the player cards layout
            playerCardsLayout.addView(newCardImageView);
        }
    }

    private void startNewRound() {
        // Clear previous round data
        playerHand.clear();
        dealerHand.clear();
        playerScore = 0;
        dealerScore = 0;
        hasDoubled = false;
        playerHasMoved = false;
        isPlayerStanding = false; // Reset the standing flag
        
        // Get bet amount
        String betText = betInput.getText().toString();
        if (betText.isEmpty()) {
            Toast.makeText(this, "Please enter a bet amount", Toast.LENGTH_SHORT).show();
            return;
        }
        
        betAmount = Integer.parseInt(betText);
        if (betAmount <= 0) {
            Toast.makeText(this, "Bet amount must be greater than 0", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (betAmount > coins) {
            Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show();
            isRoundActive = false; // Make sure round is not active
            updateButtonStates();
            return;
        }
        
        // Only set round as active if we have enough coins
        isRoundActive = true;
        
        // Initialize deck if empty
        if (deck.isEmpty()) {
            initializeDeck();
        }
        
        // Deal initial cards
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        
        // Update UI
        updateCardImages();
        updateScores();
        updateButtonStates();
        
        // Check for blackjack
        if (calculateScore(playerHand) == 21) {
            handleBlackjack();
        }
    }

    private void playerHit() {
        if (!isRoundActive) return;

        int newCard = drawCard();
        if (hasSplit) {
            if (isPlayingFirstHand) {
                playerHand.add(newCard);
                // Update UI immediately after hit
                updateCardImages();
                updateScores();
                
                // Check for bust
                int currentHandValue = calculateScore(playerHand);
                if (currentHandValue > 21) {
                    firstHandResult = "Bust - Score: " + currentHandValue;
                    firstHandResultText.setText("First Hand: " + firstHandResult);
                    firstHandResultText.setVisibility(View.VISIBLE);
                    
                    // Show first hand cards for a few seconds before switching
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    isPlayingFirstHand = false;
                    resultTextView.setText("First hand busted. Playing second hand.");
                    // Clear first hand cards and show second hand
                    updateCardImages();
                    updateScores();
                } else if (currentHandValue == 21) {
                    firstHandResult = "21";
                    firstHandResultText.setText("First Hand: " + firstHandResult);
                    firstHandResultText.setVisibility(View.VISIBLE);
                    
                    // Show first hand cards for a few seconds before switching
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    
                    isPlayingFirstHand = false;
                    resultTextView.setText("First hand 21. Playing second hand.");
                    // Clear first hand cards and show second hand
                    updateCardImages();
                    updateScores();
                }
            } else {
                splitHand.add(newCard);
                // Update UI immediately after hit
                updateCardImages();
                updateScores();
                
                // Check for bust
                int currentHandValue = calculateScore(splitHand);
                if (currentHandValue > 21) {
                    String secondHandResult = "Bust - Score: " + currentHandValue;
                    resultTextView.setText("First Hand: " + firstHandResult + "\nSecond Hand: " + secondHandResult);
                    dealerPlay();
                } else if (currentHandValue == 21) {
                    String secondHandResult = "21";
                    resultTextView.setText("First Hand: " + firstHandResult + "\nSecond Hand: " + secondHandResult);
                    dealerPlay();
                }
            }
        } else {
            playerHand.add(newCard);
            // Update UI immediately after hit
            updateCardImages();
            updateScores();

            // Check for bust
            int currentHandValue = calculateScore(playerHand);
            if (currentHandValue > 21) {
                resultTextView.setText("Bust! You lose " + betAmount + " coins!");
            coins -= betAmount;
            updateCoins(coins);
            endRound();
            } else if (currentHandValue == 21) {
            resultTextView.setText("21! Standing automatically...");
                playerStand();
            }
        }

        updateButtonStates();
    }

    private void playerStand() {
        if (hasSplit) {
            if (isPlayingFirstHand) {
                // Evaluate first hand
                firstHandResult = "Score: " + calculateScore(playerHand);
                firstHandResultText.setText("First Hand: " + firstHandResult);
                firstHandResultText.setVisibility(View.VISIBLE);
                
                // Show first hand cards for a few seconds before switching
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                // Switch to second hand
                isPlayingFirstHand = false;
                resultTextView.setText("First hand complete. Playing second hand.");
                
                // Update scores and cards to show second hand
        updateScores();
                updateCardImages();
            } else {
                // Evaluate second hand
                String secondHandResult = "Score: " + calculateScore(splitHand);
                resultTextView.setText("First Hand: " + firstHandResult + "\nSecond Hand: " + secondHandResult);
                
                // Dealer's turn
                dealerPlay();
            }
        } else {
            dealerPlay();
        }
    }

    private void playerDouble() {
        if (!isRoundActive || hasDoubled || coins < betAmount) return;

        // Double the bet amount
        betAmount *= 2;
        hasDoubled = true;

        int newCard = drawCard();
        playerHand.add(newCard);
        updateCardImagesForPlayerHand();

        updateScores();

        if (calculateScore(playerHand) > 21) {
            Log.d("Blackjack", "Player busted after doubling, deducting doubled bet amount: " + betAmount);
            coins -= betAmount;
            updateCoins(coins);
            resultTextView.setText("Bust! You lose " + betAmount + " coins!");
            endRound();
        } else {
            playerStand();
        }
    }

    private int getCardValue(int card) {
        if (card > 10) return 10; // Face cards (Jack, Queen, King) all count as 10
        return card;
    }

    private void playerSplit() {
        if (playerHand.size() == 2 && getCardValue(playerHand.get(0)) == getCardValue(playerHand.get(1))) {
            // Deduct the bet amount again for the split
            int currentBet = Integer.parseInt(betInput.getText().toString());
            if (currentBet > coins) {
                Toast.makeText(this, "Not enough coins for split!", Toast.LENGTH_SHORT).show();
                return;
            }
            coins -= currentBet;
            updateCoins(coins);

            // Create split hand with second card
            splitHand = new ArrayList<>();
            splitHand.add(playerHand.remove(1));

            // Add new card to each hand
            playerHand.add(drawCard());
            splitHand.add(drawCard());

                hasSplit = true;
            isPlayingFirstHand = true;
            updateUI();
            updateButtonStates();
            
            // Show which hand is being played
            resultTextView.setText("Playing first hand");
            
            // Preserve the bet amount
            betInput.setText(String.valueOf(currentBet));
        }
    }

    private void updateCardImagesForDealerHand(int newCard) {
        LinearLayout dealerCardsLayout = findViewById(R.id.dealerCardsLayout);

        ImageView newCardImageView = new ImageView(this);
        newCardImageView.setImageResource(getCardImageResource(newCard));

        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                getResources().getDimensionPixelSize(R.dimen.card_width_small),
                getResources().getDimensionPixelSize(R.dimen.card_height_small)
        );
        layoutParams.setMargins(8, 0, 8, 0);
        newCardImageView.setLayoutParams(layoutParams);

        dealerCardsLayout.addView(newCardImageView);
    }

    private void dealerPlay() {
        // First reveal dealer's second card
        isPlayerStanding = true;
        updateCardImages();
        updateScores();
        
        // Add a small delay to make the dealer's play visible
        try {
            Thread.sleep(300); // Reduced from 500 to 300 milliseconds
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        // Dealer must hit on soft 17
        while (calculateScore(dealerHand) < 17 || (calculateScore(dealerHand) == 17 && hasSoft17(dealerHand))) {
            int newCard = drawCard();
            dealerHand.add(newCard);
            updateCardImages();
            updateScores();
            
            // Add a small delay between dealer's hits
            try {
                Thread.sleep(300); // Reduced from 500 to 300 milliseconds
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        // Evaluate the winner
        evaluateWinner();
        
        // End the round
        endRound();
    }

    private int evaluateWinner() {
        int playerScore = calculateScore(playerHand);
        int splitScore = hasSplit ? calculateScore(splitHand) : 0;
        int dealerScore = calculateScore(dealerHand);
        Log.d("Blackjack", "=== Starting evaluateWinner ===");
        Log.d("Blackjack", "Initial coins: " + coins);
        Log.d("Blackjack", "Player score: " + playerScore + ", Dealer score: " + dealerScore);
        Log.d("Blackjack", "Has split: " + hasSplit + ", Split score: " + splitScore);

        // Player busts - always lose
        if (playerScore > 21) {
            Log.d("Blackjack", "Player busted, deducting bet amount: " + betAmount);
            coins -= betAmount;
            updateCoins(coins);
            resultTextView.setText("Player busts! You lose " + betAmount + " coins.");
            Log.d("Blackjack", "Final coins after player bust: " + coins);
            return coins;
        }

        // Dealer busts - player wins
        if (dealerScore > 21) {
            Log.d("Blackjack", "Dealer busted, adding bet amount: " + betAmount);
            coins += betAmount;
            updateCoins(coins);
            resultTextView.setText("Dealer busts! You win " + betAmount + " coins!");
            Log.d("Blackjack", "Final coins after dealer bust: " + coins);
            return coins;
        }

        // Compare scores
        if (playerScore > dealerScore) {
            Log.d("Blackjack", "Player wins, adding bet amount: " + betAmount);
            coins += betAmount;
            updateCoins(coins);
            resultTextView.setText("You win " + betAmount + " coins!");
            Log.d("Blackjack", "Final coins after player win: " + coins);
        } else if (playerScore < dealerScore) {
            Log.d("Blackjack", "Player loses, deducting bet amount: " + betAmount);
            coins -= betAmount;
            updateCoins(coins);
            resultTextView.setText("You lose " + betAmount + " coins.");
            Log.d("Blackjack", "Final coins after player loss: " + coins);
        } else {
            Log.d("Blackjack", "Push - no change in coins");
            resultTextView.setText("Push! No change in coins.");
        }

        // Handle split hand if it exists
        if (hasSplit) {
            Log.d("Blackjack", "Evaluating split hand");
            if (splitScore > 21) {
                Log.d("Blackjack", "Split hand busted, deducting bet amount: " + betAmount);
                coins -= betAmount;
                updateCoins(coins);
                resultTextView.setText(resultTextView.getText() + "\nSplit hand busts! You lose " + betAmount + " coins.");
            } else if (splitScore > dealerScore) {
                Log.d("Blackjack", "Split hand wins, adding bet amount: " + betAmount);
                coins += betAmount;
                updateCoins(coins);
                resultTextView.setText(resultTextView.getText() + "\nSplit hand wins " + betAmount + " coins!");
            } else if (splitScore < dealerScore) {
                Log.d("Blackjack", "Split hand loses, deducting bet amount: " + betAmount);
                coins -= betAmount;
                updateCoins(coins);
                resultTextView.setText(resultTextView.getText() + "\nSplit hand loses " + betAmount + " coins.");
            }
            Log.d("Blackjack", "Final coins after split hand evaluation: " + coins);
        }

        Log.d("Blackjack", "=== End of evaluateWinner, final coins: " + coins + " ===");
        return coins;
    }

    private void endRound() {
        Log.d("Blackjack", "=== Starting endRound ===");
        Log.d("Blackjack", "Current coins before endRound: " + coins);
        
        isRoundActive = false;
        updateButtonStates();
        
        // Clear pre-bet coins amount
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("pre_bet_coins_" + currentUserEmail);
            boolean success = editor.commit();
            Log.d("Blackjack", "Cleared pre-bet coins from SharedPreferences: " + success);
        }
        
        // Update background and rank after round ends
        updateBackground();
        
        // Save final coins to SharedPreferences
        if (currentUserEmail != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("coins_" + currentUserEmail, coins);
            boolean success = editor.commit();
            Log.d("Blackjack", "Saved final coins to SharedPreferences: " + success + ", coins: " + coins);
        }
        
        // Update database with final coins
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("coins", coins);
        int rowsAffected = db.update("users", values, "email = ?", new String[]{currentUserEmail});
        Log.d("Blackjack", "Database update affected " + rowsAffected + " rows, final coins: " + coins);
        db.close();
        
        // Clear result messages
        resultTextView.setText("");
        firstHandResultText.setVisibility(View.GONE);
        firstHandResult = "";
        
        // Reset split state
        hasSplit = false;
        isPlayingFirstHand = true;
        
        // Return to MainActivity with current coins
        Intent returnIntent = new Intent();
        returnIntent.putExtra("coins", coins);
        setResult(RESULT_OK, returnIntent);
        Log.d("Blackjack", "=== End of endRound, final coins: " + coins + " ===");
    }

    private void updateButtonStates() {
        boolean canDouble = playerHand.size() == 2 && !hasDoubled;
        boolean canSplit = playerHand.size() == 2 && 
                        getCardValue(playerHand.get(0)) == getCardValue(playerHand.get(1)) &&
                          !hasSplit;

        hitButton.setEnabled(isRoundActive);
        standButton.setEnabled(isRoundActive);
        doubleButton.setEnabled(isRoundActive && canDouble);
        splitButton.setEnabled(isRoundActive && canSplit);
        dealButton.setEnabled(!isRoundActive);
    }

    public int drawCard() {
        if (deck.isEmpty()) { // Only reset if deck is empty
            System.out.println("Deck is empty! Reshuffling...");
            cardsDrawn = 0;  // Reset the cards drawn counter
            initializeDeck();  // Initialize a fresh deck of cards
            resultTextView.setText("Cards are reshuffled. Deck is restored.");
        }

        cardsDrawn++;  // Increment the number of cards drawn

        // Check if a third of the deck has been drawn
        if (cardsDrawn > deck.size() / 3) {
            System.out.println("One-third of the deck has been drawn. Reshuffling...");
            cardsDrawn = 0;  // Reset the counter
            initializeDeck();  // Reshuffle the deck
            resultTextView.setText("Cards are reshuffled. Deck is restored.");
        }

        return deck.remove(0); // Draw the top card and remove it
    }

    private int calculateScore(List<Integer> hand) {
        int score = 0;
        int aces = 0;

        for (int card : hand) {
            if (card == 1) {
                aces++;
            } else if (card > 10) {
                score += 10;
            } else {
                score += card;
            }
        }

        for (int i = 0; i < aces; i++) {
            if (score + 11 <= 21) {
                score += 11;
            } else {
                score += 1;
            }
        }

        return score;
    }

    private boolean hasSoft17(List<Integer> hand) {
        return calculateScore(hand) == 17 && hand.contains(1);
    }

    private void updateScores() {
        // Update player and dealer cards
        if (hasSplit) {
            if (isPlayingFirstHand) {
                playerScoreTextView.setText("First Hand Score: " + calculateScore(playerHand));
            } else {
                playerScoreTextView.setText("Second Hand Score: " + calculateScore(splitHand));
            }
        } else {
        playerScoreTextView.setText("Player Score: " + calculateScore(playerHand));
        }
        
        dealerScoreTextView.setText("Dealer Score: " + (isPlayerStanding || !isRoundActive ? calculateScore(dealerHand) : "?"));

        // Update the images of the cards based on the hand
        updateCardImages();
    }

    private void updateCardImages() {
        try {
            // Clear previous cards
            LinearLayout playerCardsLayout = findViewById(R.id.playerCardsLayout);
            LinearLayout dealerCardsLayout = findViewById(R.id.dealerCardsLayout);
            
            if (playerCardsLayout == null || dealerCardsLayout == null) {
                Log.e("Blackjack", "Card layouts not found");
                return;
            }
            
                playerCardsLayout.removeAllViews();
                dealerCardsLayout.removeAllViews();

            // Get card dimensions
            int cardWidth = getResources().getDimensionPixelSize(R.dimen.card_width_small);
            int cardHeight = getResources().getDimensionPixelSize(R.dimen.card_height_small);

            // Update player's cards
            if (hasSplit) {
                if (isPlayingFirstHand) {
                    // Show all cards of first hand
                for (int card : playerHand) {
                        ImageView cardImageView = new ImageView(this);
                        cardImageView.setImageResource(getCardImageResource(card));
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                cardWidth,
                                cardHeight
                        );
                        layoutParams.setMargins(8, 0, 8, 0);
                        cardImageView.setLayoutParams(layoutParams);
                        playerCardsLayout.addView(cardImageView);
                    }
                } else {
                    // Show all cards of second hand
                    for (int card : splitHand) {
                        ImageView cardImageView = new ImageView(this);
                        cardImageView.setImageResource(getCardImageResource(card));
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                cardWidth,
                                cardHeight
                        );
                        layoutParams.setMargins(8, 0, 8, 0);
                        cardImageView.setLayoutParams(layoutParams);
                        playerCardsLayout.addView(cardImageView);
                    }
                    
                    // Show first hand result at the bottom
                    firstHandResultText.setText(firstHandResult);
                    firstHandResultText.setVisibility(View.VISIBLE);
                }
            } else {
                // Regular hand - show all cards
                for (int card : playerHand) {
                    ImageView cardImageView = new ImageView(this);
                    cardImageView.setImageResource(getCardImageResource(card));
                    LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                            cardWidth,
                            cardHeight
                    );
                    layoutParams.setMargins(8, 0, 8, 0);
                    cardImageView.setLayoutParams(layoutParams);
                    playerCardsLayout.addView(cardImageView);
                }
            }

            // Update dealer's cards
            if (dealerHand != null) {
                for (int i = 0; i < dealerHand.size(); i++) {
                        ImageView cardImageView = new ImageView(this);
                        if (i == 1 && (isPlayerStanding || !isRoundActive)) {
                            cardImageView.setImageResource(getCardImageResource(dealerHand.get(i)));
                        } else if (i == 1) {
                            cardImageView.setImageResource(R.drawable.card_back);
                        } else {
                            cardImageView.setImageResource(getCardImageResource(dealerHand.get(i)));
                        }
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                cardWidth,
                                cardHeight
                        );
                        layoutParams.setMargins(8, 0, 8, 0);
                        cardImageView.setLayoutParams(layoutParams);
                        dealerCardsLayout.addView(cardImageView);
                }
            }
        } catch (Exception e) {
            Log.e("Blackjack", "Error updating card images: " + e.getMessage());
        }
    }

    public void updateCardSizes(List<ImageView> playerCards) {
        if (playerCards == null || playerCards.isEmpty()) return;

        try {
            Context context = playerCards.get(0).getContext();
            if (context == null) return;

            // Define card dimensions with fallback values
            int bigWidth = 120;
            int bigHeight = 160;
            int smallWidth = 70;
            int smallHeight = 90;
            int smallerWidth = 50;
            int smallerHeight = 75;

            try {
                bigWidth = (int) context.getResources().getDimension(R.dimen.card_width_big);
                bigHeight = (int) context.getResources().getDimension(R.dimen.card_height_big);
                smallWidth = (int) context.getResources().getDimension(R.dimen.card_width_small);
                smallHeight = (int) context.getResources().getDimension(R.dimen.card_height_small);
                smallerWidth = (int) context.getResources().getDimension(R.dimen.card_width_smaller);
                smallerHeight = (int) context.getResources().getDimension(R.dimen.card_height_smaller);
            } catch (Exception e) {
                Log.e("Blackjack", "Error getting card dimensions: " + e.getMessage());
            }

            // Resize cards based on their position in the list
            for (int i = 0; i < playerCards.size(); i++) {
                ImageView card = playerCards.get(i);
                if (card == null) continue;

                ViewGroup.LayoutParams params = card.getLayoutParams();
                if (params == null) {
                    params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    );
                }

                if (i < 3) {
                    // First three cards stay big
                    params.width = bigWidth;
                    params.height = bigHeight;
                } else if (playerCards.size() > 5) {
                    // If more than 5 cards, use even smaller size
                    params.width = smallerWidth;
                    params.height = smallerHeight;
                } else {
                    // Cards after the third one become small
                    params.width = smallWidth;
                    params.height = smallHeight;
                }

                card.setLayoutParams(params);
                card.requestLayout();  // Force layout update
            }
        } catch (Exception e) {
            Log.e("Blackjack", "Error updating card sizes: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private int getCardImageResource(int cardValue) {
        switch (cardValue) {
            case 1: return R.drawable.number_1;
            case 2: return R.drawable.number_2;
            case 3: return R.drawable.number_3;
            case 4: return R.drawable.number_4;
            case 5: return R.drawable.number_5;
            case 6: return R.drawable.number_6;
            case 7: return R.drawable.number_7;
            case 8: return R.drawable.number_8;
            case 9: return R.drawable.number_9;
            case 10: return R.drawable.number_10;
            case 11: return R.drawable.number_11;
            case 12: return R.drawable.number_12;
            case 13: return R.drawable.number_13;
            default: return R.drawable.card_back;
        }
    }

    private void updateBackground() {
        if (isRoundActive) {
            // During active round, always use initial background and rank
            llMain.setBackgroundResource(initialBackgroundDrawable);
            ImageView rankImageView = findViewById(R.id.rankImageView);
            if (rankImageView != null) {
                rankImageView.setImageResource(initialRankDrawable);
            }
            return;
        }

        // Only update background and rank when round is not active
        int drawable;
        int rankDrawable;

        if (coins > 50000) {
            drawable = R.drawable.dimond;
            rankDrawable = R.drawable.dimond_r;
        }
        else if (coins > 30000) {
            drawable = R.drawable.roby1;
            rankDrawable = R.drawable.roby_r;
        }
        else if (coins > 20000) {
            drawable = R.drawable.gold;
            rankDrawable = R.drawable.gold_r;
        }
        else if (coins > 10000) {
            drawable = R.drawable.silver;
            rankDrawable = R.drawable.silver_r;
        }
        else if (coins > 5000) {
            drawable = R.drawable.bronze2;
            rankDrawable = R.drawable.bronze_r;
        }
        else {
            drawable = R.drawable.lobby;
            rankDrawable = R.drawable.lobby_r;
        }

        // Update display
        llMain.setBackgroundResource(drawable);
        
        // Update rank image
        ImageView rankImageView = findViewById(R.id.rankImageView);
        if (rankImageView != null) {
            rankImageView.setImageResource(rankDrawable);
        }
    }

    private int getCurrentBackgroundDrawable() {
        if (coins > 50000) {
            return R.drawable.dimond;
        }
        else if (coins > 30000) {
            return R.drawable.roby1;
        }
        else if (coins > 20000) {
            return R.drawable.gold;
        }
        else if (coins > 10000) {
            return R.drawable.silver;
        }
        else if (coins > 5000) {
            return R.drawable.bronze2;
        }
        else {
            return R.drawable.lobby;
        }
    }

    private int getCurrentRankDrawable() {
        if (coins > 50000) {
            return R.drawable.dimond_r;
        }
        else if (coins > 30000) {
            return R.drawable.roby_r;
        }
        else if (coins > 20000) {
            return R.drawable.gold_r;
        }
        else if (coins > 10000) {
            return R.drawable.silver_r;
        }
        else if (coins > 5000) {
            return R.drawable.bronze_r;
        }
        else {
            return R.drawable.lobby_r;
        }
    }

    private void updateCoins(int newCoins) {
        Log.d("Blackjack", "Updating coins from " + coins + " to " + newCoins);
        coins = newCoins;
        coinCountTextView.setText("Coins: " + coins);
        
        // Save updated coins to SharedPreferences
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("coins_" + currentUserEmail, coins);
            boolean success = editor.commit(); // Use commit() instead of apply() to ensure immediate write
            Log.d("Blackjack", "Saved to SharedPreferences: " + success);
        }

        // Update database
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("coins", coins);
        int rowsAffected = db.update("users", values, "email = ?", new String[]{currentUserEmail});
        Log.d("Blackjack", "Database update affected " + rowsAffected + " rows");
        db.close();

        // Only update background and rank when round is not active
        if (!isRoundActive) {
            updateBackground();
        }
    }

    private int getRankDrawable() {
        if (coins > 50000) {
            return R.drawable.dimond_r;
        }
        else if (coins > 30000) {
            return R.drawable.roby_r;
        }
        else if (coins > 20000) {
            return R.drawable.gold_r;
        }
        else if (coins > 10000) {
            return R.drawable.silver_r;
        }
        else if (coins > 5000) {
            return R.drawable.bronze_r;
        }
        else {
            return R.drawable.lobby_r; // Default rank
        }
    }

    private void handleGameResult(boolean playerWon, int betAmount) {
        if (playerWon) {
            updateCoins(coins + betAmount);
            resultTextView.setText("You won " + betAmount + " coins!");
        } else {
            updateCoins(coins - betAmount);
            resultTextView.setText("You lost " + betAmount + " coins!");
        }
        
        // Return to MainActivity with updated coins
        Intent returnIntent = new Intent();
        returnIntent.putExtra("coins", coins);
        setResult(RESULT_OK, returnIntent);
    }

    private void handleBlackjack() {
        // Check if dealer also has blackjack
        int dealerScore = calculateScore(dealerHand);
        
        if (dealerScore == 21) {
            // Both have blackjack - push
            coins += betAmount; // Return original bet
            updateCoins(coins);
            resultTextView.setText("Push! Both have Blackjack! You get your " + betAmount + " coins back!");
        } else {
            // Player wins with blackjack - pay 3:2
            int winnings = (int) (betAmount * 2.5); // 3:2 payout
            coins += winnings;
            updateCoins(coins);
            resultTextView.setText("Blackjack! You win " + (winnings - betAmount) + " coins!");
        }
        
        // End the round
        isRoundActive = false;
        updateButtonStates();
        
        // Return to MainActivity with updated coins
        Intent returnIntent = new Intent();
        returnIntent.putExtra("coins", coins);
        setResult(RESULT_OK, returnIntent);
    }

    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Instructions");

        // Create a ScrollView to contain the instructions
        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 30);

        // Blackjack Instructions
        TextView blackjackTitle = new TextView(this);
        blackjackTitle.setText("Blackjack Rules:");
        blackjackTitle.setTextSize(18);
        blackjackTitle.setTypeface(null, Typeface.BOLD);
        blackjackTitle.setTextColor(Color.BLACK);
        layout.addView(blackjackTitle);

        String[] blackjackRules = {
            "1. The goal is to get a hand value closer to 21 than the dealer without going over.",
            "2. Number cards are worth their face value.",
            "3. Face cards (Jack, Queen, King) are worth 10.",
            "4. Aces can be worth 1 or 11.",
            "5. 'Hit' to draw another card.",
            "6. 'Stand' to keep your current hand.",
            "7. 'Double' to double your bet and draw one more card.",
            "8. If you go over 21, you bust and lose.",
            "9. If the dealer busts, you win.",
            "10. If neither busts, the higher hand wins."
        };

        for (String rule : blackjackRules) {
            TextView ruleText = new TextView(this);
            ruleText.setText(rule);
            ruleText.setTextSize(16);
            ruleText.setTextColor(Color.BLACK);
            ruleText.setPadding(0, 10, 0, 10);
            layout.addView(ruleText);
        }

        // Add space between sections
        TextView space = new TextView(this);
        space.setText("\n");
        layout.addView(space);

        // Rank Instructions
        TextView rankTitle = new TextView(this);
        rankTitle.setText("Rank System:");
        rankTitle.setTextSize(18);
        rankTitle.setTypeface(null, Typeface.BOLD);
        rankTitle.setTextColor(Color.BLACK);
        layout.addView(rankTitle);

        String[] rankRules = {
            "1. Diamond Rank: Over 50,000 coins",
            "2. Ruby Rank: Over 30,000 coins",
            "3. Gold Rank: Over 20,000 coins",
            "4. Silver Rank: Over 10,000 coins",
            "5. Bronze Rank: Over 5,000 coins",
            "6. Lobby Rank: 5,000 coins or less"
        };

        for (String rule : rankRules) {
            TextView ruleText = new TextView(this);
            ruleText.setText(rule);
            ruleText.setTextSize(16);
            ruleText.setTextColor(Color.BLACK);
            ruleText.setPadding(0, 10, 0, 10);
            layout.addView(ruleText);
        }

        scrollView.addView(layout);
        builder.setView(scrollView);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    private void initializeGame() {
        // Initialize the deck
        initializeDeck();
        
        // Clear hands
        playerHand.clear();
        dealerHand.clear();
        splitHand.clear();
        
        // Reset game state
        isRoundActive = false;
        hasDoubled = false;
        hasSplit = false;
        playingFirstHand = false;
        playerHasMoved = false;
        hitCount = 0;
        
        // Reset scores
        playerScore = 0;
        dealerScore = 0;
        
        // Clear card layouts
        if (playerCardsLayout != null) {
            playerCardsLayout.removeAllViews();
        }
        if (dealerCardsLayout != null) {
            dealerCardsLayout.removeAllViews();
        }
        
        // Update UI
        updateScores();
        updateButtonStates();
        
        // Store initial background and rank
        initialBackgroundDrawable = getCurrentBackgroundDrawable();
        initialRankDrawable = getCurrentRankDrawable();
    }

    private void updateUI() {
        // Update coin count
        coinCountTextView.setText("Coins: " + coins);
        
        // Update scores
        updateScores();
        
        // Update card images
        updateCardImages();
        
        // Update button states
        updateButtonStates();
        
        // Update background and rank
        updateBackground();
        
        // Clear result text at the start of a new game
        resultTextView.setText("");
        
        // Clear bet input
        betInput.setText("");
    }

    private String evaluateHandResult(List<Integer> hand) {
        int handValue = calculateScore(hand);
        if (handValue > 21) {
            return "Bust";
        } else if (handValue == 21 && hand.size() == 2) {
            return "Blackjack";
        } else {
            return "Score: " + handValue;
        }
    }

}
