// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import frc.robot.commands.ArmMoveUpCommand;
import frc.robot.commands.
import frc.robot.subsystems.Arm;
import frc.robot.utils.POV;
import frc.robot.utils.Xbox;

public class RobotContainer {

  // Controllers
  private final Arm arm = new Arm();
  private final ArmMoveUpCommand armMoveUpCommand = new ArmMoveUpCommand(arm);
  private final ArmMoveDownCommand armMoveDownCommand = new armMoveDownCommand(arm);
  private final Arm
  
  private final Xbox pilot = new Xbox(0);
  private final Xbox operator = new Xbox(1);

  private final POV pilotPOV = new POV(pilot);
  private final POV operatorPOV = new POV(operator);

  // Subsystems

  // Commands

  public RobotContainer() {
    configureButtonBindings();
    configureDefaultCommands();
  }

  private void configureDefaultCommands() {}

  private void configureButtonBindings()
  {
    
  }

  public Command getAutonomousCommand() {
    return Commands.print("No autonomous command configured");
  }  
}
