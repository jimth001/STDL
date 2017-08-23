package dm.tools;

public class ErrorHandle {
	public static void OutputErrorInformation(String inf) {
		System.err.println(inf);
	}
	public static void OutputErrorInformation(StackTraceElement[] stes) {
		System.err.println("以下信息是由ErrorHandler输出的");
	    for(StackTraceElement ste:stes) {
            System.err.println(ste.toString());
        }
    }
    public static void OutputErrorInformation(Exception e) {
		OutputErrorInformation(e.getClass().getName());
		OutputErrorInformation(e.getMessage());
		OutputErrorInformation(e.getStackTrace());
	}
}
