package org.example.Report;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a single migration record for reports.
 */
public class MigrationRecord {
    @JsonProperty
    private String version;

    @JsonProperty
    private String description;

    @JsonProperty
    private String type;

    @JsonProperty
    private String script;

    @JsonProperty
    private int executionTime;

    @JsonProperty
    private boolean success;

    @JsonProperty
    private String installedBy;

    @JsonProperty
    private String installedOn;

    public MigrationRecord(String version, String description, String type, String script, int executionTime, boolean success, String installedBy, String installedOn) {
        this.version = version;
        this.description = description;
        this.type = type;
        this.script = script;
        this.executionTime = executionTime;
        this.success = success;
        this.installedBy = installedBy;
        this.installedOn = installedOn;
    }

    // Default constructor required for JSON deserialization
    public MigrationRecord() {
    }

    // Getters and Setters for each field (optional with @JsonProperty)
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getScript() {
        return script;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public int getExecutionTime() {
        return executionTime;
    }

    public void setExecutionTime(int executionTime) {
        this.executionTime = executionTime;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getInstalledBy() {
        return installedBy;
    }

    public void setInstalledBy(String installedBy) {
        this.installedBy = installedBy;
    }

    public String getInstalledOn() {
        return installedOn;
    }

    public void setInstalledOn(String installedOn) {
        this.installedOn = installedOn;
    }
}
