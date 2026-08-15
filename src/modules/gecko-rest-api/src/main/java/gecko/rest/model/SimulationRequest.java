package gecko.rest.model;

import jakarta.validation.constraints.Positive;
import java.util.Map;

/**
 * Request DTO for circuit simulation submissions.
 *
 * <p>The circuit can be referenced in one of three ways (exactly one is required,
 * validated by the service):
 * <ul>
 *   <li>{@code circuitId} — ID of a previously uploaded circuit (POST /api/v1/circuits/parse)</li>
 *   <li>{@code base64Circuit} — base64-encoded .ipes content</li>
 *   <li>{@code circuitFile} — server-local .ipes file path</li>
 * </ul>
 *
 * <p>{@code simulationTime} and {@code timeStep} are required for {@code circuitFile},
 * and optional otherwise (they default to the values stored in the circuit).
 */
public class SimulationRequest {

    private String circuitId;

    private String base64Circuit;

    private String circuitFile;

    @Positive(message = "Simulation time must be positive")
    private Double simulationTime;

    @Positive(message = "Time step must be positive")
    private Double timeStep;

    private Map<String, Double> parameters;

    private String solverType;  // Optional, defaults to backward-euler

    public SimulationRequest() {
    }

    public SimulationRequest(String circuitFile, Double simulationTime, Double timeStep) {
        this.circuitFile = circuitFile;
        this.simulationTime = simulationTime;
        this.timeStep = timeStep;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }

    public String getBase64Circuit() {
        return base64Circuit;
    }

    public void setBase64Circuit(String base64Circuit) {
        this.base64Circuit = base64Circuit;
    }

    public String getCircuitFile() {
        return circuitFile;
    }

    public void setCircuitFile(String circuitFile) {
        this.circuitFile = circuitFile;
    }

    public Double getSimulationTime() {
        return simulationTime;
    }

    public void setSimulationTime(Double simulationTime) {
        this.simulationTime = simulationTime;
    }

    public Double getTimeStep() {
        return timeStep;
    }

    public void setTimeStep(Double timeStep) {
        this.timeStep = timeStep;
    }

    public Map<String, Double> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Double> parameters) {
        this.parameters = parameters;
    }

    public String getSolverType() {
        return solverType;
    }

    public void setSolverType(String solverType) {
        this.solverType = solverType;
    }

    @Override
    public String toString() {
        return "SimulationRequest{" +
                "circuitId='" + circuitId + '\'' +
                ", base64Circuit=" + (base64Circuit != null ? "<present>" : "null") +
                ", circuitFile='" + circuitFile + '\'' +
                ", simulationTime=" + simulationTime +
                ", timeStep=" + timeStep +
                ", solverType='" + solverType + '\'' +
                ", parameters=" + parameters +
                '}';
    }
}
