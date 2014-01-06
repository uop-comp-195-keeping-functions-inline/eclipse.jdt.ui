// 5, 28 -> 5, 30  replaceAll = true, removeDeclaration = true
package p;

class Test { 	
}

enum E {
	E_C1(C::m);  // [1] 
	E(FI fi) {
	}
}

class C {	
	static int m(int x) {
		return x--;
	}
}

@FunctionalInterface
interface FI {
	int foo(int x);
}