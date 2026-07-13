package fr.shoqapik.btemobs.option_dialogs;

public class QuestAnswer {

    private String formattedText;
    private String action;

    public QuestAnswer(String formattedText, String action) {
        this.formattedText = formattedText;
        this.action = action;
    }

    public String getFormattedAwnser() {
        return formattedText;
    }

    public String getAction() {
        return action;
    }
}
