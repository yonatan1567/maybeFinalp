package com.example.maybefinalp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BlackjackActivity extends AppCompatActivity {
    private int coins;
    private int betAmount;
    private boolean hasDoubled = false;
    private boolean playerHasMoved = false; // Track if the player has made a move
    private boolean hasSplit = false; // Track if the player has split
    private Random random = new Random();

    private List<Integer> playerHand = new ArrayList<>();
    private List<Integer> splitHand = new ArrayList<>();
    private List<Integer> dealerHand = new ArrayList<>();

    private TextView playerScoreTextView, dealerScoreTextView, resultTextView, coinCountTextView;
    private EditText betInput;

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
    private void updateCardImagesForPlayerHand() {
        if (playerHand.size() > 2) {
            ImageView newCardImageView = new ImageView(this);
            newCardImageView.setImageResource(getCardImageResource(playerHand.get(playerHand.size() - 1)));

            // Set the size of the new card (smaller than the original)
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.card_width_small), // Define this in dimens.xml
                    getResources().getDimensionPixelSize(R.dimen.card_height_small) // Define this in dimens.xml
            );
            layoutParams.setMargins(8, 0, 8, 0); // Adjust margins to fit next to the previous cards

            newCardImageView.setLayoutParams(layoutParams);

            // Add the new card image to your player card layout dynamically
            ((LinearLayout) findViewById(R.id.playerCardsLayout)).addView(newCardImageView);
        }
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
        Log.d("Blackjack", "Starting a New Round...");

        String betText = betInput.getText().toString();
        if (betText.isEmpty() || (betAmount = Integer.parseInt(betText)) <= 0 || betAmount > coins) {
            resultTextView.setText("Invalid Bet Amount");
            return;
        }

        // Deduct bet amount
        coins -= betAmount;
        coinCountTextView.setText("Coins: " + coins);

        // Clear previous hands and card views
        playerHand.clear();
        splitHand.clear();
        dealerHand.clear();
        ((LinearLayout) findViewById(R.id.playerCardsLayout)).removeAllViews(); // Clear extra cards

        Log.d("Blackjack", "Cleared previous hands.");

        // Reset the initial card images
        // Set correct card images after drawing
        Log.d("Blackjack", "Player Hand after draw: " + playerHand);
        Log.d("Blackjack", "Dealer Hand after draw: " + dealerHand);
        dealerCard1.setImageResource(R.drawable.card_back);
        dealerCard2.setImageResource(R.drawable.card_back);

        splitCard1.setVisibility(View.GONE);
        splitCard2.setVisibility(View.GONE);

        // Draw initial cards
        // Draw initial cards
        playerHand.add(drawCard());
        playerHand.add(drawCard());
        dealerHand.add(drawCard());
        dealerHand.add(drawCard());

        updateCardImages(); // Make sure the UI updates

        Log.d("Blackjack", "Player Hand after draw: " + playerHand);
        Log.d("Blackjack", "Dealer Hand after draw: " + dealerHand);

        isPlayerStanding = false;
        isRoundActive = true;
        hasDoubled = false;
        hasSplit = false;
        playerHasMoved = false;

        updateScores();
        resultTextView.setText("");

        // Show initial cards for player and dealer
        updateCardImages(); // Ensure this is called immediately after drawing the initial cards

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

        if (hasSplit && !splitHand.isEmpty()) {
            splitHand.add(drawCard());
            updateCardImagesForSplitHand();
        } else {
            int newCard = drawCard();
            playerHand.add(newCard);
            updateCardImagesForPlayerHand();
        }

        playerHasMoved = true;
        updateScores();

        if (calculateScore(playerHand) > 21) {
            resultTextView.setText("Bust on Main Hand!");
            playerHand.clear();
        }

        if (hasSplit && calculateScore(splitHand) > 21) {
            resultTextView.setText("Bust on Split Hand!");
            splitHand.clear();
        }

        if (playerHand.isEmpty() && splitHand.isEmpty()) {
            resultTextView.setText("Both Hands Busted! You Lose!");
            endRound();
        }

        updateButtonStates();
    }


    private void playerStand() {
        if (!isRoundActive) return;

        isPlayerStanding = true;
        playDealerTurn();
        updateScores();
        evaluateWinner();
        endRound();
        updateButtonStates();
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
            updateCardImagesForDealerHand(newCard); // Add each new card visually
        }
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
        int card = random.nextInt(13) + 1;
        Log.d("Blackjack", "Drawn Card: " + card);
        return card;
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
        dealerCard1.setImageResource(getCardImageResource(dealerHand.get(0)));
        dealerCard2.setImageResource(getCardImageResource(dealerHand.get(1))); // Reveal second card
        Log.d("Blackjack", "Updating Card Images...");
        Log.d("Blackjack", "Player Hand: " + playerHand);
        Log.d("Blackjack", "Dealer Hand: " + dealerHand);

        LinearLayout playerCardsLayout = findViewById(R.id.playerCardsLayout);
        playerCardsLayout.removeAllViews(); // Clear all previous views

        // Ensure initial cards are added
        for (int i = 0; i < playerHand.size(); i++) {
            ImageView cardImageView = new ImageView(this);
            cardImageView.setImageResource(getCardImageResource(playerHand.get(i)));

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    getResources().getDimensionPixelSize(R.dimen.card_width_small),
                    getResources().getDimensionPixelSize(R.dimen.card_height_small)
            );
            layoutParams.setMargins(8, 0, 8, 0);
            cardImageView.setLayoutParams(layoutParams);

            playerCardsLayout.addView(cardImageView);
        }

        // Update dealer cards
        dealerCard1.setImageResource(getCardImageResource(dealerHand.get(0)));
        dealerCard2.setImageResource(isPlayerStanding || !isRoundActive ?
                getCardImageResource(dealerHand.get(1)) : R.drawable.card_back);

        Log.d("Blackjack", "Card images updated.");
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
