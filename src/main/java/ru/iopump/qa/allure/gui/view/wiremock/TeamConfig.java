package ru.iopump.qa.allure.gui.view.wiremock;

import java.util.List;

public class TeamConfig {
    private String name;
    private String image;
    private List<String> ports;     // Например: ["8082:8080", "8085:8081"]
    private String command;         // Например: "tail -f /dev/null"
    private List<String> volumes;   // Например: ["/host:/container"]
    private String network;         // Например: "bridge"
    private int port;
    private boolean available;

    // --- Геттеры и сеттеры ---

    public String getTeam() { return name; }
    public void setTeam(String name) { this.name = name; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public List<String> getPorts() { return ports; }
    public void setPorts(List<String> ports) { this.ports = ports; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }

    public List<String> getVolumes() { return volumes; }
    public void setVolumes(List<String> volumes) { this.volumes = volumes; }

    public String getNetwork() { return network; }
    public void setNetwork(String network) { this.network = network; }

}
