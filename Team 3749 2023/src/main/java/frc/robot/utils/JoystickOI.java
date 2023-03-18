package frc.robot.utils;

import java.util.Map;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.shuffleboard.BuiltInLayouts;
import edu.wpi.first.wpilibj.shuffleboard.Shuffleboard;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardLayout;
import edu.wpi.first.wpilibj.shuffleboard.ShuffleboardTab;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.commands.arm.MoveArm;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.intake.ArmIntake;
import frc.robot.subsystems.intake.SideIntake;
import frc.robot.subsystems.swerve.Swerve;
import frc.robot.utils.Constants.Arm.ArmSetpoints;

public class JoystickOI {
    private static String[] lastJoystickNames = new String[] { "", "", "", "", "", "" };

    private Xbox pilot;
    private Xbox operator;

    private Swerve swerve;
    private ArmIntake armIntake;
    private SideIntake sideIntake;
    private Arm arm;

    public JoystickOI(Xbox pilot, Xbox operator, Swerve swerve, ArmIntake armIntake, SideIntake sideIntake, Arm arm) {
        this.pilot = pilot;
        this.operator = operator;
        this.swerve = swerve;
        this.armIntake = armIntake;
        this.sideIntake = sideIntake;
        this.arm = arm;
    }

    public static boolean didJoysticksChange() {
        boolean joysticksChanged = false;
        for (int port = 0; port < DriverStation.kJoystickPorts; port++) {
            String name = DriverStation.getJoystickName(port);
            if (!name.equals(lastJoystickNames[port])) {
                joysticksChanged = true;
                lastJoystickNames[port] = name;
            }
        }
        return joysticksChanged;
    }

    public void getButtonBindings() {

        // if both xbox controllers are connected
        if (DriverStation.isJoystickConnected(1)) {
            pilotOperatorBindings();

        // if only one xbox controller is connected
        } else if (DriverStation.isJoystickConnected(0)) {
            pilotBindings();

        // if no joysticks are connected (ShuffleBoard buttons)
        } else {
            noJoystickBindings();

        }
    }

    public void pilotOperatorBindings() {
        // arm setpoints (buttons)
        operator.a().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.PLACE_TOP));
        operator.b().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.PLACE_MID));
        operator.x().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.GROUND_INTAKE));

        // arm setpoints (bumpers)
        operator.rightBumper().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.STING));
        operator.leftBumper().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.DOUBLE_SUBSTATION));

        operator.rightTriggerWhileHeld(() -> armIntake.setVoltage(Constants.ArmIntake.releaseObjectVoltage));
        operator.leftTriggerWhileHeld(() -> armIntake.setVoltage(Constants.ArmIntake.intakeVoltage));

        pilot.y().onTrue(Commands.runOnce(() -> sideIntake.toggleLiftSetpoint(), sideIntake));

        pilot.rightTriggerWhileHeld(() -> sideIntake.setIntakeVoltage(Constants.ArmIntake.releaseObjectVoltage),
                sideIntake);
        pilot.leftTriggerWhileHeld(() -> sideIntake.setIntakeVoltage(Constants.ArmIntake.intakeVoltage),
                sideIntake);

        // swerve button bindings
        pilot.backWhileHeld(() -> swerve.zeroHeading(), swerve);

        // swerve rotation cardinals
        pilot.povUp().whileTrue(Commands.run(() -> swerve.turnToRotation(0)));
        pilot.povLeft().whileTrue(Commands.run(() -> swerve.turnToRotation(270)));
        pilot.povDown().whileTrue(Commands.run(() -> swerve.turnToRotation(180)));
        pilot.povRight().whileTrue(Commands.run(() -> swerve.turnToRotation(90)));
    }

    public void pilotBindings() {
        // arm setpoints (buttons)
        pilot.a().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.PLACE_TOP));
        pilot.b().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.PLACE_MID));
        pilot.x().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.GROUND_INTAKE));
        pilot.y().onTrue(Commands.runOnce(() -> sideIntake.toggleLiftSetpoint(), sideIntake));

        // arm setpoints (bumpers)
        pilot.rightBumper().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.STING));
        pilot.leftBumper().onTrue(new MoveArm(arm, armIntake, ArmSetpoints.DOUBLE_SUBSTATION));

        // intake button bindings
        pilot.rightTriggerWhileHeld(() -> armIntake.setVoltage(Constants.ArmIntake.releaseObjectVoltage));
        pilot.leftTriggerWhileHeld(() -> armIntake.setVoltage(Constants.ArmIntake.intakeVoltage));

        // swerve button bindings
        pilot.backWhileHeld(() -> swerve.zeroHeading(), swerve);

        // swerve rotation cardinals
        pilot.povUp().whileTrue(Commands.run(() -> swerve.turnToRotation(0)));
        pilot.povLeft().whileTrue(Commands.run(() -> swerve.turnToRotation(270)));
        pilot.povDown().whileTrue(Commands.run(() -> swerve.turnToRotation(180)));
        pilot.povRight().whileTrue(Commands.run(() -> swerve.turnToRotation(90)));
    }

    public void noJoystickBindings() {
        ShuffleboardTab controlsTab = Shuffleboard.getTab("Controls");

            ShuffleboardLayout armCommands = controlsTab
                    .getLayout("Arm", BuiltInLayouts.kList)
                    .withSize(2, 2)
                    .withProperties(Map.of("Label position", "HIDDEN")); // hide labels for commands

            armCommands.add(new MoveArm(arm, armIntake, ArmSetpoints.PLACE_TOP));
            armCommands.add(new MoveArm(arm, armIntake, ArmSetpoints.PLACE_MID));
            armCommands.add(new MoveArm(arm, armIntake, ArmSetpoints.GROUND_INTAKE));
            armCommands.add(new MoveArm(arm, armIntake, ArmSetpoints.STING));
            armCommands.add(new MoveArm(arm, armIntake, ArmSetpoints.DOUBLE_SUBSTATION));

            ShuffleboardLayout sideIntakeCommands = controlsTab
                    .getLayout("Side Intake", BuiltInLayouts.kList)
                    .withSize(2, 2)
                    .withProperties(Map.of("Label position", "HIDDEN"));

            CommandBase liftSideIntake = Commands.runOnce(() -> sideIntake.toggleLiftSetpoint(), sideIntake);
            liftSideIntake.setName("Toggle Lift");
            CommandBase outakeSideIntake = Commands
                    .run(() -> sideIntake.setIntakeVoltage(Constants.ArmIntake.releaseObjectVoltage), sideIntake);
            outakeSideIntake.setName("Side Outake");
            CommandBase intakeSideIntake = Commands
                    .run(() -> sideIntake.setIntakeVoltage(Constants.ArmIntake.intakeVoltage), sideIntake);
            intakeSideIntake.setName("Side Intake");

            sideIntakeCommands.add(liftSideIntake);
            sideIntakeCommands.add(outakeSideIntake);
            sideIntakeCommands.add(intakeSideIntake);

            ShuffleboardLayout armIntakeCommands = controlsTab
                    .getLayout("arm", BuiltInLayouts.kList)
                    .withSize(2, 2)
                    .withProperties(Map.of("Label position", "HIDDEN"));

            CommandBase outakeArmIntake = Commands
                    .run(() -> armIntake.setVoltage(Constants.ArmIntake.releaseObjectVoltage), sideIntake);
            outakeArmIntake.setName("Arm Outake");
            CommandBase intakeArmIntake = Commands.run(() -> armIntake.setVoltage(Constants.ArmIntake.intakeVoltage),
                    sideIntake);
            intakeArmIntake.setName("Arm Intake");

            armIntakeCommands.add(outakeArmIntake);
            armIntakeCommands.add(intakeArmIntake);
    }
}
