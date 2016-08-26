// Copyright 2016 Google Inc. All Rights Reserved.
package com.google.copybara;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.copybara.config.Config;
import com.google.copybara.config.ConfigFile;
import com.google.copybara.config.SkylarkParser;
import com.google.copybara.folder.FolderDestination;
import com.google.copybara.folder.FolderDestinationOptions;
import com.google.copybara.git.GerritOptions;
import com.google.copybara.git.GitModule;
import com.google.copybara.git.GitOptions;
import com.google.copybara.util.console.Console;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Copybara tool main class.
 *
 * <p>Executes Copybara workflows independently from the environment that they are invoked from
 * (command-line, service).
 */
public class Copybara {

  protected static final ImmutableSet<Class<?>> BASIC_MODULES = ImmutableSet.<Class<?>>of(
      FolderDestination.Module.class,
      GitModule.class);

  private final SkylarkParser skylarkParser;

  public Copybara(SkylarkParser skylarkParser) {
    this.skylarkParser = Preconditions.checkNotNull(skylarkParser);
  }

  protected List<Option> getAllOptions(Map<String, String> environment) {
    return ImmutableList.of(
        new FolderDestinationOptions(),
        new GitOptions(environment.get("HOME")),
        new GerritOptions(),
        new WorkflowOptions());
  }

  public void run(Options options, ConfigFile configContents, String workflowName,
      Path baseWorkdir, @Nullable String sourceRef)
      throws RepoException, ValidationException, IOException {
    options.get(WorkflowOptions.class).setWorkflowName(workflowName);
    GeneralOptions generalOptions = options.get(GeneralOptions.class);
    Preconditions.checkArgument(!generalOptions.isValidate(), "Call validate() instead");
    Config config = skylarkParser.loadConfig(configContents, options);
    Console console = generalOptions.console();
    console.progress("Validating configuration");

    validateConfig(options, config);

    config.getActiveWorkflow().run(baseWorkdir, sourceRef);
  }

  public void validate(Options options, ConfigFile configContent, String workflowName)
      throws RepoException, ValidationException, IOException {
    options.get(WorkflowOptions.class).setWorkflowName(workflowName);
    Config config = skylarkParser.loadConfig(configContent, options);
    validateConfig(options, config);
  }

  private void validateConfig(Options options, Config config) throws ValidationException {
    Console console = options.get(GeneralOptions.class).console();
    List<String> validationMessages = validateConfig(config);
    if (!validationMessages.isEmpty()) {
      console.error("Configuration is invalid:");
      for (String validationMessage : validationMessages) {
        console.error(validationMessage);
      }
      throw new ConfigValidationException(
          "Error validating configuration: Configuration is invalid.");
    }
  }

  private void runConfigValidation(Config config, Console console) throws ConfigValidationException {
    List<String> validationMessages = validateConfig(config);
    if (!validationMessages.isEmpty()) {
      console.error("Configuration is invalid:");
      for (String validationMessage : validationMessages) {
        console.error(validationMessage);
      }
      throw new ConfigValidationException(
          "Error validating configuration: Configuration is invalid.");
    }
  }

  /**
   * Returns a list of validation error messages, if any, for the given configuration.
   */
  protected List<String> validateConfig(Config config) {
    // TODO(danielromero): Move here SkylarkParser validations once Config has all the workflows.
    // checkCondition(!workflows.isEmpty(), ...)
    return ImmutableList.of();
  }
}
