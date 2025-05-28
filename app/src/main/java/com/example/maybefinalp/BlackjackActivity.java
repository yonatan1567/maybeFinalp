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
import android.speech.tts.TextToSpeech;
import android.util.TypedValue;

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
    private long roundStartTime;
    private boolean isFinishing = false;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_blackjack);

            // Initialize SharedPreferences
            sharedPreferences = getSharedPreferences("PlayerData", MODE_PRIVATE);

            // Check if user is logged in and initialize currentUserEmail
            currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
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
            returnButton.setOnClickListener(v -> handleReturn());
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

            // Initialize TextToSpeech
            tts = new TextToSpeech(this, status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(new java.util.Locale("he"));
                }
            });

            Button speakButton = findViewById(R.id.speakButton);
            if (speakButton != null && resultTextView != null) {
                speakButton.setOnClickListener(v -> {
                    String text = resultTextView.getText().toString();
                    if (!text.isEmpty()) {
                        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
                    }
                });
            }

        } catch (Exception e) {
            Log.e("BlackjackActivity", "Error in onCreate: " + e.getMessage());
            Toast.makeText(this, "Error initializing game: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            // Save current state before destroying
            if (currentUserEmail != null) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("coins_" + currentUserEmail, coins);
                editor.apply();
            }
            
            if (backgroundMusic != null) {
                backgroundMusic.release();
                backgroundMusic = null;
            }

            if (tts != null) {
                tts.stop();
                tts.shutdown();
            }
        } catch (Exception e) {
            Log.e("Blackjack", "Error in onDestroy: " + e.getMessage());
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Handle back button the same way as return button
        handleReturn();
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

    private void updateCardImages() {
        updateCardImages(false, -1);
    }

    private void updateCardImages(boolean flipOnlyLastPlayerCard) {
        updateCardImages(flipOnlyLastPlayerCard, -1);
    }

    private void updateCardImages(boolean flipOnlyLastPlayerCard, int dealerFlipIndex) {
        try {
            if (playerCardsLayout != null) playerCardsLayout.removeAllViews();
            if (dealerCardsLayout != null) dealerCardsLayout.removeAllViews();

            int cardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
            int cardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());

            // Player cards
            for (int i = 0; i < playerHand.size(); i++) {
                CardFlipSurfaceView cardView = new CardFlipSurfaceView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, cardHeight);
                params.setMargins(8, 0, 8, 0);
                cardView.setLayoutParams(params);
                if (flipOnlyLastPlayerCard && i == playerHand.size() - 1) {
                    cardView.setCardImages(R.drawable.card_back, getCardImageResource(playerHand.get(i).intValue()));
                    cardView.flipCard();
                } else {
                    cardView.setFaceUp(getCardImageResource(playerHand.get(i).intValue()));
                }
                playerCardsLayout.addView(cardView);
            }
            // Dealer cards
            for (int i = 0; i < dealerHand.size(); i++) {
                CardFlipSurfaceView cardView = new CardFlipSurfaceView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, cardHeight);
                params.setMargins(8, 0, 8, 0);
                cardView.setLayoutParams(params);
                if (i == 1 && isRoundActive && !isPlayerStanding) {
                    cardView.setCardImages(R.drawable.card_back, R.drawable.card_back);
                    if (!flipOnlyLastPlayerCard && dealerFlipIndex == -1) cardView.flipCard();
                } else {
                    if (dealerFlipIndex >= 0) {
                        // Only the dealer card at dealerFlipIndex gets flip, others show face up
                        if (i == dealerFlipIndex) {
                            cardView.setCardImages(R.drawable.card_back, getCardImageResource(dealerHand.get(i).intValue()));
                            cardView.flipCard();
                        } else {
                            cardView.setFaceUp(getCardImageResource(dealerHand.get(i).intValue()));
                        }
                    } else if (flipOnlyLastPlayerCard) {
                        cardView.setFaceUp(getCardImageResource(dealerHand.get(i).intValue()));
                    } else {
                        cardView.setCardImages(R.drawable.card_back, getCardImageResource(dealerHand.get(i).intValue()));
                        cardView.flipCard();
                    }
                }
                dealerCardsLayout.addView(cardView);
            }
        } catch (Exception e) {
            Log.e("Blackjack", "Error updating card images: " + e.getMessage());
        }
    }

    private void animateInitialPlayerCards() {
        if (playerCardsLayout != null) playerCardsLayout.removeAllViews();
        int cardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        int cardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
        java.util.List<CardFlipSurfaceView> cardViews = new java.util.ArrayList<>();
        for (int i = 0; i < playerHand.size(); i++) {
            CardFlipSurfaceView cardView = new CardFlipSurfaceView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, cardHeight);
            params.setMargins(8, 0, 8, 0);
            cardView.setLayoutParams(params);
            cardView.setCardImages(R.drawable.card_back, getCardImageResource(playerHand.get(i).intValue()));
            playerCardsLayout.addView(cardView);
            cardViews.add(cardView);
        }
        for (int i = 0; i < cardViews.size(); i++) {
            final int idx = i;
            playerCardsLayout.postDelayed(() -> cardViews.get(idx).flipCard(), i * 300);
        }
    }

    private void animateInitialDealerCards() {
        if (dealerCardsLayout != null) dealerCardsLayout.removeAllViews();
        int cardWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60, getResources().getDisplayMetrics());
        int cardHeight = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 90, getResources().getDisplayMetrics());
        for (int i = 0; i < dealerHand.size(); i++) {
            CardFlipSurfaceView cardView = new CardFlipSurfaceView(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(cardWidth, cardHeight);
            params.setMargins(8, 0, 8, 0);
            cardView.setLayoutParams(params);
            if (i == 0) {
                cardView.setCardImages(R.drawable.card_back, getCardImageResource(dealerHand.get(i).intValue()));
                cardView.flipCard();
            } else {
                cardView.setCardImages(R.drawable.card_back, R.drawable.card_back);
            }
            dealerCardsLayout.addView(cardView);
        }
    }

    private void startNewRound() {
        try {
            // Clear previous round data
            playerHand.clear();
            dealerHand.clear();
            splitHand.clear();
            playerScore = 0;
            dealerScore = 0;
            hasDoubled = false;
            playerHasMoved = false;
            isPlayerStanding = false;
            firstHandResult = "";
            if (firstHandResultText != null) {
                firstHandResultText.setVisibility(View.GONE);
            }
            
            // Get bet amount with validation
            String betText = betInput.getText().toString().trim();
            if (betText.isEmpty()) {
                Toast.makeText(this, "Please enter a bet amount", Toast.LENGTH_SHORT).show();
                return;
            }
            
            try {
                betAmount = Integer.parseInt(betText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Please enter a valid bet amount", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (betAmount <= 0) {
                Toast.makeText(this, "Bet amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (betAmount > coins) {
                Toast.makeText(this, "Not enough coins", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Start new round
            isRoundActive = true;
            isFinishing = false;
            
            // Initialize deck if empty
            if (deck.isEmpty()) {
                initializeDeck();
            }
            
            // Clear previous UI state
            if (resultTextView != null) {
                resultTextView.setText("");
            }
            if (playerCardsLayout != null) {
                playerCardsLayout.removeAllViews();
            }
            if (dealerCardsLayout != null) {
                dealerCardsLayout.removeAllViews();
            }
            
            // Deal initial cards with validation
            try {
                int card1 = drawCard();
                int card2 = drawCard();
                int card3 = drawCard();
                int card4 = drawCard();
                
                if (card1 <= 0 || card2 <= 0 || card3 <= 0 || card4 <= 0) {
                    throw new IllegalStateException("Invalid card drawn");
                }
                
                playerHand.add(card1);
                dealerHand.add(card2);
                playerHand.add(card3);
                dealerHand.add(card4);

                // Set up flip views for first cards
                // if (playerCardFlipView != null) { ... }
                // if (dealerCardFlipView != null) { ... }
            } catch (Exception e) {
                Log.e("Blackjack", "Error dealing cards: " + e.getMessage());
                Toast.makeText(this, "Error dealing cards. Please try again.", Toast.LENGTH_SHORT).show();
                isRoundActive = false;
                updateButtonStates();
                return;
            }
            
            // Update UI
            runOnUiThread(() -> {
                try {
                    animateInitialPlayerCards();
                    animateInitialDealerCards();
                    updateScores();
                    updateButtonStates();
                    
                    // Check for blackjack
                    if (calculateScore(playerHand) == 21) {
                        handleBlackjack();
                    }
                } catch (Exception e) {
                    Log.e("Blackjack", "Error updating UI: " + e.getMessage());
                    Toast.makeText(this, "Error updating game display. Please try again.", Toast.LENGTH_SHORT).show();
                    isRoundActive = false;
                    updateButtonStates();
                }
            });
            
            // Start timing the round
            roundStartTime = System.currentTimeMillis();
            
            // Animate card flip after dealing
            // if (playerCardFlipView != null) { ... }
            // if (dealerCardFlipView != null) { ... }
        } catch (Exception e) {
            Log.e("Blackjack", "Error in startNewRound: " + e.getMessage());
            Toast.makeText(this, "Error starting new round. Please try again.", Toast.LENGTH_SHORT).show();
            isRoundActive = false;
            updateButtonStates();
        }
    }

    private void playerHit() {
        if (!isRoundActive) {
            Log.d("Blackjack", "Hit called but round is not active");
            return;
        }

        try {
            // Draw a new card
            int newCard = drawCard();
            if (newCard <= 0) {
                Log.e("Blackjack", "Invalid card drawn: " + newCard);
                return;
            }

            if (hasSplit) {
                if (isPlayingFirstHand) {
                    playerHand.add(newCard);
                    // Update UI immediately after hit
                    runOnUiThread(() -> {
                        try {
                            updateCardImages(true);
                            updateScores();
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error updating UI for first hand: " + e.getMessage());
                        }
                    });
                    
                    // Check for bust or 21
                    int currentHandValue = calculateScore(playerHand);
                    if (currentHandValue > 21) {
                        firstHandResult = "Bust - Score: " + currentHandValue;
                        firstHandResultText.setText("First hand: " + firstHandResult);
                        
                        // Switch to second hand after a delay
                        new android.os.Handler().postDelayed(() -> {
                            try {
                                isPlayingFirstHand = false;
                                runOnUiThread(() -> {
                                    try {
                                        resultTextView.setText("First hand busted. Playing second hand.");
                                        updateCardImages();
                                        updateScores();
                                    } catch (Exception e) {
                                        Log.e("Blackjack", "Error switching to second hand: " + e.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("Blackjack", "Error in delayed hand switch: " + e.getMessage());
                            }
                        }, 1000);
                    } else if (currentHandValue == 21) {
                        firstHandResult = "21";
                        firstHandResultText.setText("First hand: " + firstHandResult);
                        
                        // Switch to second hand after a delay
                        new android.os.Handler().postDelayed(() -> {
                            try {
                                isPlayingFirstHand = false;
                                runOnUiThread(() -> {
                                    try {
                                        resultTextView.setText("First hand: 21. Playing second hand.");
                                        updateCardImages();
                                        updateScores();
                                    } catch (Exception e) {
                                        Log.e("Blackjack", "Error switching to second hand: " + e.getMessage());
                                    }
                                });
                            } catch (Exception e) {
                                Log.e("Blackjack", "Error in delayed hand switch: " + e.getMessage());
                            }
                        }, 1000);
                    }
                } else {
                    splitHand.add(newCard);
                    // Update UI immediately after hit
                    runOnUiThread(() -> {
                        try {
                            updateCardImages(true);
                            updateScores();
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error updating UI for second hand: " + e.getMessage());
                        }
                    });
                    
                    // Check for bust or 21
                    int currentHandValue = calculateScore(splitHand);
                    if (currentHandValue > 21) {
                        String secondHandResult = "Bust - Score: " + currentHandValue;
                        resultTextView.setText("First hand: " + firstHandResult + "\nSecond hand: " + secondHandResult);
                    } else if (currentHandValue == 21) {
                        String secondHandResult = "21";
                        resultTextView.setText("First hand: " + firstHandResult + "\nSecond hand: " + secondHandResult);
                    }
                }
            } else {
                playerHand.add(newCard);
                // Update UI immediately after hit
                runOnUiThread(() -> {
                    try {
                        updateCardImages(true);
                        updateScores();
                    } catch (Exception e) {
                        Log.e("Blackjack", "Error updating UI for regular hand: " + e.getMessage());
                    }
                });

                // Check for bust or 21
                int currentHandValue = calculateScore(playerHand);
                if (currentHandValue > 21) {
                    runOnUiThread(() -> {
                        try {
                            // Disable all game buttons immediately
                            isRoundActive = false;
                            updateButtonStates();
                            
                            // Show bust message
                            resultTextView.setText("You lost! Loss: " + betAmount + " coins!");
                            
                            // Update coins
                            int newCoins = Math.max(0, coins - betAmount);
                            updateCoins(newCoins);
                            
                            // Save game history
                            if (currentUserEmail != null) {
                                DatabaseHelper dbHelper = new DatabaseHelper(this);
                                try {
                                    long gameDuration = System.currentTimeMillis() - roundStartTime;
                                    dbHelper.addGameHistory(currentUserEmail, "BLACKJACK", "LOSE", 0, betAmount, gameDuration);
                                    dbHelper.checkAchievements(currentUserEmail);
                                } catch (Exception e) {
                                    Log.e("Blackjack", "Error saving game history: " + e.getMessage());
                                }
                            }
                            
                            // Add a delay before ending the round
                            new android.os.Handler().postDelayed(() -> {
                                try {
                                    endRound();
                                } catch (Exception e) {
                                    Log.e("Blackjack", "Error ending round after bust: " + e.getMessage());
                                }
                            }, 2000);
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error handling bust: " + e.getMessage());
                        }
                    });
                } else if (currentHandValue == 21) {
                    runOnUiThread(() -> {
                        try {
                            resultTextView.setText("21! Standing automatically...");
                            playerStand();
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error handling 21: " + e.getMessage());
                        }
                    });
                }
            }

            // Update button states
            runOnUiThread(() -> {
                try {
                    updateButtonStates();
                } catch (Exception e) {
                    Log.e("Blackjack", "Error updating button states: " + e.getMessage());
                }
            });

        } catch (Exception e) {
            Log.e("Blackjack", "Error in playerHit: " + e.getMessage());
            // Try to recover the game state
            try {
                runOnUiThread(() -> {
                    try {
                        updateCardImages();
                        updateScores();
                        updateButtonStates();
                    } catch (Exception ex) {
                        Log.e("Blackjack", "Error recovering game state: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Log.e("Blackjack", "Error in recovery attempt: " + ex.getMessage());
            }
        }
    }

    private void playerStand() {
        if (!isRoundActive) return;

        try {
            // Set player standing flag immediately and update UI
            isPlayerStanding = true;
            updateButtonStates();  // This will make buttons gray immediately
            updateCardImages();    // This will reveal dealer's second card
            updateScores();        // This will update the scores

            if (hasSplit) {
                if (isPlayingFirstHand) {
                    // Evaluate first hand
                    firstHandResult = "Score: " + calculateScore(playerHand);
                    firstHandResultText.setText("First hand: " + firstHandResult);
                    
                    // Switch to second hand using post delayed
                    new android.os.Handler().postDelayed(() -> {
                        try {
                            // Switch to second hand
                            isPlayingFirstHand = false;
                            resultTextView.setText("First hand complete. Playing second hand.");
                            
                            // Update scores and cards to show second hand
                            runOnUiThread(() -> {
                                try {
                                    updateScores();
                                    updateCardImages();
                                    updateButtonStates();
                                } catch (Exception e) {
                                    Log.e("Blackjack", "Error updating UI for second hand: " + e.getMessage());
                                }
                            });
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error switching to second hand: " + e.getMessage());
                        }
                    }, 1000);
                } else {
                    // Evaluate second hand
                    String secondHandResult = "Score: " + calculateScore(splitHand);
                    resultTextView.setText("First hand: " + firstHandResult + "\nSecond hand: " + secondHandResult);
                    
                    // Dealer's turn
                    dealerPlay();
                }
            } else {
                // Regular hand - dealer's turn
                dealerPlay();
            }
        } catch (Exception e) {
            Log.e("Blackjack", "Error in playerStand: " + e.getMessage());
            // Try to recover
            try {
                runOnUiThread(() -> {
                    try {
                        updateScores();
                        updateCardImages();
                        updateButtonStates();
                    } catch (Exception ex) {
                        Log.e("Blackjack", "Error recovering game state: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Log.e("Blackjack", "Error in recovery attempt: " + ex.getMessage());
            }
        }
    }

    private void dealerPlay() {
        if (!isRoundActive) return;

        try {
            // First reveal dealer's second card
            updateCardImages();
            updateScores();
            
            // Add a small delay to make the dealer's play visible
            new android.os.Handler().postDelayed(() -> {
                try {
                    // Dealer must hit on soft 17
                    while (calculateScore(dealerHand) < 17 || (calculateScore(dealerHand) == 17 && hasSoft17(dealerHand))) {
                        int newCard = drawCard();
                        dealerHand.add(newCard);
                        runOnUiThread(() -> {
                            try {
                                updateCardImages(false, dealerHand.size() - 1);
                            } catch (Exception e) {
                                Log.e("Blackjack", "Error updating UI during dealer play: " + e.getMessage());
                            }
                        });
                        
                        // Add a small delay between dealer's hits
                        Thread.sleep(300);
                    }
                    
                    // After the loop, before evaluateWinner():
                    runOnUiThread(() -> {
                        try {
                            updateScores();
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error updating scores after dealer play: " + e.getMessage());
                        }
                    });
                    
                    // Evaluate the winner
                    evaluateWinner();
                    
                    // Add a delay before ending the round
                    new android.os.Handler().postDelayed(() -> {
                        try {
                            endRound();
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error ending round: " + e.getMessage());
                        }
                    }, 1500);
                    
                } catch (Exception e) {
                    Log.e("Blackjack", "Error in dealer play: " + e.getMessage());
                }
            }, 500);
            
        } catch (Exception e) {
            Log.e("Blackjack", "Error in dealerPlay: " + e.getMessage());
            // Try to recover
            try {
                runOnUiThread(() -> {
                    try {
                        updateScores();
                        updateCardImages();
                        updateButtonStates();
                    } catch (Exception ex) {
                        Log.e("Blackjack", "Error recovering game state: " + ex.getMessage());
                    }
                });
            } catch (Exception ex) {
                Log.e("Blackjack", "Error in recovery attempt: " + ex.getMessage());
            }
        }
    }

    private void playerDouble() {
        if (!isRoundActive || hasDoubled || coins < betAmount) return;

        // Double the bet amount
        betAmount *= 2;
        hasDoubled = true;

        int newCard = drawCard();
        playerHand.add(newCard);
        updateCardImages(true);

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
            // Get current bet amount
            int currentBet = Integer.parseInt(betInput.getText().toString());
            
            // Check if player has double the bet amount
            if (coins < currentBet * 2) {
                Toast.makeText(this, "You need " + (currentBet * 2) + " coins to split (double your bet)!", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Deduct the bet amount again for the split
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

    private void handleBlackjack() {
        try {
            Log.d("Blackjack", "=== Starting handleBlackjack ===");
            
            // Set game state
            isRoundActive = false;
            isPlayerStanding = true;
            
            // Reveal dealer's second card
            updateCardImages();
            updateScores();
            
            // Check if dealer also has blackjack
            final boolean dealerHasBlackjack = dealerHand.size() == 2 && calculateScore(dealerHand) == 21;
            final int finalBetAmount = betAmount;
            final int winnings = dealerHasBlackjack ? finalBetAmount : (finalBetAmount + (int)(finalBetAmount * 1.5));
            
            // Create the message
            String message;
            if (dealerHasBlackjack) {
                message = "Push! Both you and the dealer have blackjack!";
            } else {
                message = "Blackjack! You won! Winnings: " + (winnings - finalBetAmount) + " coins!";
            }
            
            // Update coins
            coins += winnings;
            
            // Update UI
            runOnUiThread(() -> {
                try {
                    Log.d("Blackjack", "Showing blackjack message: " + message);
                    
                    // Show message in TextView
                    resultTextView.setText(message);
                    
                    // Update coins display
                    updateCoins(coins);
                    
                    // Update button states
                    updateButtonStates();
                    
                    // Save game history
                    if (currentUserEmail != null) {
                        DatabaseHelper dbHelper = new DatabaseHelper(this);
                        try {
                            long gameDuration = System.currentTimeMillis() - roundStartTime;
                            String gameResult = dealerHasBlackjack ? "DRAW" : "WIN";
                            dbHelper.addGameHistory(currentUserEmail, "BLACKJACK", gameResult, winnings - finalBetAmount, 0, gameDuration);
                            dbHelper.checkAchievements(currentUserEmail);
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error saving game history: " + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    Log.e("Blackjack", "Error updating UI after blackjack: " + e.getMessage());
                }
            });
            
        } catch (Exception e) {
            Log.e("Blackjack", "Error in handleBlackjack: " + e.getMessage());
        }
    }

    private int evaluateWinner() {
        try {
            Log.d("Blackjack", "=== Starting evaluateWinner ===");
            
            int playerScore = calculateScore(playerHand);
            int splitScore = hasSplit ? calculateScore(splitHand) : 0;
            int dealerScore = calculateScore(dealerHand);
            
            Log.d("Blackjack", "Player score: " + playerScore + ", Dealer score: " + dealerScore);
            Log.d("Blackjack", "Has split: " + hasSplit + ", Split score: " + splitScore);

            String message = "";
            int tempCoinsWon = 0;
            int tempCoinsLost = 0;

            // Player busts - always lose
            if (playerScore > 21) {
                tempCoinsLost = betAmount;
                coins = Math.max(0, coins - tempCoinsLost);
                message = "You lost! Loss: " + tempCoinsLost + " coins.";
                Log.d("Blackjack", "Player busted: " + message);
            }
            // Dealer busts - player wins
            else if (dealerScore > 21) {
                tempCoinsWon = betAmount;
                coins += tempCoinsWon;
                message = "You won! The dealer busted! Winnings: " + tempCoinsWon + " coins!";
                Log.d("Blackjack", "Dealer busted: " + message);
            }
            // Compare scores
            else if (playerScore > dealerScore) {
                tempCoinsWon = betAmount;
                coins += tempCoinsWon;
                message = "You won! Winnings: " + tempCoinsWon + " coins!";
                Log.d("Blackjack", "Player wins: " + message);
            } else if (playerScore < dealerScore) {
                tempCoinsLost = betAmount;
                coins = Math.max(0, coins - tempCoinsLost);
                message = "You lost! Loss: " + tempCoinsLost + " coins.";
                Log.d("Blackjack", "Player loses: " + message);
            } else {
                message = "Push! No coins changed.";
                Log.d("Blackjack", "Push: " + message);
            }

            // Handle split hand if it exists
            if (hasSplit) {
                String splitMessage = "";
                if (splitScore > 21) {
                    tempCoinsLost += betAmount;
                    coins = Math.max(0, coins - betAmount);
                    splitMessage = "\nSecond hand busted! You lost " + betAmount + " coins.";
                } else if (splitScore > dealerScore) {
                    tempCoinsWon += betAmount;
                    coins += betAmount;
                    splitMessage = "\nSecond hand won! Winnings: " + betAmount + " coins!";
                } else if (splitScore < dealerScore) {
                    tempCoinsLost += betAmount;
                    coins = Math.max(0, coins - betAmount);
                    splitMessage = "\nSecond hand lost! You lost " + betAmount + " coins.";
                }
                message += splitMessage;
                Log.d("Blackjack", "Split hand result: " + splitMessage);
            }

            final String finalMessage = message;
            final int finalCoinsWon = tempCoinsWon;
            final int finalCoinsLost = tempCoinsLost;

            // Update UI and game state
            runOnUiThread(() -> {
                try {
                    Log.d("Blackjack", "Showing final message: " + finalMessage);
                    
                    // Show message in TextView
                    resultTextView.setText(finalMessage);
                    
                    // Update coins display
                    updateCoins(coins);
                    
                    // Save game history
                    if (currentUserEmail != null) {
                        DatabaseHelper dbHelper = new DatabaseHelper(this);
                        try {
                            long gameDuration = System.currentTimeMillis() - roundStartTime;
                            String gameResult = finalCoinsWon > finalCoinsLost ? "WIN" : (finalCoinsLost > finalCoinsWon ? "LOSE" : "DRAW");
                            dbHelper.addGameHistory(currentUserEmail, "BLACKJACK", gameResult, finalCoinsWon, finalCoinsLost, gameDuration);
                            dbHelper.checkAchievements(currentUserEmail);
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error saving game history: " + e.getMessage());
                        }
                    }
                    
                    // End the round
                    isRoundActive = false;
                    updateButtonStates();
                    updateBackground();
                    
                } catch (Exception e) {
                    Log.e("Blackjack", "Error updating UI after game end: " + e.getMessage());
                }
            });

            Log.d("Blackjack", "=== End of evaluateWinner ===");
            return coins;
            
        } catch (Exception e) {
            Log.e("Blackjack", "Error in evaluateWinner: " + e.getMessage());
            return coins;
        }
    }

    private void endRound() {
        if (!isRoundActive && !isFinishing) return;
        
        try {
            isFinishing = true;
            Log.d("Blackjack", "=== Starting endRound ===");
            
            // Just end the round but don't finish the activity
            isRoundActive = false;
            updateButtonStates();
            
            // Clear result messages
            firstHandResultText.setVisibility(View.GONE);
            firstHandResult = "";
            
            // Reset split state
            hasSplit = false;
            isPlayingFirstHand = true;
            
            // Update background and rank
            updateBackground();
            
        } catch (Exception e) {
            Log.e("Blackjack", "Error in endRound: " + e.getMessage());
        } finally {
            isFinishing = false;
        }
    }

    private void updateButtonStates() {
        boolean canDouble = playerHand.size() == 2 && !hasDoubled;
        boolean canSplit = playerHand.size() == 2 && 
                        getCardValue(playerHand.get(0)) == getCardValue(playerHand.get(1)) &&
                        !hasSplit;

        // Game buttons are only enabled during active round and when player hasn't made a move
        boolean canPlay = isRoundActive && !isPlayerStanding && !hasDoubled;
        
        // Update button states and appearance
        hitButton.setEnabled(canPlay);
        hitButton.setAlpha(canPlay ? 1.0f : 0.5f);
        
        standButton.setEnabled(canPlay);
        standButton.setAlpha(canPlay ? 1.0f : 0.5f);
        
        doubleButton.setEnabled(canPlay && canDouble);
        doubleButton.setAlpha((canPlay && canDouble) ? 1.0f : 0.5f);
        
        splitButton.setEnabled(canPlay && canSplit);
        splitButton.setAlpha((canPlay && canSplit) ? 1.0f : 0.5f);
        
        // Deal button is enabled when round is not active
        dealButton.setEnabled(!isRoundActive);
        dealButton.setAlpha(!isRoundActive ? 1.0f : 0.5f);
        
        // Return button is always enabled
        if (returnButton != null) {
            returnButton.setEnabled(true);
            returnButton.setAlpha(1.0f);
        }
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

    private void updateCoins(int newCoins) {
        Log.d("Blackjack", "Updating coins from " + coins + " to " + newCoins);
        // Ensure coins never go below 0
        coins = Math.max(0, newCoins);
        coinCountTextView.setText("Coins: " + coins);
        
        // Save updated coins to SharedPreferences
        String currentUserEmail = sharedPreferences.getString("currentUserEmail", null);
        if (currentUserEmail != null) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt("coins_" + currentUserEmail, coins);
            boolean success = editor.commit();
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

    private void handleReturn() {
        try {
            if (isRoundActive) {
                // Show confirmation dialog
                new AlertDialog.Builder(this)
                    .setTitle("Leave Game?")
                    .setMessage("Are you sure you want to leave? Your current bet will be lost.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        try {
                            // End the current round
                            isRoundActive = false;
                            isFinishing = true;
                            
                            // Save current state
                            if (currentUserEmail != null) {
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putInt("coins_" + currentUserEmail, coins);
                                editor.apply();
                            }
                            
                            // Return to main activity with current coins
                            Intent resultIntent = new Intent();
                            resultIntent.putExtra("coins", coins);
                            setResult(RESULT_OK, resultIntent);
                            finish();
                        } catch (Exception e) {
                            Log.e("Blackjack", "Error handling return button confirmation: " + e.getMessage());
                            // Try to recover
                            try {
                                Intent resultIntent = new Intent();
                                resultIntent.putExtra("coins", coins);
                                setResult(RESULT_OK, resultIntent);
                                finish();
                            } catch (Exception ex) {
                                Log.e("Blackjack", "Error in recovery: " + ex.getMessage());
                            }
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
            } else {
                // If no active round, just return with current coins
                try {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("coins", coins);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } catch (Exception e) {
                    Log.e("Blackjack", "Error handling return button: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.e("Blackjack", "Error in return button click: " + e.getMessage());
            // Try to recover
            try {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("coins", coins);
                setResult(RESULT_OK, resultIntent);
                finish();
            } catch (Exception ex) {
                Log.e("Blackjack", "Error in recovery: " + ex.getMessage());
            }
        }
    }

    private void showHelpDialog() {
        try {
            // Create a scrollable dialog with game rules
            ScrollView scrollView = new ScrollView(this);
            TextView helpText = new TextView(this);
            
            // Set text style
            helpText.setTextSize(16);
            helpText.setPadding(40, 40, 40, 40);
            helpText.setTextColor(Color.BLACK);
            
            // Set help content
            String helpContent = "Blackjack Rules:\n\n" +
                "1. Goal: Get closer to 21 than the dealer without going over.\n\n" +
                "2. Card Values:\n" +
                "   • Number cards (2-10) are worth their face value\n" +
                "   • Face cards (J, Q, K) are worth 10\n" +
                "   • Ace is worth 1 or 11 (whichever is better for your hand)\n\n" +
                "3. Game Flow:\n" +
                "   • Place your bet\n" +
                "   • Click 'Deal' to start\n" +
                "   • You get two cards, dealer gets one visible card\n\n" +
                "4. Actions:\n" +
                "   • Hit: Take another card\n" +
                "   • Stand: Keep your current hand\n" +
                "   • Double: Double your bet and take one more card\n" +
                "   • Split: If you have two cards of the same value, split them into two hands\n\n" +
                "5. Winning:\n" +
                "   • Beat the dealer's hand without going over 21\n" +
                "   • Blackjack (Ace + 10-value card) pays 1:1\n" +
                "   • Dealer must hit on soft 17\n\n" +
                "6. Special Rules:\n" +
                "   • If you go over 21, you bust and lose your bet\n" +
                "   • If the dealer busts, you win\n" +
                "   • If you and dealer tie, it's a push (bet is returned)\n\n" +
                "7. Tips:\n" +
                "   • Watch the dealer's visible card\n" +
                "   • Consider the dealer's up card when deciding to hit or stand\n" +
                "   • Double down on strong hands (10 or 11)\n" +
                "   • Split pairs of Aces and 8s\n\n" +
                "Good luck and have fun!";
            
            helpText.setText(helpContent);
            scrollView.addView(helpText);
            
            // Create and show the dialog
            new AlertDialog.Builder(this)
                .setTitle("Blackjack Help")
                .setView(scrollView)
                .setPositiveButton("Got it!", null)
                .show();
                
        } catch (Exception e) {
            Log.e("Blackjack", "Error showing help dialog: " + e.getMessage());
            // Show a simple toast if dialog creation fails
            Toast.makeText(this, "Error showing help. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeGame() {
        try {
            // Initialize game state variables
            isRoundActive = false;
            isPlayerStanding = false;
            hasDoubled = false;
            hasSplit = false;
            isPlayingFirstHand = true;
            playerHasMoved = false;
            cardsDrawn = 0;
            
            // Clear hands
            playerHand.clear();
            dealerHand.clear();
            splitHand.clear();
            
            // Initialize deck
            initializeDeck();
            
            // Reset scores
            playerScore = 0;
            dealerScore = 0;
            
            // Clear UI
            if (resultTextView != null) {
                resultTextView.setText("");
            }
            if (firstHandResultText != null) {
                firstHandResultText.setVisibility(View.GONE);
                firstHandResult = "";
            }
            if (playerCardsLayout != null) {
                playerCardsLayout.removeAllViews();
            }
            if (dealerCardsLayout != null) {
                dealerCardsLayout.removeAllViews();
            }
            
            // Update UI
            updateScores();
            updateButtonStates();
            
            // Save initial background and rank drawables
            if (llMain != null) {
                initialBackgroundDrawable = ((android.graphics.drawable.Drawable) llMain.getBackground()).getConstantState().getChangingConfigurations();
            }
            ImageView rankImageView = findViewById(R.id.rankImageView);
            if (rankImageView != null) {
                initialRankDrawable = ((android.graphics.drawable.Drawable) rankImageView.getDrawable()).getConstantState().getChangingConfigurations();
            }
            
            Log.d("Blackjack", "Game initialized successfully");
            
        } catch (Exception e) {
            Log.e("Blackjack", "Error initializing game: " + e.getMessage());
            Toast.makeText(this, "Error initializing game. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateUI() {
        try {
            // Update all UI elements
            updateScores();        // Update score displays
            updateCardImages();    // Update card images
            updateButtonStates();  // Update button states
            updateBackground();    // Update background and rank
            
            // Update bet input if needed
            if (betInput != null && betInput.getText().toString().isEmpty()) {
                betInput.setText("10"); // Default bet amount
            }
            
            // Update coin display
            if (coinCountTextView != null) {
                coinCountTextView.setText("Coins: " + coins);
            }
            
            // Update first hand result text if split
            if (hasSplit && firstHandResultText != null) {
                if (isPlayingFirstHand) {
                    firstHandResultText.setVisibility(View.GONE);
                } else {
                    firstHandResultText.setText("First hand: " + firstHandResult);
                    firstHandResultText.setVisibility(View.VISIBLE);
                }
            }
            
            // Update result text if needed
            if (resultTextView != null) {
                if (!isRoundActive && resultTextView.getText().toString().isEmpty()) {
                    resultTextView.setText("Place your bet and click Deal to start");
                }
            }
            
            Log.d("Blackjack", "UI updated successfully");
            
        } catch (Exception e) {
            Log.e("Blackjack", "Error updating UI: " + e.getMessage());
            // Try to recover UI state
            try {
                if (coinCountTextView != null) {
                    coinCountTextView.setText("Coins: " + coins);
                }
                updateButtonStates();
            } catch (Exception ex) {
                Log.e("Blackjack", "Error in UI recovery: " + ex.getMessage());
            }
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
}
