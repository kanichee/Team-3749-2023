package frc.robot.commands.swerve;

import java.util.List;

import com.pathplanner.lib.PathConstraints;
import com.pathplanner.lib.PathPlanner;
import com.pathplanner.lib.PathPlannerTrajectory;
import com.pathplanner.lib.commands.FollowPathWithEvents;
import com.pathplanner.lib.commands.PPSwerveControllerCommand;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.DriverStation.Alliance;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import frc.robot.commands.arm.MoveArm;
import frc.robot.commands.vision.AlignApriltag;
import frc.robot.subsystems.arm.Arm;
import frc.robot.subsystems.arm.ArmTrajectories;
import frc.robot.subsystems.intake.ArmIntake;
import frc.robot.subsystems.leds.LEDs;
import frc.robot.subsystems.swerve.Swerve;
import frc.robot.subsystems.vision.Limelight;
import frc.robot.utils.Constants;
import frc.robot.utils.Constants.Arm.ArmSetpoints;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;

/***
 * @author Noah Simon
 *         A class to return command sequences for auto
 *         Path Planner is used to create sequences, see docs below
 *         {@link} https://github.com/mjansen4857/pathplanner/wiki/PathPlannerLib:-Java-Usage
 */
public final class AutoCommands {
    /***
     * @param swerve      the subsystem object. Do not make a new instance
     * @param trajectory  a viable trajectory object containing information
     *                    about where the robot should go
     * @param isFirstPath if it is, it will reset odometry at its current
     *                    position
     * @return a SwerveControllerCommand based on the trajectory
     * @summary takes a trajectory and moves on it
     */
    private static Command followTrajectoryCommand(PathPlannerTrajectory traj, boolean isFirstPath,
            Swerve swerve) {
        return new SequentialCommandGroup(
                new InstantCommand(() -> {
                    // Reset odometry for the first path you run during auto
                    if (isFirstPath) {
                        swerve.setFlipGyro(true);
                        swerve.resetGyro();
                        swerve.resetOdometry(traj.getInitialHolonomicPose());
                    }
                }),
                new PPSwerveControllerCommand(
                        traj,
                        () -> swerve.getPose(), // Pose supplier
                        Constants.DriveConstants.kDriveKinematics, // SwerveDriveKinematics
                        new PIDController(1.1, 0, 0), // X controller. Tune these values for your robot. Leaving them 0
                                                      // will only use feedforwards.
                        new PIDController(1.1, 0, 0), // Y controller (usually the same values as X controller)
                        new PIDController(2, 0, 0), // Rotation controller. Tune these values for your robot. Leaving
                                                    // them 0 will only use feedforwards.
                        swerve::setModuleStates, // Module states consumer
                        false, // Should the path be automatically mirrored depending on alliance color.
                               // Optional, defaults to true
                        swerve // Requires this drive subsystem
                ));
    }

    public static Command getPlaceTop(Arm arm, ArmTrajectories armTrajectories, ArmIntake armIntake, LEDs leds) {
        return new SequentialCommandGroup(
                Commands.waitSeconds(0.1),
                new MoveArm(arm, armTrajectories, armIntake, leds, ArmSetpoints.PLACE_TOP),
                Commands.waitSeconds(0.75),
                Commands.run(() -> armIntake.setVoltage(Constants.ArmIntake.releaseConeVoltage))
                        .withTimeout(0.15),
                Commands.runOnce(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1));
    }

    public static Command getBottomTwoPiece(Swerve swerve, Arm arm, ArmTrajectories armTrajectories,
            ArmIntake armIntake,
            Limelight limelight,
            LEDs leds) {
        List<PathPlannerTrajectory> pathGroup = null;

        if (DriverStation.getAlliance() == Alliance.Blue) {
            pathGroup = PathPlanner.loadPathGroup("BLUE - BOTTOM 2 Piece", new PathConstraints(2.5, 2.5));

        } else if (DriverStation.getAlliance() == Alliance.Red) {
            pathGroup = PathPlanner.loadPathGroup("RED - BOTTOM 2 Piece", new PathConstraints(2.5, 2.5));
        }
        Command path_1 = new FollowPathWithEvents(followTrajectoryCommand(pathGroup.get(0), true, swerve),
                pathGroup.get(0).getMarkers(), Constants.AutoConstants.eventMap);

        return new SequentialCommandGroup(
                getPlaceTop(arm, armTrajectories, armIntake, leds),
                path_1,
                new AlignApriltag(swerve, limelight).withTimeout(1),
                Commands.run(() -> armIntake.setVoltage(Constants.ArmIntake.releaseConeVoltage))
                        .withTimeout(0.15),

                Commands.runOnce(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1),
                new MoveArm(arm, armTrajectories, armIntake, leds, ArmSetpoints.STOW));

    }

    public static Command getTopTwoPiece(Swerve swerve, Arm arm, ArmTrajectories armTrajectories,
            ArmIntake armIntake,
            Limelight limelight,
            LEDs leds) {
        List<PathPlannerTrajectory> pathGroup = null;

        if (DriverStation.getAlliance() == Alliance.Blue) {
            pathGroup = PathPlanner.loadPathGroup("BLUE - TOP 2 Piece", new PathConstraints(2.5, 2.5));

        } else if (DriverStation.getAlliance() == Alliance.Red) {
            pathGroup = PathPlanner.loadPathGroup("RED - TOP 2 Piece", new PathConstraints(2.5, 2.5));
        }
        Command path_1 = new FollowPathWithEvents(followTrajectoryCommand(pathGroup.get(0), true, swerve),
                pathGroup.get(0).getMarkers(), Constants.AutoConstants.eventMap);

        return new SequentialCommandGroup(
                getPlaceTop(arm, armTrajectories, armIntake, leds),
                path_1,
                new AlignApriltag(swerve, limelight).withTimeout(1),
                Commands.run(() -> armIntake.setVoltage(Constants.ArmIntake.releaseConeVoltage))
                        .withTimeout(0.15),

                Commands.run(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1),
                new MoveArm(arm, armTrajectories, armIntake, leds, ArmSetpoints.STOW));
    }

    public static Command getTopTwoPieceCharge(Swerve swerve, Arm arm, ArmTrajectories armTrajectories,
            ArmIntake armIntake, Limelight limelight,
            LEDs leds) {
        List<PathPlannerTrajectory> pathGroup = null;

        if (DriverStation.getAlliance() == Alliance.Blue) {
            pathGroup = PathPlanner.loadPathGroup("BLUE - TOP 2 Piece", new PathConstraints(2.5, 2.5));

        } else if (DriverStation.getAlliance() == Alliance.Red) {
            pathGroup = PathPlanner.loadPathGroup("RED - TOP 2 Piece", new PathConstraints(2.5, 2.5));
        }
        Command path_1 = new FollowPathWithEvents(followTrajectoryCommand(pathGroup.get(0), true, swerve),
                pathGroup.get(0).getMarkers(), Constants.AutoConstants.eventMap);
        Command path_2 = new FollowPathWithEvents(followTrajectoryCommand(pathGroup.get(1), true, swerve),
                pathGroup.get(1).getMarkers(), Constants.AutoConstants.eventMap);

        double goalHeading = DriverStation.getAlliance() == Alliance.Blue ? 180 : 0;
        return new SequentialCommandGroup(
                getPlaceTop(arm, armTrajectories, armIntake, leds),
                path_1,
                new AlignApriltag(swerve, limelight).withTimeout(1),
                Commands.run(() -> armIntake.setVoltage(Constants.ArmIntake.releaseConeVoltage))
                        .withTimeout(0.25),
                Commands.runOnce(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1),
                path_2,
                new AutoBalancingPID(swerve, goalHeading));
    }

    public static Command getMiddleCharge(Swerve swerve, Arm arm, ArmTrajectories armTrajectories,
            ArmIntake armIntake,
            Limelight limelight,
            LEDs leds) {
        PathPlannerTrajectory first = null;

        if (DriverStation.getAlliance() == Alliance.Blue) {
            first = PathPlanner.loadPath("BLUE - MIDDLE Charge", new PathConstraints(2.5, 2.5));

        } else if (DriverStation.getAlliance() == Alliance.Red) {
            first = PathPlanner.loadPath("RED - MIDDLE Charge", new PathConstraints(2.5, 2.5));
        }

        Command path_1 = new FollowPathWithEvents(followTrajectoryCommand(first, true, swerve),
                first.getMarkers(), Constants.AutoConstants.eventMap);

        double goalHeading = DriverStation.getAlliance() == Alliance.Blue ? 180 : 0;

        return new SequentialCommandGroup(
                getPlaceTop(arm, armTrajectories, armIntake, leds),
                Commands.runOnce(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1),
                path_1,
                new AutoBalancingPID(swerve, goalHeading));
    }

    public static Command getBottomTwoPieceCharge(Swerve swerve, Arm arm, ArmTrajectories armTrajectories,
            ArmIntake armIntake,
            Limelight limelight,
            LEDs leds) {
        List<PathPlannerTrajectory> pathGroup = null;

        if (DriverStation.getAlliance() == Alliance.Blue) {
            pathGroup = PathPlanner.loadPathGroup("BLUE - BOTTOM 2 Piece", new PathConstraints(2.5, 2.5));

        } else if (DriverStation.getAlliance() == Alliance.Red) {
            pathGroup = PathPlanner.loadPathGroup("RED - BOTTOM 2 Piece", new PathConstraints(2.5, 2.5));
        }
        Command path_1 = new FollowPathWithEvents(followTrajectoryCommand(pathGroup.get(0), true, swerve),
                pathGroup.get(0).getMarkers(), Constants.AutoConstants.eventMap);
        Command path_2 = new FollowPathWithEvents(followTrajectoryCommand(pathGroup.get(1), true, swerve),
                pathGroup.get(1).getMarkers(), Constants.AutoConstants.eventMap);

        double goalHeading = DriverStation.getAlliance() == Alliance.Blue ? 0 : 180;
        return new SequentialCommandGroup(
                getPlaceTop(arm, armTrajectories, armIntake, leds),
                Commands.runOnce(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1),

                path_1,
                new AlignApriltag(swerve, limelight).withTimeout(1),
                Commands.run(() -> armIntake.setVoltage(Constants.ArmIntake.releaseConeVoltage))
                        .withTimeout(0.1),
                Commands.runOnce(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1),
                path_2,
                new AutoBalancingPID(swerve, goalHeading));
    }

    public static Command get1Piece(Swerve swerve, Arm arm, ArmTrajectories armTrajectories,
            ArmIntake armIntake,
            Limelight limelight,
            LEDs leds) {

        return new SequentialCommandGroup(
                getPlaceTop(arm, armTrajectories, armIntake, leds),
                Commands.runOnce(() -> armIntake.setVoltage(Constants.ArmIntake.idleVoltage)).withTimeout(0.1),
                new MoveArm(arm, armTrajectories, armIntake, leds, ArmSetpoints.STOW));
    }

    public static Command getAprilTagAlign(Swerve swerve, Arm arm, ArmTrajectories armTrajectories,
            ArmIntake armIntake,
            Limelight limelight,
            LEDs leds) {
        return new SequentialCommandGroup(
                getPlaceTop(arm, armTrajectories, armIntake, leds));
    }


}