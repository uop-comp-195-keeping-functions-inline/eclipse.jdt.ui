package p1;

public class B {
	public void mB1() {}
	
	public void mB2() {}

	public void mA1Moved(@NonNull
	B this) {
		mB1();
		System.out.println(this + "j");
	}
}