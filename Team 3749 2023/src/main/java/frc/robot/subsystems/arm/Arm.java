package frc.robot.subsystems.arm;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.PrintCommand;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.commands.arm.ArmFollowTrajectory;
import frc.robot.utils.Constants;
import frc.robot.utils.Constants.Arm.ArmSetpoints;

/**
 * Double jointed arm subsystem built with 2 CANSparkMaxes at each joint and REV
 * Through Bore Encoders
 * 
 * TODO: increase tolerance on sting, change to brake when at setpoint
 * 
 * @author Rohin Sood
 */
public class Arm extends SubsystemBase {

    private final ArmDynamics dynamics = new ArmDynamics();

    private final CANSparkMax shoulderMotor = new CANSparkMax(Constants.Arm.right_shoulder_id, MotorType.kBrushless);
    private final DutyCycleEncoder shoulderAbsoluteEncoder = new DutyCycleEncoder(0);
    private final PIDController shoulderPIDController = new PIDController(Constants.Arm.shoulder_kP.get(), 0, 0);

    private final CANSparkMax elbowMotor = new CANSparkMax(Constants.Arm.left_elbow_id, MotorType.kBrushless);
    private final DutyCycleEncoder elbowAbsoluteEncoder = new DutyCycleEncoder(2);
    private final PIDController elbowPIDController = new PIDController(Constants.Arm.elbow_kP.get(), 0, 0);

    private final SendableChooser<Integer> presetChooser = new SendableChooser<Integer>();
    // safety stow
    private Translation2d position = new Translation2d(0.35, -0.2);
    private Constants.Arm.ArmSetpoints desiredSetpoint = ArmSetpoints.STOWED;
    private Constants.Arm.ArmSetpoints currentSetpoint = ArmSetpoints.STOWED;
    private boolean reached_sting = false;
    private boolean node_to_node = false;
    private boolean to_double_sub = false;
    private boolean top_intake_to_stowed = false;



    public Arm() {
        shoulderMotor.restoreFactoryDefaults();
        elbowMotor.restoreFactoryDefaults();
        // elbow offset is done in the get angle method
        shoulderAbsoluteEncoder.setPositionOffset(Constants.Arm.shoulder_offset);
        shoulderAbsoluteEncoder.setDistancePerRotation(360);
        elbowAbsoluteEncoder.setDistancePerRotation(360);

        elbowMotor.setInverted(true);
        shoulderMotor.setInverted(false);

        presetChooser.setDefaultOption("Stowed", 0);
        presetChooser.addOption("DS", 1);
        SmartDashboard.putData(presetChooser);

        shoulderPIDController.setTolerance(0);
        elbowPIDController.setTolerance(0);

        shoulderMotor.setIdleMode(IdleMode.kCoast);
        elbowMotor.setIdleMode(IdleMode.kCoast);
    }
    public void setArmPosition(Translation2d pos){
        position = pos;
    }

    private void moveArm() throws Exception {
        double shoulderAngle = ArmKinematics.inverse(position.getX(), position.getY()).getFirst();
        double elbowAngle = ArmKinematics.inverse(position.getX(), position.getY()).getSecond();

        double[] feedForwardOutput = dynamics.feedforward(VecBuilder.fill(shoulderAngle, elbowAngle)).getData();

        setShoulderVoltage(shoulderPIDController.calculate(getShoulderAngle(), shoulderAngle) + feedForwardOutput[0]);
        setElbowVoltage(elbowPIDController.calculate(getElbowAngle(), elbowAngle) + feedForwardOutput[1]);

        SmartDashboard.putNumber("SHOULDER SETPOINT", shoulderAngle);
        SmartDashboard.putNumber("ELBOW SETPOINT", elbowAngle);

        SmartDashboard.putNumber("SHOULDER FF", feedForwardOutput[0]);
        SmartDashboard.putNumber("ELBOW FF", feedForwardOutput[1]);

        SmartDashboard.putNumber("SHOULDER PID",
                shoulderPIDController.calculate(getShoulderAngle(), shoulderAngle) + feedForwardOutput[0]);
        SmartDashboard.putNumber("ELBOW PID",
                elbowPIDController.calculate(getElbowAngle(), elbowAngle) + feedForwardOutput[1]);
    }

    public Translation2d getArmCoordinate() {
        return ArmKinematics.forward(Math.toRadians(getShoulderAngle()), Math.toRadians(getElbowAngle()));
    }

    public void setShoulderVoltage(double voltage) {
        shoulderMotor.setVoltage(voltage);
    }

    public void setElbowVoltage(double voltage) {
        elbowMotor.setVoltage(voltage);
    }

    public void setShoulder(double percent) {
        boolean past_min_limit = getShoulderAngle() <= Constants.Arm.shoulder_min_angle && percent > 0;
        boolean past_max_limit = getShoulderAngle() >= Constants.Arm.shoulder_max_angle && percent < 0;
        if (past_min_limit || past_max_limit) {
            return;
        }
        shoulderMotor.set(percent);
    }

    public void setElbow(double percent) {
        elbowMotor.set(percent);
    }

    public double getShoulderAngle() {
        return shoulderAbsoluteEncoder.getDistance();
    }

    public double getElbowAngle() {
        return new Rotation2d(Math.toRadians(elbowAbsoluteEncoder.getAbsolutePosition() * 360 - 180))
                .rotateBy(new Rotation2d(Math.toRadians(180))).getDegrees() - Constants.Arm.elbow_offset;
    }

    public void setShoulderAngle(double angle) {
        shoulderPIDController.setSetpoint(angle);

        shoulderMotor.set(
                shoulderPIDController.calculate(
                        shoulderAbsoluteEncoder.getDistance()));
    }

    public void setElbowAngle(double angle) {
        elbowPIDController.setSetpoint(angle);

        elbowMotor.set(
                elbowPIDController.calculate(
                        getElbowAngle()));
    }

    public boolean getShoulderAtSetpoint() {
        return shoulderPIDController.atSetpoint();
    }

    public boolean getElbowAtSetpoint() {
        return elbowPIDController.atSetpoint();
    }

    public void setArmAngle(double shoulder_angle, double elbow_angle) {
        setShoulderAngle(shoulder_angle);
        setElbowAngle(elbow_angle);
    }

    public void setArmPreset() {
        double shoulder_angle;
        double elbow_angle;

        switch (presetChooser.getSelected()) {
            case (0):
                shoulder_angle = Constants.Arm.ShoulderSetpoints.STOWED.angle;
                elbow_angle = Constants.Arm.ElbowSetpoints.STOWED.angle;
                break;
            case (1):
                shoulder_angle = Constants.Arm.ShoulderSetpoints.DOUBLE_SUBSTATION.angle;
                elbow_angle = Constants.Arm.ElbowSetpoints.DOUBLE_SUBSTATION.angle;
                break;
            default:
                shoulder_angle = Constants.Arm.ShoulderSetpoints.STOWED.angle;
                elbow_angle = Constants.Arm.ElbowSetpoints.STOWED.angle;
                break;
        }

        setShoulderAngle(shoulder_angle);
        setElbowAngle(elbow_angle);
    }

    public void stop() {
        elbowMotor.stopMotor();
        shoulderMotor.stopMotor();
    }

    public void stopShoulder() {
        shoulderMotor.stopMotor();
    }

    public void stopElbow() {
        elbowMotor.stopMotor();
    }

    public void setDesiredSetpoint(ArmSetpoints setpoint){
        desiredSetpoint = setpoint;
    }
    // this one is private because it should only be updated through getCommand
    private  void setCurrentSetpoint(ArmSetpoints setpoint){
        desiredSetpoint = setpoint;
    }

    public ArmSetpoints getDesiredSetpoint(){
        return desiredSetpoint;
    }
    public ArmSetpoints getCurrentSetpoint(){
        return currentSetpoint;
    }

    public Command getTestCommand(){
        return new PrintCommand("TEST");
    }
    public Command getCommand(ArmSetpoints setpoint){
        setDesiredSetpoint(setpoint);
        Trajectory trajectory;

        if (setpoint == ArmSetpoints.CONE_TOP){
            if (setpoint == currentSetpoint){
                setCurrentSetpoint(ArmSetpoints.STOWED);
                trajectory =  ArmTrajectories.getTopNodeTrajectory(true).concatenate(ArmTrajectories.getStingTrajectory(true));
                return new ArmFollowTrajectory(this,trajectory);
            }
            setCurrentSetpoint(ArmSetpoints.CONE_TOP);
            trajectory = ArmTrajectories.getTopNodeTrajectory(false);
            if (currentSetpoint !=ArmSetpoints.STING){
                trajectory = ArmTrajectories.getStingTrajectory(false).concatenate(trajectory);
            }
            return new ArmFollowTrajectory(this, trajectory);

        }


        else if (setpoint == ArmSetpoints.CONE_MID){
            if (setpoint == currentSetpoint){
                setCurrentSetpoint(ArmSetpoints.STOWED);
                trajectory =  ArmTrajectories.getMidNodeTrajectory(true).concatenate(ArmTrajectories.getStingTrajectory(true));
                return new ArmFollowTrajectory(this,trajectory);
            }
            setCurrentSetpoint(ArmSetpoints.CONE_MID);
            trajectory = ArmTrajectories.getMidNodeTrajectory(false);
            if (currentSetpoint !=ArmSetpoints.STING){
                trajectory = ArmTrajectories.getStingTrajectory(false).concatenate(trajectory);
            }            return new ArmFollowTrajectory(this, trajectory);
        }

        else if (setpoint == ArmSetpoints.DOUBLE_SUBSTATION){
            if (setpoint == currentSetpoint){
                setCurrentSetpoint(ArmSetpoints.STOWED);
                trajectory =  ArmTrajectories.getDoubleSubstationTrajectory(true);
                return new ArmFollowTrajectory(this,trajectory);
            }
            setCurrentSetpoint(ArmSetpoints.DOUBLE_SUBSTATION);
            trajectory = ArmTrajectories.getDoubleSubstationTrajectory(false);
            return new ArmFollowTrajectory(this, trajectory);
        }

        // else if (setpoint == ArmSetpoints.TOP_INTAKE){
        //     if (setpoint == currentSetpoint){
        //          setCurrentSetpoint(ArmSetpoints.STOWED);
        //         trajectory =  ArmTrajectories.getFloorTrajectory(true);
        //         return new ArmFollowTrajectory(this,trajectory);
        //     }
        //     setCurrentSetpoint(ArmSetpoints.TOP_INTAKE);
        //     trajectory = ArmTrajectories.getFloorTrajectory(false);
        //     return new ArmFollowTrajectory(this, trajectory);
        // }

     // setpoint == ArmSetpoints.STING
        else if (setpoint == ArmSetpoints.STING) {
            if (setpoint == currentSetpoint){
                setCurrentSetpoint(ArmSetpoints.STOWED);
                trajectory =  ArmTrajectories.getStingTrajectory(true);
                return new ArmFollowTrajectory(this,trajectory);
            }
            setCurrentSetpoint(ArmSetpoints.STING);
            trajectory = ArmTrajectories.getStingTrajectory(false);
            return new ArmFollowTrajectory(this, trajectory);
        }

        else {
            return new PrintCommand("ERROR, NO TRAJECTORY FOR SETPOINT");
        }

        


        





    }

    private void updateSetpointBooleans() {
        reached_sting = getCurrentSetpoint() == ArmSetpoints.STING;

        node_to_node = (getCurrentSetpoint() == ArmSetpoints.CONE_TOP
                || getCurrentSetpoint() == ArmSetpoints.CONE_MID)
                && (getDesiredSetpoint() == ArmSetpoints.CONE_TOP
                        || getDesiredSetpoint() == ArmSetpoints.CONE_MID);

        to_double_sub = getCurrentSetpoint() == ArmSetpoints.DOUBLE_SUBSTATION
                || getDesiredSetpoint() == ArmSetpoints.DOUBLE_SUBSTATION;

        top_intake_to_stowed = (getCurrentSetpoint() == ArmSetpoints.TOP_INTAKE
                || getCurrentSetpoint() == ArmSetpoints.STOWED)
                && (getDesiredSetpoint() == ArmSetpoints.TOP_INTAKE
                        || getDesiredSetpoint() == ArmSetpoints.STOWED);

    }

    public void periodic() {
        updateSetpointBooleans();
        try {
            moveArm();

        } catch (Exception e) {
            System.out.println(e);

        }

        shoulderPIDController.setP(Constants.Arm.shoulder_kP.get());
        elbowPIDController.setP(Constants.Arm.elbow_kP.get());

        SmartDashboard.putNumber("ARM X",
                ArmKinematics.forward(Math.toRadians(getShoulderAngle()), Math.toRadians(getElbowAngle())).getX());
        SmartDashboard.putNumber("ARM Y",
                ArmKinematics.forward(Math.toRadians(getShoulderAngle()), Math.toRadians(getElbowAngle())).getY());

        SmartDashboard.putNumber("shoulder voltage", shoulderMotor.getBusVoltage() * shoulderMotor.getAppliedOutput());
        SmartDashboard.putNumber("elbow voltage", elbowMotor.getBusVoltage() * elbowMotor.getAppliedOutput());
    }

}
