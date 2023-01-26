package Elevators;

public class Request {

    private int initialFloor, destinationFloor, id;

    private int count = 0;

    public  Request (int initialFloor, int destinationFloor){
        this.initialFloor = initialFloor;
        this.destinationFloor = destinationFloor;
        this.id = ++count;
    }

    public int getInitialFloor() {
        return initialFloor;
    }

    public void setInitialFloor(int initialFloor) {
        this.initialFloor = initialFloor;
    }

    public int getDestinationFloor() {
        return destinationFloor;
    }

    public void setDestinationFloor(int destinationFloor) {
        this.destinationFloor = destinationFloor;
    }

    public int getId (){
        return id;
    }
}
