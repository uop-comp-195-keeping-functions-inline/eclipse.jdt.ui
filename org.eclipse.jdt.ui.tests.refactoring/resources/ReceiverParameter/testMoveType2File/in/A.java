package p;
@Target({ ElementType.TYPE_USE, ElementType.METHOD })
@interface NonNull {

}
class A{

class Inner{
	Inner(@NonNull A A.this){}
	public void foo1(@NonNull Inner this){
		System.out.println("Hello");
	}
}
}