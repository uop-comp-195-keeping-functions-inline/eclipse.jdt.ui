package p;
class Inner{
	Inner(){}
	public void foo1(@NonNull Inner this){
		System.out.println("Hello");
	}
}