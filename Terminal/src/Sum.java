class Sum{
	public int x;
	public int y;
	public int add(int a,int b) {
		x=a+b;
		y=x+b;
		return 0;
		
	}
	
	public static void main(String[] argv) {
		Sum obj1=new Sum();
		Sum obj=new Sum();
		int a=2;
		obj1.add(a, a+1);
		System.out.print(obj1.x);
		
	}
}