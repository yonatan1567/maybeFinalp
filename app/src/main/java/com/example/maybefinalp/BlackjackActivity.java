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
    private Button hitButton, standButton, dealButton, doubleButton, splitButton;
    private Button returnButton;
    private boolean isPlaying = true;
    private boolean isPlayerStanding = false;
    private boolean isRoundActive = false;
    // ImageView for displaying cards
    MediaPlayer backgroundMusic;
    private SharedPreferences sharedPreferences;
    private int initialBackgroundDrawable;
    private int initialRankDrawable;

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

            if (coinCountTextView == null || playerScoreTextView == null || 
                dealerScoreTextView == null || resultTextView == null || betInput == null) {
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
            Button btnToggleMusic = findViewById(R.id.btnToggleMusic);

            if (dealButton == null || hitButton == null || standButton == null || 
                doubleButton == null || splitButton == null || returnButton == null) {
                throw new IllegalStateException("Required buttons not found");
            }

            // Set up button click listeners
            dealButton.setOnClickListener(v -> startNewRound());
            hitButton.setOnClickListener(v -> playerHit());
            standButton.setOnClickListener(v -> playerStand());
            doubleButton.setOnClickListener(v -> playerDouble());
            splitButton.setOnClickListener(v -> playerSplit());
            returnButton.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("coins", coins);
                setResult(RESULT_OK, resultIntent);
                finish();
            });

            // Initialize music player
            try {
                backgroundMusic = MediaPlayer.create(this, R.raw.background_music);
                if (backgroundMusic != null) {
                    backgroundMusic.setLooping(true);
                    // Get saved music state
                    isPlaying = sharedPreferences.getBoolean("music_playing", true);
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
                        // Save music state
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean("music_playing", isPlaying);
                        editor.apply();
                    }
                });
            }

            // Update initial UI state
            coinCountTextView.setText("Coins: " + coins);
            updateButtonStates();
            updateBackground();

        } catch (Exception e) {
            Log.e("Blackjack", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing game. Please try again.", Toast.LENGTH_SHORT).show();
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
        playingFirstHand = false;
        initializeDeck();
        Log.d("Blackjack", "Starting a New Round...");

        String betText = betInput.getText().toString();
        if (betText.isEmpty() || (betAmount = Integer.parseInt(betText)) <= 0 || betAmount > coins) {
            resultTextView.setText("Invalid Bet Amount");
            return;
        }

        // Deduct the bet amount at the start of the round
        coins -= betAmount;
        updateCoins(coins);

        // Store initial background and rank before any changes
        initialBackgroundDrawable = getCurrentBackgroundDrawable();
        initialRankDrawable = getCurrentRankDrawable();

        // Apply initial background and rank immediately
        llMain.setBackgroundResource(initialBackgroundDrawable);
        ImageView rankImageView = findViewById(R.id.rankImageView);
        if (rankImageView != null) {
            rankImageView.setImageResource(initialRankDrawable);
        }

        // Store pre-bet coins amount
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("pre_bet_coins_" + currentUserEmail, coins);
            editor.apply();
        }

        // Clear previous hands
        playerHand.clear();
        splitHand.clear();
        dealerHand.clear();

        // Clear dynamically added card views from previous rounds
        ((LinearLayout) findViewById(R.id.playerCardsLayout)).removeAllViews();
        ((LinearLayout) findViewById(R.id.dealerCardsLayout)).removeAllViews();

        Log.d("Blackjack", "Cleared previous hands and UI cards.");

        // Draw initial cards
        playerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        dealerHand.add(drawCard());

        updateCardImages(); // Ensure UI updates with new cards

        Log.d("Blackjack", "Player Hand after draw: " + playerHand);
        Log.d("Blackjack", "Dealer Hand after draw: " + dealerHand);

        isPlayerStanding = false;
        isRoundActive = true;
        hasDoubled = false;
        hasSplit = false;
        playerHasMoved = false;

        updateScores();
        resultTextView.setText("");

        // Update buttons
        updateButtonStates();

        if (calculateScore(playerHand) == 21) {
            resultTextView.setText("Blackjack! You Win!");
            coins += betAmount * 2; // Add winnings
            updateCoins(coins);
            endRound();
        }
    }

    private void playerHit() {
        if (!isRoundActive) return;

        int newCard = drawCard();  // Draw a new card
        playerHand.add(newCard);    // Add it to the player hand

        // Increment the hit count
        hitCount++;

        // Create a new ImageView for the new card
        ImageView newCardImageView = new ImageView(this);
        newCardImageView.setImageResource(getCardImageResource(newCard));  // Set the image based on the card value

        // Add the new card to the layout
        LinearLayout playerCardsLayout = findViewById(R.id.playerCardsLayout);
        playerCardsLayout.addView(newCardImageView);

        // Create a list of ImageViews from the playerCardsLayout (all card ImageViews)
        List<ImageView> playerCardViews = new ArrayList<>();
        for (int i = 0; i < playerCardsLayout.getChildCount(); i++) {
            playerCardViews.add((ImageView) playerCardsLayout.getChildAt(i));
        }

        // Only reduce the card size if hit count is greater than 2 (after second hit)
        if (hitCount > 2) {
            updateCardSizes(playerCardViews);  // Adjust the size of all cards
        }

        playerHasMoved = true;
        updateScores();  // Update the score

        int playerScore = calculateScore(playerHand);

        if (playerScore > 21) {
            resultTextView.setText("Bust!");
            endRound();
            return;  // Stop further execution
        }

        // **NEW CODE: Automatically Stand on 21**
        if (playerScore == 21) {
            resultTextView.setText("21! Standing automatically...");
            playerStand();  // Auto-stand when reaching 21
            return;  // Stop further execution
        }

        updateButtonStates();  // Update button states for the next action
    }

    private void playerStand() {
        if (!isRoundActive) return;

        // If a split has occurred and the player is still on their first hand,
        // then finishing this hand should switch play to the second hand.
        if (hasSplit && playingFirstHand) {
            playingFirstHand = false; // now move to the second hand
            // Move the split hand into the main hand so the hit/stand methods work on it.
            playerHand.clear();
            playerHand.addAll(splitHand);
            splitHand.clear();

            resultTextView.setText("First hand finished. Now playing your second hand.");
            updateCardImagesForPlayerHand();
            updateScores();
            // Return without running dealer's turn yet.
            return;
        }

        // Otherwise, if no split or if already playing the second hand, end the round.
        isPlayerStanding = true;
        playDealerTurn();  // Let the dealer complete their hand
        updateCardImages();
        updateScores();
        evaluateWinner();
        endRound();
        updateButtonStates();
    }
    private void playerDouble() {
        if (!isRoundActive || hasDoubled || coins < betAmount) return;

        // Deduct the additional bet amount
        coins -= betAmount;
        updateCoins(coins);
        
        // Double the bet amount
        betAmount *= 2;

        int newCard = drawCard();
        playerHand.add(newCard);
        updateCardImagesForPlayerHand();

        updateScores();
        hasDoubled = true;

        if (calculateScore(playerHand) > 21) {
            resultTextView.setText("Bust! You Lose!");
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
        if (!isRoundActive || playerHand.size() != 2) return;

        int card1 = playerHand.get(0);
        int card2 = playerHand.get(1);

        // Allow splitting if both cards have the same value (including 10-value cards)
        if (getCardValue(card1) == getCardValue(card2)) {
            // Check if player has enough coins first
            if (coins >= betAmount) {
                // Only proceed with split if player has enough coins
                hasSplit = true;
                // Save second card for later play
                splitHand.add(playerHand.remove(1));
                
                resultTextView.setText("Hand split! Play your first hand.");
                // Set flag so we know we're playing the first hand now.
                playingFirstHand = true;
            } else {
                resultTextView.setText("Not enough coins to split.");
                return;
            }
        } else {
            resultTextView.setText("Cannot split these cards.");
            return;
        }
        updateScores();
        updateButtonStates();
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

    private void playDealerTurn() {
        while (calculateScore(dealerHand) < 17 || (calculateScore(dealerHand) == 17 && hasSoft17(dealerHand))) {
            int newCard = drawCard();
            dealerHand.add(newCard);
            updateCardImagesForDealerHand(newCard);
        }

        // Reveal the dealer's second card
        updateCardImages();
    }
    private void evaluateWinner() {
        int playerScore = calculateScore(playerHand);
        int splitScore = calculateScore(splitHand);
        int dealerScore = calculateScore(dealerHand);
        int originalBet = hasDoubled ? betAmount / 2 : betAmount; // Get original bet amount

        // Handle main hand
        if (playerScore > 21) {
            // Player busts - bet already deducted at start
            resultTextView.setText("Bust! You lose " + betAmount + " coins!");
        } else if (dealerScore > 21) {
            // Dealer busts, player wins
            if (hasDoubled) {
                coins += originalBet * 3; // Add original bet + 2x winnings
                updateCoins(coins);
                resultTextView.setText("Dealer busts! You win " + (originalBet * 2) + " coins!");
            } else {
                coins += betAmount * 2; // Add bet + winnings
                updateCoins(coins);
                resultTextView.setText("Dealer busts! You win " + betAmount + " coins!");
            }
        } else if (playerScore > dealerScore) {
            // Player wins
            if (hasDoubled) {
                coins += originalBet * 3; // Add original bet + 2x winnings
                updateCoins(coins);
                resultTextView.setText("You win " + (originalBet * 2) + " coins!");
            } else {
                coins += betAmount * 2; // Add bet + winnings
                updateCoins(coins);
                resultTextView.setText("You win " + betAmount + " coins!");
            }
        } else if (playerScore == dealerScore) {
            // Push - return original bet
            coins += betAmount;
            updateCoins(coins);
            resultTextView.setText("Push! You get your " + betAmount + " coins back!");
        } else {
            // Player loses - bet already deducted at start
            resultTextView.setText("You lose " + betAmount + " coins!");
        }

        // Handle split hand if it exists
        if (hasSplit) {
            if (splitScore > 21) {
                // Split hand busts - bet already deducted at start
                resultTextView.append("\nSplit hand busts! Lose " + betAmount + " coins!");
            } else if (dealerScore > 21) {
                // Dealer busts, split hand wins - add bet + winnings
                coins += betAmount * 2;
                updateCoins(coins);
                resultTextView.append("\nSplit hand wins " + betAmount + " coins!");
            } else if (splitScore > dealerScore) {
                // Split hand wins - add bet + winnings
                coins += betAmount * 2;
                updateCoins(coins);
                resultTextView.append("\nSplit hand wins " + betAmount + " coins!");
            } else if (splitScore == dealerScore) {
                // Split hand push - return original bet
                coins += betAmount;
                updateCoins(coins);
                resultTextView.append("\nSplit hand pushes! You get your " + betAmount + " coins back!");
            } else {
                // Split hand loses - bet already deducted at start
                resultTextView.append("\nSplit hand loses " + betAmount + " coins!");
            }
        }
        
        // Return to MainActivity with updated coins
        Intent returnIntent = new Intent();
        returnIntent.putExtra("coins", coins);
        setResult(RESULT_OK, returnIntent);
    }
    private void endRound() {
        isRoundActive = false;
        updateButtonStates();
        
        // Clear pre-bet coins amount
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove("pre_bet_coins_" + currentUserEmail);
            editor.apply();
        }
        
        // Update background and rank after round ends
        updateBackground();
        
        // Return to MainActivity with current coins
        Intent returnIntent = new Intent();
        returnIntent.putExtra("coins", coins);
        setResult(RESULT_OK, returnIntent);
    }
    private void updateButtonStates() {
        hitButton.setEnabled(isRoundActive && !hasDoubled);
        standButton.setEnabled(isRoundActive);
        dealButton.setEnabled(!isRoundActive);
        splitButton.setEnabled(
                isRoundActive &&
                        playerHand.size() == 2 &&
                        getCardValue(playerHand.get(0)) == getCardValue(playerHand.get(1)) &&
                        !hasSplit
        );
        
        // Update double button state with new conditions
        doubleButton.setEnabled(
                isRoundActive &&                    // Round must be active
                !hasDoubled &&                      // Haven't doubled yet
                playerHand.size() == 2 &&           // Only first two cards
                coins >= betAmount &&               // Have exactly enough coins to double
                !playerHasMoved                     // Haven't hit yet
        );
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
        playerScoreTextView.setText("Player Score: " + calculateScore(playerHand));
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
            
            // Clear layouts safely
            try {
                playerCardsLayout.removeAllViews();
                dealerCardsLayout.removeAllViews();
            } catch (Exception e) {
                Log.e("Blackjack", "Error clearing card layouts: " + e.getMessage());
            }

            // Get card dimensions safely
            int cardWidth = 70; // Default fallback values
            int cardHeight = 90;
            try {
                cardWidth = getResources().getDimensionPixelSize(R.dimen.card_width_small);
                cardHeight = getResources().getDimensionPixelSize(R.dimen.card_height_small);
            } catch (Exception e) {
                Log.e("Blackjack", "Error getting card dimensions: " + e.getMessage());
            }

            // Update player's cards
            if (playerHand != null) {
                for (int card : playerHand) {
                    try {
                        ImageView cardImageView = new ImageView(this);
                        cardImageView.setImageResource(getCardImageResource(card));
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                                cardWidth,
                                cardHeight
                        );
                        layoutParams.setMargins(8, 0, 8, 0);
                        cardImageView.setLayoutParams(layoutParams);
                        playerCardsLayout.addView(cardImageView);
                    } catch (Exception e) {
                        Log.e("Blackjack", "Error adding player card: " + e.getMessage());
                    }
                }
            }

            // If there's a split hand, add a divider and then the split cards
            if (hasSplit && splitHand != null && !splitHand.isEmpty()) {
                try {
                    // Add a divider
                    View divider = new View(this);
                    divider.setBackgroundColor(getResources().getColor(android.R.color.white));
                    LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            2
                    );
                    dividerParams.setMargins(0, 16, 0, 16);
                    divider.setLayoutParams(dividerParams);
                    playerCardsLayout.addView(divider);

                    // Add split hand cards
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
                } catch (Exception e) {
                    Log.e("Blackjack", "Error adding split hand cards: " + e.getMessage());
                }
            }

            // Update dealer's cards
            if (dealerHand != null) {
                for (int i = 0; i < dealerHand.size(); i++) {
                    try {
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
                    } catch (Exception e) {
                        Log.e("Blackjack", "Error adding dealer card: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            Log.e("Blackjack", "Error updating card images: " + e.getMessage());
            e.printStackTrace();
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
        else if (coins > 1050) {
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
        else if (coins > 1050) {
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
        else if (coins > 1050) {
            return R.drawable.bronze_r;
        }
        else {
            return R.drawable.lobby_r;
        }
    }

    private void updateCoins(int newCoins) {
        coins = newCoins;
        coinCountTextView.setText("Coins: " + coins);
        
        // Save updated coins to SharedPreferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("coins_" + sharedPreferences.getString("currentUserEmail", null), coins);
        editor.apply();

        // Update database
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("coins", coins);
        db.update("users", values, "email = ?", new String[]{sharedPreferences.getString("currentUserEmail", null)});
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
        else if (coins > 1050) {
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

}
