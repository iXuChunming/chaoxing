package pers.cz.chaoxing.common.quiz;

public class QuizConfig {
    private String memberinfo;
    private String resourceId;
    private boolean answered;
    private String errorReportUrl;
    private OptionInfo[] options;
    private String description;
    private String validationUrl;
    private long startTime;
    private long endTime;
    private String questionType;
    /*
    extra data
     */
    private String answerCheck;
    private String score;

    public String getMemberinfo() {
        return memberinfo;
    }

    public void setMemberinfo(String memberinfo) {
        this.memberinfo = memberinfo;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public boolean isAnswered() {
        return answered;
    }

    public void setAnswered(boolean answered) {
        this.answered = answered;
    }

    public String getErrorReportUrl() {
        return errorReportUrl;
    }

    public void setErrorReportUrl(String errorReportUrl) {
        this.errorReportUrl = errorReportUrl;
    }

    public OptionInfo[] getOptions() {
        return options;
    }

    public void setOptions(OptionInfo[] options) {
        this.options = options;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getValidationUrl() {
        return validationUrl;
    }

    public void setValidationUrl(String validationUrl) {
        this.validationUrl = validationUrl;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public String getQuestionType() {
        return questionType;
    }

    public void setQuestionType(String questionType) {
        this.questionType = questionType;
    }

    public String getAnswerCheck() {
        return answerCheck;
    }

    public void setAnswerCheck(String answerCheck) {
        this.answerCheck = answerCheck;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

}
