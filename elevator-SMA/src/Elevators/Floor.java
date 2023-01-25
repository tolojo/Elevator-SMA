package Elevators;

public class Floor {
    private int floor, frequencyCalled;
    public Floor(int floor, int frequencyCalled){
        this.floor = floor;
        this.frequencyCalled = frequencyCalled;
    }
    
    public int getFloor(){
        return floor;
    }
    
    public int getFrequencyCalled(){
        return frequencyCalled;
    }
    
    public void setFloor(int floor){
        this.floor = floor;
    }

    public void setFrequencyCalled(int frequencyCalled) {
        this.frequencyCalled = frequencyCalled;
    }
}
