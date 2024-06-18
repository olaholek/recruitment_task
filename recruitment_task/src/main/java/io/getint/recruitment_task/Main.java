package io.getint.recruitment_task;

public class Main {

    private static final String JIRA_URL = "myurl";
    private static final String USERNAME = "username";
    private static final String API_TOKEN = "token";


    public static void main(String[] args) {
        JiraSynchronizer synchronizer = new JiraSynchronizer(JIRA_URL, USERNAME, API_TOKEN);
        try {
            synchronizer.moveTasksToOtherProject("SCRUM", "SCRUM2");
            System.out.println("Tasks moved successfully!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
