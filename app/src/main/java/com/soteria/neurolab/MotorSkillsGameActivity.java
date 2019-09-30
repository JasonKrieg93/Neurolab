package com.soteria.neurolab;

import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.soteria.neurolab.database.DatabaseAccess;
import com.soteria.neurolab.models.GameAssignment;
import com.soteria.neurolab.models.GameSession;
import com.soteria.neurolab.models.Patient;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Jason Krieg
 */
public class MotorSkillsGameActivity extends AppCompatActivity {

    //UI elements
    private TextView gameTitleTextView;
    private TextView gameAttemptsTextView;
    private Button startGameButton;
    private Button playAgainButton;
    private Button exitGameButton;
    private ConstraintLayout gameLayout;

    //Database stuff
    private DatabaseAccess db;
    private Patient patient;
    private GameAssignment gameAssignment;

    //Button values
    private String[] alphabetArray = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L",
            "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};

    private List<Button> gameButtonArray = new ArrayList<>();

    private int round;
    private int current;
    private int fails;

    private LayerDrawable buttonDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Initializing some global variables
        db = DatabaseAccess.getInstance(getApplicationContext());

        // Sets the top bar title, and back button to reflect that the user is on the visual
        // memory game.
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.title_motor_skills);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            setContentView(R.layout.activity_motor_skills_game);
        }

        // Creating test patient and game assignment TODO: Delete this, will be handled by select game screen
        int patientID = 1;
        GameAssignment testGA = new GameAssignment(DatabaseAccess.GAME_ENUM.MOTOR.getGameID(), patientID, 1);
        db.createAssignment(testGA);
        // Grabs information from the select games pages intent. This will be used for determining
        // the number of times the patient can play the game and for using the users ID to
        // update the game session table in the database.
        try {
            Bundle visualBundle = getIntent().getExtras();
            patientID = visualBundle.getInt("PATIENT_ID");
            int attemptsLeft = visualBundle.getInt("ATTEMPTS");
        } catch (Exception e) {
//            Toast.makeText(getApplicationContext(),"ERROR - An error occurred during page transition : " + e,Toast.LENGTH_SHORT).show();
//            startActivity(new Intent(this, ViewPatientDetails.class)); //TODO change ViewPatientDetails to SelectGameActivity once added
//            finish();
        }

        patient = db.getPatient(patientID);
        gameAssignment = db.getAssignment(patient.getPatientID(), DatabaseAccess.GAME_ENUM.MOTOR);
        setupUI();
    }

    /**
     * Initialize UI elements on startup
     */
    private void setupUI() {
        gameTitleTextView = findViewById(R.id.motor_skills_game_title);
        gameAttemptsTextView = findViewById(R.id.motor_skills_game_incorrect_info);
        startGameButton = findViewById(R.id.motor_skills_game_start_game);
        playAgainButton = findViewById(R.id.motor_skills_game_play_again);
        exitGameButton = findViewById(R.id.motor_skills_game_exit_game);
        gameLayout = findViewById(R.id.motor_skills_game_constraint_layout);
        playAgainButton.setEnabled(false);
        playAgainButton.setVisibility(View.INVISIBLE);
        exitGameButton.setEnabled(false);
        exitGameButton.setVisibility(View.INVISIBLE);
        gameAttemptsTextView.setText(getResources().getString(R.string.visual_memory_textview_attempts_plural, String.valueOf(gameAssignment.getGameAttempts())));

        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGame();
                setupGameRound();
                startGameButton.setEnabled(false);
                startGameButton.setVisibility(View.INVISIBLE);
            }
        });

        playAgainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startGame();
                setupGameRound();
                playAgainButton.setEnabled(false);
                playAgainButton.setVisibility(View.INVISIBLE);
                exitGameButton.setEnabled(false);
                exitGameButton.setVisibility(View.INVISIBLE);
            }
        });

        gameLayout.setOnTouchListener(new View.OnTouchListener(){

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return false;
            }
        });
    }

    private void startGame(){
        //Start on round 2 - A and B
        gameTitleTextView.setText(getResources().getString(R.string.title_motor_skills));
        round = 2;
    }

    /**
     * Uses the game round to determine the buttons to display, their sizes, and renders them then
     * starts the round
     */
    private void setupGameRound() {
        //Handle game Complete
        if(round > 26){
            //TODO: Scoring and game session
            gameAttemptsTextView.setText(getResources().getString(R.string.visual_memory_textview_attempts_plural, String.valueOf(gameAssignment.getGameAttempts())));
            gameTitleTextView.setText(getResources().getString(R.string.visual_memory_textview_game_complete));
            playAgainButton.setEnabled(true);
            playAgainButton.setVisibility(View.VISIBLE);
            exitGameButton.setEnabled(true);
            exitGameButton.setVisibility(View.VISIBLE);
        //Else start a new round
        } else {
            current = 0;
            fails = 5;
            gameAttemptsTextView.setText(fails == 1 ?
                    getResources().getString(R.string.visual_memory_textview_try_singular, String.valueOf(fails)) :
                    getResources().getString(R.string.visual_memory_textview_try_plural, String.valueOf(fails)));
            gameLayout.removeAllViews();
            //Reset the button array
            gameButtonArray = new ArrayList<>();
            setGameButtons();
        }
    }

    /**
     * Will generate random x and y coordinates, if there would be no overlap of buttons by placing
     * a button at x/y then it will place the button, set the text and the onClickListener
     */
    private void setGameButtons() {
        final int DEFAULT_BUTTON_SIZE = 450;
        int buttonSize = (int) Math.round(DEFAULT_BUTTON_SIZE/((0.23 * round) + 1));
        buttonDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.motor_skills_game_button);
        final GradientDrawable gdYellow = (GradientDrawable) buttonDrawable.findDrawableByLayerId(R.id.motor_skills_button_shape);
        gdYellow.setSize(buttonSize, buttonSize);
        final GradientDrawable gdGreen = (GradientDrawable) gdYellow.getConstantState().newDrawable();
        final GradientDrawable gdNext = (GradientDrawable) gdYellow.getConstantState().newDrawable();
        final GradientDrawable gdRed = (GradientDrawable) gdYellow.getConstantState().newDrawable();
        gdGreen.mutate();
        gdGreen.setStroke(5, getResources().getColor(R.color.colorPrimary));
        gdGreen.setColor(getResources().getColor(R.color.colorGreen));
        gdNext.mutate();
        gdNext.setStroke(5, getResources().getColor(R.color.colorPrimary));
        gdRed.mutate();
        gdRed.setColor(getResources().getColor(R.color.colorWarning));
        Random rand = new Random();

        //Create one button per round
        for (int i = 0; i < round; i++) {
            int x = 0;
            int y = 0;
            gameButtonArray.add(new Button(this));
            gameLayout.addView(gameButtonArray.get(i));
            gameButtonArray.get(i).setBackground(i == current ? gdNext : gdYellow);
            gameButtonArray.get(i).setText(alphabetArray[i]);
            gameButtonArray.get(i).setTag(alphabetArray[i]);
            gameButtonArray.get(i).setTextSize(buttonSize / 2);

            //OnTouchListener handles more than just click, it can handle press and release as well
            gameButtonArray.get(i).setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    Button button = (Button) view;
                    //ACTION_UP is when you release a button
                    if(motionEvent.getAction() == MotionEvent.ACTION_UP){
                        //if it is the correct button
                        if(button.getTag().equals(alphabetArray[current])) {
                            //remove it from game
                            gameButtonArray.get(current).setEnabled(false);
                            gameButtonArray.get(current).setVisibility(View.INVISIBLE);
                            current++;
                            //if this button is the last button in the round then we start a new round
                            if (current == round) {
                                round += 3;
                                setupGameRound();
                            //else we indicate to the player which button to press next by highlighting it
                            } else {
                                gameButtonArray.get(current).setBackground(gdNext);
                            }
                        //if it is the incorrect button then we set the button to red on the down press (see below)
                        //We then want to set it back to yellow upon release
                        } else{
                            button.setBackground(gdYellow);
                        }
                        return true;
                    //Probably should have used a switch on the event but oh well
                    } else {
                        //If down press
                        if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                            //If it is the correct button then make it green
                            if(button.getTag().equals(alphabetArray[current])) {
                                button.setBackground(gdGreen);
                            //else if it is incorrect, make it red
                            } else {
                                fails --;
                                gameAttemptsTextView.setText(fails == 1 ?
                                        getResources().getString(R.string.visual_memory_textview_try_singular, String.valueOf(fails)) :
                                        getResources().getString(R.string.visual_memory_textview_try_plural, String.valueOf(fails)));
                                button.setBackground(gdRed);
                            }
                        }
                    }
                    return false;
                }
            });

            //Set random x/y coordinates for the button, if the new coordinates would put the button
            //  behind another button then set x/y coordinates again
            boolean shorterDistance = true;
            while (shorterDistance) {
                System.out.println("Setting x and y");
                //Size of the game layout minus the padding (20; 10 for each side)
                x = rand.nextInt(701);
                y = rand.nextInt(765);
                //If adding the size of the button would put x/y out of bounds (unseeable)
                //  then generate a new x/y coordinate.
                while (x + buttonSize > 701) {
                    x = rand.nextInt(701);
                }
                while (y + buttonSize > 765) {
                    y = rand.nextInt(765);
                }

                //Checking that the button doesn't overlap with another
                for (Button tempButton : gameButtonArray) {
                    //Need to subtract smallerX from largerX but don't know which is which yet
                    float largerX = x > tempButton.getX() ? x : tempButton.getX();
                    float smallerX = x < tempButton.getX() ? x : tempButton.getX();
                    float largerY = y > tempButton.getY() ? y : tempButton.getY();
                    float smallerY = y < tempButton.getY() ? y : tempButton.getY();
                    //Using pythagoras' theorem to calc the distance between 2 points
                    double distance = Math.sqrt(Math.pow(largerX - smallerX, 2) + Math.pow(largerY - smallerY, 2) * 1.0);
                    shorterDistance = distance < buttonSize ? true : false;
                    System.out.println("Distance = " + distance + ", Button Size = " + buttonSize + ", shorterDistance = " + shorterDistance);
                    if (shorterDistance)
                        //No need to keep on looping if the x/y coordinates already clash with another button
                        break;
                }
            }
            //Set x/y coordinates of the button now that we are sure they aren't out of bounds or
            //  behind another button
            gameButtonArray.get(i).setX(x);
            gameButtonArray.get(i).setY(y);
        }
    }
}

