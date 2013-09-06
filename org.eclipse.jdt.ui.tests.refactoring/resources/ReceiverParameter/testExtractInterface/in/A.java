package p;
@Target({ ElementType.TYPE_USE, ElementType.METHOD })
@interface NonNull {

}
class A{	
	public void foo1(@NonNull  A this){}
	public void foo2(@NonNull  A this, String s){}
}