package ru.iopump.qa.allure.model;

public class TestParameters {
    private String testType;
    private boolean includeAuth;
    private int numThreads = 1;
    private int rampTime = 1;
    private int loops = 1;
    private int duration = 0;

    public TestParameters() {
    }

    public String getTestType() {
        return this.testType;
    }

    public void setTestType(String testType) {
        this.testType = testType;
    }

    public boolean isIncludeAuth() {
        return this.includeAuth;
    }

    public void setIncludeAuth(boolean includeAuth) {
        this.includeAuth = includeAuth;
    }

    public int getNumThreads() {
        return this.numThreads;
    }

    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    public int getRampTime() {
        return this.rampTime;
    }

    public void setRampTime(int rampTime) {
        this.rampTime = rampTime;
    }

    public int getLoops() {
        return this.loops;
    }

    public void setLoops(int loops) {
        this.loops = loops;
    }

    public int getDuration() {
        return this.duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
