package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Servo;

/**
 * Main Teleop
 *
 * 3 October 2020
 */

@TeleOp(name = "Main Odometry Teleop")
public class MainTeleopOdometry extends LinearOpMode{
    private DcMotor motorFrontRight, motorFrontLeft, motorBackLeft, motorBackRight;

    private CRServo leftConveyor, rightConveyor, intake;
    private DcMotor outtakeRight, outtakeLeft, wobbleArm;
    private Servo flipper, wobbleClaw;

    private BNO055IMU imu;

    private IMURobot robot;

    //Figures for ring elevator calculations
    private static final double PINION_CIRCUMFERENCE = 2.57;
    private static final double ELEVATOR_HEIGHT = 5.0;
    private static final double PINION_REVOLUTIONS = ELEVATOR_HEIGHT/PINION_CIRCUMFERENCE;
    private static final double SERVO_RPM = 50.0;
    private static final double ELEVATOR_TIME = PINION_REVOLUTIONS/SERVO_RPM * 60;

    //Figures for Odometry
    final double WHEEL_DIAMETER = 1.5;
    final double WHEEL_CIRCUMFERENCE = WHEEL_DIAMETER * Math.PI;
    final double COUNTS_PER_REVOLUTION = 1280;
    //final double COUNTS_PER_INCH = 307.699557;
    final double COUNTS_PER_INCH = COUNTS_PER_REVOLUTION/WHEEL_CIRCUMFERENCE;



    //Odometry encoder wheels
    DcMotor verticalRight, verticalLeft, horizontal;

    OdometryGlobalCoordinatePosition globalPositionUpdate;

    @Override
    public void runOpMode() throws InterruptedException {
        motorFrontRight = hardwareMap.dcMotor.get("FR");
        motorFrontLeft = hardwareMap.dcMotor.get("FL");
        motorBackLeft = hardwareMap.dcMotor.get("BL");
        motorBackRight = hardwareMap.dcMotor.get("BR");

        //intake and conveyor
        intake = hardwareMap.crservo.get("intake");
        leftConveyor = hardwareMap.crservo.get("leftConveyor");
        rightConveyor = hardwareMap.crservo.get("rightConveyor");

        //wobble and flipper
        wobbleArm = hardwareMap.dcMotor.get("wobbleArm");
        wobbleClaw = hardwareMap.servo.get("wobbleClaw");
        flipper = hardwareMap.servo.get("flipper");

        //launcher  //Feb 7 - Jeff commmented out these motor definitions
        outtakeRight = hardwareMap.dcMotor.get("outtakeRight");
        outtakeLeft = hardwareMap.dcMotor.get("outtakeLeft");
        //Jeff added
        //outtakeLeft=hardwareMap.get(DcMotor.class, "outtakeLeft");
        //outtakeRight=hardwareMap.get(DcMotor.class, "outtakeRight");
        outtakeLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        outtakeLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        outtakeRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        //Encoders
        /*
        verticalLeft = hardwareMap.dcMotor.get("FL");
        verticalRight = hardwareMap.dcMotor.get("FR");
        horizontal = hardwareMap.dcMotor.get("BL");
         */
        horizontal = hardwareMap.dcMotor.get("outtakeRight");
        verticalLeft = hardwareMap.dcMotor.get("wobbleArm");
        verticalRight = hardwareMap.dcMotor.get("encoderFree");

        //Initialize imu
        imu = hardwareMap.get(BNO055IMU.class, "imu");

        //reverse the needed motors
        motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        motorBackRight.setDirection(DcMotor.Direction.REVERSE);

        leftConveyor.setDirection(CRServo.Direction.REVERSE);

        motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        robot = new IMURobot(motorFrontRight, motorFrontLeft, motorBackRight, motorBackLeft,
                imu, wobbleArm, wobbleClaw, leftConveyor, rightConveyor, flipper, intake,
                outtakeRight, outtakeLeft, this);

        robot.setupRobot();//calibrate IMU, set any required parameters

        double powerMod;
        double wobbleMod;

        waitForStart();

        globalPositionUpdate = new OdometryGlobalCoordinatePosition(verticalLeft, verticalRight, horizontal, COUNTS_PER_INCH, 75);
        Thread positionThread = new Thread(globalPositionUpdate);
        positionThread.start();


        while(opModeIsActive()){

            /*
            Checks if right bumper is pressed. If so, power is reduced
             */

            if(gamepad1.right_bumper){
                powerMod = 0.5;
            }else{
                powerMod = 1.0;
            }

            //everything intake

            //changes direction of intake
            if (gamepad1.a){
                robot.intakeReverse();
                robot.conveyorReverse();
            }else{
                //turns on intake
                if (gamepad1.left_trigger > 0.3){
                    robot.intakeOn();
                    robot.conveyorOn();

                //turns off intake
                }else{
                    robot.intakeOff();
                    robot.conveyorOff();
                }
            }

            //Ring flipper
            //Run by a servo, 1 is fully "flipped" position, 0 is fully "retracted" position
            //Hold down b button to flip ring out

            if(gamepad2.b){
                flipper.setPosition(1);
            }

            if(gamepad2.a){
                flipper.setPosition(0);
            }

            telemetry.addData("flipper position", flipper.getPosition());

            //everything shooting

            if (gamepad2.right_bumper){
                shootPowerShot();
            }
            if (gamepad2.right_trigger > 0.3){
                shootGoal();
            }

            //everything wobble

            if(gamepad2.left_bumper){
                wobbleMod = 1.0;
            }else{
                wobbleMod = .15;
            }

            wobbleArm.setPower(gamepad2.left_stick_y * wobbleMod );

            if(gamepad2.x){
                wobbleClaw.setPosition(0);
            }

            if(gamepad2.y){
                wobbleClaw.setPosition(1);
            }

            if(gamepad1.left_bumper){
                odometryDriveToPosAngular(0,20,0);
            }
            if(gamepad1.b){
                odometryDriveToPosAngular(20,0,0);
            }
            if (gamepad1.y){
                odometryDriveToPosAngular(20,20,0);
            }
            if(gamepad1.x){
                odometryNormalizeAngleNew();
            }

            //everything driving
            //Mecanum drive using trig


            double angle = Math.atan2(gamepad1.right_stick_y, gamepad1.right_stick_x) - (Math.PI / 4);
            double r = Math.hypot(gamepad1.right_stick_x, gamepad1.right_stick_y);
            double rotation = gamepad1.left_stick_x;

            double powerOne = r * Math.sin(angle);
            double powerTwo = r * Math.cos(angle);

            motorFrontLeft.setPower((powerOne - (rotation))*powerMod);
            motorFrontRight.setPower((powerTwo + (rotation))*powerMod);
            motorBackLeft.setPower((powerTwo - (rotation))*powerMod);
            motorBackRight.setPower((powerOne + (rotation))*powerMod);

            telemetry.addData("X Position", globalPositionUpdate.returnXCoordinate() / COUNTS_PER_INCH);
            telemetry.addData("Y Position", globalPositionUpdate.returnYCoordinate() / COUNTS_PER_INCH);
            telemetry.addData("test", globalPositionUpdate.returnYCoordinate());
            telemetry.addData("Orientation (Degrees)", globalPositionUpdate.returnOrientation());

            telemetry.addData("Vertical left encoder position", verticalLeft.getCurrentPosition());
            telemetry.addData("Vertical right encoder position", verticalRight.getCurrentPosition());
            telemetry.addData("horizontal encoder position", horizontal.getCurrentPosition());

            telemetry.addData("Thread Active", positionThread.isAlive());

            telemetry.update();
            idle();
        }
        globalPositionUpdate.stop();

    }

    public void robotStrafe (double power, double angle){
        //restart angle tracking
        robot.resetAngle();

        //convert direction (degrees) into radians
        double newDirection = angle * Math.PI/180 + Math.PI/4;
        //calculate powers needed using direction
        double leftPower = Math.cos(newDirection) * power;
        double rightPower = Math.sin(newDirection) * power;

        //while(opMode.opModeIsActive()){
        //Get a correction
        double correction = robot.getCorrection();
        //Use the correction to adjust robot power so robot faces straight
        robot.correctedTankStrafe(leftPower, rightPower, correction);
        //}
    }

    public double getOdometryCorrection(double currentAngle){
        double gain = 0.1; //0.01
        //Get the current angle of the robot
        double angle = globalPositionUpdate.returnOrientation();
        double correction;

        //Use the angle to calculate the correction
        if (currentAngle == angle){
            //If angle = 0, robot is moving straight; no correction needed
            correction = 0;
        }else{
            //If angle != 0, robot is not moving straight
            //Correction is negative angle (to move the robot in the opposite direction)
            //multiplied by gain; the gain is the sensitivity to angle
            //We have determined that .1 is a good gain; higher gains result in overcorrection
            //Lower gains are ineffective
            angle = angle - currentAngle;
            correction = -angle*gain;
        }

        return correction;
    }

    public void odometrySetAngle(double angle){
        if (globalPositionUpdate.returnOrientation() < angle){
            robot.turnCounterClockwise(0.5);
            while (globalPositionUpdate.returnOrientation() < angle){
                telemetry.update();
            }
        }else if (globalPositionUpdate.returnOrientation() > angle){
            robot.turnClockwise(0.5);
            while (globalPositionUpdate.returnOrientation() > angle){
                telemetry.update();
            }
        }
        robot.completeStop();
    }

    public void odometryDriveToPosAngular (double xPos, double yPos, double direction) {
        //odometryNormalizeAngleNew();
        double distanceX = xPos - (globalPositionUpdate.returnXCoordinate() / COUNTS_PER_INCH);//0
        double distanceY = yPos - (globalPositionUpdate.returnYCoordinate() / COUNTS_PER_INCH);//0
        double angle = Math.atan2(distanceY,distanceX)-(Math.PI/4);
        double distance = Math.hypot(distanceX,distanceY);//0

        double powerOne = 0.5 * Math.sin(angle);//all be 0.4
        double powerTwo = 0.5 * Math.cos(angle);//same here

        while (distance > 5){//can assume robot faces straight up?
            distanceX = xPos - (globalPositionUpdate.returnXCoordinate() / COUNTS_PER_INCH);
            distanceY = yPos - (globalPositionUpdate.returnYCoordinate() / COUNTS_PER_INCH);
            distance = Math.hypot(distanceX,distanceY);
            motorFrontLeft.setPower(powerOne);
            motorFrontRight.setPower(powerTwo);
            motorBackLeft.setPower(powerTwo);
            motorBackRight.setPower(powerOne);
            telemetry.addData("Distance: ", distance);
            telemetry.addData("DistanceX: ", distanceX);
            telemetry.addData("DistanceY: ", distanceY);
            telemetry.update();
        }
        robot.completeStop();
        //odometrySetAngle(direction);
    }

    public void odometryDriveToPosCorrection (double xPos, double yPos, double direction) {
        double distanceX = xPos - (globalPositionUpdate.returnXCoordinate());//20
        double distanceY = yPos - (globalPositionUpdate.returnYCoordinate());//0
        double angle = Math.atan2(distanceY,distanceX)-(Math.PI/4)+Math.toRadians(globalPositionUpdate.returnOrientation());
        double distance = Math.hypot(distanceX,distanceY);//20

        double powerOne = 0.4 * Math.sin(angle);//all be 0.4
        double powerTwo = 0.4 * Math.cos(angle);//same here

        double currentAngle = globalPositionUpdate.returnOrientation();

        while (distance > 5){//can assume robot faces straight up?
            distanceX = xPos - (globalPositionUpdate.returnXCoordinate());
            distanceY = yPos - (globalPositionUpdate.returnYCoordinate());
            distance = Math.hypot(distanceX,distanceY);
            double correction = getOdometryCorrection(currentAngle);
            motorFrontLeft.setPower(powerOne + correction);
            motorFrontRight.setPower(powerTwo - correction);
            motorBackLeft.setPower(powerTwo + correction);
            motorBackRight.setPower(powerOne - correction);
            telemetry.addData("Distance: ", distance);
            telemetry.addData("DistanceX: ", distanceX);
            telemetry.addData("DistanceY: ", distanceY);
            telemetry.update();
        }
        robot.completeStop();
        //odometrySetAngle(direction);
    }

    public void odometryDriveToPos (double xPos, double yPos) throws InterruptedException{
        odometryNormalizeAngle();
        double C = 0;
        while (Math.abs(globalPositionUpdate.returnXCoordinate()/COUNTS_PER_INCH - xPos) > 1) {//while distance to destination > than threshold
            double angle = 0;
            if(globalPositionUpdate.returnXCoordinate() < xPos)
                angle = 90;
            else
                angle = -90;
            robotStrafe(.4, angle);

            if(Math.abs(globalPositionUpdate.returnXCoordinate()/COUNTS_PER_INCH - xPos) <= 1){
                break;
            }
        }
        robot.completeStop();
        Thread.sleep(500);
        odometryNormalizeAngle();

        while (Math.abs(globalPositionUpdate.returnYCoordinate()/COUNTS_PER_INCH - yPos) > 1) {
            double power = 0;
            if(globalPositionUpdate.returnXCoordinate() < xPos)
                power = -0.4;
            else
                power = 0.4;
            robotStrafe(power, 0);

            if(Math.abs(globalPositionUpdate.returnYCoordinate()/COUNTS_PER_INCH - yPos) <= 1){
                break;
            }
        }
        robot.completeStop();
        Thread.sleep(500);
    }

    public void odometryNormalizeAngle() {
        while(Math.abs(globalPositionUpdate.returnOrientation()) < -5 || Math.abs(globalPositionUpdate.returnOrientation()) > 5){
            if(globalPositionUpdate.returnOrientation() > 0 + 5){// thing > 0 + threshold
                //Change threshold above ~~~~~~~~~~~~~~~~
                robot.turnCounterClockwise(0.1);
            }else if(globalPositionUpdate.returnOrientation() > 0 - 5){// thing > 0 - threshold
                //Here too ~~~~~~~~~~~~~~~~~
                robot.turnClockwise(0.1);
            }else{
                break;
            }
        }
        robot.completeStop();
    }

    public void odometryNormalizeAngleNew() {
        if (globalPositionUpdate.returnOrientation() > 5){
            robot.turnCounterClockwise(0.5);
            while (globalPositionUpdate.returnOrientation() > 5){
                telemetry.addData("Angle: ", globalPositionUpdate.returnOrientation());
                telemetry.update();
            }
            robot.completeStop();
        }else if (globalPositionUpdate.returnOrientation() < 5){
            robot.turnClockwise(0.5);
            while (globalPositionUpdate.returnOrientation() < 5){
                telemetry.addData("Angle: ", globalPositionUpdate.returnOrientation());
                telemetry.update();
            }
            robot.completeStop();
        }
    }

    public void shootPowerShot() throws InterruptedException{
        //Shot 1
        odometryDriveToPos(-39.85,62.9);
        robot.shootRingsPower();
        //Shot 2
        odometryDriveToPos(-50.7,49.0);
        robot.shootRingsPower();
        //Shot 3
        odometryDriveToPos(-39.8,62.8);
        robot.shootRingsPower();
    }

    public void shootGoal() throws InterruptedException{
        odometryDriveToPos(-15.5,67.9);
        robot.shootRings();
    }
}