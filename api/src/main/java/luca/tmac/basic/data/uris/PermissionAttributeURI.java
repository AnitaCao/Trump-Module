package luca.tmac.basic.data.uris;

public class PermissionAttributeURI {
	public static String PERMISSION_CATEGORY_URI = "luca:tmac:permission-category:permission";
	public static String PERMISSION_ID_URI = PERMISSION_CATEGORY_URI
			+ ":" + "permission_id";
	public static String RESOURCE_TYPE_URI = PERMISSION_CATEGORY_URI
			+ ":"+ "resource_type";
	public static String RESOURCE_ID_URI = PERMISSION_CATEGORY_URI
			+ ":" + "resource_id";
	public static String ACTION_URI = PERMISSION_CATEGORY_URI + ":" + "action";
	
	public static String PERMISSION_ATTRIBUTES_URI = PERMISSION_CATEGORY_URI + ":" + "attributes";
	
	public static final String WANTED_PATIENT_URI = PERMISSION_CATEGORY_URI + ":" + "wanted_patient";
	
}
