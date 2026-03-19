package org.hyw.tools.generator;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class GenerationResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private List<String> generatedFiles = new ArrayList<>();
    private long duration;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<String> getGeneratedFiles() { return generatedFiles; }
    public void setGeneratedFiles(List<String> generatedFiles) { this.generatedFiles = generatedFiles; }
    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private GenerationResult result = new GenerationResult();
        public Builder success(boolean success) { result.setSuccess(success); return this; }
        public Builder message(String message) { result.setMessage(message); return this; }
        public Builder generatedFiles(List<String> files) { result.setGeneratedFiles(files); return this; }
        public Builder duration(long duration) { result.setDuration(duration); return this; }
        public GenerationResult build() { return result; }
    }

    public static GenerationResult success(String message, List<String> files, long duration) {
        return builder().success(true).message(message).generatedFiles(files).duration(duration).build();
    }

    public static GenerationResult error(String message, long duration) {
        return builder().success(false).message(message).duration(duration).build();
    }
}
