package edu.rosehulman.umstedjr.golfballstand;

import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import edu.rosehulman.me435.FieldGps;
import edu.rosehulman.me435.FieldGpsListener;
import edu.rosehulman.me435.FieldOrientation;
import edu.rosehulman.me435.FieldOrientationListener;
import edu.rosehulman.me435.NavUtils;
import edu.rosehulman.me435.RobotActivity;


public class MainActivity extends RobotActivity
        implements AdapterView.OnItemSelectedListener, FieldGpsListener, FieldOrientationListener {
    private static final int DELAY_MS = 2000;
    private static final int LOWEST_DUTY_CYCLE = 100;
    private static final double MIN_DIST = 5.0 * Math.sqrt(2);
    public static final int HOME = -1;
    public static final int SPEED = 3; // ft/s

    private double leftDutyCycleFactor = 0.6;
    private TextView mBall1TextView, mBall2TextView, mBall3TextView;
    private TextView[] mBalls;
    private int mYB_location;
    private int mRG_location;
    private int mBW_location;
    private String[] mColors;

    private Handler mCommandHandler = new Handler();
    private FieldGps mFieldGps;
    private FieldOrientation mFieldOrientation;
    private TextView mSensorHeadingTextView;
    private TextView mGpsTextView;
    private TextView mGpsTargetTextView;
    private TextView mGpsHeadingTextView;
    private TextView mWheelSpeedTextView;

    private long mStateStartTime;
    private State mState;
    private boolean mDoneRemovingBall;
    private boolean mHasWhiteBall;

    private int mSeekingLocation;

    private enum State {
        READY_FOR_MISSION,
        WAITING_FOR_GPS,
        INITIAL_WAITING_FOR_GPS,
        WAITING_FOR_PICKUP,
        SEEKING_TARGET,
        REMOVING_BALL
    }

    private Point mSeekingTarget;

    private boolean mIsRedTeam = true;
    private Point mHomePoint;
    private Point mGreenPoint;
    private Point mRedPoint;
    private Point mBluePoint;

    private Point mYellowPoint;
    private double mGuessX, mGuessY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBall1TextView = (TextView) findViewById(R.id.Ball1_textView);
        mBall2TextView = (TextView) findViewById(R.id.Ball2_textView);
        mBall3TextView = (TextView) findViewById(R.id.Ball3_textView);
        mWheelSpeedTextView = (TextView) findViewById(R.id.wheel_speed_text_view);
        mBalls = new TextView[]{mBall1TextView, mBall2TextView, mBall3TextView};
        mColors = new String[3];

        setUpSpinners();

        //setting field and heading values
        mFieldGps = new FieldGps(this);
        mSensorHeadingTextView = (TextView) findViewById(R.id.SHeading_textView);
        mFieldOrientation = new FieldOrientation(this);

        //setting text values
        mGpsTextView = (TextView) findViewById(R.id.GPS_value_textView);
        mGpsTargetTextView = (TextView) findViewById(R.id.GPS_target_value_textView);
        mGpsHeadingTextView = (TextView) findViewById(R.id.GPSHeading_textView);

        setState(State.READY_FOR_MISSION);

        setUpTeamAndLocations();


    }

    private void setUpSpinners() {
        Spinner loc1Spinner = (Spinner) findViewById(R.id.location1_spinner);
        Spinner loc2Spinner = (Spinner) findViewById(R.id.location2_spinner);
        Spinner loc3Spinner = (Spinner) findViewById(R.id.location3_spinner);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.ball_colors_array, android.R.layout.simple_spinner_item);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        loc1Spinner.setAdapter(adapter);
        loc2Spinner.setAdapter(adapter);
        loc3Spinner.setAdapter(adapter);

        loc1Spinner.setOnItemSelectedListener(this);
        loc2Spinner.setOnItemSelectedListener(this);
        loc3Spinner.setOnItemSelectedListener(this);

        Spinner leftWheelSpinner = (Spinner) findViewById(R.id.wheel_speed_factor_spinner);
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.wheel_speed_factors_array, android.R.layout.simple_spinner_item);

        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        leftWheelSpinner.setAdapter(adapter2);
        leftWheelSpinner.setOnItemSelectedListener(this);
        leftWheelSpinner.setSelection(4);
    }

    private void setUpTeamAndLocations() {
        // Set locations based on team
        mHomePoint = new Point(0, 0);

        if (mIsRedTeam) {
            mGreenPoint = new Point(90, 50);
            mRedPoint = new Point(90, -50);
            mBluePoint = new Point(240, 50);
            mYellowPoint = new Point(240, -50);
        } else {
            mGreenPoint = new Point(240, -50);
            mRedPoint = new Point(240, 50);
            mBluePoint = new Point(90, -50);
            mYellowPoint = new Point(90, 50);
        }

        setTeamToRed(mIsRedTeam);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mFieldOrientation.registerListener(this);
        mFieldGps.requestLocationUpdates(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mFieldOrientation.unregisterListener();
        mFieldGps.removeUpdates();
    }

    @Override
    protected void onCommandReceived(String receivedCommand) {
        super.onCommandReceived(receivedCommand);
        Toast.makeText(this, receivedCommand, Toast.LENGTH_SHORT).show();
        if (receivedCommand.startsWith("Colors=")) {
            String[] ballColors = receivedCommand.substring(7).split(",");
            for (int i = 0; i < 3; i++) {
                ballColors[i] = ballColors[i].trim();
            }
            fixColors(ballColors);

            mBall1TextView.setText(ballColors[0]);
            mBall2TextView.setText(ballColors[1]);
            mBall3TextView.setText(ballColors[2]);
            setColorLocations();

        }
    }

    /* ---------- RobotActivity stuff ---------- */

    private void setState(State newState) {
        mStateStartTime = System.currentTimeMillis();
        switch (newState) {
        case READY_FOR_MISSION:
            mMovingForward = false;
            mSeekingLocation = -1;
            mSeekingTarget = null;
            mGpsTargetTextView.setText(getString(R.string.blank_field));
            sendWheelSpeed(0, 0);
            break;
        // fall-through to moving
        case INITIAL_WAITING_FOR_GPS:
        case WAITING_FOR_GPS:
        case SEEKING_TARGET:
            mMovingForward = true;
            break;
        // fall-through to not moving
        case WAITING_FOR_PICKUP:
        case REMOVING_BALL:
            mMovingForward = false;
            break;
        }
        mState = newState;
    }

    public void loop() {
        super.loop();
        switch (mState) {
        case WAITING_FOR_PICKUP:
            if (getStateTimeMs() > 8000) {
                mSeekingLocation = HOME;
                setSeekingTarget();
                setState(State.SEEKING_TARGET);
            }
            break;
        case SEEKING_TARGET:
            seekTargetAt(mSeekingTarget);
            if (NavUtils.getDistance(mGuessX, mGuessY, mSeekingTarget.x, mSeekingTarget.y) <= MIN_DIST) {
                if (mSeekingTarget != mHomePoint) {
                    setState(State.REMOVING_BALL);
                    runBallKnockScript();
                } else {
                    setState(State.WAITING_FOR_PICKUP);
                }
                sendWheelSpeed(0, 0);
            }
            break;
        case REMOVING_BALL:
            if (mDoneRemovingBall) {
                setState(State.WAITING_FOR_GPS);
                mDoneRemovingBall = false;
            }
            break;
        case WAITING_FOR_GPS:
            sendWheelSpeed((int) (255 * leftDutyCycleFactor), 255);
            if (getStateTimeMs() > 8000) {
                setState(State.SEEKING_TARGET);
            }
            break;
        case INITIAL_WAITING_FOR_GPS:
            sendWheelSpeed((int) (255 * leftDutyCycleFactor), 255);
            if (getStateTimeMs() > 20000) {
                setState(State.SEEKING_TARGET);
            }
            break;
        default:
            break;
        }
        mGpsTextView.setText(getString(R.string.xy_format, mGuessX, mGuessY));
    }

    private long getStateTimeMs() {
        return System.currentTimeMillis() - mStateStartTime;
    }

    /* ---------- Target Seeking ---------- */

    private void setSeekingTarget() {
        if (mSeekingLocation == HOME) {
            mSeekingTarget = mHomePoint;
            mGpsTargetTextView.setText(getString(R.string.xy_format, (double) mSeekingTarget.x, (double) mSeekingTarget.y));
            Toast.makeText(this, "Seeking Home", Toast.LENGTH_SHORT).show();
            return;
        }
        BallCorrector.BallColor color = BallCorrector.getColorForString(mColors[mSeekingLocation]);
        switch (color) {
        case RED:
            mSeekingTarget = mRedPoint;
            break;
        case GREEN:
            mSeekingTarget = mGreenPoint;
            break;
        case BLUE:
            mSeekingTarget = mBluePoint;
            break;
        case YELLOW:
            mSeekingTarget = mYellowPoint;
            break;
        case WHITE:
            if (mSeekingTarget.x < 180) {
                mSeekingTarget = new Point(240, mSeekingTarget.y);
            }
            break;
        default:
            mSeekingTarget = mHomePoint;
            break;
        }
        mGpsTargetTextView.setText(getString(R.string.xy_format, (double) mSeekingTarget.x, (double) mSeekingTarget.y));
        Toast.makeText(this, String.format("Ball Color: %s, Target: %s", color, mSeekingTarget), Toast.LENGTH_SHORT).show();
    }

    private void setSeekingLocation() {
        if (mSeekingLocation == mYB_location && mHasWhiteBall) {
            mSeekingLocation = mBW_location;
        } else if (mSeekingLocation == mYB_location || mSeekingLocation == mBW_location) {
            mSeekingLocation = mRG_location;
        } else {
            mSeekingLocation = HOME;
        }
    }

    private void seekTargetAt(Point p) {
        double leftDutyCycle = 255 * leftDutyCycleFactor; // TODO: change me to a Picker
        double rightDutyCycle = 255;
        double targetHeading = NavUtils.getTargetHeading(mGuessX, mGuessY, p.x, p.y);
        double leftTurnAmount = NavUtils.getLeftTurnHeadingDelta(mCurrentSensorHeading, targetHeading);
        double rightTurnAmount = NavUtils.getRightTurnHeadingDelta(mCurrentSensorHeading, targetHeading);
        if (leftTurnAmount < rightTurnAmount) {
            leftDutyCycle -= (int) (leftTurnAmount);
            leftDutyCycle = Math.max(leftDutyCycle, LOWEST_DUTY_CYCLE);
            leftDutyCycle *= leftDutyCycleFactor; // Correction factor for T'Rex
        } else {
            rightDutyCycle -= (int) (rightTurnAmount);
            rightDutyCycle = Math.max(rightDutyCycle, LOWEST_DUTY_CYCLE);
        }
        sendWheelSpeed((int) leftDutyCycle, (int) rightDutyCycle);
        mWheelSpeedTextView.setText(getString(R.string.xy_format, leftDutyCycle, rightDutyCycle));
    }

    /* ---------- Ball Stuff ---------- */

    private void runBallKnockScript() {
        Toast.makeText(this, "removing ball", Toast.LENGTH_SHORT).show();
        sendCommand(getString(R.string.gripper_close));
        switch (mSeekingLocation) {
        case 0:
            sendCommand(getString(R.string.ball_1));
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCommand(getString(R.string.ball_1_swing));
                }
            }, DELAY_MS);
            break;
        case 1:
            sendCommand(getString(R.string.ball_2));
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCommand(getString(R.string.ball_2_swing));
                }
            }, DELAY_MS);
            break;
        case 2:
            sendCommand(getString(R.string.ball_3));
            mCommandHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendCommand(getString(R.string.ball_3_swing));
                }
            }, DELAY_MS);
            break;
        }
        mCommandHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                sendCommand(getString(R.string.home_position));
                mDoneRemovingBall = true;
                Toast.makeText(MainActivity.this, "Done removing ball", Toast.LENGTH_SHORT).show();
                setSeekingLocation();
                setSeekingTarget();
            }
        }, 2 * DELAY_MS);
    }

    private void setColorLocations() {
        for (int i = 0; i < 3; i++) {
            if (textEquals(mBalls[i], "YELLOW") || textEquals(mBalls[i], "BLUE")) {
                mYB_location = i;
                mColors[i] = mBalls[i].getText().toString();
            } else if (textEquals(mBalls[i], "RED") || textEquals(mBalls[i], "GREEN")) {
                mRG_location = i;
                mColors[i] = mBalls[i].getText().toString();
            } else if (textEquals(mBalls[i], "BLACK") || textEquals(mBalls[i], "WHITE")) {
                mBW_location = i;
                mColors[i] = mBalls[i].getText().toString();
            }
        }
    }

    private void fixColors(String[] ballColorStrings) {
        BallCorrector.BallColor[] ballColors = BallCorrector.convertToBallColors(ballColorStrings);

        // fix "NONE" returns, if possible.
        BallCorrector.checkNoneToRG(ballColors);
        BallCorrector.checkNoneToYB(ballColors);
        BallCorrector.checkNoneToBW(ballColors);

        // Check Incompatible Colors
        BallCorrector.checkBW(ballColors);
        BallCorrector.checkRG(ballColors);
        BallCorrector.checkYB(ballColors);

    }

    private boolean textEquals(TextView view, String text) {
        return view.getText().toString().equalsIgnoreCase(text);
    }

    /* ---------- FieldOrientation and FieldGPS callbacks ---------- */

    @Override
    public void onLocationChanged(double x, double y, double heading, Location location) {
        if ((x != 0 && y != 0) // not setting origin
                && (Math.abs(mCurrentGpsX - x) > 100) || (Math.abs(mCurrentGpsY - y) > 100)) {
            return;
        }
        mCurrentGpsX = x;
        mCurrentGpsY = y;
        mGuessX = x;
        mGuessY = y;
        mCurrentGpsHeading = heading;
        mGpsTextView.setText(getString(R.string.xy_format, x, y));
        if (heading <= 180.0 && heading > -180.0) {
            mGpsHeadingTextView.setText(getString(R.string.degrees_format, heading));
            // Set fieldHeading to GPS heading
            mFieldOrientation.setCurrentFieldHeading(heading);
        } else {
            mGpsHeadingTextView.setText(getString(R.string.blank_field));
        }
    }

    @Override
    public void onSensorChanged(double fieldHeading, float[] orientationValues) {
        mSensorHeadingTextView.setText(getString(R.string.degrees_format, fieldHeading));
        mCurrentSensorHeading = fieldHeading;
        // Note:
        //  Azimuth is orientationValues[0]
        //    Pitch is orientationValues[1]
        //     Roll is orientationValues[2]
    }

    /* ---------- Spinner Callbacks ---------- */

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String color = parent.getItemAtPosition(position).toString();

        switch (parent.getId()) {
        case R.id.location1_spinner:
            mBall1TextView.setText(color);
            break;
        case R.id.location2_spinner:
            mBall2TextView.setText(color);
            break;
        case R.id.location3_spinner:
            mBall3TextView.setText(color);
            break;
        case R.id.wheel_speed_factor_spinner:
            // set duty cycle percent as decimal
            leftDutyCycleFactor = Double.parseDouble(parent.getItemAtPosition(position).toString()) / 100.0;


        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        parent.setSelection(parent.getFirstVisiblePosition());
    }

    /* ---------- Button Callbacks ---------- */

    public void handleClear(View view) {
        mBall1TextView.setText(getString(R.string.blank_field));
        mBall2TextView.setText(getString(R.string.blank_field));
        mBall3TextView.setText(getString(R.string.blank_field));
    }

    public void handleBallTest(View view) {
        sendCommand("CUSTOM Perform Ball Test");
    }

    public void handleCalibrate(View view) {
        sendCommand("CUSTOM Calibrate");
    }

    public void handleGo(View view) {
        setColorLocations();
        if (textEquals(mBalls[0], "BLACK")
                || textEquals(mBalls[1], "BLACK")
                || textEquals(mBalls[2], "BLACK")) {
            Toast.makeText(this, "Remove location " + Integer.toString(mYB_location + 1) + " (" + mColors[mYB_location] +
                    "), then " + Integer.toString(mRG_location + 1) + " (" + mColors[mRG_location] +
                    ").  Location " + Integer.toString(mBW_location + 1) + " (" + mColors[mBW_location] + ")."
                    , Toast.LENGTH_SHORT).show();

            mHasWhiteBall = false;

        } else if (textEquals(mBalls[0], "WHITE")
                || textEquals(mBalls[1], "WHITE")
                || textEquals(mBalls[2], "WHITE")) {
            Toast.makeText(this, "Remove location " + Integer.toString(mYB_location + 1) + " (" + mColors[mYB_location] +
                    "), then " + Integer.toString(mBW_location + 1) + " (" + mColors[mBW_location] +
                    "), then " + Integer.toString(mRG_location + 1) + " (" + mColors[mRG_location] + ")."
                    , Toast.LENGTH_SHORT).show();

            mHasWhiteBall = true;
        }
        mSeekingLocation = mYB_location;
        setSeekingTarget();
        setState(State.INITIAL_WAITING_FOR_GPS);
    }

    public void handleStop(View view) {
        setState(State.READY_FOR_MISSION);
    }

    public void handleSetOrigin(View view) {
        //Toast.makeText(this,"SetOrigin clicked",Toast.LENGTH_SHORT).show();
        mFieldGps.setCurrentLocationAsOrigin();
        mCurrentGpsX = 0;
        mCurrentGpsY = 0;
        mGuessX = 0;
        mGuessY = 0;
    }

    public void handleSetXAxis(View view) {
        //Toast.makeText(this,"Set X Axis clicked",Toast.LENGTH_SHORT).show();
        mFieldGps.setCurrentLocationAsLocationOnXAxis();
    }

    public void handleSetHeadingTo0(View view) {
        mFieldOrientation.setCurrentFieldHeading(0);
        mSensorHeadingTextView.setText(getString(R.string.degrees_format, 0.0));
    }

    public void handleTeamToggle(View view) {
        mIsRedTeam = ((ToggleButton) view).isChecked();
    }

    public void handleFakeGPS1(View view) {
        onLocationChanged(0, 0, FieldGps.NO_BEARING_AVAILABLE, null);
    }

    public void handleFakeGPS2(View view) {
        onLocationChanged(180, 0, FieldGps.NO_BEARING_AVAILABLE, null);
    }

    public void handleFakeGPS3(View view) {
        onLocationChanged(90, 50, FieldGps.NO_BEARING_AVAILABLE, null);
    }

    public void handleFakeGPS4(View view) {
        onLocationChanged(90, -50, FieldGps.NO_BEARING_AVAILABLE, null);
    }

    public void handleFakeGPS5(View view) {
        onLocationChanged(240, 50, FieldGps.NO_BEARING_AVAILABLE, null);
    }

    public void handleFakeGPS6(View view) {
        onLocationChanged(240, -50, FieldGps.NO_BEARING_AVAILABLE, null);
    }

}