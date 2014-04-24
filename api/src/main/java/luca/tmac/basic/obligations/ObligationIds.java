package luca.tmac.basic.obligations;

public class ObligationIds {
	public static final String SYSTEM_PREFIX = "system:";
	public static final String USER_PREFIX = "user:";
	public static final String ASSIGN_TASK_OBLIGATION_ID = SYSTEM_PREFIX + "assign:task:obligation";
	public static final String SHOW_DENY_REASON_OBLIGATION_ID = SYSTEM_PREFIX + "show:deny:reason";
	public static final String DECREASE_BUDGET_ID = SYSTEM_PREFIX + "decrease:budget";
	public static final String SHOW_PERMIT_MESSAGE_OBLIGATION_ID = SYSTEM_PREFIX + "show:permit:message";
	public static final String JUSTIFY_ACCESS_OBLIGATION_ID = USER_PREFIX + "justify:access";
	public static final String DURATION_OBLIGATION_ATTRIBUTE = "duration";
	public static final String ACTION_NAME_OBLIGATION_ATTRIBUTE = "action_name";
	public static final String DEADLINE_OBLIGATION_ATTRIBUTE = "deadline";
	
	public static final String START_TIME_OBLIGATION_ATTRIBUTE = "start_time";
	
	public static final String EMAIL_OBLIGATION_NAME_XML = "user:send:email";
	public static final String REST_OBLIGATION_NAME_XML = "user:rest";


}
