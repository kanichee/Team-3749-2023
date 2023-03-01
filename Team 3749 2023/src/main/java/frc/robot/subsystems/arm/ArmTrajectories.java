package frc.robot.subsystems.arm;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrajectoryConfig;
import edu.wpi.first.math.trajectory.TrajectoryGenerator;
import frc.robot.utils.Constants;

public class ArmTrajectories {
    /**
     * Create trajectory
     * 
     * @param waypoints
     * @return trajectory
     */
    private static Trajectory createTrajectory(Pose2d[] waypoints, boolean isReversed) {
        if (isReversed) {
            // iterate through waypoints from last to first
            Collections.reverse(Arrays.asList(waypoints));

            // reverse pose for each waypoint (subtract pi)
            for (int i = 0; i < waypoints.length; i++)
                waypoints[i] = waypoints[i].transformBy(
                    new Transform2d(
                        new Translation2d(0.0, 0.0),
                        new Rotation2d(Math.PI)));
        }

        // generate trajectory
        return TrajectoryGenerator.generateTrajectory(
                List.of(waypoints),
                new TrajectoryConfig(
                        Constants.Arm.maxSpeedMPS,
                        Constants.Arm.maxAccelerationMPS));
    }

    public static Trajectory getStingTrajectory(boolean isReversed) {
        Pose2d[] waypoints = new Pose2d[] {
                new Pose2d(0.3, -0.2, new Rotation2d(Math.PI / 4)),
                new Pose2d(0.5, 0.7, new Rotation2d(Math.PI / 2)),
        };

        return createTrajectory(waypoints, isReversed);
    }

    public static Trajectory getTopNodeTrajectory(boolean isReversed) {
        Pose2d[] waypoints = new Pose2d[] {
                new Pose2d(0.5, 0.7, new Rotation2d(Math.PI / 8)),
                new Pose2d(1.33, 1.0, new Rotation2d(Math.PI / 8)),
        };

        return createTrajectory(waypoints, isReversed);
    }

    public static Trajectory getMidNodeTrajectory(boolean isReversed) {
        Pose2d[] waypoints = new Pose2d[] {
                new Pose2d(0.5, 0.7, new Rotation2d(Math.PI / 3)),
                new Pose2d(0.9, 0.7, new Rotation2d(Math.PI / 3)),
        };

        return createTrajectory(waypoints, isReversed);
    }

    public static Trajectory getDoubleSubstationTrajectory(boolean isReversed) {
        Pose2d[] waypoints = new Pose2d[] {
                new Pose2d(0.3, -0.2, new Rotation2d(Math.PI / 4)),
                new Pose2d(0.75, 0.75, new Rotation2d(Math.PI / 2)),
        };

        return createTrajectory(waypoints, isReversed);
    }
}
