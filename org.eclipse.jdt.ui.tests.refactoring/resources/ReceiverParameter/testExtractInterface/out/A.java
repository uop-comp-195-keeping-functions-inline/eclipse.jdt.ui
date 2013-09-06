package p;
@Target({ ElementType.TYPE_USE, ElementType.METHOD })
@interface NonNull {

}
class A implements I{	
	/* (non-Javadoc)
	 * @see p.I#foo1()
	 */
	public void foo1(@NonNull  A this){}
	/* (non-Javadoc)
	 * @see p.I#foo2(java.lang.String)
	 */
	public void foo2(@NonNull  A this, String s){}
}