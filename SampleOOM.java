
import java.util.LinkedList;
import java.util.List;

public class SampleOOM {
	public static void main(String[] args) {
		System.out.println("Lets goo..");
    List<byte[]> list = new LinkedList<byte[]>();
    int index = 1;
    
    while (true) {
        byte[] b = new byte[10 * 512]; // 0.5 MB byte object
        list.add(b);
        //Runtime rt = Runtime.getRuntime();
        System.out.printf("Index: [%3s]%n", index++);
    }
	}
	

}
