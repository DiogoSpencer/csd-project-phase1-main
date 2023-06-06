package app;

import java.util.ArrayList;
import java.util.List;

public class DiscretePoll {

    //TODO will owner be identified by string?
    private List<Integer> possibleValues;
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

    

    public DiscretePoll(String owner, String description, List<Integer> possibleValues, int expectedNParticipants) {
        this.possibleValues = possibleValues;
        this.owner = owner;
        this.isOpen = true;
        this.description = description;
        this.expectedNVoters = expectedNParticipants;
    }

    public DiscretePoll(String owner, String description, List<Integer> possibleValues, int expectedNParticipants, List<String> authorizedUsers) {
        this.possibleValues = possibleValues;
        this.owner = owner;
        this.isOpen = false;
        this.description = description;
        this.expectedNVoters = expectedNParticipants;
    }

    public void vote(String user, int value) {
        if(possibleValues.contains(value) && !voters.contains(user) && voteValues.size() < expectedNVoters) {
            voters.add(user);
        } else {
            //todo
        }

    }

    public double drawResult() {

        

        return result;
    }



    
}
