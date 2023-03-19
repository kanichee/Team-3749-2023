package frc.robot.commands.swerve;

import frc.robot.utils.Constants;
import frc.robot.utils.SmartData;
import frc.robot.utils.Constants.VisionConstants;
import org.photonvision.targeting.PhotonTrackedTarget;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform2d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.CommandBase;
import frc.robot.subsystems.swerve.Swerve;
import frc.robot.subsystems.vision.Limelight;

/**
 * Determines a goal pose from the ArilTag fiducial ID and drives to it using the techniques from the MoveToPose command
 * 
 * @author Rohin Sood
 */
public class ApriltagAlign extends CommandBase {

    private static final Transform3d tagToGoal = new Transform3d(
            new Translation3d(0.75, 0.05, 0.0),
            new Rotation3d(0.0, 0.0, Math.PI));

    private final Swerve swerve;
    private final Limelight limelight;
    private final AprilTagFieldLayout aprilTagFieldLayout;

    private final ProfiledPIDController driveController = new ProfiledPIDController(
            0.0, 0.0, 0.0, new TrapezoidProfile.Constraints(0.0, 0.0));
    private final ProfiledPIDController turnController = new ProfiledPIDController(
            0.0, 0.0, 0.0, new TrapezoidProfile.Constraints(0.0, 0.0));
            
    private SmartData<Double> driveKP = new SmartData<Double>("Driving KP", 2.0);
    private SmartData<Double> turnKP = new SmartData<Double>("Turning KP", 2.6);

    private SmartData<Double> driveTolerance = new SmartData<Double>("Driving tolerance", 0.0); // 0.1
    private SmartData<Double> turnTolerance = new SmartData<Double>("Turning tolerance", 0.0); // 0.1

    private PhotonTrackedTarget lastTarget;

    private double driveErrorAbs;
    private double turnErrorAbs;

    private double driveVelocityScalar;

    private Pose2d goalPose;


    public ApriltagAlign(Swerve swerve, Limelight limelight) {
        this.swerve = swerve;
        this.limelight = limelight;
        this.aprilTagFieldLayout = limelight.getAprilTagFieldLayout();
        this.turnController.enableContinuousInput(-Math.PI, Math.PI);
        addRequirements(swerve);
    }

    @Override
    public void initialize() {
        
    }

    @Override
    public void execute() {

        driveController.setP(this.driveKP.get());
        turnController.setP(this.turnKP.get());
    
        if (goalPose == null){
            updateGoalPose();
            System.out.println("Goal Pose Is Null");
            return;
        }
        
        var robotPose2d = swerve.getPose();

        double currentDistance = robotPose2d.getTranslation().getDistance(this.goalPose.getTranslation());
        driveErrorAbs = currentDistance;
        driveVelocityScalar = driveController.calculate(driveErrorAbs, 0.0);
        if (driveController.atGoal())
            driveVelocityScalar = 0.0;

        double turnVelocity = turnController.calculate(
                robotPose2d.getRotation().getRadians(), this.goalPose.getRotation().getRadians());
        turnErrorAbs = Math.abs(robotPose2d.getRotation().minus(this.goalPose.getRotation()).getRadians());
        if (turnController.atGoal())
            turnVelocity = 0.0;

        Translation2d driveVelocity = new Pose2d(
                new Translation2d(),
                robotPose2d.getTranslation().minus(this.goalPose.getTranslation()).getAngle())
                .transformBy(translationToTransform(driveVelocityScalar, 0.0))
                .getTranslation();

        ChassisSpeeds chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(driveVelocity.getX(),
               driveVelocity.getY() , turnVelocity, robotPose2d.getRotation());
        // ChassisSpeeds chassisSpeeds = ChassisSpeeds.fromFieldRelativeSpeeds(0,
        // 0, turnVelocity, robotPose2d.getRotation());

        SwerveModuleState[] moduleStates = Constants.DriveConstants.kDriveKinematics
                .toSwerveModuleStates(chassisSpeeds);

        swerve.setModuleStates(moduleStates);

        SmartDashboard.putNumber("Drive X Velo", driveVelocity.getX());
        SmartDashboard.putNumber("Drive Y Velo", driveVelocity.getY());
        SmartDashboard.putNumber("Turn velo", turnVelocity);
        SmartDashboard.putNumber("Drive error", driveErrorAbs);
        SmartDashboard.putNumber("Turn error", turnErrorAbs);
        SmartDashboard.putNumber("DRIVE SCALAR", driveVelocityScalar);
    }

    @Override
    public void end(boolean interrupted) {
        swerve.stop();
    }

    @Override
    public boolean isFinished() {
        return atGoal();
    }

    /**
     * Creates a pure translating transform
     *
     * @param x The x componenet of the translation
     * @param y The y componenet of the translation
     * @return The resulting transform
     */
    public static Transform2d translationToTransform(double x, double y) {
        return new Transform2d(new Translation2d(x, y), new Rotation2d());
    }

    public boolean atGoal() {
        return driveController.atGoal() && turnController.atGoal();
    }

    private void updateGoalPose(){

        driveController.setP(driveKP.get());
        turnController.setP(turnKP.get());
        
        lastTarget = null;
        
        var robotPose2d = swerve.getPose();
        
        turnController.reset(robotPose2d.getRotation().getRadians());
        driveController.setTolerance(driveTolerance.get());
        turnController.setTolerance(turnTolerance.get());
        
        System.out.println("INTIIALIZE");
        var robotPose3d = new Pose3d(
            robotPose2d.getX(),
            robotPose2d.getY(),
            0.0,
            new Rotation3d(0.0, 0.0, robotPose2d.getRotation().getRadians()));

        var res = limelight.getLatestResult();
        if (res.hasTargets()) {
            // Find the tag we want to chase
            var targetOpt = res.getTargets().stream()
                    .filter(t -> !t.equals(lastTarget) && t.getPoseAmbiguity() <= .2 && t.getPoseAmbiguity() != -1)
                    .findFirst();

            try {
                SmartDashboard.putNumber("pose ambiguity", targetOpt.get().getPoseAmbiguity());
                SmartDashboard.putBoolean("is present", targetOpt.isPresent());
            } catch (Exception e) {
                System.out.println(e.toString());
            }

            if (targetOpt.isPresent()) {

                var target = targetOpt.get();
                // This is new target data, so recalculate the goal
                lastTarget = target;

                // Transform the robot's pose to find the camera's pose
                var cameraPose = robotPose3d.transformBy(VisionConstants.robot_to_cam);

                SmartDashboard.putNumberArray("Camera Pose",
                        new double[] { cameraPose.getX(), cameraPose.getY(), cameraPose.getZ() });

                // // Trasnform the camera's pose to the target's pose
                // var camToTarget = target.getBestCameraToTarget();
                // var targetPose = cameraPose.transformBy(camToTarget);

                // // Transform the tag's pose to set our goal
                // this.goalPose = targetPose.transformBy(tagToGoal).toPose2d();

                var targetId = target.getFiducialId();
                Pose3d aprilTagPose = aprilTagFieldLayout.getTagPose(targetId).get();
                SmartDashboard.putNumber("April Tag X", aprilTagPose.getX());
                SmartDashboard.putNumber("April Tag Y", aprilTagPose.getY());
                SmartDashboard.putNumber("April Tag Heading", aprilTagPose.getRotation().getAngle());
                this.goalPose = aprilTagPose.transformBy(tagToGoal).toPose2d();


                SmartDashboard.putNumber("Goal Pose X", goalPose.getX());
                SmartDashboard.putNumber("Goal Pose Y", goalPose.getY());
                SmartDashboard.putNumber("Goal Pose HEADING", goalPose.getRotation().getDegrees());
            }
        }
    }
}