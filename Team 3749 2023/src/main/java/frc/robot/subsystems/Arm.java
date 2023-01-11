package frc.robot.subsystems;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.utils.Constants;

/*
 * @author Bailey Say
 * 
 * Code for the arm of the robot. This does not include the claw at the end
 */

public class Arm extends SubsystemBase {
    private CANSparkMax neo_motor1 = new CANSparkMax(Constants.Arm.neo_motor1_port, MotorType.kBrushless); // Check if this is actually brushless later
    private CANSparkMax neo_motor2 = new CANSparkMax(Constants.Arm.neo_motor2_port, MotorType.kBrushless); // Check if this is actually brushless later
    private CANSparkMax neo_motor_elevator = new CANSparkMax(Constants.Arm.neo_motor_elevator_port, MotorType.kBrushless); // Check if this is actually brushless later

    public Arm() {

    }

    public void setSpeed(double speed, CANSparkMax neomotor) {
        neomotor.set(speed);
    }
    
    // getters for neos
    public CANSparkMax getNeo_motor1() {
        return neo_motor1;
    }

    public CANSparkMax getNeo_motor2() {
        return neo_motor2;
    }

    public CANSparkMax getNeo_motor_elevator() {
        return neo_motor_elevator;
    }
    
}
