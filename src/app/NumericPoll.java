package app;

import java.util.ArrayList;
import java.util.List;

public class NumericPoll {

    //TODO will owner be identified by string?
    private int end;
    private int start;
    private String owner;
    private double result;
    private boolean isOpen;
    private String description;
    private List<String> voters;
    private List<Integer> voteValues;
    private int expectedNVoters;
    // private List<Integer> rangeOfValues;
    private List<String> authorizedUsers;
    //TODO will owner be identified by string?

    

    public NumericPoll(String owner, String description, int start, int end, int expectedNParticipants) {
        this.end = end;
        this.start = start;
        this.owner = owner;
        this.isOpen = true;
        this.description = description;
        this.expectedNVoters = expectedNParticipants;
    }

    public NumericPoll(String owner, String description, int start, int end, int expectedNParticipants, List<String> authorizedUsers) {
        this.end = end;
        this.start = start;
        this.owner = owner;
        this.isOpen = false;
        this.description = description;
        this.expectedNVoters = expectedNParticipants;
    }

    public void vote(String user, int value) {
        //TODO maybe separate conditions if we're planning to log
        if(value >= start && value <= end && !voters.contains(user) && voteValues.size() < expectedNVoters) {
            voters.add(user);
        } else {
            //todo
        }

    }

    public double drawResult() {

        

        return result;
    }



    
}
