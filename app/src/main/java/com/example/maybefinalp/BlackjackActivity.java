package com.example.maybefinalp;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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

public class BlackjackActivity extends AppCompatActivity {
    private int coins;
    private int betAmount;
    // Add this at the top of your class, along with your other variables
    private int hitCount = 0;  // Track the number of hits

    private boolean hasDoubled = false;
    private boolean playerHasMoved = false; // Track if the player has made a move
    private boolean hasSplit = false; // Track if the player has split
    private Random random = new Random();

    private List<Integer> playerHand = new ArrayList<>();
    private List<Integer> splitHand = new ArrayList<>();
    private List<Integer> dealerHand = new ArrayList<>();

    private TextView playerScoreTextView, dealerScoreTextView, resultTextView, coinCountTextView;
    private EditText betInput;
    private List<Integer> deck = new ArrayList<>();

    private Button hitButton, standButton, dealButton, doubleButton, splitButton;
    private Button returnButton;

    private boolean isPlayerStanding = false;
    private boolean isRoundActive = false;

    // ImageView for displaying cards
    private ImageView playerCard1, playerCard2, dealerCard1, dealerCard2;
    private ImageView splitCard1, splitCard2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blackjack);

        coins = getIntent().getIntExtra("coins", 1000);

        // Initialize UI components
        coinCountTextView = findViewById(R.id.coinCount);
        coinCountTextView.setText("Coins: " + coins);

        playerScoreTextView = findViewById(R.id.playerScore);
        dealerScoreTextView = findViewById(R.id.dealerScore);
        resultTextView = findViewById(R.id.resultText);
        betInput = findViewById(R.id.betInput);

        // Initialize buttons
        dealButton = findViewById(R.id.dealButton);
        dealButton.setOnClickListener(v -> startNewRound());

        hitButton = findViewById(R.id.hitButton);
        hitButton.setOnClickListener(v -> playerHit());

        standButton = findViewById(R.id.standButton);
        standButton.setOnClickListener(v -> playerStand());

        doubleButton = findViewById(R.id.doubleDownButton);
        doubleButton.setOnClickListener(v -> playerDouble());

        splitButton = findViewById(R.id.splitButton);
        splitButton.setOnClickListener(v -> playerSplit());

        returnButton = findViewById(R.id.returnButton);
        returnButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            resultIntent.putExtra("coins", coins);
            setResult(RESULT_OK, resultIntent);
            finish();
        });

        // Initialize ImageViews for cards
        playerCard1 = findViewById(R.id.playerCard1);
        playerCard2 = findViewById(R.id.playerCard2);
        dealerCard1 = findViewById(R.id.dealerCard1);
        dealerCard2 = findViewById(R.id.dealerCard2);
        splitCard1 = findViewById(R.id.splitCard1);
        splitCard2 = findViewById(R.id.splitCard2);

        updateButtonStates();
    }
    private void initializeDeck() {
        deck.clear();
        for (int i = 1; i <= 13; i++) { // 1 = Ace, 11 = Jack, 12 = Queen, 13 = King
            for (int j = 0; j < 4; j++) { // 4 suits for each card
                deck.add(i);
            }
        }
        Collections.shuffle(deck); // Shuffle the deck before using
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
            ImageView newCardImageView = new ImageView(this);
            newCardImageView.setImageResource(getCardImageResource(splitHand.get(splitHand.size() - 1)));
            // Add the new card image to your split hand layout dynamically
            ((LinearLayout) findViewById(R.id.splitHandLayout)).addView(newCardImageView);
        }
    }

    private void startNewRound() {
        initializeDeck();
        Log.d("Blackjack", "Starting a New Round...");

        String betText = betInput.getText().toString();
        if (betText.isEmpty() || (betAmount = Integer.parseInt(betText)) <= 0 || betAmount > coins) {
            resultTextView.setText("Invalid Bet Amount");
            return;
        }

        // Deduct bet amount
        coins -= betAmount;
        coinCountTextView.setText("Coins: " + coins);

        // Clear previous hands
        playerHand.clear();
        splitHand.clear();
        dealerHand.clear();

        // Clear dynamically added card views from previous rounds
        ((LinearLayout) findViewById(R.id.playerCardsLayout)).removeAllViews();
        ((LinearLayout) findViewById(R.id.splitHandLayout)).removeAllViews();
        ((LinearLayout) findViewById(R.id.dealerCardsLayout)).removeAllViews();

        Log.d("Blackjack", "Cleared previous hands and UI cards.");

        // Reset initial card images
        dealerCard1.setImageResource(R.drawable.card_back);
        dealerCard2.setImageResource(R.drawable.card_back);
        splitCard1.setVisibility(View.GONE);
        splitCard2.setVisibility(View.GONE);

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
            coins += betAmount * 2.5;
            coinCountTextView.setText("Coins: " + coins);
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

        isPlayerStanding = true;
        playDealerTurn();  // This will make the dealer draw cards until they stand
        updateCardImages();  // Update the card images after the dealer's turn
        updateScores();      // Update the scores for player and dealer
        evaluateWinner();    // Evaluate who won
        endRound();          // End the round
        updateButtonStates();  // Update the button states (disable hit, etc.)
    }


    private void playerDouble() {
        if (!isRoundActive || hasDoubled || coins < betAmount) return;

        coins -= betAmount;
        betAmount *= 2;
        coinCountTextView.setText("Coins: " + coins);

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


    private void playerSplit() {
        if (!isRoundActive || playerHand.size() != 2 || !playerHand.get(0).equals(playerHand.get(1))) return;

        hasSplit = true;
        splitHand.add(playerHand.remove(1));

        if (coins >= betAmount) {
            coins -= betAmount;
            coinCountTextView.setText("Coins: " + coins);
            resultTextView.setText("Hand split! Play both hands.");
        } else {
            resultTextView.setText("Not enough coins to split.");
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

        if (dealerScore > 21 || playerScore > dealerScore) {
            coins += betAmount * 2;
        } else if (playerScore == dealerScore) {
            coins += betAmount;
        }

        if (hasSplit && (dealerScore > 21 || splitScore > dealerScore)) {
            coins += betAmount * 2;
        } else if (hasSplit && splitScore == dealerScore) {
            coins += betAmount;
        }

        coinCountTextView.setText("Coins: " + coins);
    }

    private void endRound() {
        isRoundActive = false;
        updateButtonStates();
    }

    private void updateButtonStates() {
        hitButton.setEnabled(isRoundActive && !hasDoubled);
        standButton.setEnabled(isRoundActive);
        dealButton.setEnabled(!isRoundActive);
        splitButton.setEnabled(isRoundActive && playerHand.size() == 2 && playerHand.get(0).equals(playerHand.get(1)) && !hasSplit);
    }

    private int drawCard() {
        if (deck.isEmpty()) {
            initializeDeck(); // If deck is empty, reset and shuffle
        }
        return deck.remove(0); // Draw the top card
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
        // Ensure dealer and player hands are correctly displayed
        LinearLayout playerCardsLayout = findViewById(R.id.playerCardsLayout);
        LinearLayout dealerCardsLayout = findViewById(R.id.dealerCardsLayout);

        // Clear previous cards
        playerCardsLayout.removeAllViews();
        dealerCardsLayout.removeAllViews();

        // Update player's cards with smaller size
        for (int card : playerHand) {
            ImageView cardImageView = new ImageView(this);
            cardImageView.setImageResource(getCardImageResource(card));

            // Set the smaller card size
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.card_width_small),
                    getResources().getDimensionPixelSize(R.dimen.card_height_small)
            );
            layoutParams.setMargins(8, 0, 8, 0);
            cardImageView.setLayoutParams(layoutParams);

            playerCardsLayout.addView(cardImageView);
        }

        // Update dealer's cards with smaller size
        for (int i = 0; i < dealerHand.size(); i++) {
            ImageView cardImageView = new ImageView(this);

            // Show the second dealer card if the player has stood or the round is over
            if (i == 1 && (isPlayerStanding || !isRoundActive)) {
                cardImageView.setImageResource(getCardImageResource(dealerHand.get(i)));
            } else if (i == 1) {
                cardImageView.setImageResource(R.drawable.card_back);  // Keep second card hidden until the player stands
            } else {
                cardImageView.setImageResource(getCardImageResource(dealerHand.get(i)));
            }

            // Set the smaller card size
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.card_width_small),
                    getResources().getDimensionPixelSize(R.dimen.card_height_small)
            );
            layoutParams.setMargins(8, 0, 8, 0);
            cardImageView.setLayoutParams(layoutParams);

            dealerCardsLayout.addView(cardImageView);
        }
    }

    public void updateCardSizes(List<ImageView> playerCards) {
        if (playerCards.isEmpty()) return;

        Context context = playerCards.get(0).getContext();

        // Define card dimensions
        int bigWidth = (int) context.getResources().getDimension(R.dimen.card_width_big);
        int bigHeight = (int) context.getResources().getDimension(R.dimen.card_height_big);
        int smallWidth = (int) context.getResources().getDimension(R.dimen.card_width_small);
        int smallHeight = (int) context.getResources().getDimension(R.dimen.card_height_small);
        int smallerWidth = (int) context.getResources().getDimension(R.dimen.card_width_smaller);
        int smallerHeight = (int) context.getResources().getDimension(R.dimen.card_height_smaller);

        // Resize cards based on their position in the list
        for (int i = 0; i < playerCards.size(); i++) {
            ImageView card = playerCards.get(i);
            ViewGroup.LayoutParams params = card.getLayoutParams();

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
